package ui.javafx;

// Calculates small card arcs without depending on JavaFX nodes.
final class CardArcLayout {
    private static final double ARC_DEPTH_SCALE = 1.6;
    private static final double MAX_ROTATION_DEGREES = 6.0;

    // Prevents construction of this layout utility.
    private CardArcLayout() {
    }

    // Keeps the local hand within its clipped frame while adding a shallow arc.
    static double computeHandCurveDepth(double zoneHeight, double cardHeight, double maximumDepth) {
        if (zoneHeight <= cardHeight) {
            return 0;
        }
        return Math.max(0, maximumDepth * ARC_DEPTH_SCALE);
    }

    // Uses the hand arc depth as a matching small rotation amount.
    static double computeHandRotationDepth(double curveDepth, double maximumDepth) {
        if (curveDepth <= 0 || maximumDepth <= 0) {
            return 0;
        }
        return MAX_ROTATION_DEGREES * Math.min(1.0, curveDepth / (maximumDepth * ARC_DEPTH_SCALE));
    }

    // Normalizes one card index to the range -1 through 1.
    static double normalizeCardPosition(int index, int count) {
        if (count <= 1) {
            return 0;
        }
        double centerOffset = index - (count - 1) / 2.0;
        return centerOffset / ((count - 1) / 2.0);
    }

    // Converts a normalized horizontal position into an arc offset.
    static double computeArcOffset(double normalized, double curveDepth) {
        return (Math.abs(normalized) - 1.0) * curveDepth;
    }
}
