package cards;

import player.Player;

public class JustSayNoCard extends ActionCard {
    // Creates a Just Say No counter card.
    public JustSayNoCard(int id, String name, int value) {
        super(id, name, value, "JUST_SAY_NO");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // The real effect is handled by GameManager during pending attacks.
    }
}
