package ui.javafx;

import player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Records completed matches in a CSV file that can be submitted or exported.
final class MatchHistoryRecorder {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path HISTORY_PATH = Paths.get(System.getProperty("user.dir"),
            "out", "match-history", "monopoly_deal_match_history.csv");

    // Prevents construction of this file utility.
    private MatchHistoryRecorder() {
    }

    // Appends one victory row and returns the export path.
    static Path recordVictory(Player winner, List<Player> players, GameModeConfig.Mode mode) throws IOException {
        Files.createDirectories(HISTORY_PATH.getParent());
        if (!Files.exists(HISTORY_PATH)) {
            Files.write(HISTORY_PATH,
                    "Time,Mode,Winner,Completed Sets,Players\n".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE);
        }

        String row = String.join(",",
                csv(LocalDateTime.now().format(TIME_FORMAT)),
                csv(mode.name()),
                csv(winner == null ? "Unknown" : winner.getPlayerName()),
                csv(winner == null ? "0" : String.valueOf(winner.getPropertyArea().countCompletedSets())),
                csv(playerNames(players))) + System.lineSeparator();
        Files.write(HISTORY_PATH, row.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        return HISTORY_PATH;
    }

    // Returns the current history export file path.
    static Path historyPath() {
        return HISTORY_PATH;
    }

    // Joins player names for one compact CSV field.
    private static String playerNames(List<Player> players) {
        StringBuilder names = new StringBuilder();
        if (players != null) {
            for (Player player : players) {
                if (names.length() > 0) {
                    names.append(" | ");
                }
                names.append(player.getPlayerName());
            }
        }
        return names.toString();
    }

    // Escapes one CSV cell.
    private static String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
