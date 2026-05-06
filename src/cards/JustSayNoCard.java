package cards;

import player.Player;

public class JustSayNoCard extends ActionCard {
    public JustSayNoCard(int id, String name, int value) {
        super(id, name, value, "JUST_SAY_NO");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        System.out.println(initiator.getPlayerName() + " keeps Just Say No ready for counter actions.");
    }
}
