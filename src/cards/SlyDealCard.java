package cards;

import core.GameManager;
import core.TargetInfo;
import player.Player;

public class SlyDealCard extends ActionCard {

    public SlyDealCard(int id, String name, int value) {
        super(id, name, value, "SLY_DEAL");
    }

    @Override public boolean requiresTarget() { return true; }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager gm = GameManager.getInstance();
        TargetInfo target = gm.getCurrentTargetInfo();
        gm.initiateTargetedAttack(initiator, victim -> {
            boolean moved;
            if (target != null && target.getTargetPropertyColor() != null) {
                moved = victim.getPropertyArea().stealIncompletePropertyTo(
                        initiator.getPropertyArea(),
                        target.getTargetPropertyColor(),
                        target.getTargetPropertyIndex());
            } else {
                moved = victim.getPropertyArea()
                        .stealFirstIncompletePropertyTo(initiator.getPropertyArea());
            }
            if (!moved) {
                throw new IllegalStateException("Sly Deal failed: target has no stealable incomplete property.");
            }
        });
    }
}
