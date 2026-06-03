package ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Optional;

final class GameDialogs {
    private static final ButtonType OK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CANCEL = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    private static final ButtonType YES = new ButtonType("Yes", ButtonBar.ButtonData.YES);
    private static final ButtonType NO = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

    // Prevents construction of this dialog utility class.
    private GameDialogs() {
    }

    // Shows a typed choice dialog and returns the selected value.
    static <T> Optional<T> showChoice(String title, String header, String fieldLabel,
                                      List<T> choices, T defaultValue) {
        if (choices == null || choices.isEmpty()) {
            return Optional.empty();
        }

        Dialog<T> dialog = create(title, header);
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(choices);
        comboBox.setValue(defaultValue == null ? choices.get(0) : defaultValue);
        comboBox.setMaxWidth(Double.MAX_VALUE);

        VBox content = contentBox();
        content.getChildren().addAll(fieldLabel(fieldLabel), comboBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(OK, CANCEL);
        styleButtons(dialog);
        dialog.setResultConverter(button -> button == OK ? comboBox.getValue() : null);

        return dialog.showAndWait();
    }

    // Shows a text input dialog and returns the entered text.
    static Optional<String> showTextInput(String title, String header, String fieldLabel, String defaultValue) {
        Dialog<String> dialog = create(title, header);
        TextField input = new TextField(defaultValue);
        input.setMaxWidth(Double.MAX_VALUE);

        VBox content = contentBox();
        content.getChildren().addAll(fieldLabel(fieldLabel), input);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(OK, CANCEL);
        styleButtons(dialog);
        dialog.setResultConverter(button -> button == OK ? input.getText() : null);

        return dialog.showAndWait();
    }

    // Shows a yes/no dialog and returns true only for yes.
    static boolean showConfirmation(String title, String header, String message) {
        Dialog<ButtonType> dialog = create(title, header);
        dialog.getDialogPane().setContent(messageLabel(message));
        dialog.getDialogPane().getButtonTypes().setAll(YES, NO);
        styleButtons(dialog);
        dialog.setResultConverter(button -> button);
        return dialog.showAndWait().orElse(NO) == YES;
    }

    // Shows an informational message dialog.
    static void showMessage(String title, String header, String message) {
        Dialog<ButtonType> dialog = create(title, header);
        dialog.getDialogPane().setContent(messageLabel(message));
        dialog.getDialogPane().getButtonTypes().setAll(OK);
        styleButtons(dialog);
        dialog.showAndWait();
    }

    // Creates the shared transparent styled dialog shell.
    static <T> Dialog<T> create(String title, String header) {
        Dialog<T> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(title);

        DialogPane pane = dialog.getDialogPane();
        pane.setGraphic(null);
        pane.setHeaderText(header);
        pane.getStyleClass().add("game-dialog");

        String stylesheet = GameDialogs.class.getResource("style.css") == null
                ? null
                : GameDialogs.class.getResource("style.css").toExternalForm();
        if (stylesheet != null && !pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }

        // Resize after the transparent stage is available.
        dialog.setOnShown(event -> {
            Scene scene = pane.getScene();
            if (scene != null) {
                scene.setFill(Color.TRANSPARENT);
                Stage stage = (Stage) scene.getWindow();
                stage.sizeToScene();
            }
        });
        return dialog;
    }

    // Creates a standard dialog content container.
    static VBox contentBox() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(4, 2, 0, 2));
        content.setAlignment(Pos.CENTER_LEFT);
        return content;
    }

    // Creates a standard field label.
    static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("game-dialog-field-label");
        return label;
    }

    // Creates a wrapped message label.
    static Label messageLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("game-dialog-message");
        label.setWrapText(true);
        label.setMaxWidth(520);
        return label;
    }

    // Creates a wrapped checkbox for multi-card payment dialogs.
    static CheckBox checkBox(String text) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.getStyleClass().add("game-dialog-check-box");
        checkBox.setWrapText(true);
        return checkBox;
    }

    // Creates a bold status label for dialog summaries.
    static Label statusLabel() {
        Label label = new Label();
        label.getStyleClass().add("game-dialog-status");
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        return label;
    }

    // Applies primary and secondary styles to dialog buttons.
    static void styleButtons(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        for (ButtonType type : pane.getButtonTypes()) {
            Node node = pane.lookupButton(type);
            if (node instanceof Button) {
                Button button = (Button) node;
                button.getStyleClass().add(type.getButtonData().isCancelButton()
                        ? "game-dialog-button-secondary"
                        : "game-dialog-button-primary");
            }
        }
    }
}
