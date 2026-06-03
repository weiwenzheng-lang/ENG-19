package cards;

import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;

public class HouseCard extends ActionCard {

    // Creates a House improvement card.
    public HouseCard(int id, String name, int value) {
        super(id, name, value, "HOUSE");
    }

    @Override
    public void executePlayLogic(Player player) {
        TargetInfo target = GameManager.getInstance().getCurrentTargetInfo();
        PropertyColor color = target == null ? null : target.getImprovementColor();
        // Add the house to the selected color when one was chosen.
        boolean success = color == null
                ? player.getPropertyArea().addHouseToCompleteSet()
                : player.getPropertyArea().addHouseToCompleteSet(color);

        if (!success) {
            throw new IllegalStateException("no eligible complete set for House.");
        }
    }
}
