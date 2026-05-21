package cards;

import core.GameManager;
import enums.PropertyColor;
import player.Player;
import player.Rentable;

public class WildRentCard extends ActionCard {
    private PropertyColor selectedColor;

    public WildRentCard(int id, String name, int value) {
        super(id, name, value, "WILD_RENT");
    }

    public PropertyColor[] getAvailableColors() {
        return new PropertyColor[]{
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE, PropertyColor.PINK,
                PropertyColor.ORANGE, PropertyColor.RED, PropertyColor.YELLOW,
                PropertyColor.GREEN, PropertyColor.DARK_BLUE, PropertyColor.RAILROAD,
                PropertyColor.UTILITY
        };
    }

    public void setSelectedColor(PropertyColor color) {
        this.selectedColor = color;
    }

    public PropertyColor getSelectedColor() {
        return selectedColor;
    }

    /** Any Rent targets ONE opponent, charges rent for selected color. */
    @Override public boolean requiresTarget() { return true; }

    @Override
    public void executePlayLogic(Player initiator) {
        GameManager gm = GameManager.getInstance();
        Player victim = gm.resolveTargetOrFirstOpponent(initiator);

        if (victim == null) {
            throw new IllegalStateException("no valid target for Wild Rent.");
        }
        if (selectedColor == null) {
            throw new IllegalStateException("no color selected for Wild Rent.");
        }

        Rentable set = initiator.getPropertyArea().getPropertySet(selectedColor);
        int baseRent = (set != null) ? set.calculateRent() : 0;
        if (baseRent <= 0) {
            throw new IllegalStateException("you do not own " + selectedColor + " property with rent to collect.");
        }

        int multiplier = gm.getAndResetRentMultiplier();
        int finalRent = baseRent * multiplier;

        gm.initiateAttack(initiator, victim, () -> {
            victim.getBankArea().pay(finalRent, initiator);
            System.out.printf("[WildRent] %s pays %dM for %s (multiplier: %dx)%n",
                    victim.getPlayerName(), finalRent, selectedColor, multiplier);
        });
    }
}
