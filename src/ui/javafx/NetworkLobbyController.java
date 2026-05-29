package ui.javafx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import network.LanAddressUtil;
import network.LanGameMessage;
import network.LanGameClient;
import network.LanGameListener;
import network.LanGameServer;
import network.LanRoomState;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NetworkLobbyController {
    private static final int DEFAULT_PORT = 5019;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Runnable backAction;
    private final ListView<String> playerList = new ListView<>();
    private final ListView<String> logList = new ListView<>();
    private final TextField nameField = new TextField("Player");
    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField(String.valueOf(DEFAULT_PORT));
    private final TextField chatField = new TextField();
    private final Label statusLabel = new Label("Not connected");
    private final Label addressLabel = new Label();
    private final Button hostButton = styledButton("Host Room");
    private final Button joinButton = styledButton("Join Room");
    private final Button disconnectButton = styledButton("Disconnect");
    private final Button readyButton = styledButton("Ready");
    private final Button startNetworkButton = styledButton("Start Network Game");
    private final Button testActionButton = styledButton("Send Test Action");
    private final Button sendButton = styledButton("Send");

    private LanGameServer server;
    private LanGameClient client;
    private LanRoomState currentRoomState;
    private boolean ready;

    public NetworkLobbyController(Runnable backAction) {
        this.backAction = backAction;
    }

    public BorderPane createContent() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d0f12;");

        root.setTop(createHeader());
        root.setCenter(createMainPanel());
        root.setBottom(createChatBar());

        disconnectButton.setDisable(true);
        readyButton.setDisable(true);
        startNetworkButton.setDisable(true);
        testActionButton.setDisable(true);
        sendButton.setDisable(true);
        chatField.setDisable(true);
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#f8fbf6"));
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        updateAddressLabel();
        return root;
    }

    public void dispose() {
        disconnectRoom();
    }

    private HBox createHeader() {
        Label title = new Label("Local WiFi Lobby");
        title.setTextFill(javafx.scene.paint.Color.web("#f0c978"));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));

        Button backButton = styledButton("Back");
        backButton.setOnAction(event -> {
            dispose();
            backAction.run();
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, title, spacer, backButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.setStyle("-fx-background-color: #132127;"
                + "-fx-border-color: transparent transparent rgba(0,242,255,0.14) transparent;"
                + "-fx-border-width: 0 0 1 0;");
        return header;
    }

    private HBox createMainPanel() {
        VBox controls = createControls();
        VBox players = createPlayersPanel();
        VBox logs = createLogPanel();

        HBox main = new HBox(14, controls, players, logs);
        main.setPadding(new Insets(16));
        HBox.setHgrow(logs, Priority.ALWAYS);
        return main;
    }

    private VBox createControls() {
        Label section = sectionLabel("Connection", "#cbd8ff");
        configureField(nameField, 180);
        configureField(hostField, 180);
        configureField(portField, 90);

        hostButton.setOnAction(event -> hostRoom());
        joinButton.setOnAction(event -> joinRoom());
        disconnectButton.setOnAction(event -> disconnectRoom());
        readyButton.setOnAction(event -> toggleReady());
        startNetworkButton.setOnAction(event -> startNetworkGame());
        testActionButton.setOnAction(event -> sendTestAction());

        VBox panel = panelBox(
                section,
                fieldBlock("Name", nameField),
                fieldBlock("Host IP", hostField),
                fieldBlock("Port", portField),
                hostButton,
                joinButton,
                disconnectButton,
                readyButton,
                startNetworkButton,
                testActionButton,
                statusLabel,
                addressLabel
        );
        panel.setPrefWidth(260);
        return panel;
    }

    private VBox createPlayersPanel() {
        Label title = sectionLabel("Players", "#ccecc3");
        playerList.setPrefWidth(220);
        playerList.setPlaceholder(new Label("No players"));
        VBox panel = panelBox(title, playerList);
        panel.setPrefWidth(240);
        VBox.setVgrow(playerList, Priority.ALWAYS);
        return panel;
    }

    private VBox createLogPanel() {
        Label title = sectionLabel("Room Log", "#f7d0d7");
        logList.setPlaceholder(new Label("No messages"));
        VBox panel = panelBox(title, logList);
        VBox.setVgrow(logList, Priority.ALWAYS);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private HBox createChatBar() {
        chatField.setPromptText("Message");
        chatField.setOnAction(event -> sendChat());
        HBox.setHgrow(chatField, Priority.ALWAYS);
        sendButton.setOnAction(event -> sendChat());

        HBox chat = new HBox(10, chatField, sendButton);
        chat.setPadding(new Insets(12, 16, 16, 16));
        chat.setAlignment(Pos.CENTER);
        chat.setStyle("-fx-background-color: #090d14;"
                + "-fx-border-color: #1e2a3a transparent transparent transparent;"
                + "-fx-border-width: 1 0 0 0;");
        return chat;
    }

    private VBox fieldBlock(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setTextFill(javafx.scene.paint.Color.web("#f7efe1"));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        return new VBox(6, label, field);
    }

    private VBox panelBox(javafx.scene.Node... children) {
        VBox panel = new VBox(12, children);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #111827;"
                + "-fx-border-color: #2a3040;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");
        return panel;
    }

    private void configureField(TextField field, double width) {
        field.setPrefWidth(width);
        field.setStyle("-fx-background-color: #16181b;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: rgba(0, 242, 255, 0.45);"
                + "-fx-border-radius: 8;"
                + "-fx-text-fill: #f8fbf6;"
                + "-fx-prompt-text-fill: #8ca0a8;"
                + "-fx-font-family: 'Consolas';"
                + "-fx-font-size: 13px;");
    }

    private Label sectionLabel(String text, String color) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web(color));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        return label;
    }

    private static Button styledButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTextFill(javafx.scene.paint.Color.web("#1b2a31"));
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        button.setStyle("-fx-background-color: #f0c978;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: #ffe0a1;"
                + "-fx-border-radius: 8;"
                + "-fx-padding: 9 16 9 16;");
        return button;
    }

    private void hostRoom() {
        int port = readPort();
        if (port < 0) {
            return;
        }

        disconnectRoom();
        try {
            server = new LanGameServer(port, this::appendLogFromAnyThread);
            server.start();
            hostField.setText("127.0.0.1");
            connectClient("127.0.0.1", server.getPort());
            appendLog("Hosting room on port " + server.getPort() + ".");
            updateAddressLabel();
        } catch (IOException e) {
            appendLog("Could not host room: " + e.getMessage());
            if (server != null) {
                server.stop();
                server = null;
            }
            setConnectedUi(false);
        }
    }

    private void joinRoom() {
        int port = readPort();
        if (port < 0) {
            return;
        }

        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            appendLog("Host IP is required.");
            return;
        }

        disconnectRoom();
        connectClient(host, port);
    }

    private void connectClient(String host, int port) {
        try {
            client = new LanGameClient(new LobbyNetworkListener());
            client.connect(host, port, readPlayerName());
            ready = false;
            setConnectedUi(true);
        } catch (IOException | IllegalArgumentException e) {
            appendLog("Could not connect: " + e.getMessage());
            client = null;
            setConnectedUi(false);
        }
    }

    private void disconnectRoom() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }
        ready = false;
        currentRoomState = null;
        playerList.getItems().clear();
        setConnectedUi(false);
    }

    private int readPort() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                appendLog("Port must be between 1 and 65535.");
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            appendLog("Port must be a number.");
            return -1;
        }
    }

    private String readPlayerName() {
        String name = nameField.getText().trim();
        return name.isEmpty() ? "Player" : name;
    }

    private void sendChat() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        String text = chatField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        client.sendChat(text);
        chatField.clear();
    }

    private void setConnectedUi(boolean connected) {
        hostButton.setDisable(connected);
        joinButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        readyButton.setDisable(!connected);
        readyButton.setText(ready ? "Not Ready" : "Ready");
        startNetworkButton.setDisable(!canStartNetworkGame());
        testActionButton.setDisable(!connected || currentRoomState == null || !currentRoomState.isGameStarted());
        sendButton.setDisable(!connected);
        chatField.setDisable(!connected);
        if (!connected) {
            statusLabel.setText("Not connected");
        }
    }

    private boolean canStartNetworkGame() {
        return client != null
                && client.isConnected()
                && currentRoomState != null
                && !currentRoomState.isGameStarted()
                && client.getPlayerId() == currentRoomState.getHostPlayerId();
    }

    private void toggleReady() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        ready = !ready;
        client.setReady(ready);
        setConnectedUi(true);
    }

    private void startNetworkGame() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        client.requestStartGame();
    }

    private void sendTestAction() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        client.sendGameAction("LOBBY_TEST", "Protocol test from " + readPlayerName());
    }

    private void updateAddressLabel() {
        List<String> addresses = LanAddressUtil.localIpv4Addresses();
        String text = addresses.isEmpty()
                ? "Local IP: unavailable"
                : "Local IP: " + String.join(", ", addresses);
        addressLabel.setText(text);
        addressLabel.setWrapText(true);
        addressLabel.setTextFill(javafx.scene.paint.Color.web("#8dd7ee"));
        addressLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
    }

    private void appendLogFromAnyThread(String message) {
        Platform.runLater(() -> appendLog(message));
    }

    private void appendLog(String message) {
        String time = LocalTime.now().format(TIME_FORMAT);
        logList.getItems().add(0, "[" + time + "] " + message);
    }

    private final class LobbyNetworkListener implements LanGameListener {
        @Override
        public void onStatusChanged(String status) {
            Platform.runLater(() -> statusLabel.setText(status));
        }

        @Override
        public void onPlayersChanged(List<String> players) {
            Platform.runLater(() -> playerList.getItems().setAll(players));
        }

        @Override
        public void onRoomStateChanged(LanRoomState roomState) {
            Platform.runLater(() -> {
                currentRoomState = roomState;
                statusLabel.setText(roomState.toSummary());
                setConnectedUi(client != null && client.isConnected());
            });
        }

        @Override
        public void onGameStarted() {
            Platform.runLater(() -> {
                appendLog("Start signal received. Game sync messages are enabled.");
                setConnectedUi(client != null && client.isConnected());
            });
        }

        @Override
        public void onGameMessage(LanGameMessage message) {
            Platform.runLater(() -> appendLog("Game " + message.getType()
                    + " from " + message.getSenderName()
                    + ": " + message.getPayload()));
        }

        @Override
        public void onReconnecting(int attempt, int maxAttempts) {
            Platform.runLater(() -> statusLabel.setText(
                    "Reconnecting " + attempt + "/" + maxAttempts + "..."));
        }

        @Override
        public void onLogMessage(String message) {
            Platform.runLater(() -> appendLog(message));
        }

        @Override
        public void onDisconnected() {
            Platform.runLater(() -> {
                ready = false;
                currentRoomState = null;
                playerList.getItems().clear();
                setConnectedUi(false);
                appendLog("Disconnected from room.");
            });
        }
    }
}
