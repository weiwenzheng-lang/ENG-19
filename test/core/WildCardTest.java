package core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import player.PropertySet;
import cards.PropertyCard;
import cards.PropertyWildCard;
import enums.PropertyColor;

public class WildCardTest {

    @Test
    public void testWildCardMakesSetComplete() {
        // 1. 准备深蓝色的租金规则 [1张3M, 2张8M]
        int[] blueRent = {3, 8};
        // 2. 准备绿色的租金规则 [1张2M, 2张4M, 3张7M]
        int[] greenRent = {2, 4, 7};
        // 1. 创建深蓝色套装 (需要2张凑齐)
        PropertySet mySet = new PropertySet(PropertyColor.DARK_BLUE);

        // 2. 只加入一张普通的“山顶 (The Peak)”
        mySet.addProperty(new PropertyCard(1, "The Peak", 4, PropertyColor.DARK_BLUE, false, blueRent));

        // --- 断言1：此时应该是不完整的 ---
        assertFalse(mySet.isComplete(), "只有一张牌时套装不应该完整");

        // 3. 创建一张万能牌（深蓝/绿色），并确保它的当前颜色是深蓝
        PropertyWildCard wild = new PropertyWildCard(3, "Blue/Green Wild", 4,
                PropertyColor.DARK_BLUE, PropertyColor.GREEN, blueRent,greenRent);
        wild.setCurrentColor(PropertyColor.DARK_BLUE);
        // 4. 将万能牌加入套装
        mySet.addProperty(wild);

        // --- 断言2：加入万能牌后，套装应该变成完整状态 ---
        assertTrue(mySet.isComplete(), "加入万能牌后，套装应该满足2张的要求，变为完整状态");

        // 5. 验证租金是否正确计算为 8M
        assertEquals(8, mySet.calculateRent(), "成套后的租金应该是 8M");
        System.out.println("✅ 万能牌逻辑测试通过！");
        System.out.println("描述: " + mySet.getDescription() + " | 最终租金: " + mySet.calculateRent() + "M");
    }
}