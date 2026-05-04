package enums;

public enum PropertyColor {
    BROWN(2),
    LIGHT_BLUE(3),
    PINK(3),
    ORANGE(3),
    RED(3),
    YELLOW(3),
    GREEN(3),
    DARK_BLUE(2),
    RAILROAD(4),
    UTILITY(2),
    WILD(0);
    private final int requiredCount;
    PropertyColor(int requiredCount) {
        this.requiredCount = requiredCount;
    }

    public int getRequiredCount() {
        return requiredCount;
    }
}
