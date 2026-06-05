package ai;

import core.GameManager;
import player.Player;

// Defines the strategy interface used to choose AI turn actions.
public interface AIActionStrategy {
    // Chooses the next action for the active AI player.
    AIAction decideNextAction(Player ai, GameManager game);

    // Decides whether an AI victim should counter with Just Say No.
    boolean shouldCounterWithJustSayNo(Player victim, GameManager game);
}
