package cards;

import core.GameManager;
import player.Player;

public class DealBreakerCard extends ActionCard {
    public DealBreakerCard(int id, String name, int value) {
        super(id, name, value, "DEAL_BREAKER");
    }

    @Override public boolean requiresTarget() { return true; }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager.getInstance().initiateTargetedAttack(initiator, victim -> {
            boolean moved = victim.getPropertyArea()
                    .transferFirstCompletedSetTo(initiator.getPropertyArea());
            if (!moved) {
                throw new IllegalStateException("Deal Breaker failed: target has no complete set.");
            }
        });
    }
}