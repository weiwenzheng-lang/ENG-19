package ui.javafx;

import core.GameManager;

import java.util.List;

interface NetworkGameStart {
    // Opens a game table from the network roster and shared deck seed.
    void start(List<GameManager.PlayerSetup> players, long deckSeed, int localPlayerIndex);
}
