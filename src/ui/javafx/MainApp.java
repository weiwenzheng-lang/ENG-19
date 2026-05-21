package ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainApp extends Application {
    private GameController currentController;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    private void showMainMenu() {
        VBox menu = new VBox(20);
        menu.setAlignment(Pos.CENTER);
        menu.setStyle("-fx-background-color: #132127;");

        Label title = new Label("Monopoly Deal");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        title.setTextFill(javafx.scene.paint.Color.web("#f0c978"));

        Button startBtn = styledButton("Start Game");
        startBtn.setOnAction(e -> startGame());

        Button helpBtn = styledButton("How to Play");
        helpBtn.setOnAction(e -> showHelp());

        Button exitBtn = styledButton("Exit");
        exitBtn.setOnAction(e -> Platform.exit());

        menu.getChildren().addAll(title, startBtn, helpBtn, exitBtn);
        Scene scene = new Scene(menu, 600, 450);
        primaryStage.setTitle("Monopoly Deal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startGame() {
        if (currentController != null) {
            currentController.dispose();
        }

        int playerCount = choosePlayerCount();
        if (playerCount < 2) return;

        List<String> playerNames = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            Optional<String> name = askPlayerName(i);
            if (name.isPresent() && !name.get().trim().isEmpty()) {
                playerNames.add(name.get().trim());
            } else {
                playerNames.add("Player " + i);
            }
        }

        currentController = new GameController(playerNames,
                this::showMainMenu,
                Platform::exit);

        Scene scene = new Scene(currentController.createContent(), 1180, 760);
        try {
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (NullPointerException e) {
            System.err.println("Warning: style.css not found.");
        }

        primaryStage.setTitle("Monopoly Deal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private int choosePlayerCount() {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(3, 2, 3, 4, 5);
        dialog.setTitle("Players");
        dialog.setHeaderText("How many players?");
        dialog.setContentText("Select number (2-5):");
        return dialog.showAndWait().orElse(-1);
    }

    private Optional<String> askPlayerName(int number) {
        TextInputDialog dialog = new TextInputDialog("Player " + number);
        dialog.setTitle("Player Name");
        dialog.setHeaderText("Enter name for Player " + number);
        dialog.setContentText("Name:");
        return dialog.showAndWait();
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("How to Play");
        alert.setHeaderText("Monopoly Deal Rules");
        alert.setContentText(
            "Goal: Collect 3 complete property sets of DIFFERENT colors.\n\n" +
            "Each turn: Draw 2 cards (or 5 if hand empty) → Play up to 3 cards → Discard to 7 max.\n\n" +
            "Card types:\n" +
            "- Money: Bank as cash to pay rent\n" +
            "- Property: Build color sets on the table\n" +
            "- Rent: Charge opponents rent for a color you own\n" +
            "- Action: Special effects (steal, swap, counter, etc.)\n\n" +
            "Payment: Pay with bank cash first; if insufficient, sell properties.\n" +
            "No change is given.\n\n" +
            "Just Say No: Counter any action played against you."
        );
        alert.showAndWait();
    }

    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(220);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        btn.setStyle("-fx-background-color: #f0c978; -fx-text-fill: #1b2a31;"
                + "-fx-background-radius: 8; -fx-padding: 12 24 12 24;");
        return btn;
    }
}