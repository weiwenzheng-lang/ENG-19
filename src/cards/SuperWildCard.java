package cards;

import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

/**
 * 十色全能万能牌类
 * 负责处理可以变更为任意房产颜色的逻辑
 */
public class SuperWildCard extends PropertyCard {

    // 记录当前这张牌代表的具体颜色
    private PropertyColor currentSelectedColor;

    public SuperWildCard(int id, String name, int value) {
        super(id, name, value, PropertyColor.WILD, true, null);
        this.currentSelectedColor = PropertyColor.WILD;
    }

    /**
     * 修改十色万能牌的当前颜色
     * @param newColor 玩家从 UI 界面选中的新颜色
     */
    public void setCurrentColor(PropertyColor newColor) {
        // 健壮性检查 (Lec 06 Robustness)：防止非法赋值
        if (newColor == null || newColor == PropertyColor.WILD) {
            throw new IllegalArgumentException("Error: Super Wild card must be set to a specific property color!");
        }

        // 修改当前选定颜色
        this.currentSelectedColor = newColor;

        // 同时同步修改父类的 colorGroup 字段，确保 PropertySet 逻辑一致
        this.colorGroup = newColor;

        System.out.println("[System] Super Wildcard activated! Current color changed to: " + this.currentSelectedColor);
    }

     //获取当前万能牌所代表的颜色
    @Override
    public PropertyColor getColorGroup() {
        return this.currentSelectedColor;
    }

    /**
     * 获取这张牌所有可选的颜色列表
     * 【重要】：用于 UI 层（GameController）生成颜色选择弹窗
     * @return 包含所有标准房产颜色的数组
     */
    public PropertyColor[] getAvailableColors() {
        return new PropertyColor[]{
                PropertyColor.BROWN,
                PropertyColor.LIGHT_BLUE,
                PropertyColor.PINK,
                PropertyColor.ORANGE,
                PropertyColor.RED,
                PropertyColor.YELLOW,
                PropertyColor.GREEN,
                PropertyColor.DARK_BLUE,
                PropertyColor.RAILROAD,
                PropertyColor.UTILITY
        };
    }

    /**
     * 重写描述信息，方便调试和 UI 显示
     */
    @Override
    public String toString() {
        return super.toString() + " [Current Active Color: " +
                (currentSelectedColor == PropertyColor.WILD ? "Not Selected" : currentSelectedColor) + "]";
    }
}