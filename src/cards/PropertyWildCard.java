package cards;

import enums.PropertyColor;

public class PropertyWildCard extends PropertyCard {
    private final PropertyColor colorA;
    private final PropertyColor colorB;
    private final int[] rentA;
    private final int[] rentB;

    public PropertyWildCard(int id, String name, int value, PropertyColor colorA, PropertyColor colorB, int[] rentA, int[] rentB) {
        super(id, name, value, colorA, rentA);
        this.colorA = colorA;
        this.colorB = colorB;
        this.rentA = rentA;
        this.rentB = rentB;
    }

    public PropertyColor[] getAvailableColors() {
        return new PropertyColor[]{colorA, colorB};
    }

    public void setCurrentColor(PropertyColor newColor) {
        if (newColor == colorA) {
            this.colorGroup = colorA;
            this.rentTiers = rentA;
        } else if (newColor == colorB) {
            this.colorGroup = colorB;
            this.rentTiers = rentB;
        } else {
            throw new IllegalArgumentException("Error: Wildcard can only switch between " + colorA + " and " + colorB);
        }
    }

    public PropertyColor getColorA() {
        return colorA;
    }

    public PropertyColor getColorB() {
        return colorB;
    }

    @Override
    public PropertyColor getColorGroup() {
        return this.colorGroup;
    }

    @Override
    public String toString() {
        return super.toString() + " [dual-color: " + colorA + "/" + colorB + " | current: " + colorGroup + "]";
    }
}