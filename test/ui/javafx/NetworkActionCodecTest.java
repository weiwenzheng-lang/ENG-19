package ui.javafx;

import cards.PropertyWildCard;
import cards.RentCard;
import core.TargetInfo;
import enums.PropertyColor;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Tests LAN action payload encoding and decoding.
class NetworkActionCodecTest {
    // Verifies payload escaping preserves separator characters in card names.
    @Test
    void parsePayloadRestoresEscapedSeparators() {
        String payload = "card=" + NetworkActionCodec.safe("Rent;Card=Brown\\Blue");

        Map<String, String> values = NetworkActionCodec.parsePayload(payload);

        assertEquals("Rent;Card=Brown\\Blue", values.get("card"));
    }

    // Verifies forced-deal target metadata survives encode and decode.
    @Test
    void encodePlayPayloadRoundTripsTargetProperties() {
        Player alice = new Player("1", "Alice");
        Player bob = new Player("2", "Bob");
        List<Player> players = List.of(alice, bob);
        TargetInfo original = new TargetInfo(bob,
                PropertyColor.BROWN, 1,
                PropertyColor.LIGHT_BLUE, 0);

        String payload = NetworkActionCodec.encodePlayPayload(4,
                new RentCard(1, "Brown Rent", 1, PropertyColor.BROWN, PropertyColor.BROWN),
                original,
                players);
        TargetInfo decoded = NetworkActionCodec.buildTargetInfo(
                NetworkActionCodec.parsePayload(payload),
                players);

        assertSame(bob, decoded.getTargetPlayer());
        assertEquals(PropertyColor.BROWN, decoded.getInitiatorPropertyColor());
        assertEquals(1, decoded.getInitiatorPropertyIndex());
        assertEquals(PropertyColor.LIGHT_BLUE, decoded.getTargetPropertyColor());
        assertEquals(0, decoded.getTargetPropertyIndex());
    }

    // Verifies selected card color is restored before a remote action is replayed.
    @Test
    void applyCardStateRestoresMutableCardColor() {
        PropertyWildCard wild = new PropertyWildCard(1, "Brown/Blue Wild", 1,
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE,
                PropertyColor.BROWN.getRentTiers(), PropertyColor.LIGHT_BLUE.getRentTiers());
        RentCard rent = new RentCard(2, "Brown/Blue Rent", 1,
                PropertyColor.BROWN, PropertyColor.LIGHT_BLUE);
        Map<String, String> payload = Map.of("color", "LIGHT_BLUE");

        NetworkActionCodec.applyCardState(wild, payload);
        NetworkActionCodec.applyCardState(rent, payload);

        assertEquals(PropertyColor.LIGHT_BLUE, wild.getColorGroup());
        assertEquals(PropertyColor.LIGHT_BLUE, rent.getSelectedColor());
    }

    // Verifies invalid integer fields fall back instead of breaking action replay.
    @Test
    void readIntUsesFallbackForInvalidPayloadValue() {
        int value = NetworkActionCodec.readInt(Map.of("index", "not-a-number"), "index", -1);

        assertEquals(-1, value);
    }
}
