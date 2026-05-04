package player;
import cards.PropertyCard;
import enums.PropertyColor;
import java.util.ArrayList;
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
        return cards.size() >= color.getRequiredCount();
    }

    @Override
    public int calculateRent() {
        // 老师在 Lec 03 强调的稳健性：防止空指针
        if (cards.isEmpty()) {
            return 0;
        }
        // 获取当前张数对应的基础租金
        return cards.get(0).getRentForCount(cards.size());
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
}
