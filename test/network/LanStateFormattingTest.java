package network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests lobby state formatting shown in the network UI.
class LanStateFormattingTest {
    // Verifies player display text includes host, ready, and offline markers.
    @Test
    void playerInfoFormatsAllStatusMarkers() {
        LanPlayerInfo info = new LanPlayerInfo(2, "Bob", true, false, true);

        assertEquals("#2 Bob [Host] [Ready] [Offline]", info.toDisplayText());
    }

    // Verifies room summary reports waiting state and counts.
    @Test
    void roomStateFormatsWaitingSummary() {
        LanRoomState state = new LanRoomState(false, 1, 3, 2, 1);

        assertEquals("Players: 2/3 online, ready: 1/2, waiting", state.toSummary());
    }

    // Verifies room summary reports game-started state.
    @Test
    void roomStateFormatsStartedSummary() {
        LanRoomState state = new LanRoomState(true, 1, 2, 2, 2);

        assertEquals("Players: 2/2 online, ready: 2/2, game started", state.toSummary());
    }
}
