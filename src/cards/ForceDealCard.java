package cards;

import core.GameManager;
import player.Player;

public class ForceDealCard extends ActionCard {
    public ForceDealCard(int id, String name, int value) {
        super(id, name, value, "FORCE_DEAL");
    }

    @Override public boolean requiresTarget() { return true; }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager.getInstance().initiateTargetedAttack(initiator, victim -> {
            boolean swapped = initiator.getPropertyArea()
                    .forceSwapFirstAvailableProperty(victim.getPropertyArea());
            if (!swapped) {
                throw new IllegalStateException("Force Deal failed: both players need swappable incomplete properties.");
            }
        });
    }
}