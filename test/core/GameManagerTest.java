package core;

import cards.MoneyCard;
import core.GameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {
    private GameManager game;

    @BeforeEach
    void setUp() {
        game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Alice", "Bob"));
        // Replace random hand with known cards so tests are deterministic
        game.getCurrentPlayer().getHand().getCards();
        // Clear hand and add money cards
        while (game.getCurrentPlayer().getHand().getSize() > 0) {
            game.getCurrentPlayer().getHand().removeCard(0);
        }
        game.getCurrentPlayer().getHand().addCards(List.of(
                new MoneyCard(1, "Money 1M", 1),
                new MoneyCard(2, "Money 1M", 1),
                new MoneyCard(3, "Money 1M", 1),
                new MoneyCard(4, "Money 1M", 1)
        ));
    }

    @Test
    void testActionPointsDeduction() {
        assertEquals(3, game.getActionsRemaining(), "New turn should start with 3 actions");

        game.depositCardToBank(0);
        assertEquals(2, game.getActionsRemaining(), "After 1 deposit, actions should be 2");

        game.depositCardToBank(0);
        game.depositCardToBank(0);
        assertEquals(0, game.getActionsRemaining(), "After 3 deposits, actions should be 0");

        // Try a 4th deposit -- should fail
        game.depositCardToBank(0);
        assertEquals(0, game.getActionsRemaining(), "Actions should not go negative");
    }
}