package cards;
import enums.PropertyColor;

public class PropertyWildCard extends PropertyCard {
    private final PropertyColor colorA;
    private final PropertyColor colorB;

    public PropertyWildCard(int id, String name, int value, PropertyColor colorA, PropertyColor colorB, int[] rentA, int[] rentB) {
        // 初始颜色默认为 colorA，rentTiers 传空或根据逻辑处理
        super(id, name, value, colorA, true, rentA);
        this.colorA = colorA;
        this.colorB = colorB;
    }

    public PropertyColor getColorA() {
        return colorA;
    }

    public PropertyColor getColorB() {
        return colorB;
    }

    public void setCurrentColor(PropertyColor newColor) {
        if (newColor == colorA || newColor == colorB) {
            this.colorGroup = newColor;
            System.out.println("万能牌已同步颜色至: " + this.colorGroup);
        } else {
            throw new IllegalArgumentException("错误：只能在 " + colorA + " 和 " + colorB + " 间切换");
        }
    }

    // 重写 getColorGroup 返回当前选中的颜色
    @Override
    public PropertyColor getColorGroup() {
        return this.colorGroup; // 始终返回当前颜色
    }
}