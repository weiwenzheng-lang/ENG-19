package player;
import cards.Card;
import java.util.ArrayList;
import java.util.Collections;
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

    public List<Card> getAssets() {
        return Collections.unmodifiableList(liquidAssets);
    }

    // 计算银行总资产
    public int calculateTotalFunds() {
        int total = 0;
        for (Card card : liquidAssets) {
            total += card.getMonetaryValue();
        }
        return total;
    }

    // 在 BankArea.java 中添加：
    public void pay(int amount, player.Player payee) {
        // TODO: 完整的 Monopoly 规则是：先扣银行的钱，不够再拿房产抵债。
        // 目前为了跑通逻辑，先做简单的文字打印和资产减少模拟：
        System.out.println("💸 正在从银行扣除 " + amount + "M 支付给 " + payee.getPlayerName());
        // 实际逻辑需要队员后续完善
    }
}
