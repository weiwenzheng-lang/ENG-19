package ui.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {
    
    @Override
    public void start(Stage stage) {
        int playerCount = choosePlayerCount(stage);
        if (playerCount < 2 || playerCount > 5) {
            playerCount = 3; // 默认 3 人
        }

        List<String> playerNames = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            playerNames.add("Player " + i);
        }

        GameController controller = new GameController(playerNames);
        Scene scene = new Scene(controller.createContent(), 1180, 760);
        stage.setTitle("Monopoly Deal");
        stage.setScene(scene);
        stage.show();
    }

    private int choosePlayerCount(Stage owner) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(3, 2, 3, 4, 5);
        dialog.setTitle("Game Setup");
        dialog.setHeaderText("Select Number of Players");
        dialog.setContentText("Choose the number of players (2–5):");
        dialog.initOwner(owner);
        return dialog.showAndWait().orElse(3);
    }

    public static void main(String[] args) {
        launch(args);
    }
}