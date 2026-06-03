package core;

import cards.BirthdayCard;
import cards.Card;
import cards.DealBreakerCard;
import cards.DebtCollectorCard;
import cards.DoubleTheRentCard;
import cards.ForceDealCard;
import cards.HotelCard;
import cards.HouseCard;
import cards.JustSayNoCard;
import cards.MoneyCard;
import cards.PassGoCard;
import cards.PropertyCard;
import cards.PropertyWildCard;
import cards.RentCard;
import cards.SlyDealCard;
import cards.SuperWildCard;
import cards.WildRentCard;
import enums.PropertyColor;

import java.util.ArrayList;
import java.util.List;

public class CardFactory {
    // Builds the official 106-card Monopoly Deal deck.
    public static List<Card> createInitialDeck() {
        List<Card> deck = new ArrayList<>();
        int id = 1;

        id = addProperties(deck, id);
        id = addMoney(deck, id);
        addActions(deck, id);

        if (deck.size() != 106) {
            throw new IllegalStateException("Deck size error. Expected 106 cards, created: " + deck.size());
        }
        return deck;
    }

    // Adds standard property cards and wild property cards.
    private static int addProperties(List<Card> deck, int id) {
        id = addStandardProperties(deck, id);
        return addWildProperties(deck, id);
    }

