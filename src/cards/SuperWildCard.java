package cards;

import enums.PropertyColor;

public class SuperWildCard extends PropertyCard {
    private PropertyColor currentSelectedColor;

    // Creates a ten-color wild property card.
    public SuperWildCard(int id, String name, int value) {
        super(id, name, value, PropertyColor.WILD, null);
        this.currentSelectedColor = PropertyColor.WILD;
    }

    // Sets the concrete property color represented by this wild card.
    public void setCurrentColor(PropertyColor newColor) {
        if (newColor == null || newColor == PropertyColor.WILD) {
            throw new IllegalArgumentException("Super Wild card must be set to a specific property color.");
        }

        this.currentSelectedColor = newColor;
        this.colorGroup = newColor;
        this.rentTiers = newColor.getRentTiers();

        System.out.println("[System] Super Wildcard activated. Current color changed to: "
                + this.currentSelectedColor);
    }

    @Override
    public PropertyColor getColorGroup() {
        return this.currentSelectedColor;
    }

    // Returns all legal colors for this wild card.
    public PropertyColor[] getAvailableColors() {
        return new PropertyColor[]{
                PropertyColor.BROWN,
                PropertyColor.LIGHT_BLUE,
                PropertyColor.PINK,
                PropertyColor.ORANGE,
                PropertyColor.RED,
                PropertyColor.YELLOW,
                PropertyColor.GREEN,
                PropertyColor.DARK_BLUE,
                PropertyColor.RAILROAD,
                PropertyColor.UTILITY
        };
    }

    @Override
    public String toString() {
        return super.toString() + " [Current Active Color: "
                + (currentSelectedColor == PropertyColor.WILD ? "Not Selected" : currentSelectedColor) + "]";
    }
}
