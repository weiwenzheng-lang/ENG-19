package ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import core.GameManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MainApp extends Application {
    private GameController currentController;
    private NetworkLobbyController networkController;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    @Override
    public void stop() {
        disposeCurrentViews();
    }

    private void showMainMenu() {
        disposeCurrentViews();

        StackPane screen = new StackPane();
        ImageView background = new ImageView(loadResourceImage("/assets/ui/backgrounds/menu.png"));
        background.fitWidthProperty().bind(screen.widthProperty());
        background.fitHeightProperty().bind(screen.heightProperty());
        background.setPreserveRatio(false);
        background.setSmooth(true);

        VBox menu = new VBox(18);
        menu.setAlignment(Pos.CENTER);
        menu.setMaxWidth(340);
        menu.setStyle("-fx-padding: 28 34 30 34;");

        Label title = new Label("Monopoly Deal");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        title.setTextFill(javafx.scene.paint.Color.web("#ffe4a1"));

        Button startBtn = styledButton("Start Game");
        startBtn.setOnAction(e -> startGame());

        Button lanBtn = styledButton("Local WiFi Game");
        lanBtn.setOnAction(e -> showLanLobby());

        Button wifiHelpBtn = styledButton("WiFi Guide");
        wifiHelpBtn.setOnAction(e -> NetworkLobbyController.showWifiGuide());

        Button helpBtn = styledButton("How to Play");
        helpBtn.setOnAction(e -> showHelp());

        Button exitBtn = styledButton("Exit");
        exitBtn.setOnAction(e -> Platform.exit());

        menu.getChildren().addAll(title, startBtn, lanBtn, wifiHelpBtn, helpBtn, exitBtn);
        screen.getChildren().addAll(background, menu);
        Scene scene = new Scene(screen, 1180, 664);
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

        int humanCount = chooseHumanCount(playerCount);
        if (humanCount < 1) return;

        List<String> playerNames = new ArrayList<>();
        List<Boolean> isHuman = new ArrayList<>();

        // 收集人类玩家姓名
        for (int i = 1; i <= humanCount; i++) {
            Optional<String> name = askPlayerName(i);
            if (!name.isPresent()) {
                return;
            }
            if (!name.get().trim().isEmpty()) {
                playerNames.add(name.get().trim());
            } else {
                playerNames.add("Player " + i);
            }
            isHuman.add(true);
        }

        // 自动填充 AI 玩家
        String[] botNames = {"Bot Alice", "Bot Bob", "Bot Carol", "Bot Diana"};
        for (int i = humanCount; i < playerCount; i++) {
            playerNames.add(botNames[i - humanCount]);
            isHuman.add(false);
        }

        currentController = new GameController(playerNames, isHuman,
                this::showMainMenu,
                Platform::exit);

        Scene scene = new Scene(currentController.createContent(), 1366, 768);
        applyStylesheet(scene);

        primaryStage.setTitle("Monopoly Deal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private int chooseHumanCount(int totalPlayers) {
        List<Integer> options = new ArrayList<>();
        for (int n = 1; n <= totalPlayers; n++) {
            options.add(n);
        }
        return GameDialogs.showChoice("Player Setup",
                totalPlayers + " players total. How many HUMAN players?\n"
                        + "(AI will fill remaining " + (totalPlayers - 1) + " slots if any)",
                "Humans",
                options,
                totalPlayers).orElse(-1);
    }

    private void showLanLobby() {
        disposeCurrentViews();

        networkController = new NetworkLobbyController(this::showMainMenu, this::showNetworkGame);
        Scene scene = new Scene(networkController.createContent(), 900, 600);
        applyStylesheet(scene);

        primaryStage.setTitle("Monopoly Deal - Local WiFi Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showNetworkGame(NetworkLobbyController.NetworkGameLaunch launch) {
        if (currentController != null) {
            currentController.dispose();
        }

        List<String> networkNames = launch.getPlayerNames();
        int networkCount = networkNames.size();

        // 联网玩家不足 5 人时，询问是否添加 AI
        int totalPlayers = networkCount;
        if (networkCount < GameManager.MAX_PLAYERS) {
            List<Integer> options = new ArrayList<>();
            for (int n = networkCount; n <= GameManager.MAX_PLAYERS; n++) {
                options.add(n);
            }
            totalPlayers = GameDialogs.showChoice("Add AI Players",
                    "Network has " + networkCount + " human player(s).\n"
                            + "Add AI bots to fill more slots?",
                    "Total",
                    options,
                    networkCount).orElse(networkCount);
        }

        // 构建最终的玩家名单和人类/AI 标记
        List<String> playerNames = new ArrayList<>(networkNames);
        List<Boolean> isHuman = new ArrayList<>();
        for (int i = 0; i < networkCount; i++) {
            isHuman.add(true);
        }

        String[] botNames = {"Bot Alice", "Bot Bob", "Bot Carol", "Bot Diana"};
        for (int i = networkCount; i < totalPlayers; i++) {
            playerNames.add(botNames[i - networkCount]);
            isHuman.add(false);
        }

        // 扩展 roster 以显示 AI 玩家
        List<String> fullRoster = new ArrayList<>(launch.getRosterLines());
        for (int i = networkCount; i < totalPlayers; i++) {
            fullRoster.add(botNames[i - networkCount] + "  [AI]  Ready");
        }

        Runnable leaveNetworkGame = () -> {
            if (networkController != null) {
                networkController.disconnectRoom();
                networkController = null;
            }
            showMainMenu();
        };

        currentController = new GameController(playerNames, isHuman,
                leaveNetworkGame,
                Platform::exit);
        Scene scene = new Scene(currentController.createContent(), 1366, 768);
        applyStylesheet(scene);
        currentController.showNetworkInfo(launch.getInitialStatus(), fullRoster);
        if (networkController != null) {
            networkController.setNetworkStatusSink(currentController::setNetworkStatus);
            networkController.setNetworkRosterSink(currentController::setNetworkRoster);
            networkController.setNetworkAlertSink(currentController::showNetworkAlert);
        }

        primaryStage.setTitle("Monopoly Deal - Network Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

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

    private void applyStylesheet(Scene scene) {
        try {
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (NullPointerException e) {
            System.err.println("Warning: style.css not found.");
        }
    }

    private int choosePlayerCount() {
        return GameDialogs.showChoice("Players",
                "How many players?",
                "Players",
                java.util.Arrays.asList(2, 3, 4, 5),
                3).orElse(-1);
    }

    private Optional<String> askPlayerName(int number) {
        return GameDialogs.showTextInput("Player Name",
                "Enter name for Player " + number,
                "Name",
                "Player " + number);
    }

    private void showHelp() {
        GameDialogs.showMessage("How to Play",
                "Monopoly Deal Rules",
            "Goal: Collect 3 complete property sets of DIFFERENT colors.\n\n" +
            "Each turn: Draw 2 cards (or 5 if hand empty) -> Play up to 3 cards -> Discard to 7 max.\n\n" +
            "Card types:\n" +
            "- Money: Bank as cash to pay rent\n" +
            "- Property: Build color sets on the table\n" +
            "- Rent: Charge opponents rent for a color you own\n" +
            "- Action: Special effects (steal, swap, counter, etc.)\n\n" +
            "Payment: Pay with bank cash first; if insufficient, sell properties.\n" +
            "No change is given.\n\n" +
            "Just Say No: Counter any action played against you.\n\n" +
            "AI Bots: In both local and LAN modes, you can add AI players.\n" +
            "Select fewer human players than total slots, and bots will\n" +
            "fill the remaining seats automatically."
        );
    }

    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(220);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        applyButtonStyle(btn, false);
        btn.setOnMouseEntered(event -> {
            applyButtonStyle(btn, true);
            btn.setScaleX(1.04);
            btn.setScaleY(1.04);
        });
        btn.setOnMouseExited(event -> {
            applyButtonStyle(btn, false);
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        return btn;
    }

    private void applyButtonStyle(Button btn, boolean hover) {
        btn.setStyle((hover
                ? "-fx-background-color: linear-gradient(to bottom, #fff2bd, #e0ad46);"
                : "-fx-background-color: linear-gradient(to bottom, #ffe6a5, #c89432);")
                + "-fx-text-fill: #172028; -fx-background-radius: 22;"
                + "-fx-border-color: #fff0bc; -fx-border-radius: 22;"
                + "-fx-font-weight: bold; -fx-padding: 12 24 12 24;"
                + (hover ? "-fx-effect: dropshadow(gaussian, rgba(255,230,165,0.62), 18, 0.28, 0, 0);" : ""));
    }

    private Image loadResourceImage(String path) {
        URL resource = getClass().getResource(path);
        if (resource != null) {
            return new Image(resource.toExternalForm(), 0, 0, true, true);
        }
        Path filePath = Paths.get(System.getProperty("user.dir"), "src", path.replaceFirst("^/", ""));
        return Files.isRegularFile(filePath) ? new Image(filePath.toUri().toString(), 0, 0, true, true) : null;
    }
}
