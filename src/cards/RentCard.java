package cards;

import core.GameManager;
import enums.PropertyColor;
import player.Player;
import player.Rentable;

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
        List<Player> opponents = gm.getOpponents(initiator);
        if (opponents.isEmpty()) {
            return;
        }

        Rentable set = initiator.getPropertyArea().getPropertySet(selectedColor);
        if (set == null) {
            throw new IllegalStateException("you do not own " + selectedColor + " property.");
        }

        int baseRent = set.calculateRent();
        if (baseRent <= 0) {
            throw new IllegalStateException("selected property has no rent to collect.");
        }

        int multiplier = gm.getAndResetRentMultiplier();
        int finalRent = baseRent * multiplier;

        gm.initiateGroupAttack(opponents, opponent -> opponent.getBankArea().pay(finalRent, initiator));
    }
}
