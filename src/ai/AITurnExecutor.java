package ai;

import cards.*;
import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import player.Player;

import java.util.List;

/**
 * AI 回合执行引擎。
 * 使用 JavaFX PauseTransition 控制时序，在 AI 回合中自动执行一系列游戏动作。
 * 所有操作在 JavaFX Application Thread 上执行，确保线程安全。
 */
public class AITurnExecutor {

    /** 每个动作之间的延迟（毫秒），让人类玩家看清 AI 操作 */
    private static final long ACTION_DELAY_MS = 2500;
    /** 等待 JustSayNo 计数器解决时的轮询间隔（毫秒） */
    private static final long POLL_DELAY_MS = 300;

    private final AIPlayerBrain brain;
    private Player aiPlayer;
    private GameManager game;
    private boolean running;

    public AITurnExecutor(AIPlayerBrain brain) {
        this.brain = brain;
    }

    /**
     * 开始执行 AI 的回合。必须在 JavaFX Application Thread 上调用。
     */
    public void startTurn(Player aiPlayer) {
        this.aiPlayer = aiPlayer;
        this.game = GameManager.getInstance();
        this.running = true;
        scheduleNextAction();
    }

    /**
     * 处理 Just Say No 打断。当此 AI 是被攻击方时由 GameController 调用。
     * @param victim 被攻击的 AI 玩家
     */
    public void handleInterrupt(Player victim) {
        if (victim == null) return;
        if (game == null) {
            game = GameManager.getInstance();
        }
        if (game.getPendingVictim() != victim) return;

        // 确保 aiPlayer 已设置（可能在此之前尚未轮到该 AI 的回合）
        if (aiPlayer == null) {
            aiPlayer = victim;
        }

        boolean useJSN = brain.shouldCounterWithJustSayNo(victim, game);
        int jsnIndex = findJustSayNo(victim.getHand().getCards());

        if (useJSN && jsnIndex >= 0) {
            game.counterAttackWithJustSayNo(jsnIndex);
        } else {
            game.resolvePendingAction();
        }
        // 恢复回合调度（如果被打断后状态恢复正常）
        scheduleNextAction();
    }

    /**
     * 停止 AI 执行。
     */
    public void stop() {
        this.running = false;
    }

    // ==================== 内部调度逻辑 ====================

    private void scheduleNextAction() {
        if (!running || game.isGameOver()) return;

        // 检查当前是否还是 AI 的回合
        if (game.getCurrentPlayer() != aiPlayer
                && game.getCurrentState() == GameManager.GameState.NORMAL_TURN) {
            running = false;
            return;
        }

        // 如果游戏状态是等待计数器动作，轮询等待
        if (game.getCurrentState() != GameManager.GameState.NORMAL_TURN) {
            PauseTransition wait = new PauseTransition(Duration.millis(POLL_DELAY_MS));
            wait.setOnFinished(e -> scheduleNextAction());
            wait.play();
            return;
        }

        // 如果不是当前玩家，停止执行
        if (game.getCurrentPlayer() != aiPlayer) {
            running = false;
            return;
        }

        // 做决策并执行
        AIAction action = brain.decideNextAction(aiPlayer, game);
        executeAction(action);
    }

    private void executeAction(AIAction action) {
        if (!running) return;

        try {
            switch (action.getType()) {
                case PLAY_CARD:
                    executePlayCard(action);
                    break;
                case PLAY_DOUBLE_RENT:
                    executeDoubleRent(action);
                    break;
                case DEPOSIT_TO_BANK:
                    game.depositCardToBank(action.getCardIndex());
                    break;
                case DISCARD:
                    game.discardCard(action.getCardIndex());
                    break;
                case END_TURN:
                    running = false;
                    game.endTurn();
                    return;
            }
        } catch (Exception e) {
            // 动作执行失败，记录并尝试下一个动作
            game.logEvent("[AI] Action failed: " + e.getMessage());
        }

        // 延迟后执行下一个动作
        PauseTransition delay = new PauseTransition(Duration.millis(ACTION_DELAY_MS));
        delay.setOnFinished(e -> scheduleNextAction());
        delay.play();
    }

    // ==================== 动作执行细节 ====================

    private void executePlayCard(AIAction action) {
        List<Card> hand = aiPlayer.getHand().getCards();
        int idx = action.getCardIndex();
        if (idx < 0 || idx >= hand.size()) return;

        Card card = hand.get(idx);

        // 预处理：设置万用卡/租金卡的颜色
        applyColor(card, action.getSelectedColor());

        game.executePlayerAction(idx, action.getTargetInfo());
    }

    private void executeDoubleRent(AIAction action) {
        List<Card> hand = aiPlayer.getHand().getCards();
        int doubleIdx = action.getCardIndex();
        int rentIdx = action.getRentCardIndex();
        if (doubleIdx < 0 || doubleIdx >= hand.size()) return;
        if (rentIdx < 0 || rentIdx >= hand.size()) return;

        Card rentCard = hand.get(rentIdx);

        // 预处理：设置租金卡的颜色
        applyColor(rentCard, action.getSelectedColor());

        game.executeDoubleRentAction(doubleIdx, rentIdx, action.getTargetInfo());
    }

    /**
     * 对需要颜色预选的卡牌设置颜色。
     */
    private void applyColor(Card card, PropertyColor color) {
        if (color == null) return;

        if (card instanceof SuperWildCard) {
            ((SuperWildCard) card).setCurrentColor(color);
        } else if (card instanceof PropertyWildCard) {
            ((PropertyWildCard) card).setCurrentColor(color);
        } else if (card instanceof RentCard) {
            ((RentCard) card).setSelectedColor(color);
        } else if (card instanceof WildRentCard) {
            ((WildRentCard) card).setSelectedColor(color);
        }
    }

    // ==================== 辅助方法 ====================

    private int findJustSayNo(List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardName().equals("Just Say No")) {
                return i;
            }
        }
        return -1;
    }
}
