package core;

import cards.PropertyCard;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.Player;
import player.PropertySet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SlyDealTest {
    @Test
    void stealsSinglePropertyFromIncompleteSet() {
        Player thief = new Player("1", "Thief");
        Player victim = new Player("2", "Victim");
        victim.getPropertyArea().addPropertyCard(
                new PropertyCard(1, "Boardwalk", 4, PropertyColor.DARK_BLUE, false, new int[]{3, 8}));

        boolean moved = victim.getPropertyArea()
                .stealFirstIncompletePropertyTo(thief.getPropertyArea());

        assertFalse(victim.getPropertyArea().getPropertySets().containsKey(PropertyColor.DARK_BLUE));
        PropertySet stolenSet = (PropertySet) thief.getPropertyArea().getPropertySet(PropertyColor.DARK_BLUE);
        assertEquals(1, stolenSet.getCardsCount());
        assertEquals(true, moved);
    }

    @Test
    void refusesToStealFromCompletedSet() {
        Player thief = new Player("1", "Thief");
        Player victim = new Player("2", "Victim");
        int[] rent = {3, 8};
        victim.getPropertyArea().addPropertyCard(
                new PropertyCard(1, "Boardwalk", 4, PropertyColor.DARK_BLUE, false, rent));
        victim.getPropertyArea().addPropertyCard(
                new PropertyCard(2, "Park Place", 4, PropertyColor.DARK_BLUE, false, rent));

        boolean moved = victim.getPropertyArea()
                .stealFirstIncompletePropertyTo(thief.getPropertyArea());

        assertFalse(moved);
        assertEquals(1, victim.getPropertyArea().countCompletedSets());
    }
}
