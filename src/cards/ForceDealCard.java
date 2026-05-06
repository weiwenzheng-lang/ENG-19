package cards;

import core.GameManager;
import player.Player;

public class ForceDealCard extends ActionCard {
    public ForceDealCard(int id, String name, int value) {
        super(id, name, value, "FORCE_DEAL");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        Player victim = GameManager.getInstance().resolveTargetOrFirstOpponent(initiator);
        if (victim == null) {
            System.out.println("No valid target for Force Deal.");
            return;
        }

        Runnable action = () -> {
            boolean swapped = initiator.getPropertyArea()
                    .forceSwapFirstAvailableProperty(victim.getPropertyArea());
            if (!swapped) {
                throw new IllegalStateException("Force Deal failed: both players need swappable incomplete properties.");
            }
            System.out.println("[Force Deal] " + initiator.getPlayerName()
                    + " swapped properties with " + victim.getPlayerName());
        };

        GameManager.getInstance().initiateAttack(victim, action);
    }
}
