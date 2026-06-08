package ui.javafx;

import cards.Card;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

// Builds the context menu for one card in the local player's hand.
final class HandCardMenuFactory {
    // Receives menu actions without letting the menu own game rules.
    interface Handler {
        void deposit(int cardIndex);

        void play(int cardIndex, Card card);

        void discard(int cardIndex);

        boolean discardRequired();
    }

    // Prevents construction of this menu factory.
    private HandCardMenuFactory() {
    }

    // Creates a menu with labels matched to the selected card type.
    static ContextMenu create(int cardIndex, Card card, Handler handler) {
        ContextMenu menu = new ContextMenu();
        MenuItem discard = discardItem(cardIndex, handler);

        if (card instanceof cards.MoneyCard) {
            menu.getItems().addAll(bankItem(cardIndex, handler), discard);
        } else if (card instanceof cards.PropertyCard) {
            menu.getItems().addAll(propertyItem(cardIndex, card, handler), discard);
        } else {
            menu.getItems().addAll(bankItem(cardIndex, handler), actionItem(cardIndex, card, handler), discard);
        }
        return menu;
    }

    // Creates the bank menu action.
    private static MenuItem bankItem(int cardIndex, Handler handler) {
        MenuItem item = new MenuItem("Bank Card");
        item.setOnAction(event -> handler.deposit(cardIndex));
        return item;
    }

    // Creates the property play menu action.
    private static MenuItem propertyItem(int cardIndex, Card card, Handler handler) {
        MenuItem item = new MenuItem("Play Property");
        item.setOnAction(event -> handler.play(cardIndex, card));
        return item;
    }

    // Creates the action-card play menu action.
    private static MenuItem actionItem(int cardIndex, Card card, Handler handler) {
        MenuItem item = new MenuItem("Play Action");
        item.setOnAction(event -> handler.play(cardIndex, card));
        return item;
    }

    // Creates the discard menu action.
    private static MenuItem discardItem(int cardIndex, Handler handler) {
        MenuItem item = new MenuItem("Discard");
        item.setDisable(!handler.discardRequired());
        item.setOnAction(event -> handler.discard(cardIndex));
        return item;
    }
}
