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
            System.out.println("No valid target for Wild Rent.");
            return;
        }

        Rentable set = victim.getPropertyArea().getPropertySet(selectedColor);
        int baseRent = (set != null) ? set.calculateRent() : 0;
        if (baseRent <= 0) {
            System.out.println(victim.getPlayerName() + " has no " + selectedColor + " property, no rent.");
            return;
        }

        int multiplier = gm.getAndResetRentMultiplier();
        int finalRent = baseRent * multiplier;

        gm.initiateAttack(victim, () -> {
            victim.getBankArea().pay(finalRent, initiator);
            System.out.printf("[WildRent] %s pays %dM for %s (multiplier: %dx)%n",
                    victim.getPlayerName(), finalRent, selectedColor, multiplier);
        });
    }
}