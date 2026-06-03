package player;

import cards.Card;
import cards.PropertyCard;
import core.GameManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BankArea {
    // Lets the UI choose exact bank and property assets for a debt.
    public interface PaymentResolver {
        List<Card> choosePaymentCards(Player payer, Player payee, int amount,
                                      List<Card> bankCards, List<PropertyCard> propertyCards);
    }

    private static PaymentResolver paymentResolver;

    private List<Card> liquidAssets;
    private Player owner;

    // Creates an empty bank area.
    public BankArea() {
        this.liquidAssets = new ArrayList<>();
    }

    // Links this bank area back to its owning player.
    void setOwner(Player owner) {
        this.owner = owner;
    }

    // Adds a card as money in the bank.
    public void deposit(Card card) {
        liquidAssets.add(card);
    }

    // Returns banked cards without exposing the mutable list.
    public List<Card> getAssets() {
        return Collections.unmodifiableList(liquidAssets);
    }

    // Calculates the total money value currently banked.
    public int calculateTotalFunds() {
        int total = 0;
        for (Card card : liquidAssets) {
            total += card.getMonetaryValue();
        }
        return total;
    }

    // Registers the UI payment chooser.
    public static void setPaymentResolver(PaymentResolver resolver) {
        paymentResolver = resolver;
    }

    // Pays a debt using chosen assets, optimal cash, then properties if needed.
    public void pay(int amount, Player payee) {
        if (amount <= 0) return;
        if (payUsingResolver(amount, payee)) return;

        List<Card> selected = selectOptimalCardsForPayment(amount);
        if (selected != null) {
            paySelectedCards(amount, payee, selected);
        } else {
            payAllCashThenProperties(amount, payee);
        }
    }

    // Lets the UI resolver choose an exact payment when available.
    private boolean payUsingResolver(int amount, Player payee) {
        if (owner != null && paymentResolver != null) {
            List<PropertyCard> properties = owner.getPropertyArea().getAllPropertyCards();
            // The UI receives snapshots so it cannot mutate the bank directly.
            List<Card> chosen = paymentResolver.choosePaymentCards(
                    owner,
                    payee,
                    amount,
                    Collections.unmodifiableList(new ArrayList<>(liquidAssets)),
                    Collections.unmodifiableList(new ArrayList<>(properties)));
            if (chosen != null && !chosen.isEmpty()) {
                paySelectedCards(amount, payee, chosen);
                return true;
            }
        }
        return false;
    }

    // Pays all cash first, then transfers properties for any remaining debt.
    private void payAllCashThenProperties(int amount, Player payee) {
        int totalCash = calculateTotalFunds();
        payAllCash(amount, payee);

        int stillOwe = amount - totalCash;
        if (stillOwe > 0 && owner != null) {
            int soldValue = transferPropertiesForDebt(stillOwe, payee);
            logRemainingDebt(stillOwe - soldValue);
        } else if (stillOwe > 0) {
            System.out.printf("Owe %dM, no properties to mortgage.%n", stillOwe);
            GameManager.getInstance().logEvent(ownerName() + " still owes " + stillOwe
                    + "M and has no properties.");
        }
    }

    // Transfers every bank card to the payee.
    private void payAllCash(int amount, Player payee) {
        if (liquidAssets.isEmpty()) {
            return;
        }
        List<Card> allCards = new ArrayList<>(liquidAssets);
        liquidAssets.clear();
        int givenTotal = 0;
        for (Card card : allCards) {
            payee.getBankArea().deposit(card);
            givenTotal += card.getMonetaryValue();
        }
        System.out.printf("Cash %dM insufficient for %dM, paid all cash.%n", givenTotal, amount);
        GameManager.getInstance().logEvent(ownerName() + " paid all bank cards (" + givenTotal
                + "M) to " + payee.getPlayerName() + ".");
    }

    // Transfers properties to cover the remaining debt.
    private int transferPropertiesForDebt(int stillOwe, Player payee) {
        List<PropertyCard> sold = owner.getPropertyArea().forceSellProperties(stillOwe);
        int soldValue = 0;
        for (PropertyCard card : sold) {
            soldValue += card.getMonetaryValue();
            payee.getPropertyArea().addPropertyCard(card);
        }
        if (soldValue > 0) {
            System.out.printf("Mortgaged properties worth %dM to cover debt.%n", soldValue);
            GameManager.getInstance().logEvent(owner.getPlayerName() + " transferred properties worth "
                    + soldValue + "M to " + payee.getPlayerName() + ".");
        }
        return soldValue;
    }

    // Logs unpaid debt after all available assets are transferred.
    private void logRemainingDebt(int finalOwe) {
        if (finalOwe > 0) {
            System.out.printf("Still owe %dM after mortgaging all properties.%n", finalOwe);
            GameManager.getInstance().logEvent(owner.getPlayerName() + " still owes "
                    + finalOwe + "M after all available assets.");
        }
    }

    // Transfers the selected bank or property assets to the payee.
    private void paySelectedCards(int amount, Player payee, List<Card> selectedCards) {
        int paidTotal = 0;
        List<String> paidNames = new ArrayList<>();
        for (Card card : new ArrayList<>(selectedCards)) {
            if (card == null) continue;
            if (liquidAssets.remove(card)) {
                payee.getBankArea().deposit(card);
                paidTotal += card.getMonetaryValue();
                paidNames.add(card.getCardName());
            } else if (owner != null && card instanceof PropertyCard) {
                // Property payments move to the payee's table, not their bank.
                boolean moved = owner.getPropertyArea().transferPropertyCardTo(
                        payee.getPropertyArea(),
                        (PropertyCard) card,
                        true);
                if (moved) {
                    paidTotal += card.getMonetaryValue();
                    paidNames.add(card.getCardName());
                }
            }
        }
        System.out.printf("Paid %dM, actual %dM (no change).%n", amount, paidTotal);
        String assets = paidNames.isEmpty() ? "no valid assets" : String.join(", ", paidNames);
        GameManager.getInstance().logEvent(ownerName() + " paid " + paidTotal + "M to "
                + payee.getPlayerName() + " using " + assets + ".");
        if (paidTotal < amount) {
            GameManager.getInstance().logEvent(ownerName() + " still owes "
                    + (amount - paidTotal) + "M after all selected assets.");
        }
    }

    // Returns a safe owner name for logs.
    private String ownerName() {
        return owner == null ? "Player" : owner.getPlayerName();
    }

    // Finds the smallest cash overpayment with the fewest cards.
    private List<Card> selectOptimalCardsForPayment(int required) {
        if (liquidAssets.isEmpty()) return null;

        List<Card> sorted = new ArrayList<>(liquidAssets);
        int n = sorted.size();

        BestCombination best = new BestCombination();
        // Exhaustive search is acceptable because Monopoly Deal hands are small.
        search(0, required, 0, 0, new ArrayList<>(), sorted, best);

        return best.sum >= required ? new ArrayList<>(best.cards) : null;
    }

    // Explores pay-card subsets for the best overpayment.
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

    // Tracks the current best payment subset.
    private static class BestCombination {
        List<Card> cards;
        int sum = 0;
        int count = 0;
    }
}
