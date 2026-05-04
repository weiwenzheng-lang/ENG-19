package cards;

import enums.PropertyColor;

public class SuperWildCard extends PropertyCard {

    public SuperWildCard(int id, String name, int value) {
        // 初始颜色设为 WILD，rentTiers 设为 null
        // 理由：十色牌没有自带租金表，它加入哪套房产就遵循哪套房产的租金规则
        super(id, name, value, PropertyColor.WILD, true, null);
    }

    /**
     * 切换十色万能牌的当前颜色
     * @param newColor 玩家选中的新颜色
     */
    public void setCurrentColor(PropertyColor newColor) {
        // 老师要求 (Lec 06 Robustness)：防止非法赋值
        if (newColor == null || newColor == PropertyColor.WILD) {
            throw new IllegalArgumentException("十色万能牌必须变形成一种具体的房产颜色！");
        }

        // 直接同步修改父类的 protected 字段，确保数据唯一
        this.colorGroup = newColor;
        System.out.println("十色全能牌已激活！当前颜色变更为: " + this.colorGroup);
    }

    @Override
    public PropertyColor getColorGroup() {
        return this.colorGroup;
    }
}
