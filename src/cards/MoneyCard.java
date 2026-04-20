package cards;
import player.Player;

public class MoneyCard extends Card {

    public MoneyCard(int id, String name, int value) {
        super(id, name, value);
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 金钱卡打出后的逻辑：进入玩家的银行区
        System.out.println(initiator.getPlayerName() + " deposited money: " + getCardName() + " (Value: " + getMonetaryValue() + "M)");
        // 具体的存入动作通常会在 Player 类中调用 BankArea 的方法来完成
    }
}