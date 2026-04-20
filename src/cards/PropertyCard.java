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

    // 这个 Getter 非常重要，PropertyArea 需要通过它来判断卡牌颜色并进行分类
    public PropertyColor getColorGroup() {
        return colorGroup;
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 当玩家打出这张牌时，把它放进玩家自己的房产区
        initiator.getPropertyArea().addPropertyCard(this);
    }
}