package player;

import cards.Card;
import cards.PropertyCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BankArea {
    private List<Card> liquidAssets;
    private Player owner;

    public BankArea() {
        this.liquidAssets = new ArrayList<>();
    }

    void setOwner(Player owner) {
        this.owner = owner;
    }

    public void deposit(Card card) {
        liquidAssets.add(card);
    }

    public List<Card> getAssets() {
        return Collections.unmodifiableList(liquidAssets);
    }

    public int calculateTotalFunds() {
        int total = 0;
        for (Card card : liquidAssets) {
            total += card.getMonetaryValue();
        }
        return total;
    }

    public void pay(int amount, Player payee) {
        if (amount <= 0) return;

        List<Card> selected = selectOptimalCardsForPayment(amount);
        if (selected != null) {
            liquidAssets.removeAll(selected);
            int paidTotal = 0;
            for (Card card : selected) {
                payee.getBankArea().deposit(card);
                paidTotal += card.getMonetaryValue();
            }
            System.out.printf("Paid %dM successfully, actual %dM (no change).%n", amount, paidTotal);
        } else {
            int totalCash = calculateTotalFunds();
            if (!liquidAssets.isEmpty()) {
                List<Card> allCards = new ArrayList<>(liquidAssets);
                liquidAssets.clear();
                int givenTotal = 0;
                for (Card card : allCards) {
                    payee.getBankArea().deposit(card);
                    givenTotal += card.getMonetaryValue();
                }
                System.out.printf("Cash %dM insufficient for %dM, paid all cash.%n", givenTotal, amount);
            }

            int stillOwe = amount - totalCash;
            if (stillOwe > 0 && owner != null) {
                List<PropertyCard> sold = owner.getPropertyArea().forceSellProperties(stillOwe);
                int soldValue = 0;
                for (PropertyCard card : sold) {
                    soldValue += card.getMonetaryValue();
                    payee.getBankArea().deposit(card);
                }
                if (soldValue > 0) {
                    System.out.printf("Mortgaged properties worth %dM to cover debt.%n", soldValue);
                }
                int finalOwe = stillOwe - soldValue;
                if (finalOwe > 0) {
                    System.out.printf("Still owe %dM after mortgaging all properties.%n", finalOwe);
                }
            } else if (stillOwe > 0) {
                System.out.printf("Owe %dM, no properties to mortgage.%n", stillOwe);
            }
        }
    }

    private List<Card> selectOptimalCardsForPayment(int required) {
        if (liquidAssets.isEmpty()) return null;

        List<Card> sorted = new ArrayList<>(liquidAssets);
        int n = sorted.size();

        BestCombination best = new BestCombination();
        search(0, required, 0, 0, new ArrayList<>(), sorted, best);

        return best.sum >= required ? new ArrayList<>(best.cards) : null;
    }

    private void search(int idx, int required, int curSum, int curCount,
                        List<Card> current, List<Card> allCards, BestCombination best) {
        if (curSum >= required) {
            if (best.cards == null || curSum < best.sum || (curSum == best.sum && curCount < best.count)) {
                best.sum = curSum;
                best.count = curCount;
                best.cards = new ArrayList<>(current);
            }
            return;
        }
        if (idx == allCards.size()) return;

        search(idx + 1, required, curSum, curCount, current, allCards, best);
        Card card = allCards.get(idx);
        current.add(card);
        search(idx + 1, required, curSum + card.getMonetaryValue(), curCount + 1, current, allCards, best);
        current.remove(current.size() - 1);
    }

    private static class BestCombination {
        List<Card> cards;
        int sum = 0;
        int count = 0;
    }
}