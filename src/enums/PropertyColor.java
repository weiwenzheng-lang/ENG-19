package enums;

public enum PropertyColor {
    BROWN(2, new int[]{1, 2}),
    LIGHT_BLUE(3, new int[]{1, 2, 3}),
    PINK(3, new int[]{1, 2, 4}),
    ORANGE(3, new int[]{1, 3, 5}),
    RED(3, new int[]{2, 3, 6}),
    YELLOW(3, new int[]{2, 4, 6}),
    GREEN(3, new int[]{2, 4, 7}),
    DARK_BLUE(2, new int[]{3, 8}),
    RAILROAD(4, new int[]{1, 2, 3, 4}),
    UTILITY(2, new int[]{1, 2}),
    WILD(0, null);

    private final int requiredCount;
    private final int[] rentTiers;

    PropertyColor(int requiredCount, int[] rentTiers) {
        this.requiredCount = requiredCount;
        this.rentTiers = rentTiers;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public int[] getRentTiers() {
        return rentTiers;
    }

    public int getRentForCount(int count) {
        if (rentTiers == null || count <= 0) return 0;
        int index = Math.min(count, rentTiers.length) - 1;
        return rentTiers[index];
    }

    public String getColorHex() {
        switch (this) {
            case DARK_BLUE:  return "#0d47a1";
            case GREEN:      return "#2e7d32";
            case RED:        return "#c62828";
            case YELLOW:     return "#f9a825";
            case PINK:       return "#ad1457";
            case ORANGE:     return "#ef6c00";
            case LIGHT_BLUE: return "#0288d1";
            case BROWN:      return "#4e342e";
            case RAILROAD:   return "#37474f";
            case UTILITY:    return "#558b2f";
            default:         return "#c99f4f";
        }
    }
}
