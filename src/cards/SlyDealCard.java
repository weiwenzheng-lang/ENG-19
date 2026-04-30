package cards;

import core.GameManager;
import player.Player;

public class SlyDealCard extends ActionCard {

    public SlyDealCard(int id, String name, int value) {
        // Sly Deal (暗箱操作)：偷取别人桌面上的一张房产卡（不能偷已经凑齐一整套的）
        super(id, name, value, "SLY_DEAL");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        System.out.println("😈 [ACTION ALERT] " + initiator.getPlayerName() + " 正在发动 'Sly Deal' (暗箱操作)!");

        // 1. 获取目标玩家（这里暂定自动选择第一个对手，后续队员2画UI时可以让玩家鼠标点击选择）
        if (GameManager.getInstance().getOpponents(initiator).isEmpty()) {
            System.out.println("❌ 场上没有其他对手，无法使用此卡！");
            return;
        }
        Player victim = GameManager.getInstance().getOpponents(initiator).get(0);

        // 2. 核心亮点：把“偷牌”的动作打包成一个 Runnable（命令模式）
        Runnable stealAction = () -> {
            // 这里是真正的偷牌逻辑（后续交由队员5去写具体的卡牌转移代码）
            System.out.println("🕵️ 真正的偷牌动作发生了！" + initiator.getPlayerName() + " 成功偷取了 " + victim.getPlayerName() + " 的房产。");

            // TODO (队员5的任务):
            // - 检查 victim 的 PropertyArea
            // - 挑出一张没有凑齐套的 PropertyCard
            // - 从 victim 处 remove，添加到 initiator 处
        };

        // 3. 把受害者和偷窃动作交给 GameManager！
        // 游戏会自动挂起，等待受害者决定是否打出 "Just Say No"
        GameManager.getInstance().initiateAttack(victim, stealAction);
    }
}