package core;

import cards.MoneyCard;
import player.BankArea;
import player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankAreaTest {
    private BankArea bankA;
    private BankArea bankB;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("1", "Alice");
        bob = new Player("2", "Bob");
        bankA = alice.getBankArea();
        bankB = bob.getBankArea();
    }

    @Test
    void testPayExactlyWithCombination() {
        bankA.deposit(new MoneyCard(1, "1M", 1));
        bankA.deposit(new MoneyCard(2, "2M", 2));
        bankA.deposit(new MoneyCard(3, "5M", 5));

        bankA.pay(3, bob);
        assertEquals(5, bankA.calculateTotalFunds()); // 只剩5M
        assertEquals(3, bankB.calculateTotalFunds()); // 得到1+2=3M
    }

    @Test
    void testNoChangeWhenOvershoot() {
        bankA.deposit(new MoneyCard(4, "3M", 3));
        bankA.pay(2, bob);
        assertEquals(0, bankA.calculateTotalFunds());
        assertEquals(3, bankB.calculateTotalFunds());
    }

    @Test
    void testInsufficientMoneyPaysAllCash() {
        bankA.deposit(new MoneyCard(5, "1M", 1));
        bankA.pay(5, bob);
        assertEquals(0, bankA.calculateTotalFunds());
        assertEquals(1, bankB.calculateTotalFunds());
    }

    @Test
    void testPayZeroDoesNothing() {
        int before = bankA.calculateTotalFunds();
        bankA.pay(0, bob);
        assertEquals(before, bankA.calculateTotalFunds());
        assertEquals(0, bankB.calculateTotalFunds());
    }
}