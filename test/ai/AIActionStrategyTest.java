package ai;

import cards.MoneyCard;
import core.GameManager;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests that AI decision code is exposed through the strategy interface.
class AIActionStrategyTest {
    // Verifies the concrete AI brain can be used through the strategy contract.
    @Test
    void brainImplementsActionStrategy() {
        AIActionStrategy strategy = new AIPlayerBrain();
        GameManager game = GameManager.getInstance();
        game.initializeGame(List.of("Bot", "Human"));
        Player bot = game.getCurrentPlayer();
        clearHand(bot);

        AIAction action = strategy.decideNextAction(bot, game);

        assertEquals(AIAction.Type.END_TURN, action.getType());
    }

    // Verifies the executor accepts any implementation of the strategy interface.
    @Test
    void executorDependsOnStrategyInterface() {
        AIActionStrategy strategy = new AIActionStrategy() {
            // Always ends the turn for this contract test.
            @Override
            public AIAction decideNextAction(Player ai, GameManager game) {
                return new AIAction(AIAction.Type.END_TURN);
            }

            // Never counters for this contract test.
            @Override
            public boolean shouldCounterWithJustSayNo(Player victim, GameManager game) {
                return false;
            }
        };

        assertDoesNotThrow(() -> new AITurnExecutor(strategy));
    }

    // Removes all cards so the AI has no playable action.
    private void clearHand(Player player) {
        while (player.getHand().getSize() > 0) {
            player.getHand().removeCard(0);
        }
        player.getHand().addCards(List.of(
                new MoneyCard(1, "Money 1M", 1),
                new MoneyCard(2, "Money 1M", 1),
                new MoneyCard(3, "Money 1M", 1)));
        GameManager.getInstance().depositCardToBank(0);
        GameManager.getInstance().depositCardToBank(0);
        GameManager.getInstance().depositCardToBank(0);
    }
}
