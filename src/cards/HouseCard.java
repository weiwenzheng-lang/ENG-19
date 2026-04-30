package cards;

import player.Player;
import player.PropertySet;
import player.Rentable;
import player.HouseDecorator;
import enums.PropertyColor;
import java.util.Optional;

public class HouseCard extends ActionCard {

    public HouseCard(int id, String name, int value) {
        super(id, name, value, "HOUSE");
    }

    @Override
    public void executePlayLogic(Player player) {
        // 1. 寻找一个已经凑齐且还没盖房子的套装
        // 这里的逻辑需要 PropertyArea 的配合
        Optional<PropertyColor> targetColor = player.getPropertyArea().findSetToImprove();

        if (targetColor.isPresent()) {
            PropertyColor color = targetColor.get();
            // 2. 获取原始套装
            Rentable originalSet = player.getPropertyArea().getPropertySet(color);

            // 3. 使用装饰器进行包装！
            Rentable decoratedSet = new HouseDecorator(originalSet);

            // 4. 将包装后的套装放回 PropertyArea
            player.getPropertyArea().updatePropertySet(color, decoratedSet);

            System.out.println("🏠 [BUILD] " + player.getPlayerName() + " 在 " + color + " 套装上盖了一座房子！");
            System.out.println(">> 当前描述: " + decoratedSet.getDescription());
            System.out.println(">> 新租金: " + decoratedSet.calculateRent() + "M");
        } else {
            System.out.println("❌ 无法使用房子卡：你没有凑齐的套装，或者套装已满。");
            // 提示：根据规则，如果不盖房子，这张卡也可以直接存入银行作为金钱
            player.getBankArea().deposit(this);
        }
    }
}