package player;
import cards.Card;
import java.util.ArrayList;
import java.util.List;

public class Hand {
    private List<Card> cardsInHand;

    public Hand() {
        this.cardsInHand = new ArrayList<>();
    }

    // 抽牌入局
    public void addCards(List<Card> newCards) {
        cardsInHand.addAll(newCards);
    }

    // 打出或弃掉某张牌
    public Card removeCard(int index) {
        if (index >= 0 && index < cardsInHand.size()) {
            return cardsInHand.remove(index);
        }
        return null;
    }

    public int getSize() {
        return cardsInHand.size();
    }

    // 检查回合结束时是否超标 (Max 7)
    public boolean requiresDiscard() {
        return cardsInHand.size() > 7;
    }

    public void showHand() {
        System.out.println("--- Current Hand ---");
        for (int i = 0; i < cardsInHand.size(); i++) {
            System.out.println(i + ": " + cardsInHand.get(i).getCardName());
        }
    }
}