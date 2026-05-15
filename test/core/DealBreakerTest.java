package core;

import cards.PropertyCard;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.HotelDecorator;
import player.HouseDecorator;
import player.Player;
import player.Rentable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealBreakerTest {
    @Test
    void transfersCompletedDecoratedSet() {
        Player attacker = new Player("1", "Attacker");
        Player victim = new Player("2", "Victim");
        int[] rent = {3, 8};
        victim.getPropertyArea().addPropertyCard(
                new PropertyCard(1, "Boardwalk", 4, PropertyColor.DARK_BLUE, rent));
        victim.getPropertyArea().addPropertyCard(
                new PropertyCard(2, "Park Place", 4, PropertyColor.DARK_BLUE, rent));

        Rentable withHouse = new HouseDecorator(victim.getPropertyArea().getPropertySet(PropertyColor.DARK_BLUE));
        Rentable withHotel = new HotelDecorator(withHouse);
        victim.getPropertyArea().updatePropertySet(PropertyColor.DARK_BLUE, withHotel);

        boolean moved = victim.getPropertyArea()
                .transferFirstCompletedSetTo(attacker.getPropertyArea());

        assertTrue(moved);
        assertNull(victim.getPropertyArea().getPropertySet(PropertyColor.DARK_BLUE));
        assertEquals(15, attacker.getPropertyArea().getPropertySet(PropertyColor.DARK_BLUE).calculateRent());
    }
}
