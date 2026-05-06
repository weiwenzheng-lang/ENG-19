package cards;

import core.GameManager;
import player.Player;

public class SlyDealCard extends ActionCard {

    public SlyDealCard(int id, String name, int value) {
        super(id, name, value, "SLY_DEAL");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        Player victim = GameManager.getInstance().resolveTargetOrFirstOpponent(initiator);
        if (victim == null) {
            System.out.println("No valid target for Sly Deal.");
            return;
        }

        Runnable stealAction = () -> {
            boolean moved = victim.getPropertyArea()
                    .stealFirstIncompletePropertyTo(initiator.getPropertyArea());
            if (!moved) {
                throw new IllegalStateException("Sly Deal failed: target has no stealable incomplete property.");
            }
            System.out.println("[Sly Deal] " + initiator.getPlayerName()
                    + " stole one incomplete property from " + victim.getPlayerName());
        };

        GameManager.getInstance().initiateAttack(victim, stealAction);
    }
}
