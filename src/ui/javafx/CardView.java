package ui.javafx;

import cards.Card;
import cards.MoneyCard;
import cards.PropertyCard;
import cards.RentCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CardView extends StackPane {
    public CardView(Card card) {
        this(card == null ? "Empty" : card.getCardName(),
                card == null ? "" : card.getMonetaryValue() + "M",
                styleFor(card));
    }

    public CardView(String title, String subtitle) {
        this(title, subtitle, "-fx-background-color: #f7efe1; -fx-border-color: #d8b46f;");
    }

    private CardView(String title, String subtitle, String style) {
        Label label = new Label(title + (subtitle == null || subtitle.trim().isEmpty() ? "" : "\n" + subtitle));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#21313c"));
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        setAlignment(Pos.CENTER);
        setPadding(new Insets(8));
        setPrefSize(96, 136);
        setMinSize(96, 136);
        setMaxSize(96, 136);
        getChildren().add(label);
        setStyle(style + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 10, 0.18, 0, 3);");
    }

    public static CardView back(int count) {
        return new CardView("Cards", String.valueOf(count),
                "-fx-background-color: #315a70; -fx-border-color: #b9d8e6;");
    }

    private static String styleFor(Card card) {
        if (card instanceof MoneyCard) {
            return "-fx-background-color: #d9f0ce; -fx-border-color: #79a96c;";
        }
        if (card instanceof PropertyCard) {
            return "-fx-background-color: #f3e7c8; -fx-border-color: #c99f4f;";
        }
        if (card instanceof RentCard) {
            return "-fx-background-color: #d7e5ff; -fx-border-color: #6b91d6;";
        }
        if (card != null && card.getCardName().equals("Just Say No")) {
            return "-fx-background-color: #f7d0d7; -fx-border-color: #c76073;";
        }
        if (card != null && (card.getCardName().equals("Sly Deal")
                || card.getCardName().equals("Forced Deal")
                || card.getCardName().equals("Deal Breaker")
                || card.getCardName().equals("Debt Collector"))) {
            return "-fx-background-color: #eadcff; -fx-border-color: #9b78d0;";
        }
        return "-fx-background-color: #fff2cc; -fx-border-color: #d4ad52;";
    }
}
