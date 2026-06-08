package ui.javafx;

import org.junit.jupiter.api.Test;
import player.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests the CSV victory record exported after each match.
class MatchHistoryRecorderTest {
    // Verifies a victory row is appended to the history file.
    @Test
    void recordVictoryWritesWinnerToCsv() throws Exception {
        Player winner = new Player("1", "Alice");
        Path path = MatchHistoryRecorder.recordVictory(winner, Arrays.asList(winner), GameModeConfig.Mode.LOCAL);

        String csv = new String(Files.readAllBytes(path));

        assertTrue(csv.contains("Winner"));
        assertTrue(csv.contains("Alice"));
        assertTrue(csv.contains("LOCAL"));
    }
}
