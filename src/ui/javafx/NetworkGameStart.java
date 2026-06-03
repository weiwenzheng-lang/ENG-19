package ui.javafx;

import core.GameManager;

import java.util.List;

interface NetworkGameStart {
    void start(List<GameManager.PlayerSetup> players, long deckSeed, int localPlayerIndex);
}
