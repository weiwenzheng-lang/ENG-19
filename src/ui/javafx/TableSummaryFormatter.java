package ui.javafx;

import cards.Card;
import cards.PropertyCard;
import player.Player;
import player.PropertyArea;
import player.PropertySet;
import player.Rentable;
import player.SetDecorator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Formats whole-table bank and property summaries for the UI buttons.
final class TableSummaryFormatter {
    // Prevents construction of this formatting utility.
    private TableSummaryFormatter() {
    }

    // Builds a bank report for every player.
    static String bankSummary(List<Player> players) {
        StringBuilder detail = new StringBuilder();
        for (Player player : players) {
            detail.append(player.getPlayerName())
                    .append(" - Total: ")
                    .append(player.getBankArea().calculateTotalFunds())
                    .append("M\n");
            Map<String, Integer> counts = countBankCards(player);
            if (counts.isEmpty()) {
                detail.append("  No bank cards.\n\n");
                continue;
            }
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                detail.append("  ")
                        .append(entry.getKey())
                        .append(" x")
                        .append(entry.getValue())
                        .append("\n");
            }
            detail.append("\n");
        }
        return detail.toString().trim();
    }

    // Builds a property report for every player.
    static String propertySummary(List<Player> players) {
        StringBuilder detail = new StringBuilder();
        for (Player player : players) {
            detail.append(player.getPlayerName())
                    .append(" - Properties: ")
                    .append(player.getPropertyArea().getAllPropertyCards().size())
                    .append(", complete sets: ")
                    .append(player.getPropertyArea().countCompletedSets())
                    .append("/3\n");
            List<PropertyArea.PropertySetEntry> entries = player.getPropertyArea().getPropertySetEntries();
            if (entries.isEmpty()) {
                detail.append("  No properties on table.\n\n");
                continue;
            }
            for (PropertyArea.PropertySetEntry entry : entries) {
                PropertySet root = rootSet(entry.getRentable());
                int count = root == null ? 0 : root.getCardsCount();
                detail.append("  ")
                        .append(entry.getColor())
                        .append(": ")
                        .append(count)
                        .append("/")
                        .append(entry.getColor().getRequiredCount())
                        .append(entry.getRentable().isComplete() ? " complete" : " incomplete")
                        .append("\n");
            }
            detail.append("\n");
        }
        return detail.toString().trim();
    }

    // Counts banked card names without losing action-card money entries.
    private static Map<String, Integer> countBankCards(Player player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Card card : player.getBankArea().getAssets()) {
            counts.put(card.getCardName(), counts.getOrDefault(card.getCardName(), 0) + 1);
        }
        return counts;
    }

    // Returns the undecorated property set when houses or hotels wrap it.
    private static PropertySet rootSet(Rentable rentable) {
        if (rentable instanceof SetDecorator) {
            return ((SetDecorator) rentable).getRootSet();
        }
        if (rentable instanceof PropertySet) {
            return (PropertySet) rentable;
        }
        return null;
    }
}
