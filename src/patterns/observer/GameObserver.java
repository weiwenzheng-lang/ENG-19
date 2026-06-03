package patterns.observer;

public interface GameObserver {
    // Handles game-state log messages.
    void onGameEvent(String message);

    // Handles turn-change notifications.
    void onTurnChanged(String playerName);
}
