package core;

import cards.PropertyCard;
import cards.PropertyWildCard;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.PropertyArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests key property-area operations used by action cards.
class PropertyAreaOperationsTest {
    // Verifies Forced Deal swaps selected incomplete properties.
    @Test
    void forceSwapPropertyExchangesSelectedIncompleteCards() {
        PropertyArea mine = new PropertyArea();
        PropertyArea theirs = new PropertyArea();
        PropertyCard brown = property(1, "Baltic Avenue", PropertyColor.BROWN);
        PropertyCard blue = property(2, "Oriental Avenue", PropertyColor.LIGHT_BLUE);
        mine.addPropertyCard(brown);
        theirs.addPropertyCard(blue);

        boolean swapped = mine.forceSwapProperty(theirs,
                PropertyColor.BROWN, 0,
                PropertyColor.LIGHT_BLUE, 0);

        assertTrue(swapped);
        assertEquals(PropertyColor.LIGHT_BLUE, mine.getAllPropertyCards().get(0).getColorGroup());
        assertEquals(PropertyColor.BROWN, theirs.getAllPropertyCards().get(0).getColorGroup());
    }

    // Verifies a selected completed set is transferred intact.
    @Test
    void transferCompletedSetMovesRequestedColorOnly() {
        PropertyArea source = new PropertyArea();
        PropertyArea recipient = new PropertyArea();
        source.addPropertyCard(property(1, "Boardwalk", PropertyColor.DARK_BLUE));
        source.addPropertyCard(property(2, "Park Place", PropertyColor.DARK_BLUE));
        source.addPropertyCard(property(3, "Baltic Avenue", PropertyColor.BROWN));

        boolean moved = source.transferCompletedSet(recipient, PropertyColor.DARK_BLUE);

        assertTrue(moved);
        assertFalse(source.getCompletedColors().contains(PropertyColor.DARK_BLUE));
        assertTrue(recipient.getCompletedColors().contains(PropertyColor.DARK_BLUE));
        assertEquals(1, source.getAllPropertyCards().size());
    }

    // Verifies wild cards can move between color buckets after a color swap.
    @Test
    void swapWildCardColorMovesCardToNewColorSet() {
        PropertyArea area = new PropertyArea();
        PropertyWildCard wild = new PropertyWildCard(1, "Blue/Brown Wild", 1,
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE,
                PropertyColor.BROWN.getRentTiers(), PropertyColor.LIGHT_BLUE.getRentTiers());
        area.addPropertyCard(wild);

        boolean moved = area.swapWildCardColor(wild, PropertyColor.LIGHT_BLUE);

        assertTrue(moved);
        assertEquals(PropertyColor.LIGHT_BLUE, area.getAllPropertyCards().get(0).getColorGroup());
    }

    // Creates a standard property card for the requested color.
    private PropertyCard property(int id, String name, PropertyColor color) {
        return new PropertyCard(id, name, color.getRentTiers()[0], color, color.getRentTiers());
    }
}
