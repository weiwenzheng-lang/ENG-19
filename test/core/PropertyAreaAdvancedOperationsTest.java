package core;

import cards.PropertyCard;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.PropertyArea;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests advanced PropertyArea operations used by action cards and payments.
class PropertyAreaAdvancedOperationsTest {
    // Verifies complete sets are protected from single-card transfers unless explicitly allowed.
    @Test
    void transferPropertyCardProtectsCompleteSetUnlessAllowed() {
        PropertyArea source = new PropertyArea();
        PropertyArea recipient = new PropertyArea();
        PropertyCard first = property(1, "Baltic Avenue", PropertyColor.BROWN);
        source.addPropertyCard(first);
        source.addPropertyCard(property(2, "Mediterranean Avenue", PropertyColor.BROWN));

        boolean refused = source.transferPropertyCardTo(recipient, first, false);
        boolean moved = source.transferPropertyCardTo(recipient, first, true);

        assertFalse(refused);
        assertTrue(moved);
        assertEquals(1, source.getAllPropertyCards().size());
        assertEquals(1, recipient.getAllPropertyCards().size());
        assertFalse(source.getCompletedColors().contains(PropertyColor.BROWN));
    }

    // Verifies forced property sales use incomplete sets before breaking complete sets.
    @Test
    void forceSellPropertiesUsesIncompleteCardsBeforeCompleteSets() {
        PropertyArea area = new PropertyArea();
        area.addPropertyCard(property(1, "Boardwalk", PropertyColor.DARK_BLUE));
        area.addPropertyCard(property(2, "Park Place", PropertyColor.DARK_BLUE));
        area.addPropertyCard(property(3, "Oriental Avenue", PropertyColor.LIGHT_BLUE));

        List<PropertyCard> sold = area.forceSellProperties(1);

        assertEquals(1, sold.size());
        assertEquals(PropertyColor.LIGHT_BLUE, sold.get(0).getColorGroup());
        assertTrue(area.getCompletedColors().contains(PropertyColor.DARK_BLUE));
    }

    // Verifies house and hotel improvements apply in the required order.
    @Test
    void houseAndHotelImproveOnlyEligibleCompleteSets() {
        PropertyArea area = new PropertyArea();
        area.addPropertyCard(property(1, "Boardwalk", PropertyColor.DARK_BLUE));
        area.addPropertyCard(property(2, "Park Place", PropertyColor.DARK_BLUE));

        assertTrue(area.addHouseToCompleteSet(PropertyColor.DARK_BLUE));
        assertEquals(11, area.getPropertySet(PropertyColor.DARK_BLUE).calculateRent());
        assertTrue(area.addHotelToCompleteSet(PropertyColor.DARK_BLUE));
        assertEquals(15, area.getPropertySet(PropertyColor.DARK_BLUE).calculateRent());
        assertFalse(area.addHouseToCompleteSet(PropertyColor.DARK_BLUE));
    }

    // Verifies railroads cannot receive house or hotel improvements.
    @Test
    void railroadSetsAreNotImprovementEligible() {
        PropertyArea area = new PropertyArea();
        area.addPropertyCard(property(1, "Reading Railroad", PropertyColor.RAILROAD));
        area.addPropertyCard(property(2, "Pennsylvania Railroad", PropertyColor.RAILROAD));
        area.addPropertyCard(property(3, "B&O Railroad", PropertyColor.RAILROAD));
        area.addPropertyCard(property(4, "Short Line", PropertyColor.RAILROAD));

        assertFalse(area.addHouseToCompleteSet(PropertyColor.RAILROAD));
        assertFalse(area.addHotelToCompleteSet(PropertyColor.RAILROAD));
        assertEquals(4, area.getPropertySet(PropertyColor.RAILROAD).calculateRent());
    }

    // Creates a standard property card for the requested color.
    private PropertyCard property(int id, String name, PropertyColor color) {
        return new PropertyCard(id, name, color.getRentTiers()[0], color, color.getRentTiers());
    }
}
