package player;
import cards.PropertyCard;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

public class PropertySet implements Rentable { // 👈 核心改变：实现接口
    private PropertyColor color;
    private List<PropertyCard> cards;
    private int requiredForFullSet;
    private int requiredCount;

    public PropertySet(PropertyColor color, int requiredForFullSet) {
        this.color = color;
        this.cards = new ArrayList<>();
        this.requiredForFullSet = requiredForFullSet;
        this.requiredCount = requiredCount;
    }

    public void addProperty(PropertyCard card) {
        cards.add(card);
    }

    public boolean isComplete() {
        return cards.size() >= requiredForFullSet;
    }

    // --- 实现 Rentable 接口的方法 ---
    @Override
    public int calculateRent() {
        // Monopoly 简易租金规则：假设每张牌基础租金为 2M
        // 实际开发中你可以根据颜色和卡牌数量设置更复杂的数组
        return cards.size() * 2;
    }

    @Override
    public String getDescription() {
        return color + " Set (" + cards.size() + "/" + requiredForFullSet + " cards)";
    }

    public PropertyColor getColor() { return color; }
    public int getCardsCount() { return cards.size(); }


}