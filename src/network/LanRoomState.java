package network;

public final class LanRoomState {
    private final boolean gameStarted;
    private final int hostPlayerId;
    private final int playerCount;
    private final int onlineCount;
    private final int readyCount;

    // Stores lobby-wide counts and game-start state.
    public LanRoomState(boolean gameStarted, int hostPlayerId, int playerCount, int onlineCount, int readyCount) {
        this.gameStarted = gameStarted;
        this.hostPlayerId = hostPlayerId;
        this.playerCount = playerCount;
        this.onlineCount = onlineCount;
        this.readyCount = readyCount;
    }

    // Reports whether the host already started the table.
    public boolean isGameStarted() {
        return gameStarted;
    }

    // Returns the room id of the current host.
    public int getHostPlayerId() {
        return hostPlayerId;
    }

    // Returns all remembered player sessions.
    public int getPlayerCount() {
        return playerCount;
    }

    // Returns players with active sockets.
    public int getOnlineCount() {
        return onlineCount;
    }

    // Returns online players marked ready.
    public int getReadyCount() {
        return readyCount;
    }

    // Builds the lobby status label text.
    public String toSummary() {
        return "Players: " + onlineCount + "/" + playerCount
                + " online, ready: " + readyCount + "/" + onlineCount
                + (gameStarted ? ", game started" : ", waiting");
    }
}
