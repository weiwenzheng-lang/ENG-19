package ai;

import core.TargetInfo;
import enums.PropertyColor;

// Represents one action selected by the AI brain.
public class AIAction {
    public enum Type {
        // Play one card from hand.
        PLAY_CARD,
        // Move one card into the bank.
        DEPOSIT_TO_BANK,
        // Discard a card when the hand is over the limit.
        DISCARD,
        // Play Double The Rent together with a rent card.
        PLAY_DOUBLE_RENT,
        // End the current AI turn.
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

    public AIAction(Type type, int cardIndex, int rentCardIndex,
                    TargetInfo targetInfo, PropertyColor selectedColor) {
        this.type = type;
        this.cardIndex = cardIndex;
        this.rentCardIndex = rentCardIndex;
        this.targetInfo = targetInfo;
        this.selectedColor = selectedColor;
    }

    public Type getType() {
        return type;
    }

    public int getCardIndex() {
        return cardIndex;
    }

    public int getRentCardIndex() {
        return rentCardIndex;
    }

    public TargetInfo getTargetInfo() {
        return targetInfo;
    }

    public PropertyColor getSelectedColor() {
        return selectedColor;
    }
}
