package ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {
    private GameController currentController;

    @Override
    public void start(Stage stage) {
        startGame(stage);
    }

    private void startGame(Stage stage) {
        if (currentController != null) {
            currentController.dispose();
        }

        int playerCount = choosePlayerCount(stage);
        if (playerCount < 2 || playerCount > 5) {
            playerCount = 3; // 默认 3 人
        }

        List<String> playerNames = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            playerNames.add("Player " + i);
        }

        currentController = new GameController(playerNames,
                () -> startGame(stage),
                Platform::exit);

        Scene scene = new Scene(currentController.createContent(), 1180, 760);

        // 挂载赛博朋克全局 CSS 样式
        try {
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (NullPointerException e) {
            System.err.println("Warning: style.css not found. Running with default styles.");
        }

        stage.setTitle("Cyber-Hub Transit System: Monopoly Deal");
        stage.setScene(scene);
        stage.show();
    }

    private int choosePlayerCount(Stage owner) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(3, 2, 3, 4, 5);
        dialog.setTitle("System Setup");
        dialog.setHeaderText("Initialize Transit Hub");
        dialog.setContentText("Select number of connected terminals (Players 2-5):");

        // 为弹窗也加载一点深色样式
        dialog.getDialogPane().setStyle("-fx-base: #1a1c1e; -fx-control-inner-background: #1a1c1e; -fx-text-base-color: #00f2ff;");

        if (owner != null && owner.isShowing()) {
            dialog.initOwner(owner);
        }
        return dialog.showAndWait().orElse(3);
    }
}