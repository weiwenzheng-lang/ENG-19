package cards;

import core.GameManager;
import player.Player;

public class DebtCollectorCard extends ActionCard {
    public DebtCollectorCard(int id, String name, int value) {
        super(id, name, value, "DEBT_COLLECTOR");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        Player victim = GameManager.getInstance().resolveTargetOrFirstOpponent(initiator);
        if (victim != null) {
            victim.getBankArea().pay(5, initiator);
        }
    }
}
