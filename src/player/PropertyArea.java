package player;

import enums.PropertyColor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PropertyArea {
    // 👈 注意：这里改为 Rentable 接口，这是多态的精髓
    private Map<PropertyColor, Rentable> propertySets;

    public PropertyArea() {
        propertySets = new HashMap<>();
    }

    // 辅助方法：找到可以盖房子的颜色（已凑齐且不是装饰器或特定逻辑）
    public Optional<PropertyColor> findSetToImprove() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable set = entry.getValue();
            // 逻辑：如果是完整的 PropertySet 且目前还没被装饰（或者根据你的规则判断）
            if (set instanceof PropertySet && ((PropertySet) set).isComplete()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public Rentable getPropertySet(PropertyColor color) {
        return propertySets.get(color);
    }

    public void updatePropertySet(PropertyColor color, Rentable decoratedSet) {
        propertySets.put(color, decoratedSet);
    }

    // 原有的 addPropertyCard 需要兼容处理
    public void addPropertyCard(cards.PropertyCard card) {
        PropertyColor color = card.getColorGroup();
        propertySets.computeIfAbsent(color, k -> new PropertySet(color, 2)); // 简化版

        Rentable current = propertySets.get(color);
        if (current instanceof PropertySet) {
            ((PropertySet) current).addProperty(card);
        }
    }

    // 在 PropertyArea.java 中添加：
    public int countCompletedSets() {
        int count = 0;
        for (Rentable set : propertySets.values()) {
            if (set.isComplete()) {
                count++;
            }
        }
        return count;
    }
}