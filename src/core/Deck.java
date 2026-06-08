package core;

import cards.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Deck {
    private final Stack<Card> drawPile;
    private final Stack<Card> discardPile;

    // Creates empty draw and discard piles.
    public Deck() {
        this.drawPile = new Stack<>();
        this.discardPile = new Stack<>();
    }

    // Loads and shuffles a fresh deck.
    public void initializeDeck(List<Card> allCards) {
        drawPile.clear();
        discardPile.clear();
        drawPile.addAll(allCards);
        shuffle();
    }

    // Loads and shuffles a fresh deck with a shared seed.
    public void initializeDeck(List<Card> allCards, long seed) {
        drawPile.clear();
        discardPile.clear();
        drawPile.addAll(allCards);
        shuffle(seed);
    }

    // Shuffles the draw pile randomly.
    public void shuffle() {
        Collections.shuffle(drawPile);
    }

    // Shuffles the draw pile deterministically.
    public void shuffle(long seed) {
        Collections.shuffle(drawPile, new Random(seed));
    }

    // Draws cards, reshuffling discards if the draw pile is empty.
    public List<Card> drawCards(int amount) {
        List<Card> drawnCards = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            if (drawPile.isEmpty()) {
                drawPile.addAll(discardPile);
                discardPile.clear();
                shuffle();
            }

            if (drawPile.isEmpty()) {
                break;
            }
            drawnCards.add(drawPile.pop());
        }
        return drawnCards;
    }

    // Adds one card to the discard pile.
    public void receiveDiscard(Card card) {
        discardPile.push(card);
    }

    // Returns an end-of-turn excess hand card to the bottom of the draw pile.
    public void returnToBottomOfDrawPile(Card card) {
        if (card != null) {
            drawPile.add(0, card);
        }
    }

    // Returns the number of cards left to draw.
    public int getDrawPileSize() {
        return drawPile.size();
    }

    // Returns the number of discarded cards.
    public int getDiscardPileSize() {
        return discardPile.size();
    }

    // Returns the top discard without removing it.
    public Card peekDiscardTop() {
        return discardPile.isEmpty() ? null : discardPile.peek();
    }
}
