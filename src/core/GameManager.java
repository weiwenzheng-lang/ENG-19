package core;

import cards.Card;
import player.Player;
import patterns.observer.GameObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GameManager - 游戏核心控制类 (单例模式)
 * 重构说明：
 * 1. 彻底移除了 Scanner 和硬编码的输入逻辑，改为由 UI 驱动。
 * 2. 细化了回合状态管理，确保每一阶段逻辑单一 (Single Responsibility)。
 * 3. 增强了观察者模式通知，确保 UI 能实时获取所有状态变更。
 */
public class GameManager {
    private static GameManager instance;

    private List<Player> activePlayers;
    private Deck gameDeck;
    private int currentTurnIndex;
    private int actionsRemaining; // 当前回合剩余行动力
    private boolean isGameOver;
    private TargetInfo currentTargetInfo;
    private int rentMultiplier = 1; // 新增：用于存储租金倍率，默认为 1

    // 观察者列表
    private List<GameObserver> observers;

    private GameManager() {
        this.activePlayers = new ArrayList<>();
        this.gameDeck = new Deck();
        this.observers = new ArrayList<>();
        this.currentTurnIndex = 0;
        this.isGameOver = false;
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // 1. 游戏初始化逻辑 (Initialization)

    public void initializeGame(List<String> playerNames) {
        currentTurnIndex = 0;
        actionsRemaining = 0;
        isGameOver = false;
        currentTargetInfo = null;
        rentMultiplier = 1;
        resetState();

        // 1. 初始化牌堆
        gameDeck.initializeDeck(CardFactory.createInitialDeck());

        // 2. 创建玩家
        activePlayers.clear();
        for (int i = 0; i < playerNames.size(); i++) {
            Player newPlayer = new Player(String.valueOf(i), playerNames.get(i));

            // 🌟 核心修复：游戏开始前，给每位玩家发 5 张初始手牌！
            List<Card> startingCards = gameDeck.drawCards(5);
            newPlayer.getHand().addCards(startingCards);

            activePlayers.add(newPlayer);
        }

        notifyEvent("游戏初始化完成，共 " + activePlayers.size() + " 位玩家，每人已发 5 张初始手牌。");

        // 3. 开始第一回合 (此时玩家已有5张底牌，加上回合开始抽的2张，共有7张牌可以打)
        startNewTurn();
    }

    // 2. 回合生命周期管理 (Turn Lifecycle)


    /**
     * 开始一个新回合。
     * 重构点：将原有的死循环拆解为独立的方法调用。
     */
    public void startNewTurn() {
        if (isGameOver) return;

        Player currentPlayer = getCurrentPlayer();
        actionsRemaining = 3;
        rentMultiplier = 1; // Reset at turn start

        notifyTurnChange(currentPlayer.getPlayerName());

        // 自动抽2张牌
        List<Card> drawn = gameDeck.drawCards(2);
        currentPlayer.getHand().addCards(drawn);

        notifyEvent(currentPlayer.getPlayerName() + " 抽了 2 张牌。");
    }

    /**
     * 核心动作入口：供 UI 调用，当玩家点击某张牌时执行。
     * @param cardIndex 玩家手牌的索引
     */
    public void handlePlayCard(int cardIndex) {
        executePlayerAction(cardIndex, null);
    }

    public void executePlayerAction(int cardIndex, TargetInfo target) {
        if (isGameOver) {
            notifyEvent("游戏已经结束，请开始新游戏或退出游戏。");
            return;
        }

        if (actionsRemaining <= 0) {
            notifyEvent("⚠️ 行动力不足！请结束回合。");
            return;
        }

        Player p = getCurrentPlayer();
        Card selectedCard = p.getHand().getCard(cardIndex); // 需在 Hand 类补充 getCard 方法

        if (selectedCard != null) {
            // 执行卡牌效果 (多态调用)
            currentTargetInfo = target;
            try {
                p.playCard(selectedCard);
            } finally {
                currentTargetInfo = null;
            }
            p.getHand().removeCard(cardIndex);
            gameDeck.receiveDiscard(selectedCard);

            actionsRemaining--;
            notifyEvent(p.getPlayerName() + " 打出了: " + selectedCard.getCardName() + " (剩余行动: " + actionsRemaining + ")");

            checkWinCondition();
        }
    }

    /**
     * 弃牌：不消耗行动力，将手牌直接弃入弃牌堆。
     * 用于处理手牌超过 7 张时必须弃牌的情况。
     */
    public void discardCard(int cardIndex) {
        if (isGameOver) {
            notifyEvent("游戏已经结束，请开始新游戏或退出游戏。");
            return;
        }

        Player p = getCurrentPlayer();
        Card selectedCard = p.getHand().removeCard(cardIndex);
        if (selectedCard != null) {
            gameDeck.receiveDiscard(selectedCard);
            notifyEvent(p.getPlayerName() + " 弃掉: " + selectedCard.getCardName());
        }
    }

    public void depositCardToBank(int cardIndex) {
        if (isGameOver) {
            notifyEvent("游戏已经结束，请开始新游戏或退出游戏。");
            return;
        }

        if (actionsRemaining <= 0) {
            notifyEvent("⚠️ 行动力不足！请结束回合。");
            return;
        }

        Player p = getCurrentPlayer();
        Card selectedCard = p.getHand().removeCard(cardIndex);
        if (selectedCard != null) {
            p.getBankArea().deposit(selectedCard);
            actionsRemaining--;
            notifyEvent(p.getPlayerName() + " 存入银行: " + selectedCard.getCardName()
                    + " (剩余行动: " + actionsRemaining + ")");
            checkWinCondition();
        }
    }

    /**
     * 结束当前回合
     */
    public void endTurn() {
        if (isGameOver) {
            notifyEvent("游戏已经结束，请开始新游戏或退出游戏。");
            return;
        }

        Player p = getCurrentPlayer();

        // 检查手牌上限 (7张)
        if (p.getHand().requiresDiscard()) {
            notifyEvent("⚠️ " + p.getPlayerName() + " 需要弃牌至 7 张！");
            // 这里不阻塞，由 UI 判断状态并调用 discard 方法
            return;
        }

        // 切换前先检查胜利条件
        checkWinCondition();

        // 切换到下一个玩家
        currentTurnIndex = (currentTurnIndex + 1) % activePlayers.size();
        startNewTurn();
    }

    // ==========================================
    // 3. 业务逻辑 (Business Logic)
    // ==========================================

    private void checkWinCondition() {
        for (Player p : activePlayers) {
            int completedSets = p.getPropertyArea().countCompletedSets();
            if (completedSets >= 3) {
                isGameOver = true;
                notifyEvent("Congratulations " + p.getPlayerName() + " collected 3 full property sets and wins!");
                return;
            }
        }
    }

    // 激活双倍租金效果
    public void activateDoubleRent() {
        this.rentMultiplier *= 2; // 支持叠加逻辑（如果一回合打出两张，就是4倍）
        notifyEvent("📢 [Double rent] The rent to be collected next time will be changed to " + rentMultiplier + " 倍！");
    }

    /**
     * 获取当前的租金倍率，并立即重置为 1
     * 供 RentCard 或收租逻辑调用
     */
    public int getAndResetRentMultiplier() {
        int current = this.rentMultiplier;
        this.rentMultiplier = 1; // 使用后重置，确保不影响下下张租金卡
        return current;
    }

    public Player getCurrentPlayer() {
        return activePlayers.get(currentTurnIndex);
    }

    public Player resolveTargetOrFirstOpponent(Player initiator) {
        if (currentTargetInfo != null && currentTargetInfo.getTargetPlayer() != null) {
            return currentTargetInfo.getTargetPlayer();
        }
        List<Player> opponents = getOpponents(initiator);
        return opponents.isEmpty() ? null : opponents.get(0);
    }

    public List<Player> getActivePlayers() {
        return Collections.unmodifiableList(activePlayers);
    }

    public Deck getGameDeck() {
        return gameDeck;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    /**
     * 获取当前玩家剩余行动力
     * 供 UI 显示和 JUnit 测试使用
     */
    public int getActionsRemaining() {
        return actionsRemaining;
    }

    // ==========================================
    // 4. 观察者管理 (Observer Pattern)
    // ==========================================

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyEvent(String message) {
        for (GameObserver o : observers) {
            o.onGameEvent(message);
        }
    }

    private void notifyTurnChange(String playerName) {
        for (GameObserver o : observers) {
            o.onTurnChanged(playerName);
        }
    }


    // ==========================================
    // 5. 异步中断机制 (Interrupt & State Machine)
    // 专门处理 "Just Say No" 这种打断常规流程的交互
    // ==========================================

    // 游戏状态枚举
    public enum GameState {
        NORMAL_TURN,               // 正常出牌阶段
        WAITING_FOR_COUNTER_ACTION // 等待对方使用 Just Say No 阶段
    }

    private GameState currentState = GameState.NORMAL_TURN;
    private Runnable pendingAction; // 被挂起的危险动作（例如：偷取房产的具体代码）
    private Player pendingVictim;   // 当前正在被攻击、需要做出回应的玩家

    /**
     * 供攻击类卡牌（如 SlyDealCard, RentCard）调用。
     * 发起攻击，并将实际的伤害逻辑包装成 Runnable 挂起。
     */
    public void initiateAttack(Player victim, Runnable action) {
        this.currentState = GameState.WAITING_FOR_COUNTER_ACTION;
        this.pendingAction = action;
        this.pendingVictim = victim;

        // 发送特殊格式的事件通知，UI 层监听到后会弹窗询问 victim
        notifyEvent("⚠️ [INTERRUPT_REQUEST] " + victim.getPlayerName() + " 遭到了针对！是否打出 Just Say No？");
    }

    /**
     * UI 回调 API：受害者选择“默默承受”（或超时、没有反制卡）
     */
    public void resolvePendingAction() {
        if (currentState == GameState.WAITING_FOR_COUNTER_ACTION && pendingAction != null) {
            pendingAction.run();
            notifyEvent("✅ 动作结算完成。");
            resetState();
            checkWinCondition();
        }
    }

    /**
     * UI 回调 API：受害者打出了 "Just Say No"
     * @param cardIndex Just Say No 在手牌中的位置
     */
    public void counterAttackWithJustSayNo(int cardIndex) {
        if (currentState == GameState.WAITING_FOR_COUNTER_ACTION && pendingVictim != null) {
            Card card = pendingVictim.getHand().getCard(cardIndex); // 获取但不立刻删除

            // 严谨校验：确保他打出的真的是反制卡
            if (card != null && card.getCardName().equals("Just Say No")) {
                pendingVictim.getHand().removeCard(cardIndex); // 消耗掉这张卡
                gameDeck.receiveDiscard(card); // 放入弃牌堆

                notifyEvent("🛡️ 完美防御！" + pendingVictim.getPlayerName() + " 打出了 Just Say No，攻击被无效化！");
                resetState(); // 状态恢复正常，挂起的攻击被直接抛弃！
            } else {
                notifyEvent("❌ 非法的防御卡牌！");
            }
        }
    }

    public Player getPendingVictim() {
        return pendingVictim;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    private void resetState() {
        this.currentState = GameState.NORMAL_TURN;
        this.pendingAction = null;
        this.pendingVictim = null;
    }

    // ==========================================
    // 6. 辅助方法 (Helper Methods)
    // ==========================================

    /**
     * 获取除指定玩家以外的所有对手
     * 供 RentCard (收租卡) 等需要遍历对手的卡牌调用
     */
    /**
     * 通用攻击入口：解析目标 → null 检查 → 包装进 initiateAttack。
     * 供 SlyDeal/ForceDeal/DealBreaker/DebtCollector 等卡牌调用。
     */
    public void initiateTargetedAttack(Player initiator, java.util.function.Consumer<Player> attackAction) {
        Player victim = resolveTargetOrFirstOpponent(initiator);
        if (victim == null) {
            System.out.println("No valid target for this action.");
            return;
        }
        initiateAttack(victim, () -> attackAction.accept(victim));
    }

    public List<Player> getOpponents(Player player) {
        List<Player> opponents = new ArrayList<>();
        // 遍历所有存活的玩家
        for (Player p : activePlayers) {
            // 如果不是自己，就加入到对手列表里
            if (!p.equals(player)) {
                opponents.add(p);
            }
        }
        return opponents;
    }

    /**
     * 处理全球支付（如：生日卡）
     * @param initiator 发起者（收钱的人）
     * @param amount 每人要交的金额
     */
    public void processGlobalPayment(Player initiator, int amount) {
        List<Player> opponents = getOpponents(initiator);

        notifyEvent("🎂 祝 " + initiator.getPlayerName() + " 生日快乐！每位对手需支付 " + amount + "M。");

        for (Player victim : opponents) {
            // 逻辑：从受害者银行扣钱，给发起者
            // 注意：这里调用的是队员 4 负责的 BankArea 逻辑
            // 为了保证编译通过，请确保 Player 类有 getBankArea() 方法
            victim.getBankArea().pay(amount, initiator);

            notifyEvent("💸 " + victim.getPlayerName() + " 向生日星支付了 " + amount + "M。");
        }

        // 支付完成后通知 UI 刷新数据
        notifyEvent("✅ 生日礼金收取完毕！");
    }

    /**
     * 为指定玩家抽取指定数量的牌
     * 供 Pass Go (通行证) 等需要额外抽牌的卡牌调用
     */
    public void drawCardsForPlayer(Player player, int count) {
        // 1. 从牌堆抽牌
        List<Card> drawnCards = gameDeck.drawCards(count);

        // 2. 加入玩家手牌
        player.getHand().addCards(drawnCards);

        // 3. 通知 UI 界面更新
        notifyEvent("🃏 " + player.getPlayerName() + " 额外抽了 " + count + " 张牌！");
    }
}
