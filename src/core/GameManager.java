package core;

import cards.Card;
import player.Player;
import player.PlayerType;
import patterns.observer.GameObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GameManager {
    private static GameManager instance;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    private final List<Player> activePlayers;
    private final Deck gameDeck;
    private final List<GameObserver> observers;
    private int currentTurnIndex;
    private int actionsRemaining;
    private boolean isGameOver;
    private Player winner;
    private TargetInfo currentTargetInfo;
    private int rentMultiplier = 1;

    // Tracks whether normal play is active or a counter window is open.
    public enum GameState {
        NORMAL_TURN,
        WAITING_FOR_COUNTER_ACTION
    }

    private GameState currentState = GameState.NORMAL_TURN;
    private Runnable pendingAction;
    private Consumer<Player> pendingGroupAction;
    private Player pendingVictim;
    private Player pendingAttacker;
    private Player pendingActionTarget;
    private boolean pendingActionNegated;
    private List<Player> pendingVictims;
    private int pendingVictimIndex;

    // Carries the name and control type used when creating a player.
    public static final class PlayerSetup {
        private final String name;
        private final PlayerType type;

        // Normalizes missing names and player types.
        public PlayerSetup(String name, PlayerType type) {
            this.name = name == null || name.trim().isEmpty() ? "Player" : name.trim();
            this.type = type == null ? PlayerType.HUMAN : type;
        }

        // Returns the configured display name.
        public String getName() {
            return name;
        }

        // Returns whether this seat is human or AI.
        public PlayerType getType() {
            return type;
        }
    }

    // Creates the singleton game coordinator.
    private GameManager() {
        this.activePlayers = new ArrayList<>();
        this.gameDeck = new Deck();
        this.observers = new ArrayList<>();
        this.currentTurnIndex = 0;
        this.isGameOver = false;
    }

    // Returns the singleton game coordinator.
    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // Initializes a local all-human game.
    public void initializeGame(List<String> playerNames) {
        List<PlayerSetup> setups = new ArrayList<>();
        if (playerNames != null) {
            for (String name : playerNames) {
                setups.add(new PlayerSetup(name, PlayerType.HUMAN));
            }
        }
        initializeGameWithSetups(setups, null);
    }

    // Initializes a configured game with a deterministic deck seed.
    public void initializeConfiguredGame(List<PlayerSetup> playerSetups, long deckSeed) {
        initializeGameWithSetups(playerSetups, deckSeed);
    }

    // Initializes a configured game with a random deck order.
    public void initializeConfiguredGame(List<PlayerSetup> playerSetups) {
        initializeGameWithSetups(playerSetups, null);
    }

    // Resets all state, creates players, deals hands, and starts the first turn.
    private void initializeGameWithSetups(List<PlayerSetup> playerSetups, Long deckSeed) {
        if (playerSetups == null || playerSetups.size() < MIN_PLAYERS || playerSetups.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Monopoly Deal supports 2 to 5 players.");
        }

        currentTurnIndex = 0;
        actionsRemaining = 0;
        isGameOver = false;
        winner = null;
        currentTargetInfo = null;
        rentMultiplier = 1;
        resetState();

        // Network games pass a shared seed so every client sees the same deck.
        if (deckSeed == null) {
            gameDeck.initializeDeck(CardFactory.createInitialDeck());
        } else {
            gameDeck.initializeDeck(CardFactory.createInitialDeck(), deckSeed);
        }

        activePlayers.clear();
        for (int i = 0; i < playerSetups.size(); i++) {
            PlayerSetup setup = playerSetups.get(i);
            Player newPlayer = new Player(String.valueOf(i), setup.getName());
            newPlayer.setPlayerType(setup.getType());
            newPlayer.getHand().addCards(gameDeck.drawCards(5));
            activePlayers.add(newPlayer);
        }

        notifyEvent("Game initialized with " + activePlayers.size() + " players.");
        startNewTurn();
    }

    // Starts a turn by resetting actions and drawing the official card count.
    public void startNewTurn() {
        if (isGameOver) {
            return;
        }

        Player currentPlayer = getCurrentPlayer();
        actionsRemaining = 3;
        rentMultiplier = 1;

        notifyTurnChange(currentPlayer.getPlayerName());

        List<Card> drawn = gameDeck.drawCards(currentPlayer.getHand().getSize() == 0 ? 5 : 2);
        currentPlayer.getHand().addCards(drawn);
        notifyEvent(currentPlayer.getPlayerName() + " drew " + drawn.size() + " card(s).");
        checkDrawStalemate();
    }

    // Legacy entry point for playing a card without target data.
    public void handlePlayCard(int cardIndex) {
        executePlayerAction(cardIndex, null);
    }

    // Plays one card from the current player's hand.
    public void executePlayerAction(int cardIndex, TargetInfo target) {
        if (!canUseAction()) return;

        Player player = getCurrentPlayer();
        Card selectedCard = player.getHand().getCard(cardIndex);
        if (selectedCard == null) {
            return;
        }
        if (selectedCard instanceof cards.JustSayNoCard) {
            notifyEvent("Just Say No can only be used when responding to an attack.");
            return;
        }
        if (selectedCard instanceof cards.DoubleTheRentCard) {
            notifyEvent("Double The Rent must be played together with a rent card.");
            return;
        }

        // Card classes read currentTargetInfo while executing polymorphic effects.
        currentTargetInfo = target;
        try {
            player.playCard(selectedCard);
        } catch (IllegalStateException ex) {
            notifyEvent("Cannot play " + selectedCard.getCardName() + ": " + ex.getMessage());
            return;
        } finally {
            currentTargetInfo = null;
        }

        player.getHand().removeCard(cardIndex);
        if (shouldDiscardAfterPlay(selectedCard)) {
            gameDeck.receiveDiscard(selectedCard);
        }
        actionsRemaining--;
        notifyEvent(player.getPlayerName() + " played " + selectedCard.getCardName()
                + " (actions left: " + actionsRemaining + ")");
        checkWinCondition();
    }

    // Checks common requirements for spending one normal action.
    private boolean canUseAction() {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return false;
        }
        if (currentState != GameState.NORMAL_TURN) {
            notifyEvent("Resolve the pending counter action first.");
            return false;
        }
        if (actionsRemaining <= 0) {
            notifyEvent("Not enough actions. End your turn.");
            return false;
        }
        return true;
    }

    // Reports whether a played card should go to the discard pile.
    private boolean shouldDiscardAfterPlay(Card card) {
        return !(card instanceof cards.PropertyCard
                || card instanceof cards.MoneyCard
                || card instanceof cards.HouseCard
                || card instanceof cards.HotelCard);
    }

    // Plays Double The Rent together with a rent card as a two-action combo.
    public void executeDoubleRentAction(int doubleCardIndex, int rentCardIndex, TargetInfo target) {
        if (!canUseDoubleRentAction()) return;

        Player player = getCurrentPlayer();
        Card doubleCard = player.getHand().getCard(doubleCardIndex);
        Card rentCard = player.getHand().getCard(rentCardIndex);
        if (!(doubleCard instanceof cards.DoubleTheRentCard) || !isRentCard(rentCard)) {
            notifyEvent("Double The Rent must be paired with a rent card.");
            return;
        }

        if (!playRentWithActiveMultiplier(player, rentCard, target)) return;
        discardDoubleRentCombo(player, doubleCardIndex, rentCardIndex);
        actionsRemaining -= 2;
        notifyEvent(player.getPlayerName() + " played Double The Rent with "
                + rentCard.getCardName() + " (actions left: " + actionsRemaining + ")");
        checkWinCondition();
    }

    // Checks common requirements for the two-action Double The Rent combo.
    private boolean canUseDoubleRentAction() {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return false;
        }
        if (currentState != GameState.NORMAL_TURN) {
            notifyEvent("Resolve the pending counter action first.");
            return false;
        }
        if (actionsRemaining < 2) {
            notifyEvent("Not enough actions: Double The Rent plus Rent costs 2 actions.");
            return false;
        }
        return true;
    }

    // Plays the paired rent card while Double The Rent is active.
    private boolean playRentWithActiveMultiplier(Player player, Card rentCard, TargetInfo target) {
        currentTargetInfo = target;
        try {
            activateDoubleRent();
            player.playCard(rentCard);
            return true;
        } catch (IllegalStateException ex) {
            rentMultiplier = 1;
            notifyEvent("Cannot play rent combo: " + ex.getMessage());
            return false;
        } finally {
            currentTargetInfo = null;
        }
    }

    // Removes and discards both cards from a Double The Rent combo.
    private void discardDoubleRentCombo(Player player, int doubleCardIndex, int rentCardIndex) {
        int first = Math.max(doubleCardIndex, rentCardIndex);
        int second = Math.min(doubleCardIndex, rentCardIndex);
        Card removedFirst = player.getHand().removeCard(first);
        Card removedSecond = player.getHand().removeCard(second);
        gameDeck.receiveDiscard(removedFirst);
        gameDeck.receiveDiscard(removedSecond);
    }

    // Reports whether a card is a legal Double The Rent partner.
    private boolean isRentCard(Card card) {
        return card instanceof cards.RentCard || card instanceof cards.WildRentCard;
    }

    // Returns one excess hand card to the bottom of the draw pile.
    public void discardCard(int cardIndex) {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return;
        }
        if (currentState != GameState.NORMAL_TURN) {
            notifyEvent("Resolve the pending counter action first.");
            return;
        }

        Player player = getCurrentPlayer();
        if (!player.getHand().requiresDiscard()) {
            notifyEvent("Discard only when your hand has more than 7 cards.");
            return;
        }
        Card selectedCard = player.getHand().removeCard(cardIndex);
        if (selectedCard != null) {
            gameDeck.returnToBottomOfDrawPile(selectedCard);
            notifyEvent(player.getPlayerName() + " discarded " + selectedCard.getCardName());
        }
    }

    // Moves a non-property card from hand into the current player's bank.
    public void depositCardToBank(int cardIndex) {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return;
        }
        if (currentState != GameState.NORMAL_TURN) {
            notifyEvent("Resolve the pending counter action first.");
            return;
        }
        if (actionsRemaining <= 0) {
            notifyEvent("Not enough actions. End your turn.");
            return;
        }

        Player player = getCurrentPlayer();
        Card selectedCard = player.getHand().removeCard(cardIndex);
        if (selectedCard == null) {
            return;
        }
        if (selectedCard instanceof cards.PropertyCard) {
            player.getHand().addCards(java.util.Collections.singletonList(selectedCard));
            notifyEvent("Property cards cannot be deposited to bank.");
            return;
        }

        player.getBankArea().deposit(selectedCard);
        actionsRemaining--;
        notifyEvent(player.getPlayerName() + " banked " + selectedCard.getCardName()
                + " (actions left: " + actionsRemaining + ")");
        checkWinCondition();
    }

    // Ends the current turn after enforcing hand limit and win checks.
    public void endTurn() {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return;
        }
        if (currentState != GameState.NORMAL_TURN) {
            notifyEvent("Resolve the pending counter action first.");
            return;
        }

        Player player = getCurrentPlayer();
        if (player.getHand().requiresDiscard()) {
            notifyEvent(player.getPlayerName() + " must discard down to 7 cards.");
            return;
        }

        checkWinCondition();
        if (isGameOver) {
            return;
        }

        checkDrawStalemate();
        if (isGameOver) {
            return;
        }

        resetState();
        currentTargetInfo = null;

        currentTurnIndex = (currentTurnIndex + 1) % activePlayers.size();
        startNewTurn();
    }

    // Checks whether any player has completed three distinct color sets.
    private void checkWinCondition() {
        for (Player player : activePlayers) {
            int distinctColors = player.getPropertyArea().getCompletedColors().size();
            if (distinctColors >= 3) {
                isGameOver = true;
                winner = player;
                notifyEvent("Congratulations " + player.getPlayerName()
                        + " collected 3 complete property sets of different colors and wins!");
                return;
            }
        }
    }

    // Logs when no deck cards are available to draw.
    private void checkDrawStalemate() {
        if (!isGameOver && gameDeck.getDrawPileSize() == 0 && gameDeck.getDiscardPileSize() == 0) {
            notifyEvent("Draw pile is empty. Continue playing with cards in hand.");
        }
    }

    // Doubles the next rent amount.
    public void activateDoubleRent() {
        this.rentMultiplier *= 2;
        notifyEvent("Double rent is active for the next rent card.");
    }

    // Returns and clears the current rent multiplier.
    public int getAndResetRentMultiplier() {
        int current = this.rentMultiplier;
        this.rentMultiplier = 1;
        return current;
    }

    // Returns the player whose turn is active.
    public Player getCurrentPlayer() {
        return activePlayers.get(currentTurnIndex);
    }

    // Removes a player who left an active network game and keeps turn order valid.
    public boolean removePlayerAt(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= activePlayers.size()) {
            return false;
        }

        Player removed = activePlayers.remove(playerIndex);
        notifyEvent(removed.getPlayerName() + " left the match.");
        if (activePlayers.isEmpty()) {
            isGameOver = true;
            winner = null;
            return true;
        }
        if (activePlayers.size() == 1) {
            isGameOver = true;
            winner = activePlayers.get(0);
            notifyEvent(winner.getPlayerName() + " wins because all other players left.");
            return true;
        }

        if (playerIndex < currentTurnIndex) {
            currentTurnIndex--;
        } else if (playerIndex == currentTurnIndex) {
            resetState();
            currentTargetInfo = null;
            actionsRemaining = 0;
            if (currentTurnIndex >= activePlayers.size()) {
                currentTurnIndex = 0;
            }
            startNewTurn();
        } else if (currentTurnIndex >= activePlayers.size()) {
            currentTurnIndex = 0;
        }
        return true;
    }

    // Resolves an explicit target or falls back to the first opponent.
    public Player resolveTargetOrFirstOpponent(Player initiator) {
        if (currentTargetInfo != null && currentTargetInfo.getTargetPlayer() != null) {
            return currentTargetInfo.getTargetPlayer();
        }
        List<Player> opponents = getOpponents(initiator);
        return opponents.isEmpty() ? null : opponents.get(0);
    }

    // Returns an immutable view of active players.
    public List<Player> getActivePlayers() {
        return Collections.unmodifiableList(activePlayers);
    }

    // Returns the active deck instance.
    public Deck getGameDeck() {
        return gameDeck;
    }

    // Reports whether a winner has been found.
    public boolean isGameOver() {
        return isGameOver;
    }

    // Returns the winner once the game is over.
    public Player getWinner() {
        return winner;
    }

    // Returns remaining actions for the current turn.
    public int getActionsRemaining() {
        return actionsRemaining;
    }

    // Returns target metadata for the card currently resolving.
    public TargetInfo getCurrentTargetInfo() {
        return currentTargetInfo;
    }

    // Subscribes a UI or logger to game events.
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    // Removes an event subscriber.
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    // Sends a game log message to all observers.
    private void notifyEvent(String message) {
        for (GameObserver observer : observers) {
            observer.onGameEvent(message);
        }
    }

    // Exposes logging for collaborators such as the AI executor.
    public void logEvent(String message) {
        notifyEvent(message);
    }

    // Notifies observers when a new turn begins.
    private void notifyTurnChange(String playerName) {
        for (GameObserver observer : observers) {
            observer.onTurnChanged(playerName);
        }
    }

    // Starts a targeted attack from the current player.
    public void initiateAttack(Player victim, Runnable action) {
        initiateAttack(getCurrentPlayer(), victim, action);
    }

    // Opens a Just Say No window before resolving one targeted action.
    public void initiateAttack(Player attacker, Player victim, Runnable action) {
        this.currentState = GameState.WAITING_FOR_COUNTER_ACTION;
        this.pendingAction = action;
        this.pendingGroupAction = null;
        this.pendingAttacker = attacker;
        this.pendingActionTarget = victim;
        this.pendingActionNegated = false;
        this.pendingVictim = victim;
        this.pendingVictims = null;
        this.pendingVictimIndex = 0;
        notifyEvent("[INTERRUPT_REQUEST] " + victim.getPlayerName() + " may use Just Say No.");
    }

    // Starts a group attack from the current player.
    public void initiateGroupAttack(List<Player> victims, Consumer<Player> action) {
        initiateGroupAttack(getCurrentPlayer(), victims, action);
    }

    // Opens sequential Just Say No windows for a multi-victim action.
    public void initiateGroupAttack(Player attacker, List<Player> victims, Consumer<Player> action) {
        if (victims == null || victims.isEmpty()) {
            return;
        }
        this.currentState = GameState.WAITING_FOR_COUNTER_ACTION;
        this.pendingAction = null;
        this.pendingGroupAction = action;
        this.pendingAttacker = attacker;
        this.pendingVictims = new ArrayList<>(victims);
        this.pendingVictimIndex = 0;
        this.pendingActionTarget = this.pendingVictims.get(0);
        this.pendingActionNegated = false;
        this.pendingVictim = this.pendingActionTarget;
        notifyEvent("[INTERRUPT_REQUEST] " + pendingVictim.getPlayerName()
                + " may use Just Say No.");
    }

    // Resolves the currently pending attack after counters are decided.
    public void resolvePendingAction() {
        if (currentState != GameState.WAITING_FOR_COUNTER_ACTION
                || (pendingAction == null && pendingGroupAction == null)) {
            return;
        }

        try {
            // A successful Just Say No cancels only the current pending victim/action.
            if (pendingActionNegated) {
                notifyEvent("Action cancelled"
                        + (pendingActionTarget == null ? "." : " for " + pendingActionTarget.getPlayerName() + "."));
                if (pendingGroupAction != null) {
                    advancePendingVictimOrReset();
                } else {
                    resetState();
                }
                return;
            }

            // Group attacks resolve one victim at a time.
            if (pendingGroupAction != null) {
                pendingGroupAction.accept(pendingActionTarget);
                notifyEvent("Action resolved for " + pendingActionTarget.getPlayerName() + ".");
                checkWinCondition();
                advancePendingVictimOrReset();
                return;
            }

            pendingAction.run();
            notifyEvent("Action resolved.");
            checkWinCondition();
            resetState();
        } catch (IllegalStateException ex) {
            notifyEvent("Action failed: " + ex.getMessage());
            resetState();
        }
    }

    // Moves a group attack to the next victim or restores normal turn state.
    private void advancePendingVictimOrReset() {
        if (pendingVictims != null && pendingVictimIndex < pendingVictims.size() - 1) {
            pendingVictimIndex++;
            pendingActionTarget = pendingVictims.get(pendingVictimIndex);
            pendingActionNegated = false;
            pendingVictim = pendingActionTarget;
            notifyEvent("[INTERRUPT_REQUEST] " + pendingVictim.getPlayerName()
                    + " may use Just Say No.");
            return;
        }

        resetState();
    }

    // Plays Just Say No from the pending victim's hand.
    public void counterAttackWithJustSayNo(int cardIndex) {
        if (currentState != GameState.WAITING_FOR_COUNTER_ACTION || pendingVictim == null) {
            return;
        }

        Card card = pendingVictim.getHand().getCard(cardIndex);
        if (card != null && card.getCardName().equals("Just Say No")) {
            pendingVictim.getHand().removeCard(cardIndex);
            gameDeck.receiveDiscard(card);
            notifyEvent(pendingVictim.getPlayerName() + " used Just Say No.");

            // Each counter flips whether the original action is currently cancelled.
            pendingActionNegated = !pendingActionNegated;
            Player nextResponder = getNextJustSayNoResponder(pendingVictim);
            if (nextResponder != null && hasJustSayNo(nextResponder)) {
                pendingVictim = nextResponder;
                notifyEvent("[INTERRUPT_REQUEST] " + nextResponder.getPlayerName()
                        + " may use Just Say No to counter!");
                return;
            }

            if (pendingActionNegated) {
                notifyEvent("The action is cancelled"
                        + (pendingActionTarget == null ? "." : " for " + pendingActionTarget.getPlayerName() + "."));
                if (pendingGroupAction != null) {
                    advancePendingVictimOrReset();
                } else {
                    resetState();
                    checkWinCondition();
                }
            } else {
                resolvePendingAction();
            }
        } else {
            notifyEvent("Invalid counter card.");
        }
    }

    // Alternates Just Say No response priority between attacker and target.
    private Player getNextJustSayNoResponder(Player currentResponder) {
        if (pendingAttacker == null || pendingActionTarget == null) {
            return null;
        }
        return currentResponder == pendingAttacker ? pendingActionTarget : pendingAttacker;
    }

    // Checks whether a player can continue the counter chain.
    private boolean hasJustSayNo(Player player) {
        return player.getHand().getCards().stream()
                .anyMatch(card -> card.getCardName().equals("Just Say No"));
    }

    // Returns the player currently allowed to answer an attack.
    public Player getPendingVictim() {
        return pendingVictim;
    }

    // Returns the current game state.
    public GameState getCurrentState() {
        return currentState;
    }

    // Clears all pending attack and counter state.
    private void resetState() {
        this.currentState = GameState.NORMAL_TURN;
        this.pendingAction = null;
        this.pendingGroupAction = null;
        this.pendingVictim = null;
        this.pendingAttacker = null;
        this.pendingActionTarget = null;
        this.pendingActionNegated = false;
        this.pendingVictims = null;
        this.pendingVictimIndex = 0;
    }

    // Resolves the attack target and opens a counter window.
    public void initiateTargetedAttack(Player initiator, Consumer<Player> attackAction) {
        Player victim = resolveTargetOrFirstOpponent(initiator);
        if (victim == null) {
            throw new IllegalStateException("no valid target for this action.");
        }
        initiateAttack(initiator, victim, () -> attackAction.accept(victim));
    }

    // Returns every active player except the given player.
    public List<Player> getOpponents(Player player) {
        List<Player> opponents = new ArrayList<>();
        for (Player candidate : activePlayers) {
            if (!candidate.equals(player)) {
                opponents.add(candidate);
            }
        }
        return opponents;
    }

    // Draws extra cards for a specific player.
    public void drawCardsForPlayer(Player player, int count) {
        List<Card> drawnCards = gameDeck.drawCards(count);
        player.getHand().addCards(drawnCards);
        notifyEvent(player.getPlayerName() + " drew " + drawnCards.size() + " extra card(s).");
        checkDrawStalemate();
    }
}
