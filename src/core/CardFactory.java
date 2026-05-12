package core;

import cards.*;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

public class CardFactory {
    public static List<Card> createInitialDeck() {
        List<Card> deck = new ArrayList<>();
        int id = 1;

        // 依次调用三个方法来填充牌堆
        id = addProperties(deck, id);    // 处理房产和万能牌
        id = addMoney(deck, id);         // 处理货币牌
        addActions(deck, id);            // 处理行动牌、租金卡、规则卡

        // 最终校验总数 108 张（去掉了规则卡）
        if (deck.size() != 108) {
            throw new IllegalStateException("牌堆总数错误！应该是 108 张，当前生成了: " + deck.size());
        }
        return deck;
    }

    private static int addProperties(List<Card> deck, int id) {
        int[] twoSetRent = {1, 2};
        int[] threeSetRent = {1, 2, 4};
        int[] fourSetRent = {1, 2, 3, 4};
        int[] darkBlueRent = {3, 8};
        int[] utilityRent = {1, 2};

        // 1. 标准房产卡 (28张)
        for (int i = 0; i < 2; i++) deck.add(new PropertyCard(id++, "Brown Property", 1, PropertyColor.BROWN, false, twoSetRent));
        for (int i = 0; i < 3; i++) deck.add(new PropertyCard(id++, "Light Blue Property", 1, PropertyColor.LIGHT_BLUE, false, threeSetRent));
        for (int i = 0; i < 3; i++) deck.add(new PropertyCard(id++, "Pink Property", 2, PropertyColor.PINK, false, threeSetRent));
        for (int i = 0; i < 3; i++) deck.add(new PropertyCard(id++, "Orange Property", 2, PropertyColor.ORANGE, false, threeSetRent));
        for (int i = 0; i < 3; i++) deck.add(new PropertyCard(id++, "Red Property", 3, PropertyColor.RED, false, threeSetRent));
        for (int i = 0; i < 3; i++) deck.add(new PropertyCard(id++, "Yellow Property", 3, PropertyColor.YELLOW, false, threeSetRent));
        for (int i = 0; i < 3; i++) deck.add(new PropertyCard(id++, "Green Property", 4, PropertyColor.GREEN, false, threeSetRent));
        for (int i = 0; i < 2; i++) deck.add(new PropertyCard(id++, "Dark Blue Property", 4, PropertyColor.DARK_BLUE, false, darkBlueRent));
        for (int i = 0; i < 4; i++) deck.add(new PropertyCard(id++, "Railroad Property", 2, PropertyColor.RAILROAD, false, fourSetRent));
        for (int i = 0; i < 2; i++) deck.add(new PropertyCard(id++, "Utility Property", 2, PropertyColor.UTILITY, false, utilityRent));

        // 2. 万能房产卡 (共 11 张)
        deck.add(new PropertyWildCard(id++, "Brown/Light Blue Wild", 1, PropertyColor.BROWN, PropertyColor.LIGHT_BLUE, twoSetRent, threeSetRent));
        deck.add(new PropertyWildCard(id++, "Pink/Orange Wild", 2, PropertyColor.PINK, PropertyColor.ORANGE, threeSetRent, threeSetRent));
        deck.add(new PropertyWildCard(id++, "Red/Yellow Wild", 3, PropertyColor.RED, PropertyColor.YELLOW, threeSetRent, threeSetRent));
        deck.add(new PropertyWildCard(id++, "Green/Dark Blue Wild", 4, PropertyColor.GREEN, PropertyColor.DARK_BLUE, threeSetRent, darkBlueRent));
        deck.add(new PropertyWildCard(id++, "Railroad/Utility Wild", 2, PropertyColor.RAILROAD, PropertyColor.UTILITY, fourSetRent, utilityRent));
        deck.add(new PropertyWildCard(id++, "Light Blue/Railroad Wild", 4, PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD, threeSetRent, fourSetRent));
        deck.add(new PropertyWildCard(id++, "Railroad/Green Wild", 4, PropertyColor.RAILROAD, PropertyColor.GREEN, fourSetRent, threeSetRent));
        deck.add(new PropertyWildCard(id++, "Blue/Green Wild", 4, PropertyColor.DARK_BLUE, PropertyColor.GREEN, darkBlueRent, threeSetRent));
        for (int i = 0; i < 2; i++) deck.add(new SuperWildCard(id++, "Multi-Color Wild", 0));
        deck.add(new SuperWildCard(id++, "10-Color Special Wild", 0));

        return id;
    }

    private static int addMoney(List<Card> deck, int id) {
        // 货币卡 (共 20 张)
        for (int i = 0; i < 6; i++) deck.add(new MoneyCard(id++, "1M", 1));
        for (int i = 0; i < 5; i++) deck.add(new MoneyCard(id++, "2M", 2));
        for (int i = 0; i < 3; i++) deck.add(new MoneyCard(id++, "3M", 3));
        for (int i = 0; i < 3; i++) deck.add(new MoneyCard(id++, "4M", 4));
        for (int i = 0; i < 2; i++) deck.add(new MoneyCard(id++, "5M", 5));
        deck.add(new MoneyCard(id++, "10M", 10));
        return id;
    }

    private static int addActions(List<Card> deck, int id) {
        // 1. 具体行动卡 (共 36 张)
        for (int i = 0; i < 2; i++)  deck.add(new DealBreakerCard(id++, "Deal Breaker", 5));
        for (int i = 0; i < 3; i++)  deck.add(new JustSayNoCard(id++, "Just Say No", 4));
        for (int i = 0; i < 3; i++)  deck.add(new SlyDealCard(id++, "Sly Deal", 3));
        for (int i = 0; i < 4; i++)  deck.add(new ForceDealCard(id++, "Forced Deal", 3));
        for (int i = 0; i < 3; i++)  deck.add(new DebtCollectorCard(id++, "Debt Collector", 3));
        for (int i = 0; i < 3; i++)  deck.add(new BirthdayCard(id++, "It's My Birthday", 2));
        for (int i = 0; i < 10; i++) deck.add(new PassGoCard(id++, "Pass Go", 1));
        for (int i = 0; i < 3; i++)  deck.add(new HouseCard(id++, "House", 3));
        for (int i = 0; i < 3; i++)  deck.add(new HotelCard(id++, "Hotel", 4));
        for (int i = 0; i < 2; i++)  deck.add(new DoubleTheRentCard(id++, "Double The Rent", 1));

        // 2. 租金卡 (共 13 张)
        PropertyColor[] rentColors = {
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE, PropertyColor.PINK,
                PropertyColor.ORANGE, PropertyColor.RED, PropertyColor.YELLOW,
                PropertyColor.GREEN, PropertyColor.DARK_BLUE, PropertyColor.RAILROAD,
                PropertyColor.UTILITY, PropertyColor.BROWN, PropertyColor.RED,
                PropertyColor.GREEN
        };
        for (PropertyColor color : rentColors) {
            deck.add(new RentCard(id++, color + " Rent", 1, color, color));
        }

        return id;
    }
}