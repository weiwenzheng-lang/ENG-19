package player;

import cards.Card;

public class Player {
    private final String playerId;
    private final String playerName;
    private final Hand hand;
    private final BankArea bankArea;
    private final PropertyArea propertyArea;
    private PlayerType playerType = PlayerType.HUMAN;

    // Creates a player with empty hand, bank, and property areas.
    public Player(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.hand = new Hand();
        this.bankArea = new BankArea();
        this.bankArea.setOwner(this);
        this.propertyArea = new PropertyArea();
    }

    // Returns the internal player id.
    public String getPlayerId() {
        return playerId;
    }

    // Returns the display name.
    public String getPlayerName() {
        return playerName;
    }

    // Returns this player's hand.
    public Hand getHand() {
        return hand;
    }

    // Returns this player's bank area.
    public BankArea getBankArea() {
        return bankArea;
    }

    // Returns this player's property area.
    public PropertyArea getPropertyArea() {
        return propertyArea;
    }

    // Sets whether this player is human or AI.
    public void setPlayerType(PlayerType type) {
        this.playerType = type == null ? PlayerType.HUMAN : type;
    }

    // Returns the player type.
    public PlayerType getPlayerType() {
        return playerType;
    }

    // Reports whether this player is controlled by AI.
    public boolean isAI() {
        return playerType == PlayerType.AI;
    }

    // Executes one card through polymorphic card behavior.
    public void playCard(Card card) {
        card.executePlayLogic(this);
    }

    @Override
    public String toString() {
        return playerName;
    }
}
