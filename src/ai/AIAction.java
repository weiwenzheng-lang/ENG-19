package ai;

import core.TargetInfo;
import enums.PropertyColor;

/**
 * AI 决策结果：表示 AI 想要执行的单个游戏动作。
 * 纯粹的数据载体，由 AIPlayerBrain 产生，由 AITurnExecutor 执行。
 */
public class AIAction {

    public enum Type {
        /** 打出卡牌（含房产、行动卡等，消耗 1 行动） */
        PLAY_CARD,
        /** 将卡牌存入银行（消耗 1 行动） */
        DEPOSIT_TO_BANK,
        /** 弃掉手牌（不消耗行动，仅手牌 > 7 时使用） */
        DISCARD,
        /** 双倍租金 + 租金卡组合（消耗 2 行动） */
        PLAY_DOUBLE_RENT,
        /** 结束当前回合 */
        END_TURN
    }

    private final Type type;
    private final int cardIndex;
    private final int rentCardIndex;
    private final TargetInfo targetInfo;
    private final PropertyColor selectedColor;

    public AIAction(Type type) {
        this(type, -1, -1, null, null);
    }

    public AIAction(Type type, int cardIndex) {
        this(type, cardIndex, -1, null, null);
    }

    public AIAction(Type type, int cardIndex, TargetInfo targetInfo) {
        this(type, cardIndex, -1, targetInfo, null);
    }

    public AIAction(Type type, int cardIndex, int rentCardIndex, TargetInfo targetInfo) {
        this(type, cardIndex, rentCardIndex, targetInfo, null);
    }

    public AIAction(Type type, int cardIndex, TargetInfo targetInfo, PropertyColor selectedColor) {
        this(type, cardIndex, -1, targetInfo, selectedColor);
    }

    public AIAction(Type type, int cardIndex, int rentCardIndex, TargetInfo targetInfo, PropertyColor selectedColor) {
        this.type = type;
        this.cardIndex = cardIndex;
        this.rentCardIndex = rentCardIndex;
        this.targetInfo = targetInfo;
        this.selectedColor = selectedColor;
    }

    public Type getType() { return type; }
    public int getCardIndex() { return cardIndex; }
    public int getRentCardIndex() { return rentCardIndex; }
    public TargetInfo getTargetInfo() { return targetInfo; }
    public PropertyColor getSelectedColor() { return selectedColor; }
}
