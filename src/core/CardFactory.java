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

        // 最终校验总数 106 张
        if (deck.size() != 106) {
            throw new IllegalStateException("牌堆总数错误！应该是 106 张，当前生成了: " + deck.size());
        }
        return deck;
    }

    private static int addProperties(List<Card> deck, int id) {
        int[] brownRent = PropertyColor.BROWN.getRentTiers();
        int[] lightBlueRent = PropertyColor.LIGHT_BLUE.getRentTiers();
        int[] pinkRent = PropertyColor.PINK.getRentTiers();
        int[] orangeRent = PropertyColor.ORANGE.getRentTiers();
        int[] redRent = PropertyColor.RED.getRentTiers();
        int[] yellowRent = PropertyColor.YELLOW.getRentTiers();
        int[] greenRent = PropertyColor.GREEN.getRentTiers();
        int[] darkBlueRent = PropertyColor.DARK_BLUE.getRentTiers();
        int[] railroadRent = PropertyColor.RAILROAD.getRentTiers();
        int[] utilityRent = PropertyColor.UTILITY.getRentTiers();

        // 1. 标准房产卡 (28张)
        id = addPropertyGroup(deck, id, new String[]{"Mediterranean Avenue", "Baltic Avenue"}, 1, PropertyColor.BROWN, brownRent);
        id = addPropertyGroup(deck, id, new String[]{"Connecticut Avenue", "Oriental Avenue", "Vermont Avenue"}, 1, PropertyColor.LIGHT_BLUE, lightBlueRent);
        id = addPropertyGroup(deck, id, new String[]{"STCharles Place", "States Avenue", "Virginia Avenue"}, 2, PropertyColor.PINK, pinkRent);
        id = addPropertyGroup(deck, id, new String[]{"STJames Place", "NewYork Avenue", "Tennessee Avenue"}, 2, PropertyColor.ORANGE, orangeRent);
        id = addPropertyGroup(deck, id, new String[]{"Illinois Avenue", "Indiana Avenue", "Kentucky Avenue"}, 3, PropertyColor.RED, redRent);
        id = addPropertyGroup(deck, id, new String[]{"Atlantic Avenue", "Marvin Gardens", "Ventnor Avenue"}, 3, PropertyColor.YELLOW, yellowRent);
        id = addPropertyGroup(deck, id, new String[]{"North Carolina Avenue", "Pacific Avenue", "Pennsylvania Avenue"}, 4, PropertyColor.GREEN, greenRent);
        id = addPropertyGroup(deck, id, new String[]{"Boardwalk", "Park Place"}, 4, PropertyColor.DARK_BLUE, darkBlueRent);
        id = addPropertyGroup(deck, id, new String[]{"B&O Railroad", "Pennsylvania Railroad", "Reading Railroad", "Short Line"}, 2, PropertyColor.RAILROAD, railroadRent);
        id = addPropertyGroup(deck, id, new String[]{"Electric Company", "Water works"}, 2, PropertyColor.UTILITY, utilityRent);

        // 2. 万能房产卡 (共 11 张)
        deck.add(new PropertyWildCard(id++, "Property Wild card_BlueBrown", 1, PropertyColor.BROWN, PropertyColor.LIGHT_BLUE, brownRent, lightBlueRent));
        for (int i = 0; i < 2; i++) deck.add(new PropertyWildCard(id++, "Property Wild Card_OrangePink", 2, PropertyColor.PINK, PropertyColor.ORANGE, pinkRent, orangeRent));
        for (int i = 0; i < 2; i++) deck.add(new PropertyWildCard(id++, "Property Wild Card_YellowRed", 3, PropertyColor.RED, PropertyColor.YELLOW, redRent, yellowRent));
        deck.add(new PropertyWildCard(id++, "Property Wild card_GreenDeepblue", 4, PropertyColor.GREEN, PropertyColor.DARK_BLUE, greenRent, darkBlueRent));
        deck.add(new PropertyWildCard(id++, "Property Wild Card_EnterpriseRailroad", 2, PropertyColor.RAILROAD, PropertyColor.UTILITY, railroadRent, utilityRent));
        deck.add(new PropertyWildCard(id++, "Property Wild Card_BlueRailroad", 4, PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD, lightBlueRent, railroadRent));
        deck.add(new PropertyWildCard(id++, "Property Wild Card_RailroadGreen", 4, PropertyColor.RAILROAD, PropertyColor.GREEN, railroadRent, greenRent));
        for (int i = 0; i < 2; i++) deck.add(new SuperWildCard(id++, "Property Wild Card", 0));

        return id;
    }

    private static int addPropertyGroup(List<Card> deck, int id, String[] names, int value,
                                        PropertyColor color, int[] rentTiers) {
        for (String name : names) {
            deck.add(new PropertyCard(id++, name, value, color, rentTiers));
        }
        return id;
    }

    private static int addMoney(List<Card> deck, int id) {
        // 货币卡 (共 20 张)
        for (int i = 0; i < 6; i++) deck.add(new MoneyCard(id++, "Money 1M", 1));
        for (int i = 0; i < 5; i++) deck.add(new MoneyCard(id++, "Money 2M", 2));
        for (int i = 0; i < 3; i++) deck.add(new MoneyCard(id++, "Money 3M", 3));
        for (int i = 0; i < 3; i++) deck.add(new MoneyCard(id++, "Money 4M", 4));
        for (int i = 0; i < 2; i++) deck.add(new MoneyCard(id++, "Money 5M", 5));
        deck.add(new MoneyCard(id++, "Money 10M", 10));
        return id;
    }

    private static int addActions(List<Card> deck, int id) {
        // 1. 具体行动卡 (共 36 张)
        for (int i = 0; i < 2; i++)  deck.add(new DealBreakerCard(id++, "Deal Breaker", 5));
        for (int i = 0; i < 3; i++)  deck.add(new JustSayNoCard(id++, "Just Say No", 4));
        for (int i = 0; i < 3; i++)  deck.add(new SlyDealCard(id++, "Sly Deal", 3));
        for (int i = 0; i < 3; i++)  deck.add(new ForceDealCard(id++, "Forced Deal", 3));
        for (int i = 0; i < 3; i++)  deck.add(new DebtCollectorCard(id++, "Debt Collector", 3));
        for (int i = 0; i < 3; i++)  deck.add(new BirthdayCard(id++, "It's My Birthday", 2));
        for (int i = 0; i < 10; i++) deck.add(new PassGoCard(id++, "Pass Go", 1));
        for (int i = 0; i < 3; i++)  deck.add(new HouseCard(id++, "House", 3));
        for (int i = 0; i < 2; i++)  deck.add(new HotelCard(id++, "Hotel", 4));
        for (int i = 0; i < 2; i++)  deck.add(new DoubleTheRentCard(id++, "Double The Rent", 1));

        // 2. 租金卡 (共 13 张)
        // 双色租金卡 (每种 2 张，共 10 张)
        for (int i = 0; i < 2; i++) {
            deck.add(new RentCard(id++, "Brown/Light Blue Rent", 1, PropertyColor.BROWN, PropertyColor.LIGHT_BLUE));
            deck.add(new RentCard(id++, "Pink/Orange Rent", 1, PropertyColor.PINK, PropertyColor.ORANGE));
            deck.add(new RentCard(id++, "Red/Yellow Rent", 1, PropertyColor.RED, PropertyColor.YELLOW));
            deck.add(new RentCard(id++, "Rent_GreenDeepblue", 1, PropertyColor.GREEN, PropertyColor.DARK_BLUE));
            deck.add(new RentCard(id++, "Railroad/Utility Rent", 1, PropertyColor.RAILROAD, PropertyColor.UTILITY));
        }
        // Any Rent 万能租金卡 (3 张) — 选一人 + 选颜色
        for (int i = 0; i < 3; i++) deck.add(new WildRentCard(id++, "Rent_Rainbow", 3));

        return id;
    }
}
