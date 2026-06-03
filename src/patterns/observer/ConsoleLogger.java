package patterns.observer;

public class ConsoleLogger implements GameObserver {
    @Override
    // Prints game events to the console.
    public void onGameEvent(String message) {
        System.out.println(">> [GAME LOG] " + message);
    }

    @Override
    // Prints a visible turn separator to the console.
    public void onTurnChanged(String playerName) {
        System.out.println("\n=====================================");
        System.out.println("     TURN STARTS: " + playerName);
        System.out.println("=====================================");
    }
}
