package player;
import cards.Card;
import java.util.ArrayList;
import java.util.List;

public class BankArea {
    // 银行里可能存有 MoneyCard，也可能存有转为金钱的 ActionCard
    private List<Card> liquidAssets;

    public BankArea() {
        this.liquidAssets = new ArrayList<>();
    }

    public void deposit(Card card) {
        liquidAssets.add(card);
    }

    // 计算银行总资产
    public int calculateTotalFunds() {
        int total = 0;
        for (Card card : liquidAssets) {
            total += card.getMonetaryValue();
        }
        return total;
    }
}