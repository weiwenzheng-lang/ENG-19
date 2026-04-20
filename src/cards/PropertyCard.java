package cards;
import enums.PropertyColor;
import player.Player;

public class PropertyCard extends Card {
    private PropertyColor colorGroup;
    private boolean isWildcard;

    public PropertyCard(int id, String name, int value, PropertyColor color, boolean isWildcard) {
        super(id, name, value);
        this.colorGroup = color;
        this.isWildcard = isWildcard;
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 逻辑：将此卡移动到玩家的 PropertyArea
        System.out.println(initiator.getPlayerName() + " deployed property: " + getCardName());
    }
}