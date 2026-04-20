package core;
import cards.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class Deck {
    private Stack<Card> drawPile;
    private Stack<Card> discardPile;

    public Deck() {
        this.drawPile = new Stack<>();
        this.discardPile = new Stack<>();
    }

    // 将生成的牌放入牌堆并洗牌
    public void initializeDeck(List<Card> allCards) {
        drawPile.addAll(allCards);
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(drawPile);
        System.out.println("System: Deck has been shuffled.");
    }

    // 核心逻辑：抽牌
    public List<Card> drawCards(int amount) {
        List<Card> drawnCards = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            // 如果抽牌堆空了，把弃牌堆洗入抽牌堆（Monopoly Card 的标准规则）
            if (drawPile.isEmpty()) {
                System.out.println("System: Draw pile empty! Reshuffling discard pile...");
                drawPile.addAll(discardPile);
                discardPile.clear();
                shuffle();
            }

            // 如果洗完弃牌堆依然没牌，说明所有牌都在玩家手里，跳出循环
            if (drawPile.isEmpty()) {
                break;
            }
            drawnCards.add(drawPile.pop());
        }
        return drawnCards;
    }

    // 弃牌逻辑：比如玩家打出行动牌后，牌进入弃牌堆
    public void receiveDiscard(Card card) {
        discardPile.push(card);
    }
}