package cards;

import core.GameManager;
import player.Player;

public class SlyDealCard extends ActionCard {

    public SlyDealCard(int id, String name, int value) {
        // Sly Deal (暗箱操作)：可以偷取别的玩家放在桌面上的一张房产卡（不能偷已经凑齐一整套的）
        super(id, name, value, "SLY_DEAL");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        System.out.println("😈 [ACTION ALERT] " + initiator.getPlayerName() + " 正在发动 'Sly Deal' (暗箱操作)!");

        // 【满分设计细节】卡牌自己不应该去处理控制台输入。
        // 卡牌只负责“发出指令”，具体的“选择目标并偷窃”的复杂流程，应该交给 GameManager 去调度。
        // （这符合 Clean Design 中的单一职责原则 Single Responsibility Principle）
        GameManager.getInstance().processSlyDealAttack(initiator);
    }
}