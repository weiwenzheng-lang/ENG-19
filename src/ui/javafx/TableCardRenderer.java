package ui.javafx;

import cards.Card;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import player.Player;

import java.util.ArrayList;
import java.util.List;

// Renders separated property and bank cards inside one table frame.
final class TableCardRenderer {
    private static final double SECTION_GAP = 10;
    private static final double PROPERTY_WIDTH_SHARE = 0.58;
    private static final double PROPERTY_TILT = -2.4;
    private static final double BANK_TILT = 2.4;
    private static final double OWN_TABLE_CARD_SCALE = 0.76;
    private static final double OWN_TABLE_CURVE = 9;
    private static final double OPPONENT_TABLE_CURVE = 10;
    private static final double OWN_TABLE_Y_OFFSET = 7;
    private static final double OWN_TABLE_GROUP_INSET = 18;
    private static final double OWN_TABLE_ROTATION = 2.2;
    private static final double OPPONENT_TABLE_ROTATION = 5.0;
    private static final double CARD_SHADOW_MARGIN = 4;

    // Prevents construction of this rendering helper.
    private TableCardRenderer() {
    }

    // Renders property cards on the left and bank cards on the right.
    static void render(Pane target, Player player, double cardWidth, double cardHeight,
                       boolean currentPlayerArea) {
        render(target, player, cardWidth, cardHeight, currentPlayerArea, 0);
    }

    // Renders table cards with an optional vertical adjustment for the bank section.
    static void render(Pane target, Player player, double cardWidth, double cardHeight,
                       boolean currentPlayerArea, double bankYOffset) {
        target.getChildren().clear();
        List<Card> properties = new ArrayList<>(player.getPropertyArea().getAllPropertyCards());
        List<Card> bank = new ArrayList<>(player.getBankArea().getAssets());
        if (properties.isEmpty() && bank.isEmpty()) {
            target.getChildren().add(emptyLabel(target, currentPlayerArea));
            return;
        }

        double gap = Math.min(SECTION_GAP, Math.max(6, target.getPrefWidth() * 0.035));
        double propertyWidth = Math.max(cardWidth, (target.getPrefWidth() - gap) * PROPERTY_WIDTH_SHARE);
        double bankWidth = Math.max(cardWidth, target.getPrefWidth() - propertyWidth - gap);
        if (properties.isEmpty() || bank.isEmpty()) {
            propertyWidth = properties.isEmpty() ? 0 : target.getPrefWidth();
            bankWidth = bank.isEmpty() ? 0 : target.getPrefWidth();
            gap = 0;
        } else {
            target.getChildren().add(divider(propertyWidth + gap / 2.0, target.getPrefHeight()));
        }

        boolean splitTable = !properties.isEmpty() && !bank.isEmpty();
        renderGroup(target, properties, 0, propertyWidth, cardWidth, cardHeight, currentPlayerArea,
                splitTable ? PROPERTY_TILT : 0, 0);
        renderGroup(target, bank, propertyWidth + gap, bankWidth, cardWidth, cardHeight, currentPlayerArea,
                splitTable ? BANK_TILT : 0, bankYOffset);
    }

