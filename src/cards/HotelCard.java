package cards;

import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;

public class HotelCard extends ActionCard {

    // Creates a Hotel improvement card.
    public HotelCard(int id, String name, int value) {
        super(id, name, value, "HOTEL");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        TargetInfo target = GameManager.getInstance().getCurrentTargetInfo();
        PropertyColor color = target == null ? null : target.getImprovementColor();
        // Add the hotel to the selected color when one was chosen.
        boolean success = color == null
                ? initiator.getPropertyArea().addHotelToCompleteSet()
                : initiator.getPropertyArea().addHotelToCompleteSet(color);

        if (!success) {
            throw new IllegalStateException("no eligible complete set with House for Hotel.");
        }
    }
}
