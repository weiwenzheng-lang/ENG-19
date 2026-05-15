package core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import player.*;
import cards.PropertyCard;
import enums.PropertyColor;

public class DecoratorTest {

    @Test
    public void testRentEscalation() {
        // 1. 准备深蓝色租金表：1张收3M，2张收8M
        int[] darkBlueRent = {3, 8};

        // 2. 创建一个深蓝色套装并凑齐它
        PropertySet mySet = new PropertySet(PropertyColor.DARK_BLUE);
        mySet.addProperty(new PropertyCard(1, "The Peak", 4, PropertyColor.DARK_BLUE, darkBlueRent));
        mySet.addProperty(new PropertyCard(2, "Repulse Bay", 4, PropertyColor.DARK_BLUE, darkBlueRent));

        // --- 断言1：基础租金应为 8M ---
        assertEquals(8, mySet.calculateRent(), "基础两张深蓝色的租金应该是 8M");

        // 3. 盖房子 (House)
        HouseDecorator house = new HouseDecorator(mySet);

        // --- 断言2：加房子后租金应为 8 + 3 = 11M ---
        assertEquals(11, house.calculateRent(), "加盖房子后租金应该是 11M (8+3)");

        // 4. 盖酒店 (Hotel)
        HotelDecorator hotel = new HotelDecorator(house);

        // --- 断言3：加酒店后租金应为 11 + 4 = 15M ---
        assertEquals(15, hotel.calculateRent(), "加盖酒店后租金应该是 15M (11+4)");

        // 打印最终描述，看看 toString 是否漂亮
        System.out.println(hotel.toString());

    }
    // 👈 增加测试 2：测试鲁棒性（未凑齐时盖房是否会报错）
    @Test
    public void testIllegalHousePlacement() {
        int[] darkBlueRent = {3, 8};
        PropertySet incompleteSet = new PropertySet(PropertyColor.DARK_BLUE);

        // 只加一张牌（不完整）
        incompleteSet.addProperty(new PropertyCard(1, "The Peak", 4, PropertyColor.DARK_BLUE, darkBlueRent));

        assertThrows(IllegalStateException.class, () -> {
            new HouseDecorator(incompleteSet);
        }, "在房产未凑齐时，应该抛出 IllegalStateException 阻止盖房");

        System.out.println("✅ 鲁棒性测试通过：系统成功拦截了非法盖房操作。");
    }

}