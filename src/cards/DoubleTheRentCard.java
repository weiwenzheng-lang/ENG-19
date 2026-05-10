package cards;

import core.GameManager;
import player.Player;

public class DoubleTheRentCard extends ActionCard {

    public DoubleTheRentCard(int id, String name, int value) {
        super(id, name, value, "DOUBLE_RENT");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 核心修改：调用 GameManager 的单例来激活效果
        GameManager.getInstance().activateDoubleRent();

        System.out.println("[Double Rent] " + initiator.getPlayerName() + " has activated a multiplier!");
    }
}