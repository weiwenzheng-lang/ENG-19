package cards;

import core.GameManager;
import player.Player;

public class SlyDealCard extends ActionCard {

    public SlyDealCard(int id, String name, int value) {
        super(id, name, value, "SLY_DEAL");
    }

    @Override public boolean requiresTarget() { return true; }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager.getInstance().initiateTargetedAttack(initiator, victim -> {
            boolean moved = victim.getPropertyArea()
                    .stealFirstIncompletePropertyTo(initiator.getPropertyArea());
            if (!moved) {
                throw new IllegalStateException("Sly Deal failed: target has no stealable incomplete property.");
            }
        });
    }
}