package ui.javafx;

import cards.Card;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;

// Shows a large readable version of a hand card.
final class CardPreviewDialog {
    private static final ButtonType CLOSE = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

    // Prevents construction of this dialog helper.
    private CardPreviewDialog() {
    }

    // Opens the card preview dialog.
    static void show(Card card) {
        Dialog<ButtonType> dialog = GameDialogs.create("Card Preview", card.getCardName());
        VBox content = GameDialogs.contentBox();
        content.setAlignment(Pos.CENTER);
        content.getChildren().add(new CardView(card, 260, 430));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(CLOSE);
        GameDialogs.styleButtons(dialog);
        dialog.showAndWait();
    }
}
