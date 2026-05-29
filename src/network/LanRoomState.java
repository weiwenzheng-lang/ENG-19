package network;

public final class LanRoomState {
    private final boolean gameStarted;
    private final int hostPlayerId;
    private final int playerCount;
    private final int onlineCount;
    private final int readyCount;

    public LanRoomState(boolean gameStarted, int hostPlayerId, int playerCount, int onlineCount, int readyCount) {
        this.gameStarted = gameStarted;
        this.hostPlayerId = hostPlayerId;
        this.playerCount = playerCount;
        this.onlineCount = onlineCount;
        this.readyCount = readyCount;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public int getHostPlayerId() {
        return hostPlayerId;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    public int getReadyCount() {
        return readyCount;
    }

    public String toSummary() {
        return "Players: " + onlineCount + "/" + playerCount
                + " online, ready: " + readyCount + "/" + onlineCount
                + (gameStarted ? ", game started" : ", waiting");
    }
}
