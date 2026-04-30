package core;

import cards.*;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

public class CardFactory {
    public static List<Card> createInitialDeck() {
        List<Card> fullDeck = new ArrayList<>();

        // --- 房产卡 ---
        fullDeck.add(new PropertyCard(1, "Boardwalk", 4, PropertyColor.DARK_BLUE, false));
        fullDeck.add(new PropertyCard(2, "Park Place", 4, PropertyColor.DARK_BLUE, false));

        // --- 金钱卡 ---
        fullDeck.add(new MoneyCard(10, "5M", 5));
        fullDeck.add(new MoneyCard(11, "1M", 1));

        // --- 功能卡 (使用具体的子类) ---
        // 1. 注入 Sly Deal
        fullDeck.add(new SlyDealCard(20, "Sly Deal", 3));

        // 2. 注入 House Card
        fullDeck.add(new HouseCard(30, "House", 3));

        return fullDeck;
    }
}