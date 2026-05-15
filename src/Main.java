import core.GameManager;
import patterns.observer.ConsoleLogger;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        GameManager game = GameManager.getInstance();

        // Register ConsoleLogger as an observer (demonstrates Observer pattern)
        game.addObserver(new ConsoleLogger());

        List<String> playerNames = Arrays.asList("Player A", "Player B", "Player C");
        game.initializeGame(playerNames);

        System.out.println("Game engine started, waiting for UI...");
    }
}