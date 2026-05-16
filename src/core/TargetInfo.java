package core;

import enums.PropertyColor;
import player.Player;

public class TargetInfo {
    private final Player targetPlayer;
    private final PropertyColor initiatorPropertyColor;
    private final int initiatorPropertyIndex;
    private final PropertyColor targetPropertyColor;
    private final int targetPropertyIndex;
    private final PropertyColor improvementColor;

    public TargetInfo(Player targetPlayer) {
        this(targetPlayer, null, -1, null, -1, null);
    }

    public TargetInfo(Player targetPlayer, PropertyColor targetPropertyColor, int targetPropertyIndex) {
        this(targetPlayer, null, -1, targetPropertyColor, targetPropertyIndex, null);
    }

    public TargetInfo(Player targetPlayer, PropertyColor initiatorPropertyColor, int initiatorPropertyIndex,
                      PropertyColor targetPropertyColor, int targetPropertyIndex) {
        this(targetPlayer, initiatorPropertyColor, initiatorPropertyIndex, targetPropertyColor, targetPropertyIndex, null);
    }

    public static TargetInfo forImprovement(PropertyColor improvementColor) {
        return new TargetInfo(null, null, -1, null, -1, improvementColor);
    }

    private TargetInfo(Player targetPlayer, PropertyColor initiatorPropertyColor, int initiatorPropertyIndex,
                       PropertyColor targetPropertyColor, int targetPropertyIndex, PropertyColor improvementColor) {
        this.targetPlayer = targetPlayer;
        this.initiatorPropertyColor = initiatorPropertyColor;
        this.initiatorPropertyIndex = initiatorPropertyIndex;
        this.targetPropertyColor = targetPropertyColor;
        this.targetPropertyIndex = targetPropertyIndex;
        this.improvementColor = improvementColor;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public PropertyColor getInitiatorPropertyColor() {
        return initiatorPropertyColor;
    }

    public int getInitiatorPropertyIndex() {
        return initiatorPropertyIndex;
    }

    public PropertyColor getTargetPropertyColor() {
        return targetPropertyColor;
    }

    public int getTargetPropertyIndex() {
        return targetPropertyIndex;
    }

    public PropertyColor getImprovementColor() {
        return improvementColor;
    }
}
