package network;

import java.util.List;

public interface LanGameListener {
    // Reports human-readable connection status changes.
    void onStatusChanged(String status);

    // Reports display strings for the lobby roster.
    void onPlayersChanged(List<String> players);

    // Reports typed player data for game setup.
    default void onPlayerInfosChanged(List<LanPlayerInfo> players) {
    }

    // Reports room counts and host identity.
    default void onRoomStateChanged(LanRoomState roomState) {
    }

    // Legacy start callback kept for simple tests.
    default void onGameStarted() {
    }

    // Start callback that includes the shared deck seed.
    default void onGameStarted(long deckSeed) {
        onGameStarted();
    }

    // Start callback that includes both deck seed and AI seats.
    default void onGameStarted(long deckSeed, int aiCount) {
        onGameStarted(deckSeed);
    }

    // Reports relayed in-game action or state payloads.
    default void onGameMessage(LanGameMessage message) {
    }

    // Reports reconnect progress after an unexpected socket drop.
    default void onReconnecting(int attempt, int maxAttempts) {
    }

    // Appends a message to the lobby log.
    void onLogMessage(String message);

    // Reports that the client has fully disconnected.
    void onDisconnected();
}
