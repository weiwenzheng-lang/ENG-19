package ui.javafx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests board layout coordinates used by 2-5 player backgrounds.
class BoardLayoutConfigTest {
    // Verifies the 2-player opponent frame coordinates stay aligned to the background.
    @Test
    void twoPlayerOpponentFrameMatchesBackground() {
        BoardLayoutConfig.ZoneSpec[] specs = BoardLayoutConfig.opponentSpecs(2);

        assertEquals(1, specs.length);
        assertZone(specs[0], 488, 145, 705, 164, 0, 770, 93, 160, 44);
    }

    // Verifies the 5-player local hand and table frames keep the tuned values.
    @Test
    void fivePlayerLocalFramesKeepTunedCoordinates() {
        BoardLayoutConfig.ZoneSpec table = BoardLayoutConfig.ownTableSpec(5);
        BoardLayoutConfig.ZoneSpec hand = BoardLayoutConfig.handSpec(5);
        BoardLayoutConfig.ZoneSpec name = BoardLayoutConfig.ownNameSpec(5);

        assertZone(table, 478, 570, 730, 98, 0, 0, 0, 0, 0);
        assertZone(hand, 255, 700, 1175, 160, 0, 0, 0, 0, 0);
        assertZone(name, 286, 660, 150, 48, 0, 0, 0, 0, 0);
    }

    // Verifies the top-right five-player opponent frame stays narrow enough.
    @Test
    void fivePlayerTopRightOpponentDoesNotOverlapNeighbor() {
        BoardLayoutConfig.ZoneSpec[] specs = BoardLayoutConfig.opponentSpecs(5);

        assertEquals(4, specs.length);
        assertZone(specs[2], 865, 174, 320, 122, 8, 1085, 120, 142, 44);
    }

    // Compares every coordinate in a frame spec.
    private void assertZone(BoardLayoutConfig.ZoneSpec spec,
                            double x, double y, double width, double height, double rotate,
                            double nameX, double nameY, double nameWidth, double nameHeight) {
        assertEquals(x, spec.x);
        assertEquals(y, spec.y);
        assertEquals(width, spec.width);
        assertEquals(height, spec.height);
        assertEquals(rotate, spec.rotate);
        assertEquals(nameX, spec.nameX);
        assertEquals(nameY, spec.nameY);
        assertEquals(nameWidth, spec.nameWidth);
        assertEquals(nameHeight, spec.nameHeight);
    }
}
