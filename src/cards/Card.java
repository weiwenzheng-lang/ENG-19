package cards;
import player.Player;

public abstract class Card {
    private int cardId;
    private String cardName;
    private int monetaryValue;

    public Card(int cardId, String cardName, int monetaryValue) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.monetaryValue = monetaryValue;
    }

    public int getMonetaryValue() { return monetaryValue; }
    public String getCardName() { return cardName; }

    // 抽象方法：每种卡牌被打出时的具体逻辑不同，交由子类实现
    public abstract void executePlayLogic(Player initiator);

    /** 是否需要选择目标对手（SlyDeal/ForceDeal/DealBreaker/DebtCollector） */
    public boolean requiresTarget() { return false; }
}