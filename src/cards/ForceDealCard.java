package cards;

import core.GameManager;
import core.TargetInfo;
import player.Player;

public class ForceDealCard extends ActionCard {
    public ForceDealCard(int id, String name, int value) {
        super(id, name, value, "FORCE_DEAL");
    }

    @Override public boolean requiresTarget() { return true; }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager gm = GameManager.getInstance();
        TargetInfo target = gm.getCurrentTargetInfo();
        gm.initiateTargetedAttack(initiator, victim -> {
            boolean swapped;
            if (target != null
                    && target.getInitiatorPropertyColor() != null
                    && target.getTargetPropertyColor() != null) {
                swapped = initiator.getPropertyArea().forceSwapProperty(
                        victim.getPropertyArea(),
                        target.getInitiatorPropertyColor(),
                        target.getInitiatorPropertyIndex(),
                        target.getTargetPropertyColor(),
                        target.getTargetPropertyIndex());
            } else {
                swapped = initiator.getPropertyArea()
                        .forceSwapFirstAvailableProperty(victim.getPropertyArea());
            }
            if (!swapped) {
                throw new IllegalStateException("Force Deal failed: both players need swappable properties.");
            }
        });
    }
}
