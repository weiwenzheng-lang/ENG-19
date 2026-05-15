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

        // 以第一个对手作为可被反制的目标
        GameManager.getInstance().initiateAttack(opponents.get(0), () -> {
            GameManager.getInstance().processGlobalPayment(initiator, 2);
        });
    }
}