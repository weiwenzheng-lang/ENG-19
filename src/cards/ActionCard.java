package cards;
import player.Player;

public class ActionCard extends Card {

    public ActionCard(int id, String name, int value, String actionType) {
        super(id, name, value);
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // Overridden by all subclasses
    }
}