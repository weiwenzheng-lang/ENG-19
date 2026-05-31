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

    public LanGameClient(LanGameListener listener) {
        this.listener = listener;
    }

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

    public void sendChat(String text) {
        String message = text == null ? "" : text.trim();
        if (!message.isEmpty()) {
            sendLine(LanGameProtocol.line(LanGameProtocol.CHAT, message));
        }
    }

    public void setReady(boolean ready) {
        sendLine(LanGameProtocol.line(LanGameProtocol.READY, String.valueOf(ready)));
    }

    public void requestStartGame() {
        sendLine(LanGameProtocol.line(LanGameProtocol.START_GAME));
    }

    public void sendGameAction(String type, String payload) {
        sendLine(LanGameProtocol.line(LanGameProtocol.GAME_ACTION, type, payload));
    }

    public void sendGameState(String type, String payload) {
        sendLine(LanGameProtocol.line(LanGameProtocol.GAME_STATE, type, payload));
    }

    public boolean isConnected() {
        return connected;
    }

    public int getPlayerId() {
        return playerId;
    }

    @Override
    public void close() {
        disconnect();
    }

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

    private void startReader() {
        readThread = new Thread(this::readLoop, "lan-room-reader");
        readThread.setDaemon(true);
        readThread.start();
    }

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
            if (shouldReconnect) {
                reconnectLoop();
            } else {
                notifyDisconnected();
            }
        }
    }

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

    private void handleLine(String line) {
        LanGameProtocol.Message message = LanGameProtocol.parse(line);
        String command = message.getCommand();
        if (LanGameProtocol.WELCOME.equals(command)) {
            handleWelcome(message);
        } else if (LanGameProtocol.PLAYERS.equals(command)) {
            notifyPlayers(toPlayerList(message.getFields()));
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
            notifyGameStarted();
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

    private void handleWelcome(LanGameProtocol.Message message) {
        playerId = readInt(message.getFields(), 0, -1);
        if (message.getFields().size() > 1) {
            reconnectToken = message.field(1);
        }
        notifyStatus("In room as player #" + playerId);
    }

    private List<String> toPlayerList(List<String> fields) {
        List<String> players = new ArrayList<>();
        for (LanPlayerInfo player : toPlayerInfos(fields)) {
            players.add(player.toDisplayText());
        }
        return players;
    }

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

    private LanRoomState toRoomState(List<String> fields) {
        return new LanRoomState(
                fields.size() > 0 && Boolean.parseBoolean(fields.get(0)),
                readInt(fields, 1, -1),
                readInt(fields, 2, 0),
                readInt(fields, 3, 0),
                readInt(fields, 4, 0));
    }

    private LanGameMessage toGameMessage(LanGameProtocol.Message message) {
        List<String> fields = message.getFields();
        return new LanGameMessage(
                readInt(fields, 0, -1),
                fields.size() > 1 ? fields.get(1) : "Player",
                fields.size() > 2 ? fields.get(2) : "UNKNOWN",
                fields.size() > 3 ? fields.get(3) : "");
    }

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

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void stopHeartbeat() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
    }

    private void notifyStatus(String status) {
        if (listener != null) {
            listener.onStatusChanged(status);
        }
    }

    private void notifyPlayers(List<String> players) {
        if (listener != null) {
            listener.onPlayersChanged(players);
        }
    }

    private void notifyRoomState(LanRoomState roomState) {
        if (listener != null) {
            listener.onRoomStateChanged(roomState);
        }
    }

    private void notifyGameStarted() {
        if (listener != null) {
            listener.onGameStarted();
        }
    }

    private void notifyGameMessage(LanGameMessage message) {
        if (listener != null) {
            listener.onGameMessage(message);
        }
    }

    private void notifyReconnecting(int attempt, int maxAttempts) {
        if (listener != null) {
            listener.onReconnecting(attempt, maxAttempts);
        }
    }

    private void notifyLog(String message) {
        if (listener != null) {
            listener.onLogMessage(message);
        }
    }

    private void notifyDisconnected() {
        if (listener != null) {
            listener.onStatusChanged("Disconnected");
            listener.onDisconnected();
        }
    }
}
