package cards;

import player.Player;

public abstract class Card {
    private final int cardId;
    private final String cardName;
    private final int monetaryValue;

    // Stores immutable card identity and value.
    public Card(int cardId, String cardName, int monetaryValue) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.monetaryValue = monetaryValue;
    }

    // Returns the card id used within the deck.
    public int getCardId() {
        return cardId;
    }

    // Returns the money value printed on the card.
    public int getMonetaryValue() {
        return monetaryValue;
    }

    // Returns the display name for this card.
    public String getCardName() {
        return cardName;
    }

    // Executes the card-specific play effect.
    public abstract void executePlayLogic(Player initiator);

    // Reports whether the card needs an opponent target before play.
    public boolean requiresTarget() {
        return false;
    }
}
