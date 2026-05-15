package cards;

import core.GameManager;
import player.Player;

public class HotelCard extends ActionCard {

    public HotelCard(int id, String name, int value) {
        super(id, name, value, "HOTEL");
    }

    @Override
    public void executePlayLogic(Player initiator) {
        // 酒店逻辑：尝试在玩家的地产区放置酒店
        // 这里的 addHotelToCompleteSet 是你 PropertyArea 类里需要实现的方法
        boolean success = initiator.getPropertyArea().addHotelToCompleteSet();

        if (success) {
            System.out.println("[Hotel] " + initiator.getPlayerName() + " added a Hotel to a complete set!");
        } else {
            System.out.println("[Hotel] No eligible set — depositing as money instead.");
            initiator.getBankArea().deposit(this);
        }
    }
}