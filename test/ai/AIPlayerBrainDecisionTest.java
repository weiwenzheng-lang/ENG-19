package ai;

import cards.Card;
import cards.DebtCollectorCard;
import cards.DoubleTheRentCard;
import cards.MoneyCard;
import cards.PropertyCard;
import cards.RentCard;
import cards.SlyDealCard;
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

    // Verifies AI skips Sly Deal when no opponent has a stealable incomplete property.
    @Test
    void decideNextActionSkipsTargetedCardWithoutLegalTarget() {
        GameManager game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Bot", "Opponent"));
        Player bot = game.getActivePlayers().get(0);
        Player opponent = game.getActivePlayers().get(1);
        clearHand(bot);
        opponent.getPropertyArea().addPropertyCard(property(1, "Baltic Avenue", PropertyColor.BROWN));
        opponent.getPropertyArea().addPropertyCard(property(2, "Mediterranean Avenue", PropertyColor.BROWN));
        bot.getHand().addCards(List.of(
                new SlyDealCard(3, "Sly Deal", 3),
                new MoneyCard(4, "Money 1M", 1)));

        AIAction action = new AIPlayerBrain().decideNextAction(bot, game);

        assertEquals(AIAction.Type.DEPOSIT_TO_BANK, action.getType());
        assertEquals(1, action.getCardIndex());
    }

    // Verifies AI prioritizes a strong Double The Rent combo over playing rent alone.
    @Test
    void decideNextActionPrioritizesPlayableDoubleRentCombo() {
        GameManager game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Bot", "Opponent"));
        Player bot = game.getActivePlayers().get(0);
        clearHand(bot);
        bot.getPropertyArea().addPropertyCard(property(1, "Boardwalk", PropertyColor.DARK_BLUE));
        bot.getPropertyArea().addPropertyCard(property(2, "Park Place", PropertyColor.DARK_BLUE));
        bot.getHand().addCards(List.of(
                new DoubleTheRentCard(3, "Double The Rent", 1),
                new RentCard(4, "Dark Blue Rent", 1, PropertyColor.DARK_BLUE, PropertyColor.DARK_BLUE)));

        AIAction action = new AIPlayerBrain().decideNextAction(bot, game);

        assertEquals(AIAction.Type.PLAY_DOUBLE_RENT, action.getType());
        assertEquals(0, action.getCardIndex());
        assertEquals(1, action.getRentCardIndex());
        assertEquals(PropertyColor.DARK_BLUE, action.getSelectedColor());
    }

    // Removes every card from a player's hand.
    private void clearHand(Player player) {
        while (player.getHand().getSize() > 0) {
            player.getHand().removeCard(0);
        }
    }

    // Creates a standard property card for one color.
    private PropertyCard property(int id, String name, PropertyColor color) {
        return new PropertyCard(id, name, color.getRentTiers()[0], color, color.getRentTiers());
    }
}
