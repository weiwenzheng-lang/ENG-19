package core;

import org.junit.jupiter.api.Test;
import player.PlayerType;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests active-player removal used when a LAN player leaves a match.
class GameManagerPlayerRemovalTest {
    // Verifies removing one seat keeps the remaining players active.
    @Test
    void removePlayerAtDropsSeatAndKeepsGameRunning() {
        GameManager game = GameManager.getInstance();
        game.initializeConfiguredGame(Arrays.asList(
                new GameManager.PlayerSetup("Alice", PlayerType.HUMAN),
                new GameManager.PlayerSetup("Bob", PlayerType.HUMAN),
                new GameManager.PlayerSetup("Carol", PlayerType.HUMAN)
        ), 42L);

        assertTrue(game.removePlayerAt(1));

        assertEquals(2, game.getActivePlayers().size());
        assertEquals("Alice", game.getActivePlayers().get(0).getPlayerName());
        assertEquals("Carol", game.getActivePlayers().get(1).getPlayerName());
        assertFalse(game.isGameOver());
    }

    // Verifies one remaining player wins by forfeit.
    @Test
    void removingDownToOnePlayerEndsGameWithRemainingWinner() {
        GameManager game = GameManager.getInstance();
        game.initializeConfiguredGame(Arrays.asList(
                new GameManager.PlayerSetup("Alice", PlayerType.HUMAN),
                new GameManager.PlayerSetup("Bob", PlayerType.HUMAN)
        ), 42L);

        assertTrue(game.removePlayerAt(1));

        assertTrue(game.isGameOver());
        assertEquals("Alice", game.getWinner().getPlayerName());
    }
}
