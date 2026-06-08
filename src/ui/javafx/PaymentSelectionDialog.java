package ui.javafx;

import cards.Card;
import cards.PropertyCard;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Handles human and automatic payment-card selection.
final class PaymentSelectionDialog {
    // Prevents construction of this dialog helper.
    private PaymentSelectionDialog() {
    }

    // Chooses payment cards either automatically or through a human dialog.
    static List<Card> choose(Player payer, Player payee, int amount,
                             List<Card> bankCards, List<PropertyCard> propertyCards,
                             boolean automatic) {
        if (automatic) {
            return chooseAutomatic(amount, bankCards, propertyCards);
        }
        return chooseHuman(payer, payee, amount, bankCards, propertyCards);
    }

    // Shows the payment dialog for local human players.
    private static List<Card> chooseHuman(Player payer, Player payee, int amount,
                                          List<Card> bankCards, List<PropertyCard> propertyCards) {
        List<Card> options = new ArrayList<>();
        options.addAll(bankCards);
        options.addAll(propertyCards);
        if (options.isEmpty()) {
            return Collections.emptyList();
        }

        int totalAvailable = options.stream().mapToInt(Card::getMonetaryValue).sum();
        Dialog<List<Card>> dialog = GameDialogs.create("Payment",
                payer.getPlayerName() + " owes " + payee.getPlayerName() + " " + amount + "M");

        VBox content = GameDialogs.contentBox();
        Label selectedTotal = GameDialogs.statusLabel();
        List<CheckBox> boxes = createPaymentBoxes(options, bankCards, content);
        content.getChildren().add(selectedTotal);
        dialog.getDialogPane().setContent(content);

        ButtonType payButtonType = new ButtonType("Pay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(payButtonType);
        GameDialogs.styleButtons(dialog);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(payButtonType);
        Runnable updateSelection = () ->
                updateSelectedTotal(boxes, options, amount, totalAvailable, selectedTotal, okButton);
        boxes.forEach(box -> box.selectedProperty().addListener((ignored, oldValue, newValue) -> updateSelection.run()));
        updateSelection.run();

        dialog.setResultConverter(button -> button == payButtonType
                ? selectedCards(boxes, options)
                : Collections.emptyList());
        return dialog.showAndWait().orElse(Collections.emptyList());
    }

    // Creates one checkbox per payable card.
    private static List<CheckBox> createPaymentBoxes(List<Card> options, List<Card> bankCards, VBox content) {
        List<CheckBox> boxes = new ArrayList<>();
        for (Card card : options) {
            boolean fromBank = bankCards.contains(card);
            CheckBox box = GameDialogs.checkBox((fromBank ? "Bank: " : "Property: ")
                    + card.getCardName() + " (" + card.getMonetaryValue() + "M)");
            boxes.add(box);
            content.getChildren().add(box);
        }
        return boxes;
    }

    // Updates the selected payment total and pay-button enabled state.
    private static void updateSelectedTotal(List<CheckBox> boxes, List<Card> options, int amount,
                                            int totalAvailable, Label selectedTotal, Button okButton) {
        int selected = 0;
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isSelected()) {
                selected += options.get(i).getMonetaryValue();
            }
        }
        selectedTotal.setText("Selected: " + selected + "M / Owed: " + amount + "M");
        boolean enough = selected >= amount || (totalAvailable < amount && selected == totalAvailable);
        okButton.setDisable(!enough);
    }

    // Returns the currently selected cards.
    private static List<Card> selectedCards(List<CheckBox> boxes, List<Card> options) {
        List<Card> selected = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isSelected()) {
                selected.add(options.get(i));
            }
        }
        return selected;
    }

    // Chooses low-value cards first for AI and remote automatic payment.
    private static List<Card> chooseAutomatic(int amount, List<Card> bankCards, List<PropertyCard> propertyCards) {
        List<Card> options = new ArrayList<>(bankCards);
        options.sort(Comparator.comparingInt(Card::getMonetaryValue));
        List<Card> selected = selectEnoughCards(options, amount);
        int total = selected.stream().mapToInt(Card::getMonetaryValue).sum();
        if (total >= amount) {
            return selected;
        }

        List<PropertyCard> properties = new ArrayList<>(propertyCards);
        properties.sort(Comparator.comparingInt(Card::getMonetaryValue));
        selected.addAll(selectEnoughCards(new ArrayList<Card>(properties), amount - total));
        return selected;
    }

    // Selects cards until the requested amount is reached or assets run out.
    private static List<Card> selectEnoughCards(List<Card> candidates, int amount) {
        List<Card> selected = new ArrayList<>();
        int total = 0;
        for (Card card : candidates) {
            if (total >= amount) {
                break;
            }
            selected.add(card);
            total += card.getMonetaryValue();
        }
        return selected;
    }
}
