package cards;

import enums.PropertyColor;
import player.Player;

public class PropertyCard extends Card {
    protected PropertyColor colorGroup;
    protected int[] rentTiers;

    // Creates a property card with its color group and rent table.
    public PropertyCard(int id, String name, int value, PropertyColor color, int[] rentTiers) {
        super(id, name, value);
        this.colorGroup = color;
        this.rentTiers = rentTiers;
    }

    // Returns the active property color for grouping and rent.
    public PropertyColor getColorGroup() {
        return colorGroup;
    }

    // Returns the rent for a set containing the given number of cards.
    public int getRentForCount(int count) {
        if (rentTiers == null || count <= 0) {
            return 0;
        }
        int index = Math.min(count, rentTiers.length) - 1;
        return rentTiers[index];
    }

    @Override
    public void executePlayLogic(Player initiator) {
        initiator.getPropertyArea().addPropertyCard(this);
    }
}
