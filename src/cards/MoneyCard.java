package cards;

import player.Player;

public class MoneyCard extends Card {

    // Creates a money card.
    public MoneyCard(int id, String name, int value) {
        super(id, name, value);
    }

    @Override
    public void executePlayLogic(Player initiator) {
        initiator.getBankArea().deposit(this);
        System.out.println("[BANK] " + initiator.getPlayerName() + " banked " + getCardName() + ".");
    }
}
