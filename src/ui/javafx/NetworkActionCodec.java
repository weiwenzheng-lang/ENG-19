package ui.javafx;

import cards.Card;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Encodes and decodes game-table actions sent through the LAN room.
final class NetworkActionCodec {
    // Prevents construction of this stateless codec.
    private NetworkActionCodec() {
    }

    // Encodes a normal card play for LAN replay.
    static String encodePlayPayload(int cardIndex, Card card, TargetInfo targetInfo, List<Player> players) {
        List<String> parts = new ArrayList<>();
        parts.add("index=" + cardIndex);
        parts.add("card=" + safe(card == null ? "" : card.getCardName()));
        appendCardState(parts, card);
        appendTarget(parts, targetInfo, players);
        return String.join(";", parts);
    }

    // Encodes a Double The Rent action for LAN replay.
    static String encodeDoubleRentPayload(int doubleCardIndex, Card doubleCard,
                                          int rentCardIndex, Card rentCard,
                                          TargetInfo targetInfo, List<Player> players) {
        List<String> parts = new ArrayList<>();
        parts.add("doubleIndex=" + doubleCardIndex);
        parts.add("double=" + safe(doubleCard == null ? "" : doubleCard.getCardName()));
        parts.add("rentIndex=" + rentCardIndex);
        parts.add("rent=" + safe(rentCard == null ? "" : rentCard.getCardName()));
        appendCardState(parts, rentCard);
        appendTarget(parts, targetInfo, players);
        return String.join(";", parts);
    }

    // Parses a LAN action payload into key-value fields.
    static Map<String, String> parsePayload(String payload) {
        Map<String, String> values = new HashMap<>();
        if (payload == null || payload.isEmpty()) {
            return values;
        }
        for (String part : payload.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            values.put(part.substring(0, eq), unsafe(part.substring(eq + 1)));
        }
        return values;
    }

    // Reads an integer payload field with a fallback.
    static int readInt(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Restores selected color state before replaying a remote card.
    static void applyCardState(Card card, Map<String, String> payload) {
        PropertyColor color = readColor(payload, "color");
        if (card == null || color == null) {
            return;
        }
        if (card instanceof cards.RentCard) {
            ((cards.RentCard) card).setSelectedColor(color);
        } else if (card instanceof cards.WildRentCard) {
            ((cards.WildRentCard) card).setSelectedColor(color);
        } else if (card instanceof cards.SuperWildCard) {
            ((cards.SuperWildCard) card).setCurrentColor(color);
        } else if (card instanceof cards.PropertyWildCard) {
            ((cards.PropertyWildCard) card).setCurrentColor(color);
        }
    }

    // Rebuilds target information from a network payload.
    static TargetInfo buildTargetInfo(Map<String, String> payload, List<Player> players) {
        Player target = null;
        int targetIndex = readInt(payload, "target", -1);
        if (targetIndex >= 0 && targetIndex < players.size()) {
            target = players.get(targetIndex);
        }
        PropertyColor giveColor = readColor(payload, "giveColor");
        PropertyColor takeColor = readColor(payload, "takeColor");
        PropertyColor improveColor = readColor(payload, "improveColor");
        if (improveColor != null) {
            return TargetInfo.forImprovement(improveColor).withTarget(target);
        }
        if (giveColor != null || takeColor != null) {
            int giveIndex = readInt(payload, "giveIndex", -1);
            int takeIndex = readInt(payload, "takeIndex", -1);
            if (giveColor != null) {
                return new TargetInfo(target, giveColor, giveIndex, takeColor, takeIndex);
            }
            return new TargetInfo(target, takeColor, takeIndex);
        }
        return target == null ? null : new TargetInfo(target);
    }

    // Escapes simple key-value payload separators.
    static String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace(";", "\\s")
                .replace("=", "\\e");
    }

    // Adds mutable card color state to a network payload.
    private static void appendCardState(List<String> parts, Card card) {
        if (card instanceof cards.RentCard) {
            parts.add("color=" + ((cards.RentCard) card).getSelectedColor());
        } else if (card instanceof cards.WildRentCard) {
            parts.add("color=" + ((cards.WildRentCard) card).getSelectedColor());
        } else if (card instanceof cards.PropertyCard) {
            parts.add("color=" + ((cards.PropertyCard) card).getColorGroup());
        }
    }

    // Adds target player and selected property data to a network payload.
    private static void appendTarget(List<String> parts, TargetInfo targetInfo, List<Player> players) {
        if (targetInfo == null) {
            return;
        }
        if (targetInfo.getTargetPlayer() != null) {
            parts.add("target=" + players.indexOf(targetInfo.getTargetPlayer()));
        }
        if (targetInfo.getInitiatorPropertyColor() != null) {
            parts.add("giveColor=" + targetInfo.getInitiatorPropertyColor());
            parts.add("giveIndex=" + targetInfo.getInitiatorPropertyIndex());
        }
        if (targetInfo.getTargetPropertyColor() != null) {
            parts.add("takeColor=" + targetInfo.getTargetPropertyColor());
            parts.add("takeIndex=" + targetInfo.getTargetPropertyIndex());
        }
        if (targetInfo.getImprovementColor() != null) {
            parts.add("improveColor=" + targetInfo.getImprovementColor());
        }
    }

    // Reads a property color payload field with a fallback.
    private static PropertyColor readColor(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return null;
        }
        try {
            return PropertyColor.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Reverses key-value payload escaping.
    private static String unsafe(String value) {
        return value == null ? "" : value.replace("\\e", "=")
                .replace("\\s", ";")
                .replace("\\\\", "\\");
    }
}
