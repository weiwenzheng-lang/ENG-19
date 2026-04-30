package patterns.observer;

public interface GameObserver {
    // 当游戏状态更新时，通知观察者
    void onGameEvent(String message);
    void onTurnChanged(String playerName);
}