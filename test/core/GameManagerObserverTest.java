package core;

import org.junit.jupiter.api.Test;
import patterns.observer.GameObserver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests GameManager observer notifications and turn-start drawing behavior.
class GameManagerObserverTest {
    // Verifies observers receive log and turn-change notifications.
    @Test
    void observersReceiveEventsAndCanBeRemoved() {
        GameManager game = GameManager.getInstance();
        RecordingObserver observer = new RecordingObserver();
        game.addObserver(observer);

        game.initializeGame(List.of("Alice", "Bob"));
        game.logEvent("manual event");

        assertTrue(observer.events.stream().anyMatch(event -> event.contains("Game initialized")));
        assertTrue(observer.events.contains("manual event"));
        assertEquals("Alice", observer.turns.get(0));

        game.removeObserver(observer);
        game.logEvent("after removal");

        assertTrue(observer.events.stream().noneMatch(event -> event.equals("after removal")));
    }

    // Verifies a player with an empty hand draws five cards at turn start.
    @Test
    void emptyHandDrawsFiveAtNewTurnStart() {
        GameManager game = GameManager.getInstance();
        game.initializeGame(List.of("Alice", "Bob"));
        while (game.getCurrentPlayer().getHand().getSize() > 0) {
            game.getCurrentPlayer().getHand().removeCard(0);
        }

        game.startNewTurn();

        assertEquals(5, game.getCurrentPlayer().getHand().getSize());
    }

    // Records observer callbacks for assertions.
    private static final class RecordingObserver implements GameObserver {
        private final List<String> events = new ArrayList<>();
        private final List<String> turns = new ArrayList<>();

        // Stores game log events.
        @Override
        public void onGameEvent(String message) {
            events.add(message);
        }

        // Stores turn changes.
        @Override
        public void onTurnChanged(String playerName) {
            turns.add(playerName);
        }
    }
}
