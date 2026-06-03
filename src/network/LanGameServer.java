package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;

public class LanGameServer {
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 5;

    private final int requestedPort;
    private final Consumer<String> logSink;
    private final List<PlayerSession> sessions = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private volatile boolean running;
    private volatile boolean gameStarted;
    private volatile long gameSeed;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    // Creates a room server that writes operational messages to the sink.
    public LanGameServer(int port, Consumer<String> logSink) {
        this.requestedPort = port;
        this.logSink = logSink == null ? message -> { } : logSink;
    }

    // Starts accepting lobby clients.
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(requestedPort);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "lan-room-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        logSink.accept("Room server started on port " + getPort() + ".");
    }

    // Stops the listener socket and every active client handler.
    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;
        closeServerSocket();
        for (PlayerSession session : sessions) {
            ClientHandler handler = session.handler;
            if (handler != null) {
                handler.close();
            }
        }
        sessions.clear();
        logSink.accept("Room server stopped.");
    }

    // Returns the bound port, including dynamic port 0 assignments.
    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? requestedPort : socket.getLocalPort();
    }

    // Accepts sockets and delegates each one to its own handler thread.
    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientHandler handler = new ClientHandler(socket);
                Thread thread = new Thread(handler, "lan-room-client");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                if (running) {
                    logSink.accept("Server accept failed: " + e.getMessage());
                }
            }
        }
    }

    // Closes the server socket during shutdown.
    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    // Sends one protocol frame to every online session.
    private void broadcast(String command, String... fields) {
        String line = LanGameProtocol.line(command, fields);
        for (PlayerSession session : sessions) {
            ClientHandler handler = session.handler;
            if (session.online && handler != null) {
                handler.sendLine(line);
            }
        }
    }

    // Refreshes both lobby roster and room summary.
    private void broadcastPlayersAndRoomState() {
        broadcastPlayers();
        broadcastRoomState();
    }

    // Publishes the complete player roster to every connected client.
    private void broadcastPlayers() {
        List<String> fields = new ArrayList<>();
        int hostPlayerId = getHostPlayerId();
        for (PlayerSession session : sessions) {
            fields.add(String.valueOf(session.playerId));
            fields.add(session.playerName);
            fields.add(String.valueOf(session.ready));
            fields.add(String.valueOf(session.online));
            fields.add(String.valueOf(session.playerId == hostPlayerId));
        }
        broadcast(LanGameProtocol.PLAYERS, fields.toArray(new String[0]));
    }

    // Publishes game-start state and ready counts.
    private void broadcastRoomState() {
        int onlineCount = 0;
        int readyCount = 0;
        for (PlayerSession session : sessions) {
            if (session.online) {
                onlineCount++;
                if (session.ready) {
                    readyCount++;
                }
            }
        }
        broadcast(LanGameProtocol.ROOM_STATE,
                String.valueOf(gameStarted),
                String.valueOf(getHostPlayerId()),
                String.valueOf(sessions.size()),
                String.valueOf(onlineCount),
                String.valueOf(readyCount));
    }

    // Treats the first online player as the current room host.
    private int getHostPlayerId() {
        for (PlayerSession session : sessions) {
            if (session.online) {
                return session.playerId;
            }
        }
        return sessions.isEmpty() ? -1 : sessions.get(0).playerId;
    }

    // Normalizes user-entered names before they are broadcast.
    private String cleanPlayerName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            name = "Player";
        }
        return name.length() > 24 ? name.substring(0, 24) : name;
    }

    // Finds a disconnected session that can be resumed.
    private PlayerSession findSessionByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        for (PlayerSession session : sessions) {
            if (session.reconnectToken.equals(token)) {
                return session;
            }
        }
        return null;
    }

    // Requires all online humans to be ready and total seats to be 2-5.
    private boolean areOnlinePlayersReady(int aiCount) {
        int onlineCount = 0;
        for (PlayerSession session : sessions) {
            if (!session.online) {
                continue;
            }
            onlineCount++;
            if (!session.ready) {
                return false;
            }
        }
        int totalPlayers = onlineCount + Math.max(0, aiCount);
        return onlineCount >= 1 && totalPlayers >= MIN_PLAYERS && totalPlayers <= MAX_PLAYERS;
    }

    // Owns one socket connection and its associated room session.
    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private PlayerSession session;
        private boolean registered;
        private boolean leavingRoom;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // Performs HELLO registration, then processes client messages.
        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                String firstLine = reader.readLine();
                LanGameProtocol.Message hello = LanGameProtocol.parse(firstLine);
                if (!LanGameProtocol.HELLO.equals(hello.getCommand()) || hello.getFields().isEmpty()) {
                    send(LanGameProtocol.ERROR, "Expected HELLO as the first message.");
                    return;
                }

                // Registration may create a new session or recover an old one.
                registerSession(hello);

                String line;
                while ((line = reader.readLine()) != null) {
                    handleLine(line);
                }
            } catch (IllegalArgumentException e) {
                send(LanGameProtocol.ERROR, "Bad message: " + e.getMessage());
            } catch (IOException e) {
                if (running && registered && session != null) {
                    logSink.accept(session.playerName + " connection closed: " + e.getMessage());
                }
            } finally {
                markOffline();
                close();
            }
        }

        // Registers a new session or reattaches to a saved reconnect token.
        private void registerSession(LanGameProtocol.Message hello) {
            String playerName = cleanPlayerName(hello.field(0));
            String requestedToken = hello.getFields().size() > 1 ? hello.field(1) : "";
            PlayerSession existing = findSessionByToken(requestedToken);
            if (existing != null) {
                ClientHandler oldHandler = existing.handler;
                if (oldHandler != null && oldHandler != this) {
                    oldHandler.close();
                }
                session = existing;
                session.playerName = playerName;
                session.online = true;
                session.handler = this;
                registered = true;
                send(LanGameProtocol.WELCOME,
                        String.valueOf(session.playerId),
                        session.reconnectToken,
                        String.valueOf(getHostPlayerId()));
                broadcast(LanGameProtocol.INFO, session.playerName + " reconnected.");
                broadcastPlayersAndRoomState();
                return;
            }

            // New users get a durable token for automatic reconnect.
            session = new PlayerSession(nextPlayerId.getAndIncrement(), playerName, UUID.randomUUID().toString());
            session.online = true;
            session.handler = this;
            sessions.add(session);
            registered = true;

            send(LanGameProtocol.WELCOME,
                    String.valueOf(session.playerId),
                    session.reconnectToken,
                    String.valueOf(getHostPlayerId()));
            send(LanGameProtocol.INFO, "Connected to room.");
            broadcast(LanGameProtocol.INFO, session.playerName + " joined the room.");
            broadcastPlayersAndRoomState();
        }

        // Dispatches client protocol messages.
        private void handleLine(String line) {
            LanGameProtocol.Message message = LanGameProtocol.parse(line);
            String command = message.getCommand();
            if (LanGameProtocol.CHAT.equals(command)) {
                handleChat(message);
            } else if (LanGameProtocol.READY.equals(command)) {
                handleReady(message);
            } else if (LanGameProtocol.START_GAME.equals(command)) {
                handleStartGame(message);
            } else if (LanGameProtocol.GAME_ACTION.equals(command)) {
                handleGameMessage(LanGameProtocol.GAME_ACTION, message);
            } else if (LanGameProtocol.GAME_STATE.equals(command)) {
                handleGameMessage(LanGameProtocol.GAME_STATE, message);
            } else if (LanGameProtocol.PING.equals(command)) {
                send(LanGameProtocol.PONG);
            } else if (LanGameProtocol.GOODBYE.equals(command)) {
                leavingRoom = true;
                close();
            } else {
                send(LanGameProtocol.ERROR, "Unknown command: " + command);
            }
        }

        // Rebroadcasts non-empty chat text with the sender name.
        private void handleChat(LanGameProtocol.Message message) {
            String text = message.getFields().isEmpty() ? "" : message.field(0).trim();
            if (!text.isEmpty()) {
                broadcast(LanGameProtocol.CHAT, session.playerName, text);
            }
        }

        // Updates the ready flag and informs the room.
        private void handleReady(LanGameProtocol.Message message) {
            boolean ready = message.getFields().isEmpty() || Boolean.parseBoolean(message.field(0));
            session.ready = ready;
            broadcast(LanGameProtocol.INFO,
                    session.playerName + (ready ? " is ready." : " is not ready."));
            broadcastPlayersAndRoomState();
        }

        // Starts the deterministic game when the host and seat counts are valid.
        private void handleStartGame(LanGameProtocol.Message message) {
            if (session.playerId != getHostPlayerId()) {
                send(LanGameProtocol.ERROR, "Only the host can start the network game.");
                return;
            }
            int aiCount = readInt(message.getFields(), 0, 0);
            if (!areOnlinePlayersReady(aiCount)) {
                send(LanGameProtocol.ERROR,
                        "Need 2 to 5 total players, and every online player must be ready.");
                return;
            }
            gameStarted = true;
            gameSeed = ThreadLocalRandom.current().nextLong();
            broadcast(LanGameProtocol.START_GAME,
                    String.valueOf(session.playerId),
                    String.valueOf(gameSeed),
                    String.valueOf(Math.max(0, aiCount)));
            broadcastRoomState();
        }

        // Reads optional numeric fields without rejecting the client.
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

        // Relays table-level game messages to every online client.
        private void handleGameMessage(String command, LanGameProtocol.Message message) {
            String type = message.getFields().size() > 0 ? message.field(0).trim() : "UNKNOWN";
            String payload = message.getFields().size() > 1 ? message.field(1) : "";
            if (type.isEmpty()) {
                type = "UNKNOWN";
            }
            broadcast(command, String.valueOf(session.playerId), session.playerName, type, payload);
        }

        // Marks this session offline or removes it after an intentional leave.
        private void markOffline() {
            if (!registered || session == null) {
                return;
            }

            registered = false;
            if (session.handler != this) {
                return;
            }

            if (running) {
                if (leavingRoom) {
                    sessions.remove(session);
                    broadcast(LanGameProtocol.INFO, session.playerName + " left the room.");
                } else {
                    session.handler = null;
                    session.online = false;
                    session.ready = false;
                    broadcast(LanGameProtocol.INFO, session.playerName + " disconnected. Waiting for reconnect.");
                }
                broadcastPlayersAndRoomState();
            } else {
                session.handler = null;
            }
        }

        // Encodes and sends one command to this client.
        private void send(String command, String... fields) {
            sendLine(LanGameProtocol.line(command, fields));
        }

        // Writes one already-encoded protocol frame.
        private synchronized void sendLine(String line) {
            if (writer != null) {
                writer.println(line);
            }
        }

        // Closes the client socket quietly.
        private void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Stores durable lobby state separately from transient sockets.
    private static final class PlayerSession {
        private final int playerId;
        private final String reconnectToken;
        private String playerName;
        private boolean ready;
        private boolean online;
        private ClientHandler handler;

        // Creates a reconnectable player session.
        private PlayerSession(int playerId, String playerName, String reconnectToken) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.reconnectToken = reconnectToken;
        }
    }
}
