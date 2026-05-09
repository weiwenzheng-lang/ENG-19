package core;

import cards.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardFactoryTest {
    @Test
    void createsExpectedDeckSizeAndPassGoCount() {
        List<Card> cards = CardFactory.createInitialDeck();

        assertEquals(106, cards.size());
        assertEquals(10, cards.stream()
                .filter(card -> card.getCardName().equals("Pass Go"))
                .count());
    }
}
