package ui.javafx;

import cards.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CardView extends StackPane {
    public CardView(Card card) {
        this(card == null ? "Empty" : card.getCardName(),
                card instanceof MoneyCard ? "" : (card == null ? "" : card.getMonetaryValue() + "M"),
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
        if (card == null) return "-fx-background-color: #ffffff; -fx-border-color: #cccccc;";

        if (card instanceof SuperWildCard)
            return "-fx-background-color: linear-gradient(to bottom right, #ff9a9e, #fad0c4); -fx-border-color: #ff4d4d;";

        if (card instanceof PropertyWildCard)
            return "-fx-background-color: #ffebcc; -fx-border-color: #ff9900; -fx-border-style: dashed;";

        if (card instanceof PropertyCard) {
            String hex = ((PropertyCard) card).getColorGroup().getColorHex();
            return "-fx-background-color: #fdfaf0; -fx-border-color: " + hex + "; -fx-border-width: 5 0 0 0;";
        }

        if (card instanceof MoneyCard)
            return "-fx-background-color: #e8f5e9; -fx-border-color: #4caf50;";

        if (card instanceof RentCard)
            return "-fx-background-color: #e3f2fd; -fx-border-color: #1e88e5;";

        if (card instanceof HouseCard)
            return "-fx-background-color: #e0f2f1; -fx-border-color: #009688;";

        if (card instanceof HotelCard)
            return "-fx-background-color: #ffebee; -fx-border-color: #d32f2f; -fx-border-width: 3;";

        if (card instanceof DoubleTheRentCard)
            return "-fx-background-color: #fffde7; -fx-border-color: #fbc02d; -fx-border-style: dashed;";

        if (card instanceof JustSayNoCard)
            return "-fx-background-color: #fce4ec; -fx-border-color: #e91e63;";

        if (card instanceof DealBreakerCard)
            return "-fx-background-color: #f3e5f5; -fx-border-color: #7b1fa2; -fx-border-width: 3;";

        if (card instanceof SlyDealCard || card instanceof ForceDealCard)
            return "-fx-background-color: #ede7f6; -fx-border-color: #5e35b1;";

        if (card instanceof PassGoCard)
            return "-fx-background-color: #e0f7fa; -fx-border-color: #00acc1;";

        if (card instanceof DebtCollectorCard || card instanceof BirthdayCard)
            return "-fx-background-color: #fff3e0; -fx-border-color: #fb8c00;";

        return "-fx-background-color: #f5f5f5; -fx-border-color: #9e9e9e;";
    }
}