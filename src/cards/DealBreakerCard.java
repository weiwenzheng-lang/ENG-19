package cards;

import core.GameManager;
import player.Player;

public class DealBreakerCard extends ActionCard {
    public DealBreakerCard(int id, String name, int value) {
        super(id, name, value, "DEAL_BREAKER");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        Player victim = GameManager.getInstance().resolveTargetOrFirstOpponent(initiator);
        if (victim == null) {
            System.out.println("No valid target for Deal Breaker.");
            return;
        }

        Runnable action = () -> {
            boolean moved = victim.getPropertyArea()
                    .transferFirstCompletedSetTo(initiator.getPropertyArea());
            if (!moved) {
                throw new IllegalStateException("Deal Breaker failed: target has no complete set.");
            }
            System.out.println("[Deal Breaker] " + initiator.getPlayerName()
                    + " took a complete set from " + victim.getPlayerName());
        };

        GameManager.getInstance().initiateAttack(victim, action);
    }
}
