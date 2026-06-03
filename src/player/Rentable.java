package player;

import cards.PropertyCard;
import enums.PropertyColor;

public interface Rentable {
    // Calculates rent after set size and decorations are applied.
    int calculateRent();

    // Returns a short display description for this set.
    String getDescription();

    // Reports whether the property set is complete.
    boolean isComplete();

    // Returns the set color.
    PropertyColor getColor();

    // Adds a property card to the underlying set.
    void addProperty(PropertyCard card);

    // Reports whether this set is wrapped by a house or hotel decorator.
    default boolean isDecorated() { return false; }
}
