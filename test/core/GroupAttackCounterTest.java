package core;

import cards.BirthdayCard;
import cards.JustSayNoCard;
import cards.MoneyCard;
import cards.PropertyCard;
import cards.RentCard;
import enums.PropertyColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GroupAttackCounterTest {
    private GameManager game;
    private Player alice;
    private Player bob;
    private Player carol;

    @BeforeEach
    void setUp() {
        game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Alice", "Bob", "Carol"));
        alice = game.getActivePlayers().get(0);
        bob = game.getActivePlayers().get(1);
        carol = game.getActivePlayers().get(2);
    }

    @Test
    void birthdayCollectsFromEveryOpponent() {
        bob.getBankArea().deposit(new MoneyCard(1, "2M", 2));
        carol.getBankArea().deposit(new MoneyCard(2, "2M", 2));

        new BirthdayCard(3, "It's My Birthday", 2).executePlayLogic(alice);

        assertSame(bob, game.getPendingVictim());
        game.resolvePendingAction();
        assertSame(carol, game.getPendingVictim());
        game.resolvePendingAction();

        assertEquals(0, bob.getBankArea().calculateTotalFunds());
        assertEquals(0, carol.getBankArea().calculateTotalFunds());
        assertEquals(4, alice.getBankArea().calculateTotalFunds());
        assertEquals(GameManager.GameState.NORMAL_TURN, game.getCurrentState());
    }

    @Test
    void justSayNoOnlyBlocksCurrentVictimInGroupAttack() {
        alice.getPropertyArea().addPropertyCard(new PropertyCard(4, "Brown Property", 1,
                PropertyColor.BROWN, new int[]{1, 2}));
        bob.getBankArea().deposit(new MoneyCard(5, "1M", 1));
        carol.getBankArea().deposit(new MoneyCard(6, "1M", 1));
        carol.getHand().addCards(Collections.singletonList(new JustSayNoCard(7, "Just Say No", 4)));

        new RentCard(8, "Brown Rent", 1, PropertyColor.BROWN, PropertyColor.BROWN)
                .executePlayLogic(alice);

        assertSame(bob, game.getPendingVictim());
        game.resolvePendingAction();
        assertSame(carol, game.getPendingVictim());
        game.counterAttackWithJustSayNo(carol.getHand().getSize() - 1);

        assertEquals(0, bob.getBankArea().calculateTotalFunds());
        assertEquals(1, carol.getBankArea().calculateTotalFunds());
        assertEquals(1, alice.getBankArea().calculateTotalFunds());
        assertEquals(GameManager.GameState.NORMAL_TURN, game.getCurrentState());
    }
}
