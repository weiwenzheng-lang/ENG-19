package network;

public final class LanPlayerInfo {
    private final int playerId;
    private final String playerName;
    private final boolean ready;
    private final boolean online;
    private final boolean host;

    public LanPlayerInfo(int playerId, String playerName, boolean ready, boolean online, boolean host) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.ready = ready;
        this.online = online;
        this.host = host;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isHost() {
        return host;
    }

    public String toDisplayText() {
        StringBuilder text = new StringBuilder("#")
                .append(playerId)
                .append(" ")
                .append(playerName);
        if (host) {
            text.append(" [Host]");
        }
        text.append(ready ? " [Ready]" : " [Not Ready]");
        if (!online) {
            text.append(" [Offline]");
        }
        return text.toString();
    }
}
