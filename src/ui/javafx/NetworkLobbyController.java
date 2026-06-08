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
import core.GameManager;
import network.LanAddressUtil;
import network.LanGameMessage;
import network.LanGameClient;
import network.LanGameListener;
import network.LanPlayerInfo;
import network.LanGameServer;
import network.LanRoomState;
import player.PlayerType;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NetworkLobbyController {
    private static final int DEFAULT_PORT = 5019;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Runnable backAction;
    private final NetworkGameStart gameStartAction;
    private final ListView<String> playerList = new ListView<>();
    private final ListView<String> logList = new ListView<>();
    private final TextField nameField = new TextField("Player");
    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField(String.valueOf(DEFAULT_PORT));
    private final TextField aiCountField = new TextField("0");
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
    private List<LanPlayerInfo> currentPlayers = new ArrayList<>();
    private Consumer<LanGameMessage> gameMessageHandler;
    private boolean ready;

    // Creates a lobby view that can return to the menu or open a table.
    public NetworkLobbyController(Runnable backAction, NetworkGameStart gameStartAction) {
        this.backAction = backAction;
        this.gameStartAction = gameStartAction;
    }

    // Builds the full lobby screen and initializes disabled controls.
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

    // Releases any hosted or joined room before leaving the screen.
    public void dispose() {
        disconnectRoom();
    }

    // Refreshes lobby controls after returning from a finished network table.
    public void markBetweenMatches() {
        ready = false;
        if (client != null && client.isConnected()) {
            client.setReady(false);
            statusLabel.setText("Back in room. Ready for another match.");
            setConnectedUi(true);
        }
    }

    // Creates the bridge used by the game table after a network game starts.
    public NetworkGameBridge createGameBridge() {
        LanGameClient activeClient = client;
        return new NetworkGameBridge() {
            @Override
            // Returns this client's room id for ownership checks.
            public int getLocalPlayerId() {
                return activeClient == null ? -1 : activeClient.getPlayerId();
            }

            @Override
            // Relays a table action through the lobby client.
            public void sendGameAction(String type, String payload) {
                if (activeClient != null && activeClient.isConnected()) {
                    activeClient.sendGameAction(type, payload);
                }
            }

            @Override
            // Registers the table-level network message handler.
            public void setGameMessageHandler(Consumer<LanGameMessage> handler) {
                gameMessageHandler = handler;
            }
        };
    }

    // Creates the top title bar and back navigation.
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

    // Lays out connection controls, player roster, and logs.
    private HBox createMainPanel() {
        VBox controls = createControls();
        VBox players = createPlayersPanel();
        VBox logs = createLogPanel();

        HBox main = new HBox(14, controls, players, logs);
        main.setPadding(new Insets(16));
        HBox.setHgrow(logs, Priority.ALWAYS);
        return main;
    }

    // Builds the host/join controls and AI seat field.
    private VBox createControls() {
        Label section = sectionLabel("Connection", "#cbd8ff");
        configureField(nameField, 180);
        configureField(hostField, 180);
        configureField(portField, 90);
        configureField(aiCountField, 90);

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
                fieldBlock("AI seats", aiCountField),
                hostButton,
                joinButton,
                disconnectButton,
                readyButton,
                startNetworkButton,
                statusLabel,
                addressLabel
        );
        panel.setPrefWidth(260);
        return panel;
    }

    // Builds the visible player roster panel.
    private VBox createPlayersPanel() {
        Label title = sectionLabel("Players", "#ccecc3");
        playerList.setPrefWidth(220);
        playerList.setPlaceholder(new Label("No players"));
        VBox panel = panelBox(title, playerList);
        panel.setPrefWidth(240);
        VBox.setVgrow(playerList, Priority.ALWAYS);
        return panel;
    }

    // Builds the room activity log panel.
    private VBox createLogPanel() {
        Label title = sectionLabel("Room Log", "#f7d0d7");
        logList.setPlaceholder(new Label("No messages"));
        VBox panel = panelBox(title, logList);
        VBox.setVgrow(logList, Priority.ALWAYS);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    // Builds the chat input bar.
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

    // Creates a label-field stack.
    private VBox fieldBlock(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setTextFill(javafx.scene.paint.Color.web("#f7efe1"));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        return new VBox(6, label, field);
    }

    // Applies the shared dark panel style.
    private VBox panelBox(javafx.scene.Node... children) {
        VBox panel = new VBox(12, children);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #111827;"
                + "-fx-border-color: #2a3040;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");
        return panel;
    }

    // Applies the shared input field style.
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

    // Creates a colored section heading.
    private Label sectionLabel(String text, String color) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web(color));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        return label;
    }

    // Creates a lobby button with consistent visual treatment.
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

    // Starts an in-process room server and joins it as the host.
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

    // Joins a room hosted on another machine.
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

    // Creates and connects the client half of the lobby.
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

    // Disconnects the client and stops the local server if this user hosts.
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
        currentPlayers = new ArrayList<>();
        playerList.getItems().clear();
        setConnectedUi(false);
    }

    // Reads and validates the TCP port field.
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

    // Returns a non-empty player name.
    private String readPlayerName() {
        String name = nameField.getText().trim();
        return name.isEmpty() ? "Player" : name;
    }

    // Sends chat text through the active client.
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

    // Keeps buttons synchronized with connection and room state.
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

    // Allows only the connected room host to request a game start.
    private boolean canStartNetworkGame() {
        return client != null
                && client.isConnected()
                && currentRoomState != null
                && client.getPlayerId() == currentRoomState.getHostPlayerId();
    }

    // Toggles the local ready state and sends it to the server.
    private void toggleReady() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        ready = !ready;
        client.setReady(ready);
        setConnectedUi(true);
    }

    // Requests a table start with the selected AI seat count.
    private void startNetworkGame() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        int aiCount = readAiCount();
        if (aiCount < 0) {
            return;
        }
        client.requestStartGame(aiCount);
    }

    // Reads and validates AI seats against total 2-5 player rules.
    private int readAiCount() {
        try {
            int aiCount = Integer.parseInt(aiCountField.getText().trim());
            if (aiCount < 0 || aiCount > GameManager.MAX_PLAYERS - 1) {
                appendLog("AI seats must be between 0 and 4.");
                return -1;
            }
            int onlineCount = currentRoomState == null ? 0 : currentRoomState.getOnlineCount();
            int totalPlayers = onlineCount + aiCount;
            if (totalPlayers < GameManager.MIN_PLAYERS || totalPlayers > GameManager.MAX_PLAYERS) {
                appendLog("Online players plus AI seats must total 2 to 5.");
                return -1;
            }
            return aiCount;
        } catch (NumberFormatException e) {
            appendLog("AI seats must be a number.");
            return -1;
        }
    }

    // Sends a simple relay test message while a table is active.
    private void sendTestAction() {
        if (client == null || !client.isConnected()) {
            appendLog("Not connected.");
            return;
        }
        client.sendGameAction("LOBBY_TEST", "Protocol test from " + readPlayerName());
    }

    // Shows local addresses that other players can type into Host IP.
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

    // Moves server-thread log messages onto the JavaFX thread.
    private void appendLogFromAnyThread(String message) {
        Platform.runLater(() -> appendLog(message));
    }

    // Adds a timestamped line at the top of the lobby log.
    private void appendLog(String message) {
        String time = LocalTime.now().format(TIME_FORMAT);
        logList.getItems().add(0, "[" + time + "] " + message);
    }

    // Receives network callbacks and applies them on the JavaFX thread.
    private final class LobbyNetworkListener implements LanGameListener {
        @Override
        public void onStatusChanged(String status) {
            Platform.runLater(() -> statusLabel.setText(status));
        }

        @Override
        // Refreshes the legacy display roster.
        public void onPlayersChanged(List<String> players) {
            Platform.runLater(() -> playerList.getItems().setAll(players));
        }

        @Override
        // Saves typed roster data for deterministic game setup.
        public void onPlayerInfosChanged(List<LanPlayerInfo> players) {
            Platform.runLater(() -> currentPlayers = new ArrayList<>(players));
        }

        @Override
        // Refreshes room counts and start-button availability.
        public void onRoomStateChanged(LanRoomState roomState) {
            Platform.runLater(() -> {
                currentRoomState = roomState;
                statusLabel.setText(roomState.toSummary());
                setConnectedUi(client != null && client.isConnected());
            });
        }

        @Override
        // Opens the game table once the server broadcasts the shared seed.
        public void onGameStarted(long deckSeed, int aiCount) {
            Platform.runLater(() -> {
                startNetworkGameView(deckSeed, aiCount);
            });
        }

        @Override
        // Routes game messages into the active table when one exists.
        public void onGameMessage(LanGameMessage message) {
            Platform.runLater(() -> {
                if (gameMessageHandler != null) {
                    gameMessageHandler.accept(message);
                } else {
                    appendLog("Game " + message.getType()
                            + " from " + message.getSenderName()
                            + ": " + message.getPayload());
                }
            });
        }

        // Builds player setups from the network roster plus AI seats.
        private void startNetworkGameView(long fallbackSeed, int aiCount) {
            long seed = fallbackSeed;
            appendLog("Start signal received. Opening game table.");
            if (client == null || currentPlayers.isEmpty()) {
                appendLog("Cannot open game table yet: player roster is not ready.");
                return;
            }
            ready = false;
            client.setReady(false);

            List<LanPlayerInfo> sortedPlayers = new ArrayList<>(currentPlayers);
            sortedPlayers.sort(java.util.Comparator.comparingInt(LanPlayerInfo::getPlayerId));
            List<GameManager.PlayerSetup> setups = new ArrayList<>();
            int localPlayerIndex = 0;
            for (int i = 0; i < sortedPlayers.size(); i++) {
                LanPlayerInfo player = sortedPlayers.get(i);
                setups.add(new GameManager.PlayerSetup(player.getPlayerName(), PlayerType.HUMAN));
                if (player.getPlayerId() == client.getPlayerId()) {
                    localPlayerIndex = i;
                }
            }
            // AI seats are appended after online humans so all clients agree.
            for (int i = 1; i <= aiCount && setups.size() < GameManager.MAX_PLAYERS; i++) {
                setups.add(new GameManager.PlayerSetup("AI " + i, PlayerType.AI));
            }
            gameStartAction.start(setups, seed, localPlayerIndex);
        }

        @Override
        // Updates the status label during automatic reconnect attempts.
        public void onReconnecting(int attempt, int maxAttempts) {
            Platform.runLater(() -> statusLabel.setText(
                    "Reconnecting " + attempt + "/" + maxAttempts + "..."));
        }

        @Override
        // Appends server and client log messages.
        public void onLogMessage(String message) {
            Platform.runLater(() -> appendLog(message));
        }

        @Override
        // Resets lobby UI after a final disconnect.
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
