package ui.javafx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests table-card layout calculations that keep rotated cards inside the frame.
class TableCardRendererLayoutTest {
    // Verifies local table cards reserve extra width for rotation and shadow.
    @Test
    void ownTableCardsReserveEdgeInsetForRotation() {
        double inset = TableCardRenderer.computeEdgeInset(58, 96, 2.4, true);

        assertTrue(inset > 4);
        assertTrue(inset < 20);
    }

    // Verifies opponent cards also reserve safe edge room in tilted frames.
    @Test
    void opponentTableCardsReserveEdgeInsetForRotation() {
        double inset = TableCardRenderer.computeEdgeInset(74, 122, 5.0, false);

        assertTrue(inset > 4);
        assertTrue(inset < 30);
    }
}
