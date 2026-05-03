package player;
import cards.PropertyCard;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.List;

public class PropertySet implements Rentable{
    private PropertyColor color;
    private List<PropertyCard> cards;
    private int requiredForFullSet; // 凑齐这套需要几张牌？（比如深蓝色需要2张，绿色需要3张）

    public PropertySet(PropertyColor color, int requiredForFullSet) {
        this.color = color;
        this.cards = new ArrayList<>();
        this.requiredForFullSet = requiredForFullSet;
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
        return cards.size() >= requiredForFullSet;
    }

    @Override
    public int calculateRent() {
        // 老师在 Lec 03 强调的稳健性：防止空指针
        if (cards.isEmpty()) {
            return 0;
        }
        // 获取当前张数对应的基础租金
        // 假设 PropertyCard 类有一个 getRentForCount 方法
        return cards.get(0).getRentForCount(cards.size());
    }
    @Override
    public PropertyColor getColor() {
        return color;
    }
    @Override
    public String getDescription() {
        return color + " 房产套装 (当前 " + cards.size() + "/" + requiredForFullSet + ")";
    }
    //老师在lec02提到的一个重要原则 写toString 方法
    @Override
    public String toString() {
        return String.format("[PropertySet] 颜色:%s | 进度:%d/%d | 当前租金:%dM",
                color, cards.size(), requiredForFullSet, calculateRent());
    }

    public int getCardsCount() {
        return cards.size();
    }
}
