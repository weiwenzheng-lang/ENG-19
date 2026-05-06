package ui.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        GameController controller = new GameController();
        Scene scene = new Scene(controller.createContent(), 1180, 760);
        stage.setTitle("Monopoly Deal");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