    // Renders one side of the table frame.
    private static void renderGroup(Pane target, List<Card> cards, double startX, double zoneWidth,
                                    double cardWidth, double cardHeight, boolean currentPlayerArea,
                                    double rotationBias, double extraYOffset) {
        if (cards.isEmpty() || zoneWidth <= 0) {
            return;
        }

        double effectiveHeight = adjustedCardHeight(target.getPrefHeight(), cardHeight);
        if (currentPlayerArea) {
            effectiveHeight *= OWN_TABLE_CARD_SCALE;
        }
        double effectiveWidth = cardWidth * (effectiveHeight / cardHeight);
        double tightGap = isTightOpponentZone(target, cardHeight) ? 2 : 6;
        int rows = cards.size() > 5 && target.getPrefHeight() >= effectiveHeight * 1.55 ? 2 : 1;
        int perRow = (int) Math.ceil(cards.size() / (double) rows);
        double groupInset = currentPlayerArea ? OWN_TABLE_GROUP_INSET : 0;
        double edgeInset = computeEdgeInset(effectiveWidth, effectiveHeight, rotationBias, currentPlayerArea);
        double renderStartX = startX + groupInset + edgeInset;
        double renderZoneWidth = Math.max(effectiveWidth, zoneWidth - groupInset * 2 - edgeInset * 2);
        double step = computeCardStep(perRow, renderZoneWidth, effectiveWidth, tightGap, true);
        double rowGap = rows == 1 ? 0 : Math.min(effectiveHeight * 0.58,
                (target.getPrefHeight() - effectiveHeight) / (rows - 1));
        double requestedCurve = cards.size() <= 1 ? 0
                : (currentPlayerArea ? OWN_TABLE_CURVE : OPPONENT_TABLE_CURVE);
        double availableCurve = Math.max(0,
                target.getPrefHeight() - effectiveHeight - rowGap * (rows - 1) - 2);
        double curveDepth = Math.min(requestedCurve, availableCurve);
        double usedHeight = effectiveHeight + rowGap * (rows - 1) + curveDepth;
        double startY = Math.max(1, (target.getPrefHeight() - usedHeight) / 2.0)
                + (currentPlayerArea ? OWN_TABLE_Y_OFFSET : 0);
        if (currentPlayerArea) {
            double maxStartY = Math.max(1, target.getPrefHeight() - usedHeight - 1);
            startY = Math.min(startY, maxStartY);
        }

        for (int i = 0; i < cards.size(); i++) {
            int row = i / perRow;
            int column = i % perRow;
            int rowCount = Math.min(perRow, cards.size() - row * perRow);
            double rowWidth = effectiveWidth + step * Math.max(0, rowCount - 1);
            double rowStartX = renderStartX + Math.max(2, (renderZoneWidth - rowWidth) / 2.0);
            double centerOffset = column - (rowCount - 1) / 2.0;
            double normalized = rowCount <= 1 ? 0 : centerOffset / ((rowCount - 1) / 2.0);
            CardView cardView = new CardView(cards.get(i), effectiveWidth, effectiveHeight);
            cardView.setLayoutX(rowStartX + column * step);
            cardView.setLayoutY(startY + extraYOffset + row * rowGap + Math.abs(normalized) * curveDepth);
            cardView.setRotate(rotationBias
                    + normalized * (currentPlayerArea ? OWN_TABLE_ROTATION : OPPONENT_TABLE_ROTATION));
            target.getChildren().add(cardView);
        }
    }

    // Reserves room for the visual bounds of rotated cards inside the clipped frame.
    static double computeEdgeInset(double cardWidth, double cardHeight, double rotationBias,
                                   boolean currentPlayerArea) {
        double rotationSpread = currentPlayerArea ? OWN_TABLE_ROTATION : OPPONENT_TABLE_ROTATION;
        double maxRotation = Math.abs(rotationBias) + rotationSpread;
        double radians = Math.toRadians(maxRotation);
        double rotatedWidth = Math.abs(cardWidth * Math.cos(radians))
                + Math.abs(cardHeight * Math.sin(radians));
        double rotationMargin = Math.max(0, (rotatedWidth - cardWidth) / 2.0);
        return Math.ceil(rotationMargin + CARD_SHADOW_MARGIN);
    }

    // Creates the centered empty-state label.
    private static Label emptyLabel(Pane target, boolean currentPlayerArea) {
        Label empty = new Label(currentPlayerArea ? "No table cards" : "No cards on table");
        empty.setTextFill(javafx.scene.paint.Color.web("rgba(255,255,255,0.70)"));
        empty.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        empty.setAlignment(Pos.CENTER);
        empty.setPrefSize(target.getPrefWidth(), target.getPrefHeight());
        empty.setMinSize(target.getPrefWidth(), target.getPrefHeight());
        empty.setMaxSize(target.getPrefWidth(), target.getPrefHeight());
        return empty;
    }

    // Adds a very subtle separator between property and bank sections.
    private static Region divider(double x, double height) {
        Region divider = new Region();
        divider.setLayoutX(x);
        divider.setLayoutY(Math.max(3, height * 0.08));
        divider.setPrefSize(1.5, Math.max(12, height * 0.84));
        divider.setStyle("-fx-background-color: rgba(255, 226, 156, 0.28);");
        return divider;
    }

    // Computes a spacing step that can overlap when the section gets crowded.
    private static double computeCardStep(int count, double zoneWidth, double cardWidth, double gap,
                                          boolean allowOverlap) {
        if (count <= 1) {
            return 0;
        }
        double natural = cardWidth + gap;
        double maxStep = (zoneWidth - cardWidth - 4) / (count - 1);
        double minimum = allowOverlap ? cardWidth * 0.28 : cardWidth;
        if (maxStep >= minimum) {
            return Math.min(natural, maxStep);
        }
        return Math.max(5, maxStep);
    }

    // Leaves vertical room for rotated cards in short table frames.
    private static double adjustedCardHeight(double zoneHeight, double cardHeight) {
        if (zoneHeight <= cardHeight + 6) {
            return Math.max(48, zoneHeight - 8);
        }
        return cardHeight;
    }

    // Detects the shallow top opponent frames used in the five-player layout.
    private static boolean isTightOpponentZone(Pane target, double cardHeight) {
        return target.getPrefHeight() <= cardHeight + 12 && target.getPrefWidth() <= 380;
    }
}
