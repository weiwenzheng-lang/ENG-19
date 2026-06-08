package cards;

import core.GameManager;
import player.Player;
import java.util.List;

public class BirthdayCard extends ActionCard {

    // Creates an It's My Birthday action card.
    public BirthdayCard(int id, String name, int value) {
        super(id, name, value, "BIRTHDAY");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        List<Player> opponents = GameManager.getInstance().getOpponents(initiator);
        if (opponents.isEmpty()) return;

        // Each opponent may counter before paying birthday money.
        GameManager.getInstance().initiateGroupAttack(initiator, opponents, victim -> {
            victim.getBankArea().pay(2, initiator);
            GameManager.getInstance().logEvent(victim.getPlayerName()
                    + " paid " + initiator.getPlayerName() + " 2M birthday money.");
        });
    }
}
