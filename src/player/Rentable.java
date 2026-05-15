package player;

import cards.PropertyCard;
import enums.PropertyColor;

public interface Rentable {
    int calculateRent();
    String getDescription();
    boolean isComplete();
    PropertyColor getColor();
    void addProperty(PropertyCard card);

    default boolean isDecorated() { return false; }
    default PropertySet asPropertySet() { return (PropertySet) this; }
}