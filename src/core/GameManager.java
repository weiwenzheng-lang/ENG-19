package core;

import cards.Card;
import player.Player;
import patterns.observer.GameObserver;
import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private static GameManager instance;
    private List<Player> activePlayers;
    private Deck gameDeck;
    private int currentTurnIndex;

    // 观察者列表
    private List<GameObserver> observers;

    private GameManager() {
        activePlayers = new ArrayList<>();
        gameDeck = new Deck();
        observers = new ArrayList<>();
        currentTurnIndex = 0;
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // --- Observer Pattern 管理 ---
    public void addObserver(GameObserver observer) {
        observers.add(observer);
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

    // --- 供行动卡调用的全局方法 ---
    public void drawCardsForPlayer(Player player, int amount) {
        List<cards.Card> drawn = gameDeck.drawCards(amount);
        player.getHand().addCards(drawn);
        notifyEvent(player.getPlayerName() + " drew " + amount + " cards.");
    }

    public void processGlobalPayment(Player payee, int amount) {
        for (Player payer : activePlayers) {
            if (!payer.equals(payee)) {
                notifyEvent(payer.getPlayerName() + " must pay " + amount + "M to " + payee.getPlayerName());
                // 这里后续接入 payer.pay(amount) 的逻辑
            }
        }
    }

    // --- 处理 Sly Deal (偷牌) 的特殊交互逻辑 ---
    public void processSlyDealAttack(Player attacker) {
        // 1. 查找有效的目标玩家 (除了自己以外的其他玩家)
        List<Player> validTargets = new ArrayList<>();
        for (Player p : activePlayers) {
            if (!p.equals(attacker)) {
                validTargets.add(p);
            }
        }

        if (validTargets.isEmpty()) {
            System.out.println("没有可偷窃的目标玩家。");
            return;
        }

        // 2. 简化的交互：直接让系统自动随机偷取/或让攻击者选择 (这里先做提示，后续可扩充选择逻辑)
        // 为了目前能顺畅运行，我们先假设偷取目标列表里的第一个人。
        Player victim = validTargets.get(0);
        notifyEvent("🎯 " + attacker.getPlayerName() + " 锁定了目标: " + victim.getPlayerName() + "，准备偷取其房产！");

        // TODO: 这里后续要完善 "Just Say No" 的反制逻辑以及真正的卡牌转移逻辑。
        // 目前先消耗行动牌并打印信息。
    }

    // ==========================================
    // Lecture 8 Refactoring: Extract Method 示范
    // 将一整个庞大的循环拆分成干净利落的小方法
    // ==========================================

    public void startGameLoop() {
        notifyEvent("The Monopoly Deal Game has officially started!");
        boolean gameWon = false;

        while (!gameWon) {
            Player currentPlayer = activePlayers.get(currentTurnIndex);
            notifyTurnChange(currentPlayer.getPlayerName());

            // 1. 抽牌阶段
            executeDrawPhase(currentPlayer);

            // 2. 出牌阶段
            executeActionPhase(currentPlayer);

            // 3. 弃牌阶段
            executeDiscardPhase(currentPlayer);

            // 4. 胜负判定
            if (checkWinCondition(currentPlayer)) {
                notifyEvent("🎉 " + currentPlayer.getPlayerName() + " has collected 3 full sets and WON THE GAME!");
                gameWon = true;
            } else {
                // 轮转到下一位玩家
                currentTurnIndex = (currentTurnIndex + 1) % activePlayers.size();
            }
        }
    }

    private void executeDrawPhase(Player player) {
        // 如果手牌为空，按规则抽 5 张；否则抽 2 张
        int drawAmount = player.getHand().getSize() == 0 ? 5 : 2;
        notifyEvent(player.getPlayerName() + " is drawing " + drawAmount + " cards.");
        drawCardsForPlayer(player, drawAmount);
    }


    private boolean checkWinCondition(Player player) {
        return player.getPropertyArea().countCompletedSets() >= 3;
    }



    // 补充 initializeGame 方法，否则 Main 里的报错也会存在
    public void initializeGame(int numPlayers) {
        List<cards.Card> allCards = CardFactory.createStandardDeck();
        gameDeck.initializeDeck(allCards);

        for (int i = 1; i <= numPlayers; i++) {
            Player p = new Player("P" + i, "Player_" + i);
            activePlayers.add(p);
        }
        notifyEvent("游戏初始化完成，共 " + numPlayers + " 位玩家。");
    }

    // 替换现有的 executeActionPhase
    private void executeActionPhase(Player player) {
        player.resetActions();
        notifyEvent(player.getPlayerName() + " is in the Action Phase (Max 3 actions).");

        boolean endTurn = false;
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        // 循环条件：行动点 > 0 且玩家没有选择结束
        while (player.getActionsRemaining() > 0 && !endTurn) {
            System.out.println("\n[Action Phase] 剩余行动点: " + player.getActionsRemaining());
            player.getHand().showHand();
            System.out.println("请输入要打出的卡牌编号 (输入 -1 结束出牌阶段):");

            try {
                int choice = Integer.parseInt(scanner.nextLine()); // 稳健的读取方式

                if (choice == -1) {
                    endTurn = true;
                    notifyEvent(player.getPlayerName() + " 主动结束了出牌阶段。");
                } else {
                    cards.Card selectedCard = player.getHand().removeCard(choice);
                    if (selectedCard != null) {
                        player.playCard(selectedCard);
                        notifyEvent(player.getPlayerName() + " 成功打出了: " + selectedCard.getCardName());
                    } else {
                        System.out.println("❌ 无效的编号，该卡牌不存在，请重新输入。");
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ 输入格式错误，请输入纯数字。");
            }
        }
    }

    // 替换现有的 executeDiscardPhase (强制弃牌至 7 张)
    private void executeDiscardPhase(Player player) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        // 循环检查：只要手牌超过 7 张，就必须一直弃牌
        while (player.getHand().requiresDiscard()) {
            notifyEvent("⚠️ 警告: " + player.getPlayerName() + " 手牌超过 7 张 (当前 " + player.getHand().getSize() + " 张)，必须弃牌！");
            player.getHand().showHand();
            System.out.println("请输入要丢弃的卡牌编号: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                cards.Card discarded = player.getHand().removeCard(choice);

                if (discarded != null) {
                    gameDeck.receiveDiscard(discarded); // 扔进系统的弃牌堆
                    notifyEvent("🗑️ " + player.getPlayerName() + " 丢弃了 " + discarded.getCardName());
                } else {
                    System.out.println("❌ 无效的编号，请重新输入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ 输入格式错误，请输入纯数字。");
            }
        }
        notifyEvent("✅ " + player.getPlayerName() + " 弃牌阶段结束，回合完美闭环。");
    }

    // 在 GameManager.java 中添加：
    public List<Player> getOpponents(Player player) {
        List<Player> opponents = new ArrayList<>();
        for (Player p : activePlayers) {
            if (!p.equals(player)) {
                opponents.add(p);
            }
        }
        return opponents;
    }
}