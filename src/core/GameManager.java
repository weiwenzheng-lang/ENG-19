package core;
import player.Player;
import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private static GameManager instance;

    private List<Player> activePlayers;
    private Deck gameDeck; // 引入刚刚写的卡组控制

    private GameManager() {
        activePlayers = new ArrayList<>();
        gameDeck = new Deck();
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // 完整版的游戏初始化
    public void initializeGame(int playerCount) {
        System.out.println("System: Initializing game with " + playerCount + " players.");

        // 1. 利用工厂造牌，并塞入牌堆洗牌
        gameDeck.initializeDeck(CardFactory.createStandardDeck());

        // 2. 创建玩家，并让每人抽 5 张初始手牌
        for (int i = 1; i <= playerCount; i++) {
            Player p = new Player("P" + i, "Player " + i);
            // 这里假设我们在 Player 类里写了 drawInitialHand(Deck deck) 方法
            // p.drawInitialHand(gameDeck);
            activePlayers.add(p);
        }
    }

    // 检查是否有人胜利
    public boolean checkGlobalWinCondition() {
        for (Player p : activePlayers) {
            // 通过调用 PropertyArea 的方法来判断
            if (p.getPropertyArea().countCompletedSets() >= 3) {
                System.out.println("🏆 Game Over! " + p.getPlayerName() + " wins!");
                return true;
            }
        }
        return false;
    }
}