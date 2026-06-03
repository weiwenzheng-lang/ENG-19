package cards;
import player.Player;

public class ActionCard extends Card {

    // Creates a base action card; subclasses provide the real effect.
    public ActionCard(int id, String name, int value, String actionType) {
        super(id, name, value);
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // Subclasses override this method with card-specific behavior.
    }
}
