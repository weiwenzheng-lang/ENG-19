package cards;
import player.Player;

public class ActionCard extends Card {
    private String actionType; // 例如 "Sly Deal", "Just Say No"

    public ActionCard(int id, String name, int value, String actionType) {
        super(id, name, value);
        this.actionType = actionType;
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 逻辑：触发特殊的行动卡效果或异步事件
        System.out.println(initiator.getPlayerName() + " played action: " + actionType);
    }
}