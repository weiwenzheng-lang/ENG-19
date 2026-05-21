package player;
import cards.PropertyCard;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PropertySet implements Rentable{
    private PropertyColor color;
    private List<PropertyCard> cards;

    public PropertySet(PropertyColor color) {
        this.color = color;
        this.cards = new ArrayList<>();
    }
    @Override
    public void addProperty(PropertyCard card) {
        //确保颜色相同
        if (card.getColorGroup() == this.color) {
            cards.add(card);
        }
    }

    // 核心判断：这套房产是否已经凑齐？(用于触发游戏胜利条件或允许建房子/酒店)
    @Override
    public boolean isComplete() {
        if (cards.size() < color.getRequiredCount()) return false;
        // Must have at least one standard (non-wild) property card
        for (PropertyCard c : cards) {
            if (!(c instanceof cards.PropertyWildCard || c instanceof cards.SuperWildCard)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int calculateRent() {
        if (cards.isEmpty()) {
            return 0;
        }
        return color.getRentForCount(cards.size());
    }
    @Override
    public PropertyColor getColor() {
        return color;
    }
    @Override
    public String getDescription() {
        return color + " 房产套装 (当前 " + cards.size() + "/" + color.getRequiredCount() + ")";
    }
    //老师在lec02提到的一个重要原则 写toString 方法
    @Override
    public String toString() {
        return String.format("[PropertySet] 颜色:%s | 进度:%d/%d | 当前租金:%dM",
                color, cards.size(),color.getRequiredCount(), calculateRent());
    }

    public int getCardsCount() {
        return cards.size();
    }
    public void removeProperty(PropertyCard card) {
        this.cards.remove(card);
    }

    public List<PropertyCard> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
