package cards;

import core.GameManager;
import player.Player;

public class DebtCollectorCard extends ActionCard {
    // Creates a Debt Collector action card.
    public DebtCollectorCard(int id, String name, int value) {
        super(id, name, value, "DEBT_COLLECTOR");
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public void executePlayLogic(Player initiator) {
        Player victim = GameManager.getInstance().resolveTargetOrFirstOpponent(initiator);
        if (victim != null) {
            // The target can answer before paying 5M.
            GameManager.getInstance().initiateAttack(initiator, victim, () -> {
                victim.getBankArea().pay(5, initiator);
            });
        }
    }
}
