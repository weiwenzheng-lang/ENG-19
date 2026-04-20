import core.GameManager;

public class Main {
    public static void main(String[] args) {
        // 获取单例主控
        GameManager game = GameManager.getInstance();

        // 比如初始化一个 4 人游戏
        game.initializeGame(4);
        game.startGameLoop();
    }
}