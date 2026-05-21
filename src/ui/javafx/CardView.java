package ui.javafx;

import cards.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CardView extends StackPane {

    public CardView(Card card) {
        setPrefSize(100, 140);
        setMinSize(100, 140);
        setMaxSize(100, 140);

        VBox cardBody = new VBox();
        cardBody.setAlignment(Pos.TOP_CENTER);
        cardBody.setStyle("-fx-background-color: " + getCardBackground(card) + "; "
                + "-fx-background-radius: 10; "
                + "-fx-border-color: " + getAccentColor(card) + "; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 10;");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(getAccentColor(card)).deriveColor(1, 1, 1, 0.6));
        glow.setRadius(10);
        glow.setSpread(0.2);
        cardBody.setEffect(glow);

        Region headerBar = new Region();
        headerBar.setPrefHeight(10);
        headerBar.setStyle("-fx-background-color: " + getHeaderColor(card) + "; "
                + "-fx-background-radius: 8 8 0 0;");

        Label nameLabel = new Label(card.getCardName());
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web(getPrimaryTextColor(card)));
        nameLabel.setPadding(new Insets(8, 3, 2, 3));

        Label typeLabel = new Label(getCardTypeShortName(card));
        typeLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        typeLabel.setTextFill(Color.web(getSecondaryTextColor(card)));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label("¥ " + card.getMonetaryValue() + "M");
        valueLabel.setFont(Font.font("Consolas", FontWeight.BLACK, 14));
        valueLabel.setTextFill(Color.web(getValueTextColor(card)));
        valueLabel.setPadding(new Insets(0, 0, 8, 0));

        cardBody.getChildren().addAll(headerBar, nameLabel, typeLabel, spacer, valueLabel);

        this.setOnMouseEntered(e -> {
            this.setTranslateY(-10);
            glow.setRadius(20);
        });
        this.setOnMouseExited(e -> {
            this.setTranslateY(0);
            glow.setRadius(10);
        });

        getChildren().add(cardBody);
        addDualColorBorder(card);
    }

    public CardView(String title, String subtitle) {
        setPrefSize(100, 140);
        setMinSize(100, 140);
        setMaxSize(100, 140);

        VBox cardBody = new VBox(8);
        cardBody.setAlignment(Pos.CENTER);
        cardBody.setStyle("-fx-background-color: repeating-linear-gradient(45deg, #111, #111 10px, #222 10px, #222 20px); "
                + "-fx-background-radius: 10; -fx-border-color: #00f2ff; -fx-border-width: 2; -fx-border-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#00f2ff"));
        titleLabel.setFont(Font.font("Consolas", FontWeight.BLACK, 14));

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setTextFill(Color.web("#a0aab5"));
        subtitleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));

        cardBody.getChildren().addAll(titleLabel, subtitleLabel);
        getChildren().add(cardBody);
    }

    private String getAccentColor(Card card) {
        if (card instanceof PropertyWildCard) return ((PropertyWildCard) card).getColorA().getColorHex();
        if (card instanceof PropertyCard) return ((PropertyCard) card).getColorGroup().getColorHex();
        if (card instanceof MoneyCard) return "#c9a44b";
        if (card instanceof DealBreakerCard) return "#9b59b6";
        if (card instanceof JustSayNoCard) return "#e91e63";
        if (card instanceof SlyDealCard || card instanceof ForceDealCard) return "#5e35b1";
        if (card instanceof DebtCollectorCard || card instanceof BirthdayCard) return "#fb8c00";
        if (card instanceof PassGoCard) return "#00acc1";
        if (card instanceof DoubleTheRentCard) return "#c9a44b";
        if (card instanceof HouseCard) return "#009688";
        if (card instanceof HotelCard) return "#d32f2f";
        if (card instanceof RentCard || card instanceof WildRentCard) return "#1e88e5";
        if (card instanceof ActionCard) return "#ff007f";
        return "#00f2ff";
    }

    private String getHeaderColor(Card card) {
        return getAccentColor(card);
    }

    private String getCardBackground(Card card) {
        if (card instanceof MoneyCard)
            return "linear-gradient(to bottom, #1a1608, #0d0a04)";
        return "linear-gradient(to bottom right, #1e2230, #10131c)";
    }

    private String getPrimaryTextColor(Card card) {
        return "#ffffff";
    }

    private String getSecondaryTextColor(Card card) {
        return "#a0aab5";
    }

    private String getValueTextColor(Card card) {
        if (card instanceof MoneyCard) return "#ffd700";
        if (card instanceof ActionCard) return "#00f2ff";
        return "#00ff9f";
    }

    private void addDualColorBorder(Card card) {
        if (!(card instanceof PropertyWildCard)) return;

        PropertyWildCard wildCard = (PropertyWildCard) card;
        String firstColor = wildCard.getColorA().getColorHex();
        String secondColor = wildCard.getColorB().getColorHex();

        Pane overlay = new Pane();
        overlay.setMinSize(100, 140);
        overlay.setPrefSize(100, 140);
        overlay.setMaxSize(100, 140);
        overlay.setMouseTransparent(true);

        overlay.getChildren().addAll(
                borderLine(2, 1, 98, 1, firstColor),
                borderLine(1, 2, 1, 138, firstColor),
                borderLine(99, 2, 99, 138, secondColor),
                borderLine(2, 139, 98, 139, secondColor));

        getChildren().add(overlay);
    }

    private Line borderLine(double startX, double startY, double endX, double endY, String color) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.web(color));
        line.setStrokeWidth(3);
        line.setMouseTransparent(true);
        return line;
    }

    private String getCardTypeShortName(Card card) {
        if (card instanceof PropertyWildCard) return "[WILD ASSET]";
        if (card instanceof PropertyCard) return "[ASSET]";
        if (card instanceof MoneyCard) return "[FUNDS]";
        if (card instanceof RentCard) return "[RENTAL]";
        if (card instanceof ActionCard) return "[ACTION]";
        return "[SYS]";
    }

    public static CardView back(int count) {
        return new CardView("HUB_DECK", count + " CARDS");
    }
}
