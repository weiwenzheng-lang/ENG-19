package cards;

import core.GameManager;
import player.Player;

public class BirthdayCard extends ActionCard {

    public BirthdayCard(int id, String name, int value) {
        super(id, name, value, "IT_IS_MY_BIRTHDAY");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        System.out.println("🎂 [ACTION] " + initiator.getPlayerName() + " played 'It's My Birthday'! Everyone must pay 2M.");

        // 向全局管理器发起收款请求，遍历其他玩家扣款
        GameManager.getInstance().processGlobalPayment(initiator, 2);
    }
}