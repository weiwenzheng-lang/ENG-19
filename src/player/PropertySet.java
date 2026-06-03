package player;

import cards.PropertyCard;
import enums.PropertyColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertySet implements Rentable {
    private final PropertyColor color;
    private final List<PropertyCard> cards;

    // Creates an empty property set for one color.
    public PropertySet(PropertyColor color) {
        this.color = color;
        this.cards = new ArrayList<>();
    }

    @Override
    public void addProperty(PropertyCard card) {
        if (card.getColorGroup() == this.color) {
            cards.add(card);
        }
    }

    @Override
    public boolean isComplete() {
        return cards.size() >= color.getRequiredCount();
    }

    @Override
    public int calculateRent() {
        if (cards.isEmpty()) {
            return 0;
        }
        return color.getRentForCount(cards.size());
    }

    @Override
    public PropertyColor getColor() {
        return color;
    }

    @Override
    public String getDescription() {
        return color + " property set (" + cards.size() + "/" + color.getRequiredCount() + ")";
    }

    @Override
    public String toString() {
        return String.format("[PropertySet] color:%s | progress:%d/%d | rent:%dM",
                color, cards.size(), color.getRequiredCount(), calculateRent());
    }

    // Returns the number of cards in this set.
    public int getCardsCount() {
        return cards.size();
    }

    // Removes one property from this set.
    public void removeProperty(PropertyCard card) {
        this.cards.remove(card);
    }

    // Exposes a read-only view of the property cards.
    public List<PropertyCard> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
