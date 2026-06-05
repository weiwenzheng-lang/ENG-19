package core;

import cards.Card;
import cards.DealBreakerCard;
import cards.DebtCollectorCard;
import cards.ForceDealCard;
import cards.MoneyCard;
import cards.PassGoCard;
import cards.RentCard;
import cards.SlyDealCard;
import cards.WildRentCard;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests which cards require target information before being played.
class CardTargetRequirementTest {
    // Verifies targeted action cards declare their targeting requirement.
    @Test
    void targetedActionCardsRequireTargets() {
        assertTrue(new DealBreakerCard(1, "Deal Breaker", 5).requiresTarget());
        assertTrue(new DebtCollectorCard(2, "Debt Collector", 3).requiresTarget());
        assertTrue(new ForceDealCard(3, "Forced Deal", 3).requiresTarget());
        assertTrue(new SlyDealCard(4, "Sly Deal", 3).requiresTarget());
        assertTrue(new WildRentCard(5, "Rent_Rainbow", 3).requiresTarget());
    }

    // Verifies non-targeted cards can be played without target dialogs.
    @Test
    void nonTargetedCardsDoNotRequireTargets() {
        Card money = new MoneyCard(6, "Money 1M", 1);
        Card passGo = new PassGoCard(7, "Pass Go", 1);
        Card normalRent = new RentCard(8, "Brown/Light Blue Rent", 1,
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE);

        assertFalse(money.requiresTarget());
        assertFalse(passGo.requiresTarget());
        assertFalse(normalRent.requiresTarget());
    }
}
