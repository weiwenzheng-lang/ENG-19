package network;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class LanNetworkSmokeVerifier {
    private static final long TIMEOUT_MS = 6000;

    public static void main(String[] args) throws Exception {
        verifyProtocolRoundTrip();

        List<String> serverLog = new CopyOnWriteArrayList<>();
        LanGameServer server = new LanGameServer(0, serverLog::add);
        server.start();
        int port = server.getPort();

        TestListener aliceListener = new TestListener("Alice");
        TestListener bobListener = new TestListener("Bob");
        TestListener charlieListener = new TestListener("Charlie");
        LanGameClient alice = new LanGameClient(aliceListener);
        LanGameClient bob = new LanGameClient(bobListener);
        LanGameClient charlie = new LanGameClient(charlieListener);

        try {
            alice.connect("127.0.0.1", port, "Alice");
            await("Alice receives welcome", () -> alice.getPlayerId() == 1);
            bob.connect("127.0.0.1", port, "Bob");
            await("both players are visible", () ->
                    aliceListener.latestRoomState != null
                            && aliceListener.latestRoomState.getOnlineCount() == 2
                            && bobListener.latestRoomState != null
                            && bobListener.latestRoomState.getOnlineCount() == 2);
            await("stable player ids and host assignment", () ->
                    alice.getPlayerId() == 1
                            && bob.getPlayerId() == 2
                            && aliceListener.latestRoomState != null
                            && aliceListener.latestRoomState.getHostPlayerId() == 1);

            bob.requestStartGame();
            await("non-host start is rejected", () -> bobListener.containsLog("Only the host can start"));
            alice.requestStartGame();
            await("unready start is rejected", () -> aliceListener.containsLog("every online player must be ready"));

            bob.sendChat("hello from Bob");
            await("chat is broadcast to Alice", () -> aliceListener.containsLog("Bob: hello from Bob"));
            await("chat is broadcast to Bob", () -> bobListener.containsLog("Bob: hello from Bob"));

            alice.setReady(true);
            bob.setReady(true);
            await("both players are ready", () ->
                    aliceListener.latestRoomState != null
                            && aliceListener.latestRoomState.getReadyCount() == 2
                            && bobListener.latestRoomState != null
                            && bobListener.latestRoomState.getReadyCount() == 2);

            alice.requestStartGame();
            check(aliceListener.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "Alice should receive START_GAME");
            check(bobListener.started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "Bob should receive START_GAME");
            await("room state reports game started", () ->
                    aliceListener.latestRoomState != null
                            && aliceListener.latestRoomState.isGameStarted()
                            && bobListener.latestRoomState != null
                            && bobListener.latestRoomState.isGameStarted());

            alice.sendGameAction("LOBBY_TEST", "payload-123");
            await("game action reaches Bob", () -> bobListener.hasGameMessage(1, "Alice", "LOBBY_TEST", "payload-123"));
            bob.sendGameState("STATE_SYNC", "board=ok;unicode=玩家");
            await("game state reaches Alice", () -> aliceListener.hasGameMessage(2, "Bob", "STATE_SYNC", "board=ok;unicode=玩家"));

            charlie.connect("127.0.0.1", port, "Charlie");
            await("Charlie joins", () ->
                    aliceListener.latestRoomState != null
                            && aliceListener.latestRoomState.getOnlineCount() == 3
                            && charlie.getPlayerId() == 3);
            forceSocketClose(charlie);
            await("Charlie auto-reconnects", () -> charlieListener.containsLog("Reconnected to room."));
            check(charlie.getPlayerId() == 3, "Reconnect should keep Charlie's player id");

            bob.disconnect();
            await("Bob leaves cleanly", () ->
                    aliceListener.latestRoomState != null
                            && aliceListener.latestRoomState.getOnlineCount() == 2
                            && aliceListener.latestRoomState.getPlayerCount() == 2);

            System.out.println("LAN network smoke verification passed.");
        } finally {
            alice.disconnect();
            bob.disconnect();
            charlie.disconnect();
            server.stop();
        }
    }

    private static void verifyProtocolRoundTrip() {
        String line = LanGameProtocol.line("TEST", "plain", "tabs\tinside", "unicode 玩家", "");
        LanGameProtocol.Message message = LanGameProtocol.parse(line);
        check("TEST".equals(message.getCommand()), "Protocol command should round-trip");
        check(message.getFields().size() == 4, "Protocol field count should round-trip");
        check("tabs\tinside".equals(message.field(1)), "Protocol should preserve tabs inside fields");
        check("unicode 玩家".equals(message.field(2)), "Protocol should preserve unicode");
        check("".equals(message.field(3)), "Protocol should preserve empty fields");
    }

    private static void forceSocketClose(LanGameClient client) throws ReflectiveOperationException, IOException {
        Field socketField = LanGameClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        Socket socket = (Socket) socketField.get(client);
        socket.close();
    }

    private static void await(String label, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for " + label);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestListener implements LanGameListener {
        private final String name;
        private final List<String> logs = new CopyOnWriteArrayList<>();
        private final List<List<String>> playerSnapshots = new CopyOnWriteArrayList<>();
        private final List<LanGameMessage> gameMessages = new CopyOnWriteArrayList<>();
        private final CountDownLatch started = new CountDownLatch(1);
        private volatile LanRoomState latestRoomState;
        private volatile String status;

        private TestListener(String name) {
            this.name = name;
        }

        @Override
        public void onStatusChanged(String status) {
            this.status = status;
            logs.add(name + " status: " + status);
        }

        @Override
        public void onPlayersChanged(List<String> players) {
            playerSnapshots.add(new ArrayList<>(players));
        }

        @Override
        public void onRoomStateChanged(LanRoomState roomState) {
            latestRoomState = roomState;
        }

        @Override
        public void onGameStarted() {
            started.countDown();
        }

        @Override
        public void onGameMessage(LanGameMessage message) {
            gameMessages.add(message);
        }

        @Override
        public void onReconnecting(int attempt, int maxAttempts) {
            logs.add(name + " reconnecting " + attempt + "/" + maxAttempts);
        }

        @Override
        public void onLogMessage(String message) {
            logs.add(message);
        }

        @Override
        public void onDisconnected() {
            logs.add(name + " disconnected");
        }

        private boolean containsLog(String text) {
            return logs.stream().anyMatch(log -> log.contains(text));
        }

        private boolean hasGameMessage(int senderId, String senderName, String type, String payload) {
            return gameMessages.stream().anyMatch(message ->
                    message.getSenderId() == senderId
                            && Objects.equals(message.getSenderName(), senderName)
                            && Objects.equals(message.getType(), type)
                            && Objects.equals(message.getPayload(), payload));
        }
    }
}
