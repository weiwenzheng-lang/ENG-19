package cards;

import core.GameManager;
import player.Player;
import java.util.List;

public class BirthdayCard extends ActionCard {

    public BirthdayCard(int id, String name, int value) {
        super(id, name, value, "BIRTHDAY");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        List<Player> opponents = GameManager.getInstance().getOpponents(initiator);
        if (opponents.isEmpty()) return;

        // 核心修复：遍历所有对手，让他们每个人都有机会独立触发 Just Say No 机制
        for (Player victim : opponents) {
            GameManager.getInstance().initiateAttack(victim, () -> {
                victim.getBankArea().pay(2, initiator);
                System.out.println("🎂 " + victim.getPlayerName() + " 向 " + initiator.getPlayerName() + " 支付了 2M 生日礼金！");
            });
        }
    }
}