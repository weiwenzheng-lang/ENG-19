package ui.javafx;

import cards.MoneyCard;
import cards.PropertyCard;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests global bank and property summaries shown by table buttons.
class TableSummaryFormatterTest {
    // Verifies bank summary includes all players, card counts, and totals.
    @Test
    void bankSummaryIncludesEveryPlayerAndCardCounts() {
        Player alice = new Player("1", "Alice");
        Player bob = new Player("2", "Bob");
        alice.getBankArea().deposit(new MoneyCard(1, "Money 1M", 1));
        alice.getBankArea().deposit(new MoneyCard(2, "Money 1M", 1));
        bob.getBankArea().deposit(new MoneyCard(3, "Money 5M", 5));

        String summary = TableSummaryFormatter.bankSummary(Arrays.asList(alice, bob));

        assertTrue(summary.contains("Alice - Total: 2M"));
        assertTrue(summary.contains("Money 1M x2"));
        assertTrue(summary.contains("Bob - Total: 5M"));
    }

    // Verifies property summary distinguishes complete and incomplete sets.
    @Test
    void propertySummaryReportsCompleteAndIncompleteSets() {
        Player alice = new Player("1", "Alice");
        alice.getPropertyArea().addPropertyCard(property(1, "Boardwalk", PropertyColor.DARK_BLUE));
        alice.getPropertyArea().addPropertyCard(property(2, "Park Place", PropertyColor.DARK_BLUE));
        alice.getPropertyArea().addPropertyCard(property(3, "Baltic Avenue", PropertyColor.BROWN));

        String summary = TableSummaryFormatter.propertySummary(Arrays.asList(alice));

        assertTrue(summary.contains("Properties: 3"));
        assertTrue(summary.contains("DARK_BLUE: 2/2 complete"));
        assertTrue(summary.contains("BROWN: 1/2 incomplete"));
    }

    // Creates a standard property card for one color.
    private PropertyCard property(int id, String name, PropertyColor color) {
        return new PropertyCard(id, name, color.getRentTiers()[0], color, color.getRentTiers());
    }
}
