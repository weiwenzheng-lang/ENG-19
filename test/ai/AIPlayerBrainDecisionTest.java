package ai;

import cards.Card;
import cards.DebtCollectorCard;
import cards.MoneyCard;
import cards.PropertyCard;
import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests AI target and payment decisions used during automated turns.
class AIPlayerBrainDecisionTest {
    // Verifies targeted debt collection chooses the opponent with the largest bank.
    @Test
    void debtCollectorTargetsRichestOpponent() {
        GameManager game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Bot", "Poor", "Rich"));
        Player bot = game.getActivePlayers().get(0);
        Player poor = game.getActivePlayers().get(1);
        Player rich = game.getActivePlayers().get(2);
        poor.getBankArea().deposit(new MoneyCard(1, "Money 1M", 1));
        rich.getBankArea().deposit(new MoneyCard(2, "Money 5M", 5));

        TargetInfo target = new AIPlayerBrain().chooseTarget(bot,
                new DebtCollectorCard(3, "Debt Collector", 3), game);

        assertSame(rich, target.getTargetPlayer());
    }

    // Verifies AI prefers an exact bank payment before sacrificing property.
    @Test
    void paymentUsesBestBankSubsetBeforeProperties() {
        AIPlayerBrain brain = new AIPlayerBrain();
        Player payer = new Player("1", "Payer");
        Player payee = new Player("2", "Payee");
        Card one = new MoneyCard(1, "Money 1M", 1);
        Card two = new MoneyCard(2, "Money 2M", 2);
        Card five = new MoneyCard(3, "Money 5M", 5);
        PropertyCard property = new PropertyCard(4, "Boardwalk", 4,
                PropertyColor.DARK_BLUE, PropertyColor.DARK_BLUE.getRentTiers());

        List<Card> selected = brain.choosePaymentCards(payer, payee, 3,
                List.of(one, two, five), List.of(property));

        assertEquals(2, selected.size());
        assertTrue(selected.contains(one));
        assertTrue(selected.contains(two));
    }
}
