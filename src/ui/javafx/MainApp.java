package ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import core.GameManager;
import player.PlayerType;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MainApp extends Application {
    private GameController currentController;
    private NetworkLobbyController networkController;
    private Stage primaryStage;

    // Launches the JavaFX application.
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    // Stores the primary stage and opens the main menu.
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    @Override
    // Releases active controllers when JavaFX stops.
    public void stop() {
        disposeCurrentViews();
    }

    // Shows the first screen with all game mode choices.
    private void showMainMenu() {
        disposeCurrentViews();
        StackPane screen = createMenuScreen();
        Scene scene = new Scene(screen, 1180, 664);
        primaryStage.setTitle("Monopoly Deal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Builds the menu background and button stack.
    private StackPane createMenuScreen() {
        StackPane screen = new StackPane();
        screen.getChildren().addAll(createMenuBackground(screen), createMainMenuBox());
        return screen;
    }

    // Creates the full-window menu background.
    private ImageView createMenuBackground(StackPane screen) {
        ImageView background = new ImageView(loadResourceImage("/assets/ui/backgrounds/menu.png"));
        background.fitWidthProperty().bind(screen.widthProperty());
        background.fitHeightProperty().bind(screen.heightProperty());
        background.setPreserveRatio(false);
        background.setSmooth(true);
        return background;
    }

    // Creates the main menu title and mode buttons.
    private VBox createMainMenuBox() {
        VBox menu = new VBox(18);
        menu.setAlignment(Pos.CENTER);
        menu.setMaxWidth(340);
        menu.setStyle("-fx-padding: 28 34 30 34;");
        Button startBtn = styledButton("Local Players");
        startBtn.setOnAction(e -> startGame());

        Button aiBtn = styledButton("Local + AI");
        aiBtn.setOnAction(e -> startMixedAiGame());

        Button lanBtn = styledButton("LAN + AI");
        lanBtn.setOnAction(e -> showLanLobby());

        Button helpBtn = styledButton("How to Play");
        helpBtn.setOnAction(e -> showHelp());

        Button exitBtn = styledButton("Exit");
        exitBtn.setOnAction(e -> Platform.exit());

        menu.getChildren().addAll(createTitleLabel(), startBtn, aiBtn, lanBtn, helpBtn, exitBtn);
        return menu;
    }

    // Creates the main menu title.
    private Label createTitleLabel() {
        Label title = new Label("Monopoly Deal");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        title.setTextFill(javafx.scene.paint.Color.web("#ffe4a1"));
        return title;
    }

    // Starts same-computer human multiplayer.
    private void startGame() {
        if (currentController != null) {
            currentController.dispose();
        }

        int playerCount = choosePlayerCount();
        if (playerCount < 2) return;

        List<String> playerNames = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            Optional<String> name = askPlayerName(i);
            if (!name.isPresent()) {
                return;
            }
            if (!name.get().trim().isEmpty()) {
                playerNames.add(name.get().trim());
            } else {
                playerNames.add("Player " + i);
            }
        }

        currentController = new GameController(playerNames,
                this::showMainMenu,
                Platform::exit);

        Scene scene = new Scene(currentController.createContent(), 1366, 768);
        applyStylesheet(scene);

        primaryStage.setTitle("Monopoly Deal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Starts same-computer play with humans and AI seats.
    private void startMixedAiGame() {
        if (currentController != null) {
            currentController.dispose();
        }

        int playerCount = choosePlayerCount();
        if (playerCount < 2) return;

        int humanCount = chooseHumanCount(playerCount);
        if (humanCount < 1) return;

        List<GameManager.PlayerSetup> playerSetups = new ArrayList<>();
        for (int i = 1; i <= humanCount; i++) {
            Optional<String> humanName = askPlayerName(i);
            if (!humanName.isPresent()) {
                return;
            }
            String name = humanName.get().trim().isEmpty() ? "Player " + i : humanName.get().trim();
            playerSetups.add(new GameManager.PlayerSetup(name, PlayerType.HUMAN));
        }
        for (int i = humanCount + 1; i <= playerCount; i++) {
            playerSetups.add(new GameManager.PlayerSetup("AI " + (i - humanCount), PlayerType.AI));
        }

        currentController = new GameController(GameModeConfig.ai(playerSetups),
                this::showMainMenu,
                Platform::exit);

        Scene scene = new Scene(currentController.createContent(), 1366, 768);
        applyStylesheet(scene);

        primaryStage.setTitle("Monopoly Deal - Local + AI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Opens the LAN lobby screen.
    private void showLanLobby() {
        disposeCurrentViews();

        networkController = new NetworkLobbyController(this::showMainMenu, this::startNetworkGame);
        Scene scene = new Scene(networkController.createContent(), 900, 600);
        applyStylesheet(scene);

        primaryStage.setTitle("Monopoly Deal - Local WiFi Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Opens a network-synchronized game table.
    private void startNetworkGame(List<GameManager.PlayerSetup> players, long deckSeed, int localPlayerIndex) {
        if (networkController == null) {
            return;
        }
        NetworkGameBridge bridge = networkController.createGameBridge();
        currentController = new GameController(
                GameModeConfig.network(players, deckSeed, localPlayerIndex, bridge),
                this::showMainMenu,
                Platform::exit);
        Scene scene = new Scene(currentController.createContent(), 1366, 768);
        applyStylesheet(scene);
        primaryStage.setTitle("Monopoly Deal - Local WiFi Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Disposes any active game or lobby controller.
    private void disposeCurrentViews() {
        if (currentController != null) {
            currentController.dispose();
            currentController = null;
        }
        if (networkController != null) {
            networkController.dispose();
            networkController = null;
        }
    }

    // Applies the shared JavaFX stylesheet when available.
    private void applyStylesheet(Scene scene) {
        try {
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (NullPointerException e) {
            System.err.println("Warning: style.css not found.");
        }
    }

    // Prompts for the official 2-5 player count.
    private int choosePlayerCount() {
        return GameDialogs.showChoice("Players",
                "How many players?",
                "Players",
                Arrays.asList(2, 3, 4, 5),
                3).orElse(-1);
    }

    // Prompts for how many seats are controlled by local humans.
    private int chooseHumanCount(int totalPlayers) {
        List<Integer> choices = new ArrayList<>();
        for (int i = 1; i < totalPlayers; i++) {
            choices.add(i);
        }
        return GameDialogs.showChoice("Human Players",
                "How many people will share this computer?",
                "Human players",
                choices,
                Math.min(2, totalPlayers - 1)).orElse(-1);
    }

    // Prompts for one player name.
    private Optional<String> askPlayerName(int number) {
        return GameDialogs.showTextInput("Player Name",
                "Enter name for Player " + number,
                "Name",
                "Player " + number);
    }

    // Shows the short in-game help text.
    private void showHelp() {
        GameDialogs.showMessage("How to Play",
                "Monopoly Deal Rules",
            "Goal: Collect 3 complete property sets of DIFFERENT colors.\n\n" +
            "Each turn: Draw 2 cards (or 5 if hand empty) -> Play up to 3 cards -> Discard to 7 max.\n\n" +
            "Table buttons:\n" +
            "- Draw: Shows draw pile and discard pile status\n" +
            "- Bank: Shows the current visible player's money in bank\n" +
            "- Properties: Shows color-set progress and rent values\n" +
            "- Pass Go / Actions: Lists action cards in hand\n" +
            "- Trade / Opponents: Shows each opponent's visible status\n" +
            "- End Turn: Ends the current real player's turn after returning excess hand cards to the draw pile bottom\n\n" +
            "Card types:\n" +
            "- Money: Bank as cash to pay rent\n" +
            "- Property: Build color sets on the table\n" +
            "- Rent: Charge opponents rent for a color you own\n" +
            "- Action: Special effects (steal, swap, counter, etc.)\n\n" +
            "Payment: Pay with bank cash first; if insufficient, sell properties.\n" +
            "No change is given.\n\n" +
            "Just Say No: Counter any action played against you."
        );
    }

    // Creates a styled main menu button.
    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(220);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        applyButtonStyle(btn, false);
        btn.setOnMouseEntered(event -> {
            applyButtonStyle(btn, true);
            btn.setOpacity(0.96);
        });
        btn.setOnMouseExited(event -> {
            applyButtonStyle(btn, false);
            btn.setOpacity(1.0);
        });
        return btn;
    }

    // Applies normal or hover styling to a main menu button.
    private void applyButtonStyle(Button btn, boolean hover) {
        btn.setStyle((hover
                ? "-fx-background-color: linear-gradient(to bottom, #fff2bd, #e0ad46);"
                : "-fx-background-color: linear-gradient(to bottom, #ffe6a5, #c89432);")
                + "-fx-text-fill: #172028; -fx-background-radius: 22;"
                + "-fx-border-color: #fff0bc; -fx-border-radius: 22;"
                + "-fx-font-weight: bold; -fx-padding: 12 24 12 24;"
                + (hover ? "-fx-effect: dropshadow(gaussian, rgba(255,230,165,0.62), 18, 0.28, 0, 0);" : ""));
    }

    // Loads an image from resources, then falls back to the source tree.
    private Image loadResourceImage(String path) {
        URL resource = getClass().getResource(path);
        if (resource != null) {
            return new Image(resource.toExternalForm(), 0, 0, true, true);
        }
        Path filePath = Paths.get(System.getProperty("user.dir"), "src", path.replaceFirst("^/", ""));
        return Files.isRegularFile(filePath) ? new Image(filePath.toUri().toString(), 0, 0, true, true) : null;
    }
}
