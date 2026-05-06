package core;

import player.Player;

public class TargetInfo {
    private final Player targetPlayer;

    public TargetInfo(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }
}
