package cards;

import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;

public class DealBreakerCard extends ActionCard {
    // Creates a Deal Breaker action card.
    public DealBreakerCard(int id, String name, int value) {
        super(id, name, value, "DEAL_BREAKER");
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager gm = GameManager.getInstance();
        TargetInfo target = gm.getCurrentTargetInfo();
        PropertyColor chosenColor = (target != null) ? target.getImprovementColor() : null;

        // Steal the selected complete set, or fall back to the first complete set.
        gm.initiateTargetedAttack(initiator, victim -> {
            boolean moved;
            if (chosenColor != null) {
                moved = victim.getPropertyArea()
                        .transferCompletedSet(initiator.getPropertyArea(), chosenColor);
            } else {
                moved = victim.getPropertyArea()
                        .transferFirstCompletedSetTo(initiator.getPropertyArea());
            }
            if (!moved) {
                throw new IllegalStateException("Deal Breaker failed: target has no complete set.");
            }
        });
    }
}
