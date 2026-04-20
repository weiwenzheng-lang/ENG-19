package core;
import cards.*;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

public class CardFactory {

    // 静态工厂方法：生成一整副初始牌并返回
    public static List<Card> createStandardDeck() {
        List<Card> fullDeck = new ArrayList<>();

        // 1. 生成金钱卡 (示例：实际需要按照规则表的数量写循环)
        fullDeck.add(new MoneyCard(1, "1M", 1));
        fullDeck.add(new MoneyCard(2, "2M", 2));
        fullDeck.add(new MoneyCard(3, "5M", 5));

        // 2. 生成房产卡 (示例：深蓝色 Boardwalk 和 Park Place)
        fullDeck.add(new PropertyCard(10, "Boardwalk", 4, PropertyColor.DARK_BLUE, false));
        fullDeck.add(new PropertyCard(11, "Park Place", 4, PropertyColor.DARK_BLUE, false));

        // 3. 生成行动卡 (示例)
        fullDeck.add(new ActionCard(20, "Sly Deal", 3, "SLY_DEAL"));
        fullDeck.add(new ActionCard(21, "Just Say No", 4, "JUST_SAY_NO"));

        // TODO: 后期根据游戏说明书，把 106 张牌按比例 push 进 fullDeck

        return fullDeck;
    }
}