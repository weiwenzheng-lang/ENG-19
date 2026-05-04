package cards;
import enums.PropertyColor;
import player.Player;

public class PropertyCard extends Card {
    protected PropertyColor colorGroup;
    private boolean isWildcard;
    private int[] rentTiers; // 存储租金阶梯的数组，例如 [1, 2, 4]

    public PropertyCard(int id, String name, int value, PropertyColor color, boolean isWildcard,int[] rentTiers) {
        super(id, name, value);
        this.colorGroup = color;
        this.isWildcard = isWildcard;
        this.rentTiers = rentTiers;
    }

    // 这个 Getter 非常重要，PropertyArea 需要通过它来判断卡牌颜色并进行分类
    public PropertyColor getColorGroup() {
        return colorGroup;
    }

    // 供 PropertySet调用 (根据手里同色房产的张数，自动匹配对应档次的租金)
    public int getRentForCount(int count) {
        if (rentTiers == null || count <= 0) return 0;

        // 防止索引越界（比如你有4张牌但表里只有3档，就按最高档算）
        int index = Math.min(count, rentTiers.length) - 1;
        return rentTiers[index];
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 当玩家打出这张牌时，把它放进玩家自己的房产区
        initiator.getPropertyArea().addPropertyCard(this);
    }
}
