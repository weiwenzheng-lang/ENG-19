package cards;
import player.Player;

public class MoneyCard extends Card {

    public MoneyCard(int id, String name, int value) {
        super(id, name, value);
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 严谨逻辑：直接调用 Player 的银行区域进行存储
        initiator.getBankArea().deposit(this);
        System.out.println("💰 [BANK] " + initiator.getPlayerName() + " 将 " + getCardName() + " 存入了银行。");
    }
}