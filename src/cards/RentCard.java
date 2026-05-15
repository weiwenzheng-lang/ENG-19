package cards;

import player.Player;
import player.Rentable;
import enums.PropertyColor;
import core.GameManager;
import java.util.List;

public class RentCard extends ActionCard {
    private final PropertyColor color1;
    private final PropertyColor color2;
    private PropertyColor selectedColor;

    public RentCard(int id, String name, int value, PropertyColor c1, PropertyColor c2) {
        super(id, name, value, "RENT");
        this.color1 = c1;
        this.color2 = c2;
        this.selectedColor = c1;
    }

    public boolean isMultiColor() {
        return color1 != color2;
    }

    public PropertyColor[] getColorOptions() {
        if (isMultiColor()) {
            return new PropertyColor[]{color1, color2};
        }
        return new PropertyColor[]{color1};
    }

    public void setSelectedColor(PropertyColor color) {
        if (color == color1 || color == color2) {
            this.selectedColor = color;
        }
    }

    public PropertyColor getSelectedColor() {
        return selectedColor;
    }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager gm = GameManager.getInstance();
        int multiplier = gm.getAndResetRentMultiplier();
        List<Player> opponents = gm.getOpponents(initiator);

        if (opponents.isEmpty()) return;

        // Wrap in initiateAttack so Just Say No can counter
        gm.initiateAttack(opponents.get(0), () -> {
            for (Player opponent : opponents) {
                int baseRent = 0;

                Rentable set = opponent.getPropertyArea().getPropertySet(selectedColor);
                if (set != null) {
                    baseRent = set.calculateRent();
                }

                int finalRent = baseRent * multiplier;

                if (finalRent > 0) {
                    System.out.printf("[Rent] %s pays %dM (multiplier: %dx)%n",
                            opponent.getPlayerName(), finalRent, multiplier);
                    opponent.getBankArea().pay(finalRent, initiator);
                } else {
                    System.out.println(opponent.getPlayerName() + " has no matching property, no rent to pay.");
                }
            }
        });
    }
}