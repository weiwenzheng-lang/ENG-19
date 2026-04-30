package core;

import core.Deck;
import cards.Card;
import cards.MoneyCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {
    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
        List<Card> initialCards = new ArrayList<>();
        // 故意只放入 3 张牌，用于测试抽空重洗逻辑
        initialCards.add(new MoneyCard(1, "1M", 1));
        initialCards.add(new MoneyCard(2, "2M", 2));
        initialCards.add(new MoneyCard(3, "3M", 3));
        deck.initializeDeck(initialCards);
    }

    @Test
    void testDrawCardsNormally() {
        List<Card> drawn = deck.drawCards(2);
        assertEquals(2, drawn.size(), "应该成功抽出 2 张牌");
    }

    @Test
    void testReshuffleWhenEmpty() {
        // 先抽走所有的 3 张牌
        deck.drawCards(3);

        // 模拟游戏过程：玩家打出 2 张牌，进入弃牌堆
        deck.receiveDiscard(new MoneyCard(4, "4M", 4));
        deck.receiveDiscard(new MoneyCard(5, "5M", 5));

        // 核心测试：牌堆此时是空的，再次抽牌应触发弃牌堆洗入抽牌堆机制
        List<Card> drawnAgain = deck.drawCards(2);
        assertEquals(2, drawnAgain.size(), "当抽牌堆为空时，系统应自动将弃牌堆重洗并成功发牌");
    }
}