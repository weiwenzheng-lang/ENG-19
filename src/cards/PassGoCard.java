package cards;

import core.GameManager;
import player.Player;

// 继承自 ActionCard，利用多态处理不同技能，消灭 Switch 语句！
public class PassGoCard extends ActionCard {

    public PassGoCard(int id, String name, int value) {
        super(id, name, value, "PASS_GO");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        System.out.println("⚡ [ACTION] " + initiator.getPlayerName() + " played 'Pass Go' and draws 2 extra cards!");

        // 逻辑：向 GameManager 请求从牌堆再抽两张牌
        GameManager.getInstance().drawCardsForPlayer(initiator, 2);
    }
}