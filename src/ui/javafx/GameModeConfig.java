package ui.javafx;

import core.GameManager;
import player.PlayerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GameModeConfig {
    // Defines which table services are active for this session.
    enum Mode {
        LOCAL,
        AI,
        NETWORK
    }

    final List<GameManager.PlayerSetup> players;
    final Mode mode;
    final Long deckSeed;
    final int localPlayerIndex;
    final NetworkGameBridge networkBridge;

    // Stores all immutable inputs needed to create a game table.
    private GameModeConfig(List<GameManager.PlayerSetup> players, Mode mode,
                           Long deckSeed, int localPlayerIndex,
                           NetworkGameBridge networkBridge) {
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.mode = mode;
        this.deckSeed = deckSeed;
        this.localPlayerIndex = localPlayerIndex;
        this.networkBridge = networkBridge;
    }

    // Creates a same-machine human multiplayer configuration.
    static GameModeConfig local(List<GameManager.PlayerSetup> players) {
        return new GameModeConfig(players, Mode.LOCAL, null, 0, null);
    }

    // Creates a same-machine mixed human and AI configuration.
    static GameModeConfig ai(List<GameManager.PlayerSetup> players) {
        return new GameModeConfig(players, Mode.AI, null, 0, null);
    }

    // Creates a LAN configuration using a shared deck seed.
    static GameModeConfig network(List<GameManager.PlayerSetup> players, long deckSeed,
                                  int localPlayerIndex, NetworkGameBridge bridge) {
        return new GameModeConfig(players, Mode.NETWORK, deckSeed, localPlayerIndex, bridge);
    }

    // Reports whether this table relays actions through the network bridge.
    boolean isNetwork() {
        return mode == Mode.NETWORK;
    }

    // Reports whether at least one seat is AI controlled.
    boolean hasAi() {
        for (GameManager.PlayerSetup setup : players) {
            if (setup.getType() == PlayerType.AI) {
                return true;
            }
        }
        return false;
    }
}
