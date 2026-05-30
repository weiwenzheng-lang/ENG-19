package cards;

import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;

public class HouseCard extends ActionCard {

    public HouseCard(int id, String name, int value) {
        super(id, name, value, "HOUSE");
    }

    @Override
    public void executePlayLogic(Player player) {
        TargetInfo target = GameManager.getInstance().getCurrentTargetInfo();
        PropertyColor color = target == null ? null : target.getImprovementColor();
        boolean success = color == null
                ? player.getPropertyArea().addHouseToCompleteSet()
                : player.getPropertyArea().addHouseToCompleteSet(color);

        if (!success) {
            throw new IllegalStateException("no eligible complete set for House.");
        }
    }
}
