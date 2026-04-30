package player;
import cards.Card;

public class Player {
    private String playerId;
    private String playerName;
    private int actionsRemaining; // 核心：剩余行动步数

    private Hand hand;
    private BankArea bankArea;
    private PropertyArea propertyArea;

    public Player(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.hand = new Hand();
        this.bankArea = new BankArea();
        this.propertyArea = new PropertyArea();
        this.actionsRemaining = 0;
    }

    // --- 修正爆红的方法 ---
    public void resetActions() {
        this.actionsRemaining = 3; // 每个回合开始重置为 3 点
    }

    public int getActionsRemaining() {
        return actionsRemaining;
    }

    public void useAction() {
        this.actionsRemaining--;
    }

    public String getPlayerName() { return playerName; }
    public Hand getHand() { return hand; }
    public BankArea getBankArea() { return bankArea; }
    public PropertyArea getPropertyArea() { return propertyArea; }

    // 打牌逻辑：这个方法会被 GameManager 调用
    public void playCard(Card card) {
        if (actionsRemaining > 0) {
            card.executePlayLogic(this);
            useAction(); // 每打一张牌扣除一点
        }
    }
}