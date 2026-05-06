package ui.javafx;

import cards.Card;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class CardView extends StackPane {
    public CardView(Card card) {
        this(card == null ? "Empty" : card.getCardName(),
                card == null ? "" : card.getMonetaryValue() + "M");
    }

    public CardView(String title, String subtitle) {
        Label label = new Label(title + (subtitle == null || subtitle.trim().isEmpty() ? "" : "\n" + subtitle));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER);
        setPadding(new Insets(8));
        setPrefSize(96, 136);
        setMinSize(96, 136);
        setMaxSize(96, 136);
        getChildren().add(label);
        setStyle("-fx-background-color: white; -fx-border-color: #263238; "
                + "-fx-border-radius: 6; -fx-background-radius: 6;");
    }

    public static CardView back(int count) {
        CardView view = new CardView("Cards", String.valueOf(count));
        view.setStyle("-fx-background-color: #1b5e20; -fx-border-color: #0b2f12; "
                + "-fx-border-radius: 6; -fx-background-radius: 6;");
        return view;
    }
}
