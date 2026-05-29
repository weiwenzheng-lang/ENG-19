package core;

import cards.*;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComprehensiveTest {

    // ===== Win Condition Tests =====

    @Test
    void detectsWinWithThreeCompleteSets() {
        Player p = new Player("1", "Winner");
        // Brown set (2 cards)
        p.getPropertyArea().addPropertyCard(new PropertyCard(1, "B1", 1, PropertyColor.BROWN, new int[]{1, 2}));
        p.getPropertyArea().addPropertyCard(new PropertyCard(2, "B2", 1, PropertyColor.BROWN, new int[]{1, 2}));
        // Red set (3 cards)
        p.getPropertyArea().addPropertyCard(new PropertyCard(3, "R1", 3, PropertyColor.RED, new int[]{1, 2, 4}));
        p.getPropertyArea().addPropertyCard(new PropertyCard(4, "R2", 3, PropertyColor.RED, new int[]{1, 2, 4}));
        p.getPropertyArea().addPropertyCard(new PropertyCard(5, "R3", 3, PropertyColor.RED, new int[]{1, 2, 4}));
        // Green set (3 cards)
        p.getPropertyArea().addPropertyCard(new PropertyCard(6, "G1", 4, PropertyColor.GREEN, new int[]{1, 2, 4}));
        p.getPropertyArea().addPropertyCard(new PropertyCard(7, "G2", 4, PropertyColor.GREEN, new int[]{1, 2, 4}));
        p.getPropertyArea().addPropertyCard(new PropertyCard(8, "G3", 4, PropertyColor.GREEN, new int[]{1, 2, 4}));

        assertEquals(3, p.getPropertyArea().countCompletedSets());
    }

    @Test
    void doesNotCountIncompleteSets() {
        Player p = new Player("1", "Loser");
        p.getPropertyArea().addPropertyCard(new PropertyCard(1, "B1", 1, PropertyColor.BROWN, new int[]{1, 2}));
        // Only 1 of 2 needed for Brown

        assertEquals(0, p.getPropertyArea().countCompletedSets());
    }

    // ===== SuperWildCard Rent Fix Test =====

    @Test
    void superWildCardHasCorrectRentAfterColorSet() {
        SuperWildCard wild = new SuperWildCard(1, "Multi-Color Wild", 0);
        wild.setCurrentColor(PropertyColor.DARK_BLUE);

        PropertySet set = new PropertySet(PropertyColor.DARK_BLUE);
        set.addProperty(new PropertyCard(2, "Boardwalk", 4, PropertyColor.DARK_BLUE, new int[]{3, 8}));
        set.addProperty(wild);

        assertTrue(set.isComplete());
        assertEquals(8, set.calculateRent(), "SuperWildCard should use DARK_BLUE rent tiers (8M for 2 cards)");
    }

    @Test
    void superWildCardRailroadRent() {
        SuperWildCard wild = new SuperWildCard(1, "Multi-Color Wild", 0);
        wild.setCurrentColor(PropertyColor.RAILROAD);

        PropertySet set = new PropertySet(PropertyColor.RAILROAD);
        set.addProperty(wild);
        set.addProperty(new PropertyCard(2, "R1", 2, PropertyColor.RAILROAD, new int[]{1, 2, 3, 4}));
        set.addProperty(new PropertyCard(3, "R2", 2, PropertyColor.RAILROAD, new int[]{1, 2, 3, 4}));

        assertEquals(3, set.calculateRent(), "3 railroads = 3M rent");
    }

    // ===== PropertyWildCard Rent Tier Switching =====

    @Test
    void propertyWildCardSwitchesRentTiersOnColorChange() {
        int[] brownRent = {1, 2};
        int[] lightBlueRent = {1, 2, 3};
        PropertyWildCard wild = new PropertyWildCard(1, "Brown/LB Wild", 1,
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE, brownRent, lightBlueRent);

        // Default: BROWN
        assertEquals(PropertyColor.BROWN, wild.getColorGroup());

        // Switch to LIGHT_BLUE
        wild.setCurrentColor(PropertyColor.LIGHT_BLUE);
        assertEquals(PropertyColor.LIGHT_BLUE, wild.getColorGroup());

        PropertySet set = new PropertySet(PropertyColor.LIGHT_BLUE);
        set.addProperty(wild);
        set.addProperty(new PropertyCard(2, "LB1", 1, PropertyColor.LIGHT_BLUE, lightBlueRent));
        set.addProperty(new PropertyCard(3, "LB2", 1, PropertyColor.LIGHT_BLUE, lightBlueRent));

        assertTrue(set.isComplete());
        assertEquals(3, set.calculateRent(), "3 Light Blue cards = 3M rent");
    }

    // ===== Rent + Double Rent =====

    @Test
    void doubleRentStacksMultiplier() {
        GameManager gm = GameManager.getInstance();
        gm.activateDoubleRent();
        gm.activateDoubleRent();
        int mult = gm.getAndResetRentMultiplier();
        assertEquals(4, mult, "Two Double The Rent = 4x multiplier");
    }

    @Test
    void doubleRentResetsAfterUse() {
        GameManager gm = GameManager.getInstance();
        gm.activateDoubleRent();
        gm.getAndResetRentMultiplier();
        assertEquals(1, gm.getAndResetRentMultiplier(), "Multiplier should reset to 1");
    }

    // ===== Discard Flow =====

    @Test
    void discardRemovesCardFromHand() {
        Player p = new Player("1", "Discarder");
        p.getHand().addCards(List.of(
                new MoneyCard(1, "1M", 1),
                new MoneyCard(2, "2M", 2),
                new MoneyCard(3, "3M", 3)
        ));
        assertEquals(3, p.getHand().getSize());

        p.getHand().removeCard(1);
        assertEquals(2, p.getHand().getSize());
    }

    // ===== Hotel Fallback =====

    @Test
    void hotelCardDepositsToBankWhenNoEligibleSet() {
        Player p = new Player("1", "Builder");
        int before = p.getBankArea().calculateTotalFunds();
        HotelCard hotel = new HotelCard(1, "Hotel", 4);
        hotel.executePlayLogic(p);
        assertEquals(before + 4, p.getBankArea().calculateTotalFunds(),
                "Hotel should deposit to bank when no eligible set");
    }

    // ===== Payment with Mortgage =====

    @Test
    void paymentWithInsufficientCashTriggersMortgage() {
        Player debtor = new Player("1", "Debtor");
        Player creditor = new Player("2", "Creditor");

        // Give debtor a property but no cash
        debtor.getPropertyArea().addPropertyCard(
                new PropertyCard(1, "Brown1", 1, PropertyColor.BROWN, new int[]{1, 2}));
        debtor.getBankArea().deposit(new MoneyCard(2, "1M", 1));

        debtor.getBankArea().pay(5, creditor);

        // Property should have been sold to cover debt
        assertTrue(debtor.getPropertyArea().getPropertySet(PropertyColor.BROWN) == null
                || debtor.getPropertyArea().getPropertySet(PropertyColor.BROWN).calculateRent() == 0);
    }

    // ===== Deck Size Verification =====

    @Test
    void deckHasCorrectNumberOfCards() {
        List<Card> cards = CardFactory.createInitialDeck();
        assertEquals(106, cards.size());
    }

    // ===== Rent Card - incomplete sets still pay =====

    @Test
    void rentChargesForIncompleteSets() {
        Player renter = new Player("1", "Renter");
        Player victim = new Player("2", "Victim");

        // Victim has 1 Red property (incomplete, needs 3)
        victim.getPropertyArea().addPropertyCard(
                new PropertyCard(1, "Red1", 3, PropertyColor.RED, new int[]{2, 3, 6}));

        Rentable set = victim.getPropertyArea().getPropertySet(PropertyColor.RED);
        assertNotNull(set);
        assertFalse(set.isComplete());
        assertEquals(2, set.calculateRent(), "Incomplete set should still return rent based on card count");
    }
}
