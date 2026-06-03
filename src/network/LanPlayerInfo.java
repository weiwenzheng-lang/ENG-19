package network;

public final class LanPlayerInfo {
    private final int playerId;
    private final String playerName;
    private final boolean ready;
    private final boolean online;
    private final boolean host;

    // Stores one player's lobby identity and current state.
    public LanPlayerInfo(int playerId, String playerName, boolean ready, boolean online, boolean host) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.ready = ready;
        this.online = online;
        this.host = host;
    }

    // Returns the stable room id for this player.
    public int getPlayerId() {
        return playerId;
    }

    // Returns the current display name.
    public String getPlayerName() {
        return playerName;
    }

    // Reports whether this player is ready.
    public boolean isReady() {
        return ready;
    }

    // Reports whether this player is currently connected.
    public boolean isOnline() {
        return online;
    }

    // Reports whether this player is the current room host.
    public boolean isHost() {
        return host;
    }

    // Builds a compact lobby display string.
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
