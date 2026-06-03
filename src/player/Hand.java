package player;

import cards.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
    private final List<Card> cardsInHand;

    // Creates an empty hand.
    public Hand() {
        this.cardsInHand = new ArrayList<>();
    }

    // Adds drawn cards to the hand.
    public void addCards(List<Card> newCards) {
        cardsInHand.addAll(newCards);
    }

    // Removes and returns one card by index.
    public Card removeCard(int index) {
        if (index >= 0 && index < cardsInHand.size()) {
            return cardsInHand.remove(index);
        }
        return null;
    }

    // Returns the current hand size.
    public int getSize() {
        return cardsInHand.size();
    }

    // Checks whether the hand exceeds the official seven-card limit.
    public boolean requiresDiscard() {
        return cardsInHand.size() > 7;
    }

    // Prints the hand for console debugging.
    public void showHand() {
        System.out.println("--- Current Hand ---");
        for (int i = 0; i < cardsInHand.size(); i++) {
            System.out.println(i + ": " + cardsInHand.get(i).getCardName());
        }
    }

    // Returns a card by index without mutating the hand.
    public Card getCard(int index) {
        if (index >= 0 && index < cardsInHand.size()) {
            return cardsInHand.get(index);
        }
        return null;
    }

    // Exposes a read-only view of the hand.
    public List<Card> getCards() {
        return Collections.unmodifiableList(cardsInHand);
    }
}