    // Adds fixed-color property cards.
    private static int addStandardProperties(List<Card> deck, int id) {
        id = addPropertyGroup(deck, id, new String[]{"Mediterranean Avenue", "Baltic Avenue"}, 1,
                PropertyColor.BROWN, PropertyColor.BROWN.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"Connecticut Avenue", "Oriental Avenue", "Vermont Avenue"},
                1, PropertyColor.LIGHT_BLUE, PropertyColor.LIGHT_BLUE.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"STCharles Place", "States Avenue", "Virginia Avenue"},
                2, PropertyColor.PINK, PropertyColor.PINK.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"STJames Place", "NewYork Avenue", "Tennessee Avenue"},
                2, PropertyColor.ORANGE, PropertyColor.ORANGE.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"Illinois Avenue", "Indiana Avenue", "Kentucky Avenue"},
                3, PropertyColor.RED, PropertyColor.RED.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"Atlantic Avenue", "Marvin Gardens", "Ventnor Avenue"},
                3, PropertyColor.YELLOW, PropertyColor.YELLOW.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"North Carolina Avenue", "Pacific Avenue", "Pennsylvania Avenue"},
                4, PropertyColor.GREEN, PropertyColor.GREEN.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"Boardwalk", "Park Place"}, 4,
                PropertyColor.DARK_BLUE, PropertyColor.DARK_BLUE.getRentTiers());
        id = addPropertyGroup(deck, id, new String[]{"B&O Railroad", "Pennsylvania Railroad", "Reading Railroad",
                "Short Line"}, 2, PropertyColor.RAILROAD, PropertyColor.RAILROAD.getRentTiers());
        return addPropertyGroup(deck, id, new String[]{"Electric Company", "Water works"}, 2,
                PropertyColor.UTILITY, PropertyColor.UTILITY.getRentTiers());
    }

    // Adds two-color property wild cards and super wild cards.
    private static int addWildProperties(List<Card> deck, int id) {
        deck.add(new PropertyWildCard(id++, "Property Wild card_BlueBrown", 1,
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE,
                PropertyColor.BROWN.getRentTiers(), PropertyColor.LIGHT_BLUE.getRentTiers()));
        for (int i = 0; i < 2; i++) {
            deck.add(new PropertyWildCard(id++, "Property Wild Card_OrangePink", 2,
                    PropertyColor.PINK, PropertyColor.ORANGE,
                    PropertyColor.PINK.getRentTiers(), PropertyColor.ORANGE.getRentTiers()));
        }
        for (int i = 0; i < 2; i++) {
            deck.add(new PropertyWildCard(id++, "Property Wild Card_YellowRed", 3,
                    PropertyColor.RED, PropertyColor.YELLOW,
                    PropertyColor.RED.getRentTiers(), PropertyColor.YELLOW.getRentTiers()));
        }
        deck.add(new PropertyWildCard(id++, "Property Wild card_GreenDeepblue", 4,
                PropertyColor.GREEN, PropertyColor.DARK_BLUE,
                PropertyColor.GREEN.getRentTiers(), PropertyColor.DARK_BLUE.getRentTiers()));
        deck.add(new PropertyWildCard(id++, "Property Wild Card_EnterpriseRailroad", 2,
                PropertyColor.RAILROAD, PropertyColor.UTILITY,
                PropertyColor.RAILROAD.getRentTiers(), PropertyColor.UTILITY.getRentTiers()));
        deck.add(new PropertyWildCard(id++, "Property Wild Card_BlueRailroad", 4,
                PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD,
                PropertyColor.LIGHT_BLUE.getRentTiers(), PropertyColor.RAILROAD.getRentTiers()));
        deck.add(new PropertyWildCard(id++, "Property Wild Card_RailroadGreen", 4,
                PropertyColor.RAILROAD, PropertyColor.GREEN,
                PropertyColor.RAILROAD.getRentTiers(), PropertyColor.GREEN.getRentTiers()));
        for (int i = 0; i < 2; i++) {
            deck.add(new SuperWildCard(id++, "Property Wild Card", 0));
        }
        return id;
    }

    // Adds all cards in one color group.
    private static int addPropertyGroup(List<Card> deck, int id, String[] names, int value,
                                        PropertyColor color, int[] rentTiers) {
        for (String name : names) {
            deck.add(new PropertyCard(id++, name, value, color, rentTiers));
        }
        return id;
    }

    // Adds money cards.
    private static int addMoney(List<Card> deck, int id) {
        for (int i = 0; i < 6; i++) deck.add(new MoneyCard(id++, "Money 1M", 1));
        for (int i = 0; i < 5; i++) deck.add(new MoneyCard(id++, "Money 2M", 2));
        for (int i = 0; i < 3; i++) deck.add(new MoneyCard(id++, "Money 3M", 3));
        for (int i = 0; i < 3; i++) deck.add(new MoneyCard(id++, "Money 4M", 4));
        for (int i = 0; i < 2; i++) deck.add(new MoneyCard(id++, "Money 5M", 5));
        deck.add(new MoneyCard(id++, "Money 10M", 10));
        return id;
    }

    // Adds action and rent cards.
    private static int addActions(List<Card> deck, int id) {
        for (int i = 0; i < 2; i++) deck.add(new DealBreakerCard(id++, "Deal Breaker", 5));
        for (int i = 0; i < 3; i++) deck.add(new JustSayNoCard(id++, "Just Say No", 4));
        for (int i = 0; i < 3; i++) deck.add(new SlyDealCard(id++, "Sly Deal", 3));
        for (int i = 0; i < 3; i++) deck.add(new ForceDealCard(id++, "Forced Deal", 3));
        for (int i = 0; i < 3; i++) deck.add(new DebtCollectorCard(id++, "Debt Collector", 3));
        for (int i = 0; i < 3; i++) deck.add(new BirthdayCard(id++, "It's My Birthday", 2));
        for (int i = 0; i < 10; i++) deck.add(new PassGoCard(id++, "Pass Go", 1));
        for (int i = 0; i < 3; i++) deck.add(new HouseCard(id++, "House", 3));
        for (int i = 0; i < 2; i++) deck.add(new HotelCard(id++, "Hotel", 4));
        for (int i = 0; i < 2; i++) deck.add(new DoubleTheRentCard(id++, "Double The Rent", 1));

        for (int i = 0; i < 2; i++) {
            deck.add(new RentCard(id++, "Brown/Light Blue Rent", 1,
                    PropertyColor.BROWN, PropertyColor.LIGHT_BLUE));
            deck.add(new RentCard(id++, "Pink/Orange Rent", 1,
                    PropertyColor.PINK, PropertyColor.ORANGE));
            deck.add(new RentCard(id++, "Red/Yellow Rent", 1,
                    PropertyColor.RED, PropertyColor.YELLOW));
            deck.add(new RentCard(id++, "Rent_GreenDeepblue", 1,
                    PropertyColor.GREEN, PropertyColor.DARK_BLUE));
            deck.add(new RentCard(id++, "Railroad/Utility Rent", 1,
                    PropertyColor.RAILROAD, PropertyColor.UTILITY));
        }
        for (int i = 0; i < 3; i++) {
            deck.add(new WildRentCard(id++, "Rent_Rainbow", 3));
        }

        return id;
    }
}
