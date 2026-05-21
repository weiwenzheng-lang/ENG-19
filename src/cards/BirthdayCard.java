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

        GameManager.getInstance().initiateGroupAttack(initiator, opponents, victim -> {
            victim.getBankArea().pay(2, initiator);
            System.out.println(victim.getPlayerName() + " paid " + initiator.getPlayerName() + " 2M birthday money.");
        });
    }
}
