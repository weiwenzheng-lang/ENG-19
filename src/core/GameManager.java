package core;
import player.Player;
import java.util.ArrayList;
import java.util.List;

public class GameManager {
    // 单例模式实例
    private static GameManager instance;

    private List<Player> activePlayers;
    private int currentTurnIndex;

    // 私有构造函数，防止外部直接 new
    private GameManager() {
        activePlayers = new ArrayList<>();
        currentTurnIndex = 0;
    }

    // 获取全局唯一实例
    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    // 初始化游戏
    public void initializeGame(int playerCount) {
        System.out.println("Initializing game with " + playerCount + " players.");
        for (int i = 1; i <= playerCount; i++) {
            activePlayers.add(new Player("P" + i, "Player " + i));
        }
        // TODO: 初始化卡组 (Deck) 并洗牌
    }

    // 执行主循环
    public void startGameLoop() {
        System.out.println("Game started!");
        // TODO: 完善回合轮转逻辑
    }
}