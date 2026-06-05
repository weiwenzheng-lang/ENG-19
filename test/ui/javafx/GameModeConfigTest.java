package ui.javafx;

import core.GameManager;
import org.junit.jupiter.api.Test;
import player.PlayerType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests session configuration for local, AI, and network games.
class GameModeConfigTest {
    // Verifies local mode has no network bridge and no AI by default.
    @Test
    void localConfigReportsNoNetworkOrAi() {
        GameModeConfig config = GameModeConfig.local(List.of(
                new GameManager.PlayerSetup("Alice", PlayerType.HUMAN),
                new GameManager.PlayerSetup("Bob", PlayerType.HUMAN)));

        assertEquals(GameModeConfig.Mode.LOCAL, config.mode);
        assertFalse(config.isNetwork());
        assertFalse(config.hasAi());
    }

    // Verifies mixed same-computer games report AI seats.
    @Test
    void aiConfigReportsAiSeat() {
        GameModeConfig config = GameModeConfig.ai(List.of(
                new GameManager.PlayerSetup("Alice", PlayerType.HUMAN),
                new GameManager.PlayerSetup("Bot", PlayerType.AI)));

        assertEquals(GameModeConfig.Mode.AI, config.mode);
        assertTrue(config.hasAi());
    }

    // Verifies network mode stores the shared seed and local player index.
    @Test
    void networkConfigStoresSeedAndLocalIndex() {
        GameModeConfig config = GameModeConfig.network(List.of(
                new GameManager.PlayerSetup("Host", PlayerType.HUMAN),
                new GameManager.PlayerSetup("Guest", PlayerType.HUMAN)), 123L, 1, null);

        assertTrue(config.isNetwork());
        assertEquals(123L, config.deckSeed);
        assertEquals(1, config.localPlayerIndex);
    }
}
