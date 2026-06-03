package ui.javafx;

import network.LanGameMessage;

import java.util.function.Consumer;

interface NetworkGameBridge {
    // Returns the local network player id.
    int getLocalPlayerId();

    // Sends one table action through the network room.
    void sendGameAction(String type, String payload);

    // Registers a table callback for incoming room messages.
    void setGameMessageHandler(Consumer<LanGameMessage> handler);
}
