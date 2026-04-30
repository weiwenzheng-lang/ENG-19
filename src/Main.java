import core.GameManager;
import patterns.observer.ConsoleLogger;

public class Main {
    public static void main(String[] args) {
        // 1. 获取单例主控
        GameManager game = GameManager.getInstance();

        // 2. [关键修复] 注册观察者！如果没有这一步，游戏过程将没有任何输出
        game.addObserver(new ConsoleLogger());

        // 3. 初始化并启动 2 人游戏 (方便测试)
        game.initializeGame(2);
        game.startGameLoop();
    }
}