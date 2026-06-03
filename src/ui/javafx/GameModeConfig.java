package ui.javafx;

import core.GameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GameModeConfig {
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

    private GameModeConfig(List<GameManager.PlayerSetup> players, Mode mode,
                           Long deckSeed, int localPlayerIndex,
                           NetworkGameBridge networkBridge) {
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
        this.mode = mode;
        this.deckSeed = deckSeed;
        this.localPlayerIndex = localPlayerIndex;
        this.networkBridge = networkBridge;
    }

    static GameModeConfig local(List<GameManager.PlayerSetup> players) {
        return new GameModeConfig(players, Mode.LOCAL, null, 0, null);
    }

    static GameModeConfig ai(List<GameManager.PlayerSetup> players) {
        return new GameModeConfig(players, Mode.AI, null, 0, null);
    }

    static GameModeConfig network(List<GameManager.PlayerSetup> players, long deckSeed,
                                  int localPlayerIndex, NetworkGameBridge bridge) {
        return new GameModeConfig(players, Mode.NETWORK, deckSeed, localPlayerIndex, bridge);
    }

    boolean isNetwork() {
        return mode == Mode.NETWORK;
    }

    boolean hasAi() {
        return mode == Mode.AI;
    }
}
