package core;

import cards.DoubleTheRentCard;
import cards.MoneyCard;
import cards.PropertyCard;
import cards.RentCard;
import enums.PropertyColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests high-level GameManager action flows used by the UI and AI.
class GameManagerActionFlowTest {
    private GameManager game;
    private Player alice;
    private Player bob;

    // Starts a clean two-player game and clears Alice's auto-dealt hand.
    @BeforeEach
    void setUp() {
        game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Alice", "Bob"));
        alice = game.getActivePlayers().get(0);
        bob = game.getActivePlayers().get(1);
        clearHand(alice);
        clearHand(bob);
    }

    // Verifies playing a property spends one action and places the card on the table.
    @Test
    void executePlayerActionPlaysPropertyToTable() {
        alice.getHand().addCards(List.of(property(1, "Baltic Avenue", PropertyColor.BROWN)));

        game.executePlayerAction(0, null);

        assertEquals(2, game.getActionsRemaining());
        assertEquals(0, alice.getHand().getSize());
        assertEquals(1, alice.getPropertyArea().getAllPropertyCards().size());
    }

    // Verifies property cards cannot be banked and do not consume an action.
    @Test
    void depositCardToBankRejectsPropertyWithoutSpendingAction() {
        alice.getHand().addCards(List.of(property(1, "Baltic Avenue", PropertyColor.BROWN)));

        game.depositCardToBank(0);

        assertEquals(3, game.getActionsRemaining());
        assertEquals(1, alice.getHand().getSize());
        assertEquals(0, alice.getBankArea().calculateTotalFunds());
    }

    // Verifies a player cannot end the turn while still above the hand limit.
    @Test
    void endTurnRefusesWhenCurrentPlayerMustDiscard() {
        alice.getHand().addCards(List.of(
                money(1, 1), money(2, 1), money(3, 1), money(4, 1),
                money(5, 1), money(6, 1), money(7, 1), money(8, 1)));

        game.endTurn();

        assertSame(alice, game.getCurrentPlayer());
        assertEquals(3, game.getActionsRemaining());
    }

    // Verifies Double The Rent resolves through the pending attack flow.
    @Test
    void doubleRentComboChargesAfterPendingActionResolves() {
        alice.getPropertyArea().addPropertyCard(property(1, "Baltic Avenue", PropertyColor.BROWN));
        alice.getPropertyArea().addPropertyCard(property(2, "Mediterranean Avenue", PropertyColor.BROWN));
        alice.getHand().addCards(List.of(
                new DoubleTheRentCard(3, "Double The Rent", 1),
                new RentCard(4, "Brown Rent", 1, PropertyColor.BROWN, PropertyColor.BROWN)));
        bob.getBankArea().deposit(money(5, 2));
        bob.getBankArea().deposit(money(6, 2));

        game.executeDoubleRentAction(0, 1, null);
        assertEquals(GameManager.GameState.WAITING_FOR_COUNTER_ACTION, game.getCurrentState());
        assertEquals(1, game.getActionsRemaining());

        game.resolvePendingAction();

        assertEquals(GameManager.GameState.NORMAL_TURN, game.getCurrentState());
        assertEquals(0, bob.getBankArea().calculateTotalFunds());
        assertEquals(4, alice.getBankArea().calculateTotalFunds());
    }

    // Removes every card from a player's hand.
    private void clearHand(Player player) {
        while (player.getHand().getSize() > 0) {
            player.getHand().removeCard(0);
        }
    }

    // Creates a money card with a simple display name.
    private MoneyCard money(int id, int value) {
        return new MoneyCard(id, "Money " + value + "M", value);
    }

    // Creates a standard property card for the requested color.
    private PropertyCard property(int id, String name, PropertyColor color) {
        return new PropertyCard(id, name, color.getRentTiers()[0], color, color.getRentTiers());
    }
}
