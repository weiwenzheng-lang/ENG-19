package core;

import cards.MoneyCard;
import core.GameManager;
import player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ActionCardPaymentTest {

    private GameManager game;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Alice", "Bob"));
        alice = game.getActivePlayers().get(0);
        bob = game.getActivePlayers().get(1);
    }

    @Test
    void testDebtCollectorCard_ExactPayment() {
        bob.getBankArea().deposit(new MoneyCard(1, "5M", 5));
        int beforeAlice = alice.getBankArea().calculateTotalFunds();
        bob.getBankArea().pay(5, alice);
        assertEquals(0, bob.getBankArea().calculateTotalFunds());
        assertEquals(beforeAlice + 5, alice.getBankArea().calculateTotalFunds());
    }

    @Test
    void testDebtCollectorCard_NoChange_Overshoot() {
        bob.getBankArea().deposit(new MoneyCard(2, "10M", 10));
        int beforeAlice = alice.getBankArea().calculateTotalFunds();
        bob.getBankArea().pay(5, alice);
        assertEquals(0, bob.getBankArea().calculateTotalFunds());
        assertEquals(beforeAlice + 10, alice.getBankArea().calculateTotalFunds());
    }

    @Test
    void testDebtCollectorCard_InsufficientCash() {
        bob.getBankArea().deposit(new MoneyCard(3, "2M", 2));
        int beforeAlice = alice.getBankArea().calculateTotalFunds();
        bob.getBankArea().pay(5, alice);
        assertEquals(0, bob.getBankArea().calculateTotalFunds());
        assertEquals(beforeAlice + 2, alice.getBankArea().calculateTotalFunds());
        // 控制台会输出仍需强制抵债 3M，但不影响测试
    }
}