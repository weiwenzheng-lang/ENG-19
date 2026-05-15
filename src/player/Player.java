package player;
import cards.Card;

public class Player {
    private String playerId;
    private String playerName;

    private Hand hand;
    private BankArea bankArea;
    private PropertyArea propertyArea;

    public Player(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.hand = new Hand();
        this.bankArea = new BankArea();
        this.bankArea.setOwner(this);
        this.propertyArea = new PropertyArea();
    }

    public String getPlayerName() { return playerName; }
    public Hand getHand() { return hand; }
    public BankArea getBankArea() { return bankArea; }
    public PropertyArea getPropertyArea() { return propertyArea; }

    // 打牌逻辑：这个方法会被 GameManager 调用
    public void playCard(Card card) {
        card.executePlayLogic(this);
    }

    @Override
    public String toString() {
        return playerName;
    }
}
