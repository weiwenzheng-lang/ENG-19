package core;

import cards.Card;
import player.Player;
import patterns.observer.GameObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GameManager {
    private static GameManager instance;

    private final List<Player> activePlayers;
    private final Deck gameDeck;
    private final List<GameObserver> observers;
    private int currentTurnIndex;
    private int actionsRemaining;
    private boolean isGameOver;
    private TargetInfo currentTargetInfo;
    private int rentMultiplier = 1;

    public enum GameState {
        NORMAL_TURN,
        WAITING_FOR_COUNTER_ACTION
    }

    private GameState currentState = GameState.NORMAL_TURN;
    private Runnable pendingAction;
    private Consumer<Player> pendingGroupAction;
    private Player pendingVictim;
    private List<Player> pendingVictims;
    private int pendingVictimIndex;

    private GameManager() {
        this.activePlayers = new ArrayList<>();
        this.gameDeck = new Deck();
        this.observers = new ArrayList<>();
        this.currentTurnIndex = 0;
        this.isGameOver = false;
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void initializeGame(List<String> playerNames) {
        currentTurnIndex = 0;
        actionsRemaining = 0;
        isGameOver = false;
        currentTargetInfo = null;
        rentMultiplier = 1;
        resetState();

        gameDeck.initializeDeck(CardFactory.createInitialDeck());

        activePlayers.clear();
        for (int i = 0; i < playerNames.size(); i++) {
            Player newPlayer = new Player(String.valueOf(i), playerNames.get(i));
            newPlayer.getHand().addCards(gameDeck.drawCards(5));
            activePlayers.add(newPlayer);
        }

        notifyEvent("Game initialized with " + activePlayers.size() + " players.");
        startNewTurn();
    }

    public void startNewTurn() {
        if (isGameOver) {
            return;
        }

        Player currentPlayer = getCurrentPlayer();
        actionsRemaining = 3;
        rentMultiplier = 1;

        notifyTurnChange(currentPlayer.getPlayerName());

        List<Card> drawn = gameDeck.drawCards(2);
        currentPlayer.getHand().addCards(drawn);
        notifyEvent(currentPlayer.getPlayerName() + " drew " + drawn.size() + " card(s).");
        checkDrawStalemate();
    }

    public void handlePlayCard(int cardIndex) {
        executePlayerAction(cardIndex, null);
    }

    public void executePlayerAction(int cardIndex, TargetInfo target) {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return;
        }
        if (actionsRemaining <= 0) {
            notifyEvent("Not enough actions. End your turn.");
            return;
        }

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
        gameDeck.receiveDiscard(selectedCard);
        actionsRemaining--;
        notifyEvent(player.getPlayerName() + " played " + selectedCard.getCardName()
                + " (actions left: " + actionsRemaining + ")");
        checkWinCondition();
    }

    public void executeDoubleRentAction(int doubleCardIndex, int rentCardIndex, TargetInfo target) {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return;
        }
        if (actionsRemaining < 2) {
            notifyEvent("Not enough actions: Double The Rent plus Rent costs 2 actions.");
            return;
        }

        Player player = getCurrentPlayer();
        Card doubleCard = player.getHand().getCard(doubleCardIndex);
        Card rentCard = player.getHand().getCard(rentCardIndex);
        if (!(doubleCard instanceof cards.DoubleTheRentCard) || !(rentCard instanceof cards.RentCard)) {
            notifyEvent("Double The Rent must be paired with a rent card.");
            return;
        }

        currentTargetInfo = target;
        try {
            activateDoubleRent();
            player.playCard(rentCard);
        } catch (IllegalStateException ex) {
            rentMultiplier = 1;
            notifyEvent("Cannot play rent combo: " + ex.getMessage());
            return;
        } finally {
            currentTargetInfo = null;
        }

        int first = Math.max(doubleCardIndex, rentCardIndex);
        int second = Math.min(doubleCardIndex, rentCardIndex);
        Card removedFirst = player.getHand().removeCard(first);
        Card removedSecond = player.getHand().removeCard(second);
        gameDeck.receiveDiscard(removedFirst);
        gameDeck.receiveDiscard(removedSecond);
        actionsRemaining -= 2;
        notifyEvent(player.getPlayerName() + " played Double The Rent with "
                + rentCard.getCardName() + " (actions left: " + actionsRemaining + ")");
        checkWinCondition();
    }

    public void discardCard(int cardIndex) {
        if (isGameOver) {
            notifyEvent("Game is already over.");
            return;
        }

        Player player = getCurrentPlayer();
        Card selectedCard = player.getHand().removeCard(cardIndex);
        if (selectedCard != null) {
            gameDeck.receiveDiscard(selectedCard);
            notifyEvent(player.getPlayerName() + " discarded " + selectedCard.getCardName());
        }
    }

    public void depositCardToBank(int cardIndex) {
        if (isGameOver) {
            notifyEvent("Game is already over.");
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
        if (selectedCard instanceof cards.HouseCard || selectedCard instanceof cards.HotelCard) {
            player.getHand().addCards(Collections.singletonList(selectedCard));
            notifyEvent("House/Hotel cannot be banked from this menu; play it on a complete set.");
            return;
        }

        player.getBankArea().deposit(selectedCard);
        actionsRemaining--;
        notifyEvent(player.getPlayerName() + " banked " + selectedCard.getCardName()
                + " (actions left: " + actionsRemaining + ")");
        checkWinCondition();
    }

    public void endTurn() {
        if (isGameOver) {
            notifyEvent("Game is already over.");
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

        currentTurnIndex = (currentTurnIndex + 1) % activePlayers.size();
        startNewTurn();
    }

    private void checkWinCondition() {
        for (Player player : activePlayers) {
            int completedSets = player.getPropertyArea().countCompletedSets();
            if (completedSets >= 3) {
                isGameOver = true;
                notifyEvent("Congratulations " + player.getPlayerName()
                        + " collected 3 full property sets and wins!");
                return;
            }
        }
    }

    private void checkDrawStalemate() {
        if (!isGameOver && gameDeck.getDrawPileSize() == 0 && gameDeck.getDiscardPileSize() == 0) {
            notifyEvent("Draw pile is empty. Continue playing with cards in hand.");
        }
    }

    public void activateDoubleRent() {
        this.rentMultiplier *= 2;
        notifyEvent("Double rent is active for the next rent card.");
    }

    public int getAndResetRentMultiplier() {
        int current = this.rentMultiplier;
        this.rentMultiplier = 1;
        return current;
    }

    public Player getCurrentPlayer() {
        return activePlayers.get(currentTurnIndex);
    }

    public Player resolveTargetOrFirstOpponent(Player initiator) {
        if (currentTargetInfo != null && currentTargetInfo.getTargetPlayer() != null) {
            return currentTargetInfo.getTargetPlayer();
        }
        List<Player> opponents = getOpponents(initiator);
        return opponents.isEmpty() ? null : opponents.get(0);
    }

    public List<Player> getActivePlayers() {
        return Collections.unmodifiableList(activePlayers);
    }

    public Deck getGameDeck() {
        return gameDeck;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public int getActionsRemaining() {
        return actionsRemaining;
    }

    public TargetInfo getCurrentTargetInfo() {
        return currentTargetInfo;
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyEvent(String message) {
        for (GameObserver observer : observers) {
            observer.onGameEvent(message);
        }
    }

    private void notifyTurnChange(String playerName) {
        for (GameObserver observer : observers) {
            observer.onTurnChanged(playerName);
        }
    }

    public void initiateAttack(Player victim, Runnable action) {
        this.currentState = GameState.WAITING_FOR_COUNTER_ACTION;
        this.pendingAction = action;
        this.pendingGroupAction = null;
        this.pendingVictim = victim;
        this.pendingVictims = null;
        this.pendingVictimIndex = 0;
        notifyEvent("[INTERRUPT_REQUEST] " + victim.getPlayerName() + " may use Just Say No.");
    }

    public void initiateGroupAttack(List<Player> victims, Runnable action) {
        if (victims == null || victims.isEmpty()) {
            action.run();
            return;
        }
        this.currentState = GameState.WAITING_FOR_COUNTER_ACTION;
        this.pendingAction = action;
        this.pendingGroupAction = null;
        this.pendingVictims = null;
        this.pendingVictimIndex = 0;
        this.pendingVictim = victims.get(0);
        notifyEvent("[INTERRUPT_REQUEST] " + pendingVictim.getPlayerName()
                + " may use Just Say No.");
    }

    public void initiateGroupAttack(List<Player> victims, Consumer<Player> action) {
        if (victims == null || victims.isEmpty()) {
            return;
        }
        this.currentState = GameState.WAITING_FOR_COUNTER_ACTION;
        this.pendingAction = null;
        this.pendingGroupAction = action;
        this.pendingVictims = new ArrayList<>(victims);
        this.pendingVictimIndex = 0;
        this.pendingVictim = this.pendingVictims.get(0);
        notifyEvent("[INTERRUPT_REQUEST] " + pendingVictim.getPlayerName()
                + " may use Just Say No.");
    }

    public void resolvePendingAction() {
        if (currentState != GameState.WAITING_FOR_COUNTER_ACTION
                || (pendingAction == null && pendingGroupAction == null)) {
            return;
        }

        try {
            if (pendingGroupAction != null) {
                pendingGroupAction.accept(pendingVictim);
                notifyEvent("Action resolved for " + pendingVictim.getPlayerName() + ".");
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

    private void advancePendingVictimOrReset() {
        if (pendingVictims != null && pendingVictimIndex < pendingVictims.size() - 1) {
            pendingVictimIndex++;
            pendingVictim = pendingVictims.get(pendingVictimIndex);
            notifyEvent("[INTERRUPT_REQUEST] " + pendingVictim.getPlayerName()
                    + " may use Just Say No.");
            return;
        }

        resetState();
    }

    public void counterAttackWithJustSayNo(int cardIndex) {
        if (currentState != GameState.WAITING_FOR_COUNTER_ACTION || pendingVictim == null) {
            return;
        }

        Card card = pendingVictim.getHand().getCard(cardIndex);
        if (card != null && card.getCardName().equals("Just Say No")) {
            pendingVictim.getHand().removeCard(cardIndex);
            gameDeck.receiveDiscard(card);
            notifyEvent(pendingVictim.getPlayerName() + " used Just Say No.");
            if (pendingGroupAction != null) {
                advancePendingVictimOrReset();
            } else {
                notifyEvent("The action is cancelled.");
                resetState();
            }
        } else {
            notifyEvent("Invalid counter card.");
        }
    }

    public Player getPendingVictim() {
        return pendingVictim;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    private void resetState() {
        this.currentState = GameState.NORMAL_TURN;
        this.pendingAction = null;
        this.pendingGroupAction = null;
        this.pendingVictim = null;
        this.pendingVictims = null;
        this.pendingVictimIndex = 0;
    }

    public void initiateTargetedAttack(Player initiator, Consumer<Player> attackAction) {
        Player victim = resolveTargetOrFirstOpponent(initiator);
        if (victim == null) {
            throw new IllegalStateException("no valid target for this action.");
        }
        initiateAttack(victim, () -> attackAction.accept(victim));
    }

    public List<Player> getOpponents(Player player) {
        List<Player> opponents = new ArrayList<>();
        for (Player candidate : activePlayers) {
            if (!candidate.equals(player)) {
                opponents.add(candidate);
            }
        }
        return opponents;
    }

    public void processGlobalPayment(Player initiator, int amount) {
        List<Player> opponents = getOpponents(initiator);
        for (Player victim : opponents) {
            victim.getBankArea().pay(amount, initiator);
        }
        notifyEvent("Global payment complete.");
    }

    public void drawCardsForPlayer(Player player, int count) {
        List<Card> drawnCards = gameDeck.drawCards(count);
        player.getHand().addCards(drawnCards);
        notifyEvent(player.getPlayerName() + " drew " + drawnCards.size() + " extra card(s).");
        checkDrawStalemate();
    }
}
