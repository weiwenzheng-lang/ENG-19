package ai;

import cards.Card;
import cards.PropertyWildCard;
import cards.RentCard;
import cards.SuperWildCard;
import cards.WildRentCard;
import core.GameManager;
import enums.PropertyColor;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import player.Player;

import java.util.List;

// Runs AI turns on the JavaFX thread with short visual pauses.
public class AITurnExecutor {
    private static final long ACTION_DELAY_MS = 2500;
    private static final long POLL_DELAY_MS = 300;

    private final AIActionStrategy brain;
    private Player aiPlayer;
    private GameManager game;
    private boolean running;

    // Stores the AI brain used for decisions.
    public AITurnExecutor(AIActionStrategy brain) {
        this.brain = brain;
    }

    // Starts or restarts the current AI turn.
    public void startTurn(Player aiPlayer) {
        this.aiPlayer = aiPlayer;
        this.game = GameManager.getInstance();
        this.running = true;
        scheduleNextAction();
    }

    // Answers a pending Just Say No prompt for an AI victim.
    public void handleInterrupt(Player victim) {
        if (victim == null) return;
        if (game == null) {
            game = GameManager.getInstance();
        }
        if (game.getPendingVictim() != victim) return;

        if (aiPlayer == null) {
            aiPlayer = victim;
        }

        boolean useJSN = brain.shouldCounterWithJustSayNo(victim, game);
        int jsnIndex = findJustSayNo(victim.getHand().getCards());

        if (useJSN && jsnIndex >= 0) {
            game.counterAttackWithJustSayNo(jsnIndex);
        } else {
            game.resolvePendingAction();
        }
        scheduleNextAction();
    }

    // Stops pending AI scheduling.
    public void stop() {
        this.running = false;
    }

    // Schedules the next AI decision when the game is ready.
    private void scheduleNextAction() {
        if (!running || game.isGameOver()) return;

        if (game.getCurrentPlayer() != aiPlayer
                && game.getCurrentState() == GameManager.GameState.NORMAL_TURN) {
            running = false;
            return;
        }

        if (game.getCurrentState() != GameManager.GameState.NORMAL_TURN) {
            PauseTransition wait = new PauseTransition(Duration.millis(POLL_DELAY_MS));
            wait.setOnFinished(e -> scheduleNextAction());
            wait.play();
            return;
        }

        if (game.getCurrentPlayer() != aiPlayer) {
            running = false;
            return;
        }

        AIAction action = brain.decideNextAction(aiPlayer, game);
        executeAction(action);
    }

    // Applies one AI action to the game state.
    private void executeAction(AIAction action) {
        if (!running) return;

        try {
            switch (action.getType()) {
                case PLAY_CARD:
                    executePlayCard(action);
                    break;
                case PLAY_DOUBLE_RENT:
                    executeDoubleRent(action);
                    break;
                case DEPOSIT_TO_BANK:
                    game.depositCardToBank(action.getCardIndex());
                    break;
                case DISCARD:
                    game.discardCard(action.getCardIndex());
                    break;
                case END_TURN:
                    running = false;
                    game.endTurn();
                    return;
            }
        } catch (Exception e) {
            game.logEvent("[AI] Action failed: " + e.getMessage());
        }

        PauseTransition delay = new PauseTransition(Duration.millis(ACTION_DELAY_MS));
        delay.setOnFinished(e -> scheduleNextAction());
        delay.play();
    }

    // Plays a standard AI card after applying any chosen color.
    private void executePlayCard(AIAction action) {
        List<Card> hand = aiPlayer.getHand().getCards();
        int idx = action.getCardIndex();
        if (idx < 0 || idx >= hand.size()) return;

        Card card = hand.get(idx);
        applyColor(card, action.getSelectedColor());

        game.executePlayerAction(idx, action.getTargetInfo());
    }

    // Plays Double The Rent with its paired rent card.
    private void executeDoubleRent(AIAction action) {
        List<Card> hand = aiPlayer.getHand().getCards();
        int doubleIdx = action.getCardIndex();
        int rentIdx = action.getRentCardIndex();
        if (doubleIdx < 0 || doubleIdx >= hand.size()) return;
        if (rentIdx < 0 || rentIdx >= hand.size()) return;

        Card rentCard = hand.get(rentIdx);
        applyColor(rentCard, action.getSelectedColor());

        game.executeDoubleRentAction(doubleIdx, rentIdx, action.getTargetInfo());
    }

    // Applies a preselected color to wild property or rent cards.
    private void applyColor(Card card, PropertyColor color) {
        if (color == null) return;

        if (card instanceof SuperWildCard) {
            ((SuperWildCard) card).setCurrentColor(color);
        } else if (card instanceof PropertyWildCard) {
            ((PropertyWildCard) card).setCurrentColor(color);
        } else if (card instanceof RentCard) {
            ((RentCard) card).setSelectedColor(color);
        } else if (card instanceof WildRentCard) {
            ((WildRentCard) card).setSelectedColor(color);
        }
    }

    // Finds a Just Say No card in the given hand.
    private int findJustSayNo(List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardName().equals("Just Say No")) {
                return i;
            }
        }
        return -1;
    }
}
