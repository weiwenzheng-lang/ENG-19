package network;

import java.util.List;

public interface LanGameListener {
    void onStatusChanged(String status);

    void onPlayersChanged(List<String> players);

    default void onPlayerInfosChanged(List<LanPlayerInfo> players) {
    }

    default void onRoomStateChanged(LanRoomState roomState) {
    }

    default void onGameStarted() {
    }

    default void onGameStarted(long deckSeed) {
        onGameStarted();
    }

    default void onGameMessage(LanGameMessage message) {
    }

    default void onReconnecting(int attempt, int maxAttempts) {
    }

    void onLogMessage(String message);

    void onDisconnected();
}
