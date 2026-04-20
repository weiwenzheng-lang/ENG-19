package player;
import cards.PropertyCard;
import enums.PropertyColor;
import java.util.HashMap;
import java.util.Map;

public class PropertyArea {
    // 核心数据结构：将颜色映射到对应的套牌集合
    private Map<PropertyColor, PropertySet> propertySets;

    public PropertyArea() {
        propertySets = new HashMap<>();
    }

    // 打出房产卡：将其加入对应的颜色集合中
    public void addPropertyCard(PropertyCard card) {
        PropertyColor color = card.getColorGroup(); // 注意：需要在 PropertyCard 类中补充 getColorGroup() 方法

        // 如果是第一次打出这个颜色的牌，初始化一个 Set
        propertySets.putIfAbsent(color, new PropertySet(color, getDefaultRequiredCount(color)));

        propertySets.get(color).addProperty(card);
        System.out.println("System: Added to PropertyArea. Current " + color + " count: " + propertySets.get(color).getCardsCount());
    }

    // 核心游戏循环调用：统计已完成的房产套数（判定胜利）
    public int countCompletedSets() {
        int completedCount = 0;
        for (PropertySet set : propertySets.values()) {
            if (set.isComplete()) {
                completedCount++;
            }
        }
        return completedCount;
    }

    // 临时辅助方法：根据 Monopoly 规则返回凑齐该颜色需要的数量
    private int getDefaultRequiredCount(PropertyColor color) {
        if (color == PropertyColor.DARK_BLUE || color == PropertyColor.BROWN || color == PropertyColor.UTILITY) return 2;
        if (color == PropertyColor.RAILROAD) return 4;
        return 3; // 大部分颜色需要 3 张
    }
}