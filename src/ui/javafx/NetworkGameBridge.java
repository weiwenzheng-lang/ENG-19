package ui.javafx;

import network.LanGameMessage;

import java.util.function.Consumer;

interface NetworkGameBridge {
    int getLocalPlayerId();

    void sendGameAction(String type, String payload);

    void setGameMessageHandler(Consumer<LanGameMessage> handler);
}
