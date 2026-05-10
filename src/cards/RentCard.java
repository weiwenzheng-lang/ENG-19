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
    public void executePlayLogic(Player initiator) {
        // 1. 获取倍率（只获取一次，对本次收租的所有对手生效）
        int multiplier = GameManager.getInstance().getAndResetRentMultiplier();

        // 2. 遍历每个对手
        for (Player opponent : GameManager.getInstance().getOpponents(initiator)) {
            int opponentTotalBaseRent = 0;

            // 3. 检查对手是否有 color1 的完整套装
            Rentable set1 = opponent.getPropertyArea().getPropertySet(color1);
            // 规则：必须存在且是“完整套装”
            if (set1 != null && set1.isComplete()) {
                opponentTotalBaseRent += set1.calculateRent();
            }

            // 4. 检查对手是否有 color2 的完整套装
            // 注意：有些租金卡可能只有一种颜色，此时需要判断 color2 不为空
            if (color2 != null) {
                Rentable set2 = opponent.getPropertyArea().getPropertySet(color2);
                if (set2 != null && set2.isComplete()) {
                    opponentTotalBaseRent += set2.calculateRent();
                }
            }

            // 5. 应用双倍倍数并结算
            int finalRent = opponentTotalBaseRent * multiplier;

            if (finalRent > 0) {
                String logMsg = String.format("💰 [收租] %s 拥有目标完整套装，需支付 %dM (倍率: %dx)",
                        opponent.getPlayerName(), finalRent, multiplier);
                System.out.println(logMsg);

                // 执行支付：从受害者银行给发起者钱
                opponent.getBankArea().pay(finalRent, initiator);
            } else {
                System.out.println("ℹ️ " + opponent.getPlayerName() + " 没有对应的完整套装，无需支付租金。");
            }
        }
    }
}