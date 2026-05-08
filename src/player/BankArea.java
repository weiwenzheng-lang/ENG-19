package player;

import cards.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BankArea {
    private List<Card> liquidAssets;

    public BankArea() {
        this.liquidAssets = new ArrayList<>();
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
            System.out.printf("✅ 支付 %dM 成功，实际支付 %dM（不找零）%n", amount, paidTotal);
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
                System.out.printf("⚠️ 现金总额 %dM 不足 %dM，已将全部现金支付。%n", givenTotal, amount);
            } else {
                System.out.printf("⚠️ 银行没有任何现金，无法支付 %dM。%n", amount);
            }
            int stillOwe = amount - totalCash;
            if (stillOwe > 0) {
                System.out.printf("🏚️ 【待完善】仍需强制抵债 %dM（后续由 GameManager 实现房产抵债）%n", stillOwe);
            }
        }
    }

    /**
     * 找到总金额 >= required 且超额最小的卡片组合（不找零）
     * 如果所有卡片总和仍小于 required，返回 null。
     */
    private List<Card> selectOptimalCardsForPayment(int required) {
        if (liquidAssets.isEmpty()) return null;

        // 收集所有卡的面额及其索引（支持重复面额）
        List<Card> sorted = new ArrayList<>(liquidAssets);
        int n = sorted.size();

        // 回溯搜索最优组合
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

        // 不选当前卡
        search(idx + 1, required, curSum, curCount, current, allCards, best);
        // 选当前卡
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