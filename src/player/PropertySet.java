package player;
import cards.PropertyCard;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

public class PropertySet {
    private PropertyColor color;
    private List<PropertyCard> cards;
    private int requiredForFullSet; // 凑齐这套需要几张牌？（比如深蓝色需要2张，绿色需要3张）

    public PropertySet(PropertyColor color, int requiredForFullSet) {
        this.color = color;
        this.cards = new ArrayList<>();
        this.requiredForFullSet = requiredForFullSet;
    }

    public void addProperty(PropertyCard card) {
        cards.add(card);
    }

    // 核心判断：这套房产是否已经凑齐？(用于触发游戏胜利条件或允许建房子/酒店)
    public boolean isComplete() {
        return cards.size() >= requiredForFullSet;
    }

    public PropertyColor getColor() {
        return color;
    }

    public int getCardsCount() {
        return cards.size();
    }
}