package cards;

import core.GameManager;
import player.Player;

public class DoubleTheRentCard extends ActionCard {

    // Creates a Double The Rent action card.
    public DoubleTheRentCard(int id, String name, int value) {
        super(id, name, value, "DOUBLE_RENT");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager.getInstance().activateDoubleRent();
        System.out.println("[Double Rent] " + initiator.getPlayerName() + " activated a multiplier.");
    }
}
