package cards;

import core.GameManager;
import player.Player;

public class PassGoCard extends ActionCard {

    // Creates a Pass Go action card.
    public PassGoCard(int id, String name, int value) {
        super(id, name, value, "PASS_GO");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager.getInstance().drawCardsForPlayer(initiator, 2);
    }
}
