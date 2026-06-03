package network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LanGameClient implements Closeable {
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    private final LanGameListener listener;
    // Serializes writes from UI actions, heartbeats, and reconnect handling.
    private final Object writeLock = new Object();

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread readThread;
    private ScheduledExecutorService heartbeat;
    private volatile boolean connected;
    private volatile boolean manuallyClosed;
    private volatile boolean reconnecting;
    private String host;
    private int port;
    private String playerName;
    private String reconnectToken = "";
    private int playerId = -1;

    // Creates a client that reports all room events through the listener.
    public LanGameClient(LanGameListener listener) {
        this.listener = listener;
    }

    // Opens the room socket and sends the first HELLO message.
    public void connect(String host, int port, String playerName) throws IOException {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }

        this.host = host;
        this.port = port;
        this.playerName = playerName;
        manuallyClosed = false;
        openConnection();
        notifyStatus("Connected to " + host + ":" + port);
    }

    // Sends a non-empty chat message to the server.
    public void sendChat(String text) {
        String message = text == null ? "" : text.trim();
        if (!message.isEmpty()) {
            sendLine(LanGameProtocol.line(LanGameProtocol.CHAT, message));
        }
    }

    // Updates this player's ready state in the lobby.
    public void setReady(boolean ready) {
        sendLine(LanGameProtocol.line(LanGameProtocol.READY, String.valueOf(ready)));
    }

    // Requests a human-only network game.
    public void requestStartGame() {
        requestStartGame(0);
    }

    // Requests a network game with extra local AI seats.
    public void requestStartGame(int aiCount) {
        sendLine(LanGameProtocol.line(LanGameProtocol.START_GAME, String.valueOf(Math.max(0, aiCount))));
    }

    // Sends an action that should be applied by the game table.
    public void sendGameAction(String type, String payload) {
        sendLine(LanGameProtocol.line(LanGameProtocol.GAME_ACTION, type, payload));
    }

    // Sends state synchronization data from the game table.
    public void sendGameState(String type, String payload) {
        sendLine(LanGameProtocol.line(LanGameProtocol.GAME_STATE, type, payload));
    }

    // Reports whether the current socket is active.
    public boolean isConnected() {
        return connected;
    }

    // Returns the room-assigned player id.
    public int getPlayerId() {
        return playerId;
    }

    @Override
    public void close() {
        disconnect();
    }

    // Leaves the room and closes all client-side resources.
    public void disconnect() {
        manuallyClosed = true;
        if (!connected) {
            closeSocket();
            stopHeartbeat();
            return;
        }

        sendLine(LanGameProtocol.line(LanGameProtocol.GOODBYE));
        connected = false;
        closeSocket();
        stopHeartbeat();
        notifyDisconnected();
    }

    // Opens the TCP connection and restarts the listener services.
    private void openConnection() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        connected = true;
        sendLine(LanGameProtocol.line(LanGameProtocol.HELLO, playerName, reconnectToken));
        startReader();
        startHeartbeat();
    }

    // Starts the background reader thread.
    private void startReader() {
        readThread = new Thread(this::readLoop, "lan-room-reader");
        readThread.setDaemon(true);
        readThread.start();
    }

    // Sends periodic PING frames so broken sockets are detected quickly.
    private void startHeartbeat() {
        stopHeartbeat();
        heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lan-room-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleAtFixedRate(() -> {
            if (connected) {
                sendLine(LanGameProtocol.line(LanGameProtocol.PING));
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    // Reads server messages until the socket closes.
    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            if (connected) {
                notifyLog("Connection closed: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            notifyLog("Bad server message: " + e.getMessage());
        } finally {
            boolean shouldReconnect = connected && !manuallyClosed;
            connected = false;
            closeSocket();
            stopHeartbeat();
            // A non-manual close is treated as a recoverable network drop.
            if (shouldReconnect) {
                reconnectLoop();
            } else {
                notifyDisconnected();
            }
        }
    }

    // Attempts a short reconnect sequence using the saved reconnect token.
    private void reconnectLoop() {
        if (reconnecting) {
            return;
        }

        reconnecting = true;
        for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS && !manuallyClosed; attempt++) {
            notifyReconnecting(attempt, MAX_RECONNECT_ATTEMPTS);
            try {
                Thread.sleep(Math.min(1000L * attempt, 4000L));
                openConnection();
                reconnecting = false;
                notifyStatus("Reconnected");
                notifyLog("Reconnected to room.");
                return;
            } catch (IOException e) {
                notifyLog("Reconnect attempt " + attempt + " failed: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        reconnecting = false;
        notifyDisconnected();
    }

    // Routes a parsed protocol line to the listener callback model.
    private void handleLine(String line) {
        LanGameProtocol.Message message = LanGameProtocol.parse(line);
        String command = message.getCommand();
        if (LanGameProtocol.WELCOME.equals(command)) {
            handleWelcome(message);
        } else if (LanGameProtocol.PLAYERS.equals(command)) {
            List<LanPlayerInfo> playerInfos = toPlayerInfos(message.getFields());
            notifyPlayerInfos(playerInfos);
            notifyPlayers(toPlayerList(playerInfos));
        } else if (LanGameProtocol.ROOM_STATE.equals(command)) {
            notifyRoomState(toRoomState(message.getFields()));
        } else if (LanGameProtocol.INFO.equals(command)) {
            notifyLog(message.getFields().isEmpty() ? "" : message.field(0));
        } else if (LanGameProtocol.CHAT.equals(command)) {
            String sender = message.getFields().size() > 0 ? message.field(0) : "Player";
            String text = message.getFields().size() > 1 ? message.field(1) : "";
            notifyLog(sender + ": " + text);
        } else if (LanGameProtocol.START_GAME.equals(command)) {
            notifyLog("Network game started.");
            long seed = readLong(message.getFields(), 1, System.currentTimeMillis());
            int aiCount = readInt(message.getFields(), 2, 0);
            notifyGameStarted(seed, aiCount);
        } else if (LanGameProtocol.GAME_ACTION.equals(command) || LanGameProtocol.GAME_STATE.equals(command)) {
            notifyGameMessage(toGameMessage(message));
        } else if (LanGameProtocol.PONG.equals(command)) {
            notifyStatus("Connected");
        } else if (LanGameProtocol.ERROR.equals(command)) {
            notifyLog("Server error: " + (message.getFields().isEmpty() ? "" : message.field(0)));
        } else {
            notifyLog("Unknown server message: " + command);
        }
    }

    // Saves the server-assigned identity and reconnect token.
    private void handleWelcome(LanGameProtocol.Message message) {
        playerId = readInt(message.getFields(), 0, -1);
        if (message.getFields().size() > 1) {
            reconnectToken = message.field(1);
        }
        notifyStatus("In room as player #" + playerId);
    }

    // Converts typed player info into display strings for older UI paths.
    private List<String> toPlayerList(List<LanPlayerInfo> playerInfos) {
        List<String> players = new ArrayList<>();
        for (LanPlayerInfo player : playerInfos) {
            players.add(player.toDisplayText());
        }
        return players;
    }

    // Parses the flat PLAYERS payload into player models.
    private List<LanPlayerInfo> toPlayerInfos(List<String> fields) {
        List<LanPlayerInfo> players = new ArrayList<>();
        for (int i = 0; i + 4 < fields.size(); i += 5) {
            players.add(new LanPlayerInfo(
                    readInt(fields, i, -1),
                    fields.get(i + 1),
                    Boolean.parseBoolean(fields.get(i + 2)),
                    Boolean.parseBoolean(fields.get(i + 3)),
                    Boolean.parseBoolean(fields.get(i + 4))));
        }
        return players;
    }

    // Parses room-level state counts and host identity.
    private LanRoomState toRoomState(List<String> fields) {
        return new LanRoomState(
                fields.size() > 0 && Boolean.parseBoolean(fields.get(0)),
                readInt(fields, 1, -1),
                readInt(fields, 2, 0),
                readInt(fields, 3, 0),
                readInt(fields, 4, 0));
    }

    // Parses a table message sent through the room relay.
    private LanGameMessage toGameMessage(LanGameProtocol.Message message) {
        List<String> fields = message.getFields();
        return new LanGameMessage(
                readInt(fields, 0, -1),
                fields.size() > 1 ? fields.get(1) : "Player",
                fields.size() > 2 ? fields.get(2) : "UNKNOWN",
                fields.size() > 3 ? fields.get(3) : "");
    }

    // Reads an integer field without failing the socket loop.
    private int readInt(List<String> fields, int index, int fallback) {
        if (index >= fields.size()) {
            return fallback;
        }
        try {
            return Integer.parseInt(fields.get(index));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Reads a long field without failing the socket loop.
    private long readLong(List<String> fields, int index, long fallback) {
        if (index >= fields.size()) {
            return fallback;
        }
        try {
            return Long.parseLong(fields.get(index));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Writes one protocol frame and triggers reconnect on write failure.
    private void sendLine(String line) {
        synchronized (writeLock) {
            if (writer == null) {
                return;
            }
            writer.println(line);
            if (writer.checkError() && connected) {
                connected = false;
                closeSocket();
                stopHeartbeat();
                if (!manuallyClosed) {
                    reconnectLoop();
                } else {
                    notifyDisconnected();
                }
            }
        }
    }

    // Closes the socket quietly during normal cleanup paths.
    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    // Stops the heartbeat executor when no socket is active.
    private void stopHeartbeat() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
    }

    // Reports a status label update to the UI layer.
    private void notifyStatus(String status) {
        if (listener != null) {
            listener.onStatusChanged(status);
        }
    }

    // Reports display-ready player names.
    private void notifyPlayers(List<String> players) {
        if (listener != null) {
            listener.onPlayersChanged(players);
        }
    }

    // Reports typed player info for game start setup.
    private void notifyPlayerInfos(List<LanPlayerInfo> players) {
        if (listener != null) {
            listener.onPlayerInfosChanged(players);
        }
    }

    // Reports room counts and host identity.
    private void notifyRoomState(LanRoomState roomState) {
        if (listener != null) {
            listener.onRoomStateChanged(roomState);
        }
    }

    // Reports the deterministic deck seed and AI seat count.
    private void notifyGameStarted(long deckSeed, int aiCount) {
        if (listener != null) {
            listener.onGameStarted(deckSeed, aiCount);
        }
    }

    // Reports a relayed game action or state message.
    private void notifyGameMessage(LanGameMessage message) {
        if (listener != null) {
            listener.onGameMessage(message);
        }
    }

    // Reports the current reconnect attempt to the UI layer.
    private void notifyReconnecting(int attempt, int maxAttempts) {
        if (listener != null) {
            listener.onReconnecting(attempt, maxAttempts);
        }
    }

    // Appends a lobby log message.
    private void notifyLog(String message) {
        if (listener != null) {
            listener.onLogMessage(message);
        }
    }

    // Reports a final disconnected state.
    private void notifyDisconnected() {
        if (listener != null) {
            listener.onStatusChanged("Disconnected");
            listener.onDisconnected();
        }
    }
}
