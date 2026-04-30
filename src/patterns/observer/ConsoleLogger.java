package patterns.observer;

public class ConsoleLogger implements GameObserver {
    @Override
    public void onGameEvent(String message) {
        // 现实中这里可以替换为更新 JavaFX 界面的代码
        System.out.println(">> [GAME LOG] " + message);
    }

    @Override
    public void onTurnChanged(String playerName) {
        System.out.println("\n=====================================");
        System.out.println("     🏁 TURN STARTS: " + playerName);
        System.out.println("=====================================");
    }
}