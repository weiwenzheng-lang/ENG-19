package cards;

import core.GameManager;
import player.Player;

public class BirthdayCard extends ActionCard {

    public BirthdayCard(int id, String name, int value) {
        // 生日卡：向所有人收 2M
        super(id, name, value, "BIRTHDAY");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 直接调用 GameManager 提供的全球支付接口
        // 这里的 2 是 Monopoly Deal 标准规则的金额
        GameManager.getInstance().processGlobalPayment(initiator, 2);
    }
}