package cards;

import player.Player;
import player.Rentable;
import enums.PropertyColor;
import core.GameManager;

public class RentCard extends ActionCard {
    private PropertyColor color1;
    private PropertyColor color2;

    public RentCard(int id, String name, int value, PropertyColor c1, PropertyColor c2) {
        super(id, name, value, "RENT");
        this.color1 = c1;
        this.color2 = c2;
    }

    @Override
    public void executePlayLogic(Player player) {
        // 1. 获取所有对手
        for (Player opponent : GameManager.getInstance().getOpponents(player)) {
            // 2. 计算租金 (装饰器模式在这里发威了！)
            // 不管对方有没有盖房子，只要调用 calculateRent()，装饰器会自动算出加成后的价格
            Rentable set = opponent.getPropertyArea().getPropertySet(color1);
            int rentAmount = (set != null) ? set.calculateRent() : 0;

            if (rentAmount > 0) {
                System.out.println("💰 " + opponent.getPlayerName() + " 需要向 " + player.getPlayerName() + " 支付 " + rentAmount + "M 租金");
                // 3. 扣钱逻辑 (需要队友在 BankArea 实现)
                opponent.getBankArea().pay(rentAmount, player);
            }
        }
    }
}