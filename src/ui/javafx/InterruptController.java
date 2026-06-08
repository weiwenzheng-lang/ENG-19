package ui.javafx;

import ai.AITurnExecutor;
import cards.Card;
import core.GameManager;
import player.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Handles Just Say No interrupt choices for AI, local humans, and remote humans.
final class InterruptController {
    private final GameManager game;
    private final AITurnExecutor aiExecutor;
    private final GameModeConfig modeConfig;
    private final Predicate<Player> localPlayerCheck;
    private final NetworkActionSender networkActionSender;
    private final Consumer<String> eventSink;
    private final Runnable renderCallback;

    // Stores the collaborators needed to resolve interrupt prompts.
    InterruptController(GameManager game,
                        AITurnExecutor aiExecutor,
                        GameModeConfig modeConfig,
                        Predicate<Player> localPlayerCheck,
                        NetworkActionSender networkActionSender,
                        Consumer<String> eventSink,
                        Runnable renderCallback) {
        this.game = game;
        this.aiExecutor = aiExecutor;
        this.modeConfig = modeConfig;
        this.localPlayerCheck = localPlayerCheck;
        this.networkActionSender = networkActionSender;
        this.eventSink = eventSink == null ? message -> { } : eventSink;
        this.renderCallback = renderCallback == null ? () -> { } : renderCallback;
    }

    // Routes the pending counter prompt to the owner of the victim player.
    void handleInterruptRequest() {
        if (game.getCurrentState() != GameManager.GameState.WAITING_FOR_COUNTER_ACTION) {
            return;
        }

        Player victim = game.getPendingVictim();
        if (victim == null) {
            return;
        }
        if (victim.isAI()) {
            handleAiInterrupt(victim);
            return;
        }
        if (isRemoteHumanVictim(victim)) {
            waitForRemoteInterrupt(victim);
            return;
        }
        handleLocalHumanInterrupt(victim);
    }

    // Lets the AI executor decide whether to counter.
    private void handleAiInterrupt(Player victim) {
        aiExecutor.handleInterrupt(victim);
        renderCallback.run();
    }

    // Reports whether the pending victim belongs to another LAN client.
    private boolean isRemoteHumanVictim(Player victim) {
        return modeConfig.isNetwork() && !localPlayerCheck.test(victim);
    }

    // Logs that a remote human must respond from their own machine.
    private void waitForRemoteInterrupt(Player victim) {
        eventSink.accept("[Network] Waiting for " + victim.getPlayerName() + " to answer Just Say No.");
    }

    // Prompts the local human victim for Just Say No.
    private void handleLocalHumanInterrupt(Player victim) {
        int jsnIndex = findJustSayNoIndex(victim);
        if (jsnIndex >= 0) {
            resolveLocalCounterChoice(victim, jsnIndex);
        } else {
            resolveWithoutCounter(victim);
        }
        renderCallback.run();
    }

    // Finds the first Just Say No card in the victim hand.
    private int findJustSayNoIndex(Player victim) {
        List<Card> hand = victim.getHand().getCards();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardName().equals("Just Say No")) {
                return i;
            }
        }
        return -1;
    }

    // Applies the local human's counter choice.
    private void resolveLocalCounterChoice(Player victim, int jsnIndex) {
        boolean counter = GameDialogs.showConfirmation("Counter Action",
                victim.getPlayerName() + " is under attack!",
                "Use Just Say No to counter?");
        if (counter) {
            game.counterAttackWithJustSayNo(jsnIndex);
            networkActionSender.send("JUST_SAY_NO", "index=" + jsnIndex + ";card=Just Say No");
        } else {
            game.resolvePendingAction();
            networkActionSender.send("RESOLVE_PENDING", "");
        }
    }

    // Resolves the pending action when no counter card exists.
    private void resolveWithoutCounter(Player victim) {
        GameDialogs.showMessage("Counter Action",
                victim.getPlayerName() + " is under attack!",
                "No Just Say No available. The action will proceed.");
        game.resolvePendingAction();
        networkActionSender.send("RESOLVE_PENDING", "");
    }

    // Names the network sender dependency for interrupt results.
    interface NetworkActionSender {
        void send(String type, String payload);
    }
}
