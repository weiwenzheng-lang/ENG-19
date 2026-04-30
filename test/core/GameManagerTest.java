package core;

import core.GameManager;
import player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {
    private GameManager game;

    @BeforeEach
    void setUp() {
        // 重置单例状态并初始化
        game = GameManager.getInstance();
        game.initializeGame(Arrays.asList("Alice", "Bob"));
    }

    @Test
    void testActionPointsDeduction() {
        // 1. 初始化时应该是 3 点
        assertEquals(3, game.getActionsRemaining(), "新回合开始应有 3 点行动力");

        // 2. 模拟打出一张牌（假设手牌索引为 0）
        game.handlePlayCard(0);

        // 3. 断言：行动力应该变成 2
        assertEquals(2, game.getActionsRemaining(), "打出一张牌后，行动力应扣除 1 点");

        // 4. 连续打完剩下的 2 次
        game.handlePlayCard(0);
        game.handlePlayCard(0);

        // 5. 核心测试：断言行动力耗尽
        assertEquals(0, game.getActionsRemaining(), "打出 3 张牌后，行动力必须为 0");

        // 6. 额外测试：尝试打第 4 张牌，行动力不应变成负数，仍应为 0
        game.handlePlayCard(0);
        assertEquals(0, game.getActionsRemaining(), "行动力耗尽后继续出牌，行动力不应继续减少");
    }
}