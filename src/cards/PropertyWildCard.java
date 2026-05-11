package cards;

import enums.PropertyColor;

public class PropertyWildCard extends PropertyCard {
    private final PropertyColor colorA;
    private final PropertyColor colorB;

    public PropertyWildCard(int id, String name, int value, PropertyColor colorA, PropertyColor colorB, int[] rentA, int[] rentB) {

         //初始颜色默认为 colorA
        super(id, name, value, colorA, true, rentA);
        this.colorA = colorA;
        this.colorB = colorB;
    }

    /**
     * 获取这张牌可选的两种颜色
     * 供 UI 层生成两个选项按钮
     */
    public PropertyColor[] getAvailableColors() {
        return new PropertyColor[]{colorA, colorB};
    }

    /**
     * 切换当前激活的颜色
     * @param newColor 玩家选择的新颜色
     */
    public void setCurrentColor(PropertyColor newColor) {
        if (newColor == colorA || newColor == colorB) {
            // 同步修改父类的颜色属性，确保 PropertyArea 能将其归入正确的组
            this.colorGroup = newColor;
            System.out.println("[System] Dual-color wildcard color updated to: " + this.colorGroup);
        } else {
            throw new IllegalArgumentException("Error: Wildcard can only switch between " + colorA + " and " + colorB );
        }
    }

    public PropertyColor getColorA() {
        return colorA;
    }

    public PropertyColor getColorB() {
        return colorB;
    }

    // 重写以确保返回的是当前玩家选定的那个颜色
    @Override
    public PropertyColor getColorGroup() {
        return this.colorGroup;
    }

    @Override
    public String toString() {
        return super.toString() + " [dual-color: " + colorA + "/" + colorB + " | current: " + colorGroup + "]";
    }
}