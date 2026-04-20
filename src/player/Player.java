package player;
import cards.Card;

public class Player {
    private String playerId;
    private String playerName;
    private int actionsRemaining;

    // 玩家的三大核心区域 (组合关系)
    private Hand hand;
    private BankArea bankArea;
    private PropertyArea propertyArea;

    public Player(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.hand = new Hand();
        this.bankArea = new BankArea();
        this.propertyArea = new PropertyArea();
    }

    public String getPlayerName() { return playerName; }

    public void playCard(Card card) {
        // 基础打牌逻辑入口
        card.executePlayLogic(this);
        actionsRemaining--;
    }

    // ... 其他 getter 和核心方法 ...
    // 在 player.Player 类中补充这些获取区域的方法
    public PropertyArea getPropertyArea() {
        return propertyArea;
    }

    public BankArea getBankArea() {
        return bankArea;
    }

    public Hand getHand() {
        return hand;
    }
}