import core.GameManager;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 1. 获取核心大脑单例
        GameManager game = GameManager.getInstance();

        // 2. 准备玩家名单（你可以根据你们之前的逻辑来，这里先写死几个名字作为测试）
        List<String> playerNames = Arrays.asList("玩家A", "玩家B", "玩家C");

        // 3. 用新方法初始化游戏并自动开启第一回合！
        game.initializeGame(playerNames);

        System.out.println("✅ 游戏底层引擎启动成功，等待 UI 接入...");
    }
}