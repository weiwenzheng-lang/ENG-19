package core;

import cards.Card;
import cards.MoneyCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

// Tests draw-pile behavior and discard-pile reshuffle when the draw pile is empty.
class DeckTest {
    private Deck deck;

    // Creates a small deck so reshuffle behavior is easy to trigger.
    @BeforeEach
    void setUp() {
        deck = new Deck();
        List<Card> initialCards = new ArrayList<>();
        initialCards.add(new MoneyCard(1, "1M", 1));
        initialCards.add(new MoneyCard(2, "2M", 2));
        initialCards.add(new MoneyCard(3, "3M", 3));
        deck.initializeDeck(initialCards);
    }

    // Draws cards from a non-empty draw pile.
    @Test
    void testDrawCardsNormally() {
        List<Card> drawn = deck.drawCards(2);
        assertEquals(2, drawn.size(), "Should draw two cards from the draw pile.");
    }

    // Reshuffles the discard pile when the draw pile is empty.
    @Test
    void testReshuffleWhenEmpty() {
        deck.drawCards(3);
        deck.receiveDiscard(new MoneyCard(4, "4M", 4));
        deck.receiveDiscard(new MoneyCard(5, "5M", 5));

        List<Card> drawnAgain = deck.drawCards(2);

        assertEquals(2, drawnAgain.size(), "Should reshuffle discard cards into the draw pile.");
    }

    // Peeks the discard pile without removing the top card.
    @Test
    void peekDiscardTopDoesNotRemoveCard() {
        Card top = new MoneyCard(4, "4M", 4);

        assertNull(deck.peekDiscardTop());
        deck.receiveDiscard(top);

        assertSame(top, deck.peekDiscardTop());
        assertEquals(1, deck.getDiscardPileSize());
    }

    // Returns a card to the bottom of the draw pile so it is drawn after existing cards.
    @Test
    void returnToBottomOfDrawPilePreservesExistingDrawOrder() {
        deck.drawCards(3);
        Card bottom = new MoneyCard(4, "4M", 4);
        deck.returnToBottomOfDrawPile(bottom);

        List<Card> drawn = deck.drawCards(1);

        assertSame(bottom, drawn.get(0));
    }
}
