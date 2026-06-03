package ui.javafx;

import ai.AIPlayerBrain;
import ai.AITurnExecutor;
import cards.Card;
import cards.PropertyCard;
import cards.RentCard;
import core.GameManager;
import core.TargetInfo;
import network.LanGameMessage;
import patterns.observer.GameObserver;
import player.Player;
import player.PlayerType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class GameController implements GameObserver {
    private static final double BOARD_WIDTH = 1672;
    private static final double BOARD_HEIGHT = 941;
    private static final double HAND_CARD_WIDTH = 90;
    private static final double HAND_CARD_HEIGHT = 149;
    private static final double OWN_TABLE_CARD_WIDTH = 56;
    private static final double OWN_TABLE_CARD_HEIGHT = 92;
    private static final double OPPONENT_CARD_WIDTH = 74;
    private static final double OPPONENT_CARD_HEIGHT = 122;
    private static final double PILE_CARD_WIDTH = 70;
    private static final double PILE_CARD_HEIGHT = 116;

    private final GameManager game = GameManager.getInstance();
    private final StackPane root = new StackPane();
    private final Pane boardPane = new Pane();
    private final ImageView boardBackground = new ImageView();
    private final Pane handView = new Pane();
    private final HBox opponents = new HBox(10);
    private final Pane bankView = new Pane();
    private final Pane propertyView = new Pane();
    private final Pane ownTableView = new Pane();
    private final Pane centerPileView = new Pane();
    private final List<PlayerZone> opponentZones = new ArrayList<>();
    private final ListView<String> logView = new ListView<>();
    private final TextFlow turnStatus = new TextFlow();
    private final Label ownNameLabel = new Label();
    private final Label ownStatsLabel = new Label();
    private final Label ownSetsLabel = new Label();
    private final ProgressBar ownSetsProgress = new ProgressBar(0);
    private final Label deckLabel = new Label("Deck");
    private final Label discardLabel = new Label("Discard");
    private final CardView drawPileView = CardView.back(0, PILE_CARD_WIDTH, PILE_CARD_HEIGHT);
    private final CardView discardPileView = new CardView("Discard", "", PILE_CARD_WIDTH, PILE_CARD_HEIGHT);
    private final Button endTurnButton = imageButton("end_turn.png", "End Turn", 210, 64);
    private final HBox gameOverActions = new HBox(10);
    private final HBox quickActions = new HBox(8);
    private final Runnable newGameAction;
    private final Runnable exitGameAction;
    private final GameModeConfig modeConfig;
    private final AITurnExecutor aiExecutor = new AITurnExecutor(new AIPlayerBrain());
    private final int playerCount;
    private boolean applyingRemoteAction;
    private boolean disposed;

    public GameController(List<String> playerNames) {
        this(playerNames, () -> {}, Platform::exit);
    }

    public GameController(List<String> playerNames, Runnable newGameAction, Runnable exitGameAction) {
        this(GameModeConfig.local(toHumanSetups(playerNames)), newGameAction, exitGameAction);
    }

    public GameController(GameModeConfig modeConfig, Runnable newGameAction, Runnable exitGameAction) {
        this.newGameAction = newGameAction;
        this.exitGameAction = exitGameAction;
        this.modeConfig = modeConfig;
        this.playerCount = modeConfig.players.size();
        player.BankArea.setPaymentResolver(this::choosePaymentCardsForPayment);
        game.addObserver(this);
        if (modeConfig.deckSeed == null) {
            game.initializeConfiguredGame(modeConfig.players);
        } else {
            game.initializeConfiguredGame(modeConfig.players, modeConfig.deckSeed);
        }
        if (modeConfig.networkBridge != null) {
            modeConfig.networkBridge.setGameMessageHandler(this::handleNetworkMessage);
        }
    }

    private static List<GameManager.PlayerSetup> toHumanSetups(List<String> playerNames) {
        List<GameManager.PlayerSetup> setups = new ArrayList<>();
        for (String name : playerNames) {
            setups.add(new GameManager.PlayerSetup(name, PlayerType.HUMAN));
        }
        return setups;
    }

    public StackPane createContent() {
        root.getChildren().clear();
        root.setStyle("-fx-background-color: #0d0f12;");
        boardPane.getChildren().clear();
        boardPane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        boardPane.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        boardPane.setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);

        boardBackground.setImage(loadResourceImage("/assets/ui/backgrounds/board_" + playerCount + "p.png"));
        boardBackground.setFitWidth(BOARD_WIDTH);
        boardBackground.setFitHeight(BOARD_HEIGHT);
        boardBackground.setPreserveRatio(false);
        boardPane.getChildren().add(boardBackground);

        createBoardOverlay();
        root.getChildren().add(boardPane);

        NumberBinding scale = Bindings.min(root.widthProperty().divide(BOARD_WIDTH),
                root.heightProperty().divide(BOARD_HEIGHT));
        boardPane.scaleXProperty().bind(scale);
        boardPane.scaleYProperty().bind(scale);
        renderAll();
        scheduleAiIfNeeded();
        return root;
    }

    private void createBoardOverlay() {
        opponentZones.clear();
        for (ZoneSpec spec : opponentSpecs(playerCount)) {
            PlayerZone zone = new PlayerZone(spec);
            configurePane(zone.cards, spec.x, spec.y, spec.width, spec.height, spec.rotate);
            configureNameLabel(zone.name, spec.nameX, spec.nameY + 1, spec.nameWidth, 24);
            configureStatsLabel(zone.stats, spec.nameX + 4, spec.nameY + 26,
                    spec.nameWidth - 48, 17);
            configureSetsProgress(zone.setsProgress, spec.nameX + 8, spec.nameY + spec.nameHeight - 6,
                    spec.nameWidth - 16, 8);
            configureSetsLabel(zone.setsLabel, spec.nameX + spec.nameWidth - 42, spec.nameY + 26,
                    38, 17);
            opponentZones.add(zone);
            boardPane.getChildren().addAll(zone.cards, zone.name, zone.stats, zone.setsProgress, zone.setsLabel);
        }

        ZoneSpec ownTable = ownTableSpec(playerCount);
        configurePane(ownTableView, ownTable.x, ownTable.y, ownTable.width, ownTable.height, ownTable.rotate);
        ZoneSpec hand = handSpec(playerCount);
        configurePane(handView, hand.x, hand.y, hand.width, hand.height, hand.rotate);
        configurePane(centerPileView, 545, 352, 582, 190, 0);
        boardPane.getChildren().addAll(ownTableView, handView, centerPileView);

        ZoneSpec ownName = ownNameSpec(playerCount);
        configureNameLabel(ownNameLabel, ownName.x, ownName.y + 1, ownName.width, 24);
        configureStatsLabel(ownStatsLabel, ownName.x + 4, ownName.y + 26, ownName.width - 48, 17);
        configureSetsProgress(ownSetsProgress, ownName.x + 8, ownName.y + ownName.height - 6,
                ownName.width - 16, 8);
        configureSetsLabel(ownSetsLabel, ownName.x + ownName.width - 42, ownName.y + 26, 38, 17);
        boardPane.getChildren().addAll(ownNameLabel, ownStatsLabel, ownSetsProgress, ownSetsLabel);

        turnStatus.setLayoutX(686);
        turnStatus.setLayoutY(382);
        turnStatus.setPrefSize(300, 110);
        turnStatus.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        turnStatus.setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
        boardPane.getChildren().add(turnStatus);

        Button deckInfo = imageButton("draw.png", "Deck", 92, 32);
        deckInfo.setTooltip(new Tooltip("Deck status"));
        deckInfo.setOnAction(event -> showDeckSummary());
        Button bankInfo = imageButton("bank.png", "Bank", 92, 32);
        bankInfo.setTooltip(new Tooltip("Bank summary"));
        bankInfo.setOnAction(event -> showBankSummary());
        Button propertyInfo = imageButton("properties.png", "Properties", 102, 32);
        propertyInfo.setTooltip(new Tooltip("Property summary"));
        propertyInfo.setOnAction(event -> showPropertySummary());
        Button actionInfo = imageButton("pass_go.png", "Actions", 92, 32);
        actionInfo.setTooltip(new Tooltip("Action cards in hand"));
        actionInfo.setOnAction(event -> showActionSummary());
        Button opponentInfo = imageButton("trade.png", "Opponents", 92, 32);
        opponentInfo.setTooltip(new Tooltip("Opponent table summary"));
        opponentInfo.setOnAction(event -> showOpponentsSummary());
        quickActions.getChildren().setAll(deckInfo, bankInfo, propertyInfo, actionInfo, opponentInfo);
        quickActions.setLayoutX(1088);
        quickActions.setLayoutY(706);
        quickActions.setAlignment(Pos.CENTER);
        boardPane.getChildren().add(quickActions);

        logView.setLayoutX(24);
        logView.setLayoutY(22);
        logView.setPrefSize(365, 150);
        logView.setStyle("-fx-control-inner-background: rgba(5,8,12,0.62); "
                + "-fx-background-color: rgba(5,8,12,0.50); -fx-background-radius: 12;"
                + "-fx-border-color: rgba(255,218,142,0.45); -fx-border-radius: 12;"
                + "-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        boardPane.getChildren().add(logView);

        endTurnButton.setLayoutX(1430);
        endTurnButton.setLayoutY(842);
        endTurnButton.setOnAction(event -> {
            requestEndTurn();
        });

        Button newGame = imageButton("confirm.png", "New Game", 180, 56);
        newGame.setOnAction(event -> newGameAction.run());
        Button exitGame = imageButton("cancel.png", "Exit Game", 180, 56);
        exitGame.setOnAction(event -> exitGameAction.run());
        gameOverActions.getChildren().setAll(exitGame, newGame);
        gameOverActions.setLayoutX(1230);
        gameOverActions.setLayoutY(842);
        gameOverActions.setAlignment(Pos.CENTER);
        gameOverActions.setVisible(false);
        gameOverActions.setManaged(false);
        boardPane.getChildren().addAll(endTurnButton, gameOverActions);
    }

    private ZoneSpec[] opponentSpecs(int count) {
        switch (count) {
            case 2:
                return new ZoneSpec[]{
                        new ZoneSpec(488, 166, 705, 164, 0, 770, 96, 160, 44)
                };
            case 3:
                return new ZoneSpec[]{
                        new ZoneSpec(158, 162, 470, 220, -28, 226, 154, 186, 45),
                        new ZoneSpec(1044, 162, 470, 220, 28, 1342, 154, 188, 45)
                };
            case 4:
                return new ZoneSpec[]{
                        new ZoneSpec(126, 198, 410, 224, -28, 226, 192, 150, 44),
                        new ZoneSpec(512, 158, 640, 150, 0, 806, 96, 150, 44),
                        new ZoneSpec(1136, 198, 410, 224, 28, 1420, 190, 152, 44)
                };
            default:
                return new ZoneSpec[]{
                        new ZoneSpec(110, 194, 380, 212, -28, 258, 168, 148, 44),
                        new ZoneSpec(485, 174, 360, 122, 0, 650, 120, 138, 44),
                        new ZoneSpec(865, 174, 360, 122, 0, 1090, 120, 142, 44),
                        new ZoneSpec(1182, 194, 380, 212, 28, 1454, 188, 132, 44)
                };
        }
    }

    private ZoneSpec ownTableSpec(int count) {
        switch (count) {
            case 2:
                return area(410, 590, 852, 98, 0);
            case 3:
                return area(421, 568, 828, 96, 0);
            case 4:
                return area(389, 572, 893, 96, 0);
            default:
                return area(478, 560, 730, 98, 0);
        }
    }

    private ZoneSpec handSpec(int count) {
        switch (count) {
            case 2:
                return area(145, 748, 1385, 176, 0);
            case 3:
                return area(260, 718, 1155, 178, 0);
            case 4:
                return area(252, 733, 1203, 180, 0);
            default:
                return area(255, 758, 1175, 160, 0);
        }
    }

    private ZoneSpec ownNameSpec(int count) {
        switch (count) {
            case 4:
                return area(260, 686, 150, 48, 0);
            case 5:
                return area(324, 660, 150, 48, 0);
            default:
                return area(264, 630, 150, 48, 0);
        }
    }

    private ZoneSpec area(double x, double y, double width, double height, double rotate) {
        return new ZoneSpec(x, y, width, height, rotate, 0, 0, 0, 0);
    }

    private void configurePane(Pane pane, double x, double y, double width, double height, double rotate) {
        pane.setLayoutX(x);
        pane.setLayoutY(y);
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        pane.setRotate(rotate);
        pane.setClip(new Rectangle(width, height));
        pane.setPickOnBounds(false);
    }

    private void configureNameLabel(Label label, double x, double y, double width, double height) {
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefSize(width, height);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#f8e7b4"));
        label.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 17));
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 5, 0.35, 0, 1);");
    }

    private void configureStatsLabel(Label label, double x, double y, double width, double height) {
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefSize(width, height);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#f8e7b4"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
    }

    private void configureSetsProgress(ProgressBar progress, double x, double y, double width, double height) {
        progress.setLayoutX(x);
        progress.setLayoutY(y);
        progress.setPrefSize(width, height);
        progress.setMinSize(width, height);
        progress.setMaxSize(width, height);
        progress.setStyle("-fx-accent: #ffd66b; -fx-control-inner-background: rgba(0,0,0,0.38);"
                + "-fx-background-insets: 0; -fx-padding: 0;");
    }

    private void configureSetsLabel(Label label, double x, double y, double width, double height) {
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefSize(width, height);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#ffe7a6"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        label.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 4, 0.2, 0, 1);");
    }

    private Image loadResourceImage(String path) {
        URL resource = getClass().getResource(path);
        if (resource != null) {
            return new Image(resource.toExternalForm(), 0, 0, true, true);
        }
        Path filePath = Paths.get(System.getProperty("user.dir"), "src", path.replaceFirst("^/", ""));
        return Files.isRegularFile(filePath) ? new Image(filePath.toUri().toString(), 0, 0, true, true) : null;
    }

    private Button imageButton(String fileName, String fallbackText, double width, double height) {
        Button button = new Button(fallbackText);
        Image image = loadResourceImage("/assets/ui/buttons/" + fileName);
        if (image != null && !image.isError()) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            button.setText("");
            button.setGraphic(imageView);
        } else {
            styleButton(button);
        }
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;"
                + "-fx-border-color: transparent; -fx-padding: 0; -fx-cursor: hand;"
                + "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        button.setOnMouseEntered(event -> {
            button.setScaleX(1.04);
            button.setScaleY(1.04);
            button.setOpacity(0.96);
        });
        button.setOnMouseExited(event -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setOpacity(1.0);
        });
        return button;
    }

    private static class ZoneSpec {
        final double x;
        final double y;
        final double width;
        final double height;
        final double rotate;
        final double nameX;
        final double nameY;
        final double nameWidth;
        final double nameHeight;

        ZoneSpec(double x, double y, double width, double height, double rotate,
                 double nameX, double nameY, double nameWidth, double nameHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rotate = rotate;
            this.nameX = nameX;
            this.nameY = nameY;
            this.nameWidth = nameWidth;
            this.nameHeight = nameHeight;
        }
    }

    private static class PlayerZone {
        final ZoneSpec spec;
        final Pane cards = new Pane();
        final Label name = new Label();
        final Label stats = new Label();
        final ProgressBar setsProgress = new ProgressBar(0);
        final Label setsLabel = new Label();

        PlayerZone(ZoneSpec spec) {
            this.spec = spec;
        }
    }

    private VBox createCenter() {
        endTurnButton.setOnAction(event -> {
            requestEndTurn();
        });
        styleButton(endTurnButton);

        Button newGame = new Button("New Game");
        newGame.setOnAction(event -> newGameAction.run());
        styleButton(newGame);

        Button exitGame = new Button("Exit Game");
        exitGame.setOnAction(event -> exitGameAction.run());
        styleButton(exitGame);

        gameOverActions.getChildren().setAll(exitGame, newGame);
        gameOverActions.setAlignment(Pos.CENTER);
        gameOverActions.setVisible(false);
        gameOverActions.setManaged(false);

        HBox piles = new HBox(18,
                drawPileView,
                discardPileView);
        piles.setAlignment(Pos.CENTER);

        VBox center = new VBox(18, title("Table"), turnStatus, deckLabel, discardLabel,
                piles, endTurnButton, gameOverActions);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(18));
        center.setStyle("-fx-background-color: #0f1722;"
                + "-fx-border-color: #2a3040; -fx-border-width: 0 1 0 1;");
        turnStatus.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        turnStatus.setStyle("-fx-padding: 2 0 2 0;");
        styleInfoLabel(deckLabel, "#cfe7dd");
        styleInfoLabel(discardLabel, "#ead8b3");
        return center;
    }

    /**
     * 浼樺寲鐐?锛氬交搴曟敼閫犲簳閮ㄥ竷灞€銆?
     * 灏嗗師鏈瀭鐩村爢鍙犵殑 Bank 鍜?Properties 鎷嗚В寮€锛屼笌 Hand 骞舵帓褰㈡垚妯悜涓夊垪锛堝乏銆佷腑銆佸彸锛夈€?
     * 楂樺害澶уぇ缂╁噺锛屽畬缇庡绾充竴鏁存帓鍗＄墝锛屼笉闇€瑕佷换浣曞瀭鐩存粴鍔ㄣ€?
     */
    private ScrollPane createPlayerPanel() {
        Label hand = sectionLabel("Hand", "#ffd7a8");
        Label bank = sectionLabel("Bank", "#ccecc3");
        Label property = sectionLabel("Properties", "#cbd8ff");

        // 灏嗗畠浠媶鍒嗕负鐙珛鐨勪笁鍒?
        VBox handColumn = new VBox(8, hand, handView);
        VBox bankColumn = new VBox(8, bank, bankView);
        VBox propertyColumn = new VBox(8, property, propertyView);

        // 妯悜骞舵帓缁勫悎
        HBox content = new HBox(12, handColumn, bankColumn, propertyColumn);
        HBox.setHgrow(handColumn, Priority.ALWAYS);
        HBox.setHgrow(bankColumn, Priority.ALWAYS);
        HBox.setHgrow(propertyColumn, Priority.ALWAYS);

        // 閰嶅悎 100x140 鐨勬柊鐗堝崱鐗岃瀹氬悎鐞嗙殑鏈€灏忓搴?
        handColumn.setMinWidth(360);
        bankColumn.setMinWidth(220);
        propertyColumn.setMinWidth(360);

        VBox panel = new VBox(content);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: linear-gradient(to bottom, #0d1117, #090d14); "
                + "-fx-border-color: #1e2a3a; -fx-border-width: 1 0 0 0;");
        panel.setFillWidth(true);

        handView.setStyle("-fx-background-color: rgba(255,215,168,0.05);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        bankView.setStyle("-fx-background-color: rgba(204,236,195,0.05);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        propertyView.setStyle("-fx-background-color: rgba(203,216,255,0.05);"
                + "-fx-background-radius: 10; -fx-padding: 10;");

        VBox.setVgrow(handView, Priority.ALWAYS);
        VBox.setVgrow(bankView, Priority.ALWAYS);
        VBox.setVgrow(propertyView, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 鑷€傚簲楂樺害璋冩暣锛?40px 鍒氬ソ瀹岀編涓嶇暀鐧藉绾充竴鎺?140px 楂樺害鐨勫崱鐗屼笌鍒嗙被鏍囬
        scroll.setPrefViewportHeight(240);
        scroll.setMaxHeight(260);
        scroll.setStyle("-fx-background-color: #17262d; -fx-background: #17262d;"
                + "-fx-border-color: #314a55; -fx-border-width: 1 0 0 0;");
        return scroll;
    }

    /**
     * 浼樺寲鐐?锛氫慨澶峀og鏃ュ織妗嗘枃瀛楅鑹插拰鑳屾櫙銆?
     * 绉婚櫎鍘熷厛纭紪鐮佺殑榛勮壊鑳屾櫙锛屼娇鍏惰窡 style.css 鐨勬殫榛戠鎶€椋庢牸铻嶄负涓€浣撱€?
     */
    private VBox createLogPanel() {
        Label logTitle = sectionLabel("Log", "#f7d0d7");
        VBox panel = new VBox(8, logTitle, logView);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: #070a10; "
                + "-fx-border-color: rgba(0,242,255,0.08); -fx-border-width: 0 0 0 1;");

        // 灏嗗唴閮ㄨ儗鏅敼鎴愪笌璧涘崥鏈嬪厠 css 缁熶竴鐨勬繁鑹诧紙#16181b锛夛紝杩欐牱 css 閲岀殑鐏拌壊鍜岀豢鑹查珮浜枃鏈氨鑳藉畬缇庣湅娓呬簡锛?
        logView.setStyle("-fx-control-inner-background: #16181b; "
                + "-fx-background-color: #16181b; -fx-background-radius: 8;"
                + "-fx-border-color: #3b424a; -fx-border-radius: 8;"
                + "-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        logView.setCellFactory(list -> {
            javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setWrapText(true);
                setStyle("-fx-text-fill: #f8fbf6; -fx-background-color: #16181b;"
                        + "-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
            }
            };
            cell.prefWidthProperty().bind(list.widthProperty().subtract(24));
            return cell;
        });
        VBox.setVgrow(logView, Priority.ALWAYS);
        return panel;
    }

    private boolean winPopupShown = false;

    private void renderAll() {
        Player current = game.getCurrentPlayer();
        renderTurnStatus(current);
        renderPiles();

        boolean gameOver = game.isGameOver();
        endTurnButton.setDisable(gameOver || !canControlCurrentPlayer());
        gameOverActions.setVisible(gameOver);
        gameOverActions.setManaged(gameOver);

        if (gameOver && !winPopupShown) {
            winPopupShown = true;
            GameDialogs.showMessage("Game Over",
                    current.getPlayerName() + " Wins!",
                    "Collected 3 complete property sets!");
        }

        Player viewPlayer = getViewPlayer();
        renderOpponents(viewPlayer);
        renderHand(viewPlayer);
        renderOwnTable(viewPlayer);
        renderOwnInfo(viewPlayer);
    }

    private void renderOpponents(Player current) {
        List<Player> visibleOpponents = new ArrayList<>();
        for (Player player : game.getActivePlayers()) {
            if (player == current) continue;
            visibleOpponents.add(player);
        }
        for (int i = 0; i < opponentZones.size(); i++) {
            PlayerZone zone = opponentZones.get(i);
            if (i >= visibleOpponents.size()) {
                zone.cards.getChildren().clear();
                zone.name.setText("");
                zone.stats.setText("");
                zone.setsProgress.setProgress(0);
                zone.setsLabel.setText("");
                continue;
            }
            Player player = visibleOpponents.get(i);
            zone.name.setText(player.getPlayerName());
            zone.stats.setText(playerStats(player));
            updateSetsProgress(zone.setsProgress, zone.setsLabel, player);
            renderTableCards(zone.cards, player, OPPONENT_CARD_WIDTH, OPPONENT_CARD_HEIGHT, false);
        }
    }

    private void renderHand(Player current) {
        handView.getChildren().clear();
        List<Card> cards = current.getHand().getCards();
        double step = computeCardStep(cards.size(), handView.getPrefWidth(), HAND_CARD_WIDTH, 12, false);
        double rowWidth = cards.isEmpty() ? 0 : HAND_CARD_WIDTH + step * (cards.size() - 1);
        double startX = Math.max(10, (handView.getPrefWidth() - rowWidth) / 2.0);
        double baseY = Math.max(8, (handView.getPrefHeight() - HAND_CARD_HEIGHT) / 2.0);
        for (int i = 0; i < cards.size(); i++) {
            int index = i;
            CardView cardView = new CardView(cards.get(i), HAND_CARD_WIDTH, HAND_CARD_HEIGHT);
            cardView.setLayoutX(startX + i * step);
            cardView.setLayoutY(baseY);
            if (!game.isGameOver() && canControlCurrentPlayer() && current == game.getCurrentPlayer()) {
                cardView.setOnMouseClicked(event -> {
                    cardView.toFront();
                    showCardMenu(cardView, index, cards.get(index));
                });
            }
            handView.getChildren().add(cardView);
        }
    }

    private void renderOwnTable(Player current) {
        renderTableCards(ownTableView, current, OWN_TABLE_CARD_WIDTH, OWN_TABLE_CARD_HEIGHT, true);
    }

    private void renderOwnInfo(Player current) {
        ownNameLabel.setText(current.getPlayerName());
        ownStatsLabel.setText(playerStats(current));
        updateSetsProgress(ownSetsProgress, ownSetsLabel, current);
    }

    private void renderPiles() {
        centerPileView.getChildren().clear();

        CardView drawPile = CardView.back(game.getGameDeck().getDrawPileSize(), PILE_CARD_WIDTH, PILE_CARD_HEIGHT);
        drawPile.setLayoutX(34);
        drawPile.setLayoutY(28);

        Card discardTop = game.getGameDeck().peekDiscardTop();
        CardView discardPile = discardTop == null
                ? new CardView("DISCARD", "EMPTY", PILE_CARD_WIDTH, PILE_CARD_HEIGHT)
                : new CardView(discardTop, PILE_CARD_WIDTH, PILE_CARD_HEIGHT);
        discardPile.setLayoutX(478);
        discardPile.setLayoutY(28);

        deckLabel.setText("Draw: " + game.getGameDeck().getDrawPileSize());
        deckLabel.setLayoutX(22);
        deckLabel.setLayoutY(148);
        deckLabel.setPrefWidth(95);
        stylePileLabel(deckLabel);

        discardLabel.setText(discardTop == null ? "Discard" : "Discard: " + discardTop.getCardName());
        discardLabel.setLayoutX(426);
        discardLabel.setLayoutY(148);
        discardLabel.setPrefWidth(150);
        stylePileLabel(discardLabel);

        centerPileView.getChildren().addAll(drawPile, discardPile, deckLabel, discardLabel);
    }

    private void renderTableCards(Pane target, Player player, double cardWidth, double cardHeight,
                                  boolean currentPlayerArea) {
        target.getChildren().clear();
        List<Card> cards = getTableCards(player);
        if (cards.isEmpty()) {
            Label empty = new Label(currentPlayerArea ? "No table cards" : "No cards on table");
            empty.setTextFill(javafx.scene.paint.Color.web("rgba(255,255,255,0.70)"));
            empty.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            empty.setLayoutX(14);
            empty.setLayoutY(12);
            target.getChildren().add(empty);
            return;
        }

        int rows = cards.size() > 7 && target.getPrefHeight() >= cardHeight * 1.65 ? 2 : 1;
        int perRow = (int) Math.ceil(cards.size() / (double) rows);
        double step = computeCardStep(perRow, target.getPrefWidth(), cardWidth, 8, true);
        double firstRowWidth = perRow <= 0 ? 0 : cardWidth + step * (Math.min(perRow, cards.size()) - 1);
        double startX = Math.max(8, (target.getPrefWidth() - firstRowWidth) / 2.0);
        double rowGap = rows == 1 ? 0 : Math.min(cardHeight * 0.62,
                (target.getPrefHeight() - cardHeight) / (rows - 1));
        double usedHeight = cardHeight + rowGap * (rows - 1);
        double startY = Math.max(0, (target.getPrefHeight() - usedHeight) / 2.0);

        for (int i = 0; i < cards.size(); i++) {
            int row = i / perRow;
            int column = i % perRow;
            int rowCount = Math.min(perRow, cards.size() - row * perRow);
            double rowWidth = cardWidth + step * Math.max(0, rowCount - 1);
            double rowStartX = Math.max(8, (target.getPrefWidth() - rowWidth) / 2.0);
            Card tableCard = cards.get(i);
            CardView cardView = new CardView(tableCard, cardWidth, cardHeight);
            cardView.setLayoutX(rowStartX + column * step);
            cardView.setLayoutY(startY + row * rowGap);
            target.getChildren().add(cardView);
        }
    }

    private double computeCardStep(int count, double zoneWidth, double cardWidth, double gap,
                                   boolean allowOverlap) {
        if (count <= 1) {
            return 0;
        }
        double natural = cardWidth + gap;
        double maxStep = (zoneWidth - cardWidth - 16) / (count - 1);
        double noOverlapMinimum = allowOverlap ? cardWidth * 0.35 : cardWidth;
        if (maxStep >= noOverlapMinimum) {
            return Math.min(natural, maxStep);
        }
        return Math.max(6, maxStep);
    }

    private List<Card> getTableCards(Player player) {
        List<Card> cards = new ArrayList<>();
        cards.addAll(player.getBankArea().getAssets());
        cards.addAll(player.getPropertyArea().getAllPropertyCards());
        return cards;
    }

    private Player getViewPlayer() {
        if (modeConfig.isNetwork()) {
            List<Player> players = game.getActivePlayers();
            int index = Math.max(0, Math.min(modeConfig.localPlayerIndex, players.size() - 1));
            return players.get(index);
        }
        return game.getCurrentPlayer();
    }

    private boolean isLocalPlayer(Player player) {
        if (!modeConfig.isNetwork()) {
            return true;
        }
        List<Player> players = game.getActivePlayers();
        int index = players.indexOf(player);
        return index == modeConfig.localPlayerIndex;
    }

    private boolean canControlCurrentPlayer() {
        if (game.isGameOver() || game.getCurrentState() != GameManager.GameState.NORMAL_TURN) {
            return false;
        }
        Player current = game.getCurrentPlayer();
        if (current.isAI()) {
            return false;
        }
        return isLocalPlayer(current);
    }

    private void requestEndTurn() {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        performEndTurn(true);
    }

    private void performEndTurn(boolean broadcast) {
        game.endTurn();
        if (broadcast) {
            sendNetworkAction("END_TURN", "");
        }
        renderAll();
    }

    private void requestDeposit(int cardIndex) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        Card card = game.getCurrentPlayer().getHand().getCard(cardIndex);
        String payload = "card=" + safe(card == null ? "" : card.getCardName());
        game.depositCardToBank(cardIndex);
        sendNetworkAction("BANK", payload);
        renderAll();
    }

    private void requestDiscard(int cardIndex) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        Card card = game.getCurrentPlayer().getHand().getCard(cardIndex);
        String payload = "card=" + safe(card == null ? "" : card.getCardName());
        game.discardCard(cardIndex);
        sendNetworkAction("DISCARD", payload);
        renderAll();
    }

    private void requestPlayCard(int cardIndex, TargetInfo targetInfo) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        Card card = game.getCurrentPlayer().getHand().getCard(cardIndex);
        String payload = encodePlayPayload(card, targetInfo);
        game.executePlayerAction(cardIndex, targetInfo);
        sendNetworkAction("PLAY", payload);
        renderAll();
    }

    private void requestDoubleRent(int doubleCardIndex, int rentCardIndex, TargetInfo targetInfo) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        Card doubleCard = doubleCardIndex >= 0 && doubleCardIndex < hand.size() ? hand.get(doubleCardIndex) : null;
        Card rentCard = rentCardIndex >= 0 && rentCardIndex < hand.size() ? hand.get(rentCardIndex) : null;
        String payload = encodeDoubleRentPayload(doubleCard, rentCard, targetInfo);
        game.executeDoubleRentAction(doubleCardIndex, rentCardIndex, targetInfo);
        sendNetworkAction("DOUBLE_RENT", payload);
        renderAll();
    }

    private void sendNetworkAction(String type, String payload) {
        if (applyingRemoteAction || modeConfig.networkBridge == null) {
            return;
        }
        modeConfig.networkBridge.sendGameAction(type, payload);
    }

    private String encodePlayPayload(Card card, TargetInfo targetInfo) {
        List<String> parts = new ArrayList<>();
        parts.add("card=" + safe(card == null ? "" : card.getCardName()));
        appendCardState(parts, card);
        appendTarget(parts, targetInfo);
        return String.join(";", parts);
    }

    private String encodeDoubleRentPayload(Card doubleCard, Card rentCard, TargetInfo targetInfo) {
        List<String> parts = new ArrayList<>();
        parts.add("double=" + safe(doubleCard == null ? "" : doubleCard.getCardName()));
        parts.add("rent=" + safe(rentCard == null ? "" : rentCard.getCardName()));
        appendCardState(parts, rentCard);
        appendTarget(parts, targetInfo);
        return String.join(";", parts);
    }

    private void appendCardState(List<String> parts, Card card) {
        if (card instanceof cards.RentCard) {
            parts.add("color=" + ((cards.RentCard) card).getSelectedColor());
        } else if (card instanceof cards.WildRentCard) {
            parts.add("color=" + ((cards.WildRentCard) card).getSelectedColor());
        } else if (card instanceof cards.PropertyCard) {
            parts.add("color=" + ((cards.PropertyCard) card).getColorGroup());
        }
    }

    private void appendTarget(List<String> parts, TargetInfo targetInfo) {
        if (targetInfo == null) {
            return;
        }
        if (targetInfo.getTargetPlayer() != null) {
            parts.add("target=" + game.getActivePlayers().indexOf(targetInfo.getTargetPlayer()));
        }
        if (targetInfo.getInitiatorPropertyColor() != null) {
            parts.add("giveColor=" + targetInfo.getInitiatorPropertyColor());
            parts.add("giveIndex=" + targetInfo.getInitiatorPropertyIndex());
        }
        if (targetInfo.getTargetPropertyColor() != null) {
            parts.add("takeColor=" + targetInfo.getTargetPropertyColor());
            parts.add("takeIndex=" + targetInfo.getTargetPropertyIndex());
        }
        if (targetInfo.getImprovementColor() != null) {
            parts.add("improveColor=" + targetInfo.getImprovementColor());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace(";", "\\s")
                .replace("=", "\\e");
    }

    private Map<String, String> parsePayload(String payload) {
        Map<String, String> values = new java.util.HashMap<>();
        if (payload == null || payload.isEmpty()) {
            return values;
        }
        for (String part : payload.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            values.put(part.substring(0, eq), unsafe(part.substring(eq + 1)));
        }
        return values;
    }

    private String unsafe(String value) {
        return value == null ? "" : value.replace("\\e", "=")
                .replace("\\s", ";")
                .replace("\\\\", "\\");
    }

    private int readInt(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private enums.PropertyColor readColor(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return null;
        }
        try {
            return enums.PropertyColor.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void handleNetworkMessage(LanGameMessage message) {
        if (!modeConfig.isNetwork() || message == null || modeConfig.networkBridge == null) {
            return;
        }
        if (message.getSenderId() == modeConfig.networkBridge.getLocalPlayerId()) {
            return;
        }
        Platform.runLater(() -> applyRemoteAction(message));
    }

    private void applyRemoteAction(LanGameMessage message) {
        applyingRemoteAction = true;
        try {
            String type = message.getType();
            Map<String, String> payload = parsePayload(message.getPayload());
            if ("END_TURN".equals(type)) {
                performEndTurn(false);
            } else if ("BANK".equals(type)) {
                int index = findCardIndexByName(game.getCurrentPlayer(), payload.getOrDefault("card", ""));
                game.depositCardToBank(index);
                renderAll();
            } else if ("DISCARD".equals(type)) {
                int index = findCardIndexByName(game.getCurrentPlayer(), payload.getOrDefault("card", ""));
                game.discardCard(index);
                renderAll();
            } else if ("PLAY".equals(type)) {
                applyRemotePlay(payload);
            } else if ("DOUBLE_RENT".equals(type)) {
                applyRemoteDoubleRent(payload);
            }
        } catch (Exception ex) {
            onGameEvent("[Network] Could not apply remote action: " + ex.getMessage());
        } finally {
            applyingRemoteAction = false;
        }
    }

    private void applyRemotePlay(Map<String, String> payload) {
        Player current = game.getCurrentPlayer();
        int cardIndex = findCardIndexByName(current, payload.getOrDefault("card", ""));
        Card card = current.getHand().getCard(cardIndex);
        if (card == null) {
            onGameEvent("[Network] Remote card not found: " + payload.getOrDefault("card", ""));
            return;
        }
        applyRemoteCardState(card, payload);
        game.executePlayerAction(cardIndex, buildTargetInfo(payload));
        renderAll();
    }

    private void applyRemoteDoubleRent(Map<String, String> payload) {
        Player current = game.getCurrentPlayer();
        int doubleIndex = findCardIndexByName(current, payload.getOrDefault("double", ""));
        int rentIndex = findCardIndexByName(current, payload.getOrDefault("rent", ""));
        Card rent = current.getHand().getCard(rentIndex);
        applyRemoteCardState(rent, payload);
        game.executeDoubleRentAction(doubleIndex, rentIndex, buildTargetInfo(payload));
        renderAll();
    }

    private void applyRemoteCardState(Card card, Map<String, String> payload) {
        enums.PropertyColor color = readColor(payload, "color");
        if (card == null || color == null) {
            return;
        }
        if (card instanceof cards.RentCard) {
            ((cards.RentCard) card).setSelectedColor(color);
        } else if (card instanceof cards.WildRentCard) {
            ((cards.WildRentCard) card).setSelectedColor(color);
        } else if (card instanceof cards.SuperWildCard) {
            ((cards.SuperWildCard) card).setCurrentColor(color);
        } else if (card instanceof cards.PropertyWildCard) {
            ((cards.PropertyWildCard) card).setCurrentColor(color);
        }
    }

    private TargetInfo buildTargetInfo(Map<String, String> payload) {
        Player target = null;
        int targetIndex = readInt(payload, "target", -1);
        if (targetIndex >= 0 && targetIndex < game.getActivePlayers().size()) {
            target = game.getActivePlayers().get(targetIndex);
        }
        enums.PropertyColor giveColor = readColor(payload, "giveColor");
        enums.PropertyColor takeColor = readColor(payload, "takeColor");
        enums.PropertyColor improveColor = readColor(payload, "improveColor");
        if (improveColor != null) {
            return TargetInfo.forImprovement(improveColor).withTarget(target);
        }
        if (giveColor != null || takeColor != null) {
            int giveIndex = readInt(payload, "giveIndex", -1);
            int takeIndex = readInt(payload, "takeIndex", -1);
            if (giveColor != null) {
                return new TargetInfo(target, giveColor, giveIndex, takeColor, takeIndex);
            }
            return new TargetInfo(target, takeColor, takeIndex);
        }
        return target == null ? null : new TargetInfo(target);
    }

    private int findCardIndexByName(Player player, String cardName) {
        if (player == null || cardName == null) {
            return -1;
        }
        List<Card> hand = player.getHand().getCards();
        for (int i = 0; i < hand.size(); i++) {
            if (cardName.equals(hand.get(i).getCardName())) {
                return i;
            }
        }
        return -1;
    }

    private String playerStats(Player player) {
        return String.format("Bank %dM Hand %d",
                player.getBankArea().calculateTotalFunds(),
                player.getHand().getSize());
    }

    private void updateSetsProgress(ProgressBar progress, Label label, Player player) {
        int completed = player.getPropertyArea().countCompletedSets();
        progress.setProgress(calculateSetProgress(player));
        label.setText(completed + "/3");
    }

    private double calculateSetProgress(Player player) {
        Map<enums.PropertyColor, Double> bestProgressByColor = new EnumMap<>(enums.PropertyColor.class);
        for (player.PropertyArea.PropertySetEntry entry : player.getPropertyArea().getPropertySetEntries()) {
            player.PropertySet root = getRootSet(entry.getRentable());
            if (root == null) {
                continue;
            }
            double setProgress = Math.min(1.0,
                    root.getCardsCount() / (double) entry.getColor().getRequiredCount());
            bestProgressByColor.merge(entry.getColor(), setProgress, Math::max);
        }
        double progress = bestProgressByColor.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum() / 3.0;
        return Math.min(1.0, progress);
    }

    private void stylePileLabel(Label label) {
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#f8e7b4"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        label.setStyle("-fx-background-color: rgba(0,0,0,0.52); -fx-background-radius: 8;"
                + "-fx-padding: 3 6 3 6;");
    }

    private void showDeckSummary() {
        Card discardTop = game.getGameDeck().peekDiscardTop();
        GameDialogs.showMessage("Deck",
                "Draw and discard",
                "Draw pile: " + game.getGameDeck().getDrawPileSize() + "\n"
                        + "Discard pile: " + game.getGameDeck().getDiscardPileSize() + "\n"
                        + "Discard top: " + (discardTop == null ? "Empty" : discardTop.getCardName()));
    }

    private void showBankSummary() {
        Player current = game.getCurrentPlayer();
        StringBuilder detail = new StringBuilder();
        detail.append("Total: ").append(current.getBankArea().calculateTotalFunds()).append("M\n");
        if (current.getBankArea().getAssets().isEmpty()) {
            detail.append("No bank cards yet.");
        } else {
            for (Card card : current.getBankArea().getAssets()) {
                detail.append(card.getCardName()).append(" (").append(card.getMonetaryValue()).append("M)\n");
            }
        }
        GameDialogs.showMessage("Bank", current.getPlayerName(), detail.toString());
    }

    private void showPropertySummary() {
        Player current = game.getCurrentPlayer();
        StringBuilder detail = new StringBuilder();
        detail.append("Complete sets: ").append(current.getPropertyArea().countCompletedSets()).append("/3\n");
        List<player.PropertyArea.PropertySetEntry> entries = current.getPropertyArea().getPropertySetEntries();
        if (entries.isEmpty()) {
            detail.append("No properties on table.");
        } else {
            for (player.PropertyArea.PropertySetEntry entry : entries) {
                player.PropertySet root = getRootSet(entry.getRentable());
                int count = root == null ? 0 : root.getCardsCount();
                detail.append(entry.getColor())
                        .append(": ").append(count).append("/")
                        .append(entry.getColor().getRequiredCount())
                        .append(" | Rent ").append(entry.getRentable().calculateRent()).append("M\n");
            }
        }
        GameDialogs.showMessage("Properties", current.getPlayerName(), detail.toString());
    }

    private void showActionSummary() {
        Player current = game.getCurrentPlayer();
        StringBuilder detail = new StringBuilder();
        int actionCount = 0;
        for (Card card : current.getHand().getCards()) {
            if (!(card instanceof cards.PropertyCard) && !(card instanceof cards.MoneyCard)) {
                actionCount++;
                detail.append(card.getCardName()).append(" (").append(card.getMonetaryValue()).append("M)\n");
            }
        }
        if (actionCount == 0) {
            detail.append("No action cards in hand.");
        }
        GameDialogs.showMessage("Actions", current.getPlayerName(), detail.toString());
    }

    private void showOpponentsSummary() {
        Player current = game.getCurrentPlayer();
        StringBuilder detail = new StringBuilder();
        for (Player player : game.getOpponents(current)) {
            detail.append(player.getPlayerName())
                    .append(": ")
                    .append(player.getPropertyArea().countCompletedSets()).append("/3 sets, ")
                    .append(player.getBankArea().calculateTotalFunds()).append("M bank, ")
                    .append(player.getHand().getSize()).append(" cards in hand\n");
        }
        GameDialogs.showMessage("Opponents", "Table status", detail.toString());
    }

    private player.PropertySet getRootSet(player.Rentable rentable) {
        if (rentable instanceof player.SetDecorator) {
            return ((player.SetDecorator) rentable).getRootSet();
        }
        if (rentable instanceof player.PropertySet) {
            return (player.PropertySet) rentable;
        }
        return null;
    }

    private void showCardMenu(CardView owner, int cardIndex, Card card) {
        ContextMenu menu = new ContextMenu();

        MenuItem bank = new MenuItem("Deposit to bank");
        bank.setOnAction(event -> {
            requestDeposit(cardIndex);
        });

        MenuItem discard = new MenuItem("Discard");
        discard.setDisable(!game.getCurrentPlayer().getHand().requiresDiscard());
        discard.setOnAction(event -> {
            requestDiscard(cardIndex);
        });

        MenuItem play = new MenuItem("Play card");
        play.setOnAction(event -> {
            if (card instanceof cards.DoubleTheRentCard) {
                playDoubleRent(cardIndex);
                return;
            }

            TargetInfo targetInfo = null;
            if (card instanceof cards.HouseCard) {
                targetInfo = chooseImprovementTarget(game.getCurrentPlayer().getPropertyArea().getHouseEligibleColors());
                if (targetInfo == null) {
                    onGameEvent("No eligible complete set for House.");
                    return;
                }
            } else if (card instanceof cards.HotelCard) {
                targetInfo = chooseImprovementTarget(game.getCurrentPlayer().getPropertyArea().getHotelEligibleColors());
                if (targetInfo == null) {
                    onGameEvent("No eligible complete set for Hotel.");
                    return;
                }
            }

            if (card instanceof cards.RentCard && ((cards.RentCard) card).isMultiColor()) {
                cards.RentCard rentCard = (cards.RentCard) card;
                enums.PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
                if (selectedColor == null) {
                    onGameEvent("Cancelled " + card.getCardName());
                    return;
                }
                rentCard.setSelectedColor(selectedColor);
            } else if (card instanceof cards.WildRentCard) {
                cards.WildRentCard wrCard = (cards.WildRentCard) card;
                enums.PropertyColor selectedColor = chooseColor(wrCard.getAvailableColors());
                if (selectedColor == null) {
                    onGameEvent("Cancelled " + card.getCardName());
                    return;
                }
                wrCard.setSelectedColor(selectedColor);
            } else if (card instanceof cards.SuperWildCard || card instanceof cards.PropertyWildCard) {
                enums.PropertyColor[] options;
                if (card instanceof cards.SuperWildCard) {
                    options = ((cards.SuperWildCard) card).getAvailableColors();
                } else {
                    options = ((cards.PropertyWildCard) card).getAvailableColors();
                }

                enums.PropertyColor selectedColor = chooseColor(options);

                if (selectedColor == null) {
                    onGameEvent("Cancelled " + card.getCardName());
                    return;
                }

                if (card instanceof cards.SuperWildCard) {
                    ((cards.SuperWildCard) card).setCurrentColor(selectedColor);
                } else {
                    ((cards.PropertyWildCard) card).setCurrentColor(selectedColor);
                }
            }
            if (needsTarget(card)) {
                targetInfo = chooseTarget(card);
                if (targetInfo == null) {
                    onGameEvent("Cancelled " + card.getCardName());
                    return;
                }
            }
            requestPlayCard(cardIndex, targetInfo);
        });

        if (card instanceof cards.MoneyCard) {
            menu.getItems().addAll(bank, discard);
        } else {
            menu.getItems().addAll(bank, discard, play);
        }
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    private boolean needsTarget(Card card) {
        return card.requiresTarget();
    }

    private enums.PropertyColor chooseColor(enums.PropertyColor[] colorOptions) {
        List<enums.PropertyColor> options = java.util.Arrays.asList(colorOptions);
        return GameDialogs.showChoice("Select Color",
                "Choose the color for this card",
                "Color",
                options,
                options.get(0)).orElse(null);
    }

    private TargetInfo chooseTarget(Card card) {
        java.util.List<Player> choices = new java.util.ArrayList<>(game.getOpponents(game.getCurrentPlayer()));
        if (card instanceof cards.SlyDealCard) {
            choices.removeIf(player -> player.getPropertyArea().getStealableIncompleteColors().isEmpty());
        } else if (card instanceof cards.ForceDealCard) {
            if (game.getCurrentPlayer().getPropertyArea().getPropertyColorsWithCards().isEmpty()) {
                return null;
            }
            choices.removeIf(player -> player.getPropertyArea().getPropertyColorsWithCards().isEmpty());
        } else if (card instanceof cards.DealBreakerCard) {
            choices.removeIf(player -> player.getPropertyArea().countCompletedSets() == 0);
        }
        if (choices.isEmpty()) {
            onGameEvent("No available opponent.");
            return null;
        }
        Optional<Player> selected = GameDialogs.showChoice("Choose Target",
                "Select a player to perform the action on",
                "Target",
                FXCollections.observableArrayList(choices),
                choices.get(0));
        if (!selected.isPresent()) {
            return null;
        }

        Player target = selected.get();
        if (card instanceof cards.SlyDealCard) {
            PropertyPick targetCard = choosePropertyCard(target, target.getPropertyArea().getStealableIncompleteColors(),
                    "Choose property to steal", false);
            if (targetCard == null) {
                return null;
            }
            return new TargetInfo(target, targetCard.color, targetCard.index);
        }
        if (card instanceof cards.ForceDealCard) {
            PropertyPick mine = choosePropertyCard(game.getCurrentPlayer(),
                    game.getCurrentPlayer().getPropertyArea().getPropertyColorsWithCards(),
                    "Choose your property to give", false);
            if (mine == null) {
                return null;
            }
            PropertyPick theirs = choosePropertyCard(target, target.getPropertyArea().getPropertyColorsWithCards(),
                    "Choose target property to receive", false);
            if (theirs == null) {
                return null;
            }
            return new TargetInfo(target, mine.color, mine.index, theirs.color, theirs.index);
        }
        if (card instanceof cards.DealBreakerCard) {
            java.util.List<enums.PropertyColor> completed = target.getPropertyArea().getCompletedColorsList();
            if (completed.size() > 1) {
                enums.PropertyColor chosen = chooseColor(completed.toArray(new enums.PropertyColor[0]));
                if (chosen == null) return null;
                return TargetInfo.forImprovement(chosen).withTarget(target);
            }
            return new TargetInfo(target);
        }
        return new TargetInfo(target);
    }

    private TargetInfo chooseImprovementTarget(List<enums.PropertyColor> colors) {
        if (colors.isEmpty()) {
            return null;
        }
        Optional<enums.PropertyColor> selected = GameDialogs.showChoice("Choose Property Set",
                "Select a set",
                "Set",
                colors,
                colors.get(0));
        return selected.map(TargetInfo::forImprovement).orElse(null);
    }

    private PropertyPick choosePropertyCard(Player owner, List<enums.PropertyColor> colors, String title) {
        return choosePropertyCard(owner, colors, title, true);
    }

    private PropertyPick choosePropertyCard(Player owner, List<enums.PropertyColor> colors, String title,
                                            boolean includeComplete) {
        java.util.List<PropertyPick> picks = new java.util.ArrayList<>();
        for (enums.PropertyColor color : colors) {
            List<cards.PropertyCard> cards = owner.getPropertyArea().getCards(color, includeComplete);
            for (int i = 0; i < cards.size(); i++) {
                picks.add(new PropertyPick(color, i, color + " - " + cards.get(i).getCardName()));
            }
        }
        if (picks.isEmpty()) {
            return null;
        }
        return GameDialogs.showChoice(title,
                owner.getPlayerName(),
                "Property",
                picks,
                picks.get(0)).orElse(null);
    }

    private void playDoubleRent(int doubleCardIndex) {
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        java.util.List<Integer> rentIndexes = new java.util.ArrayList<>();
        java.util.List<String> choices = new java.util.ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (i != doubleCardIndex
                    && (hand.get(i) instanceof cards.RentCard || hand.get(i) instanceof cards.WildRentCard)) {
                rentIndexes.add(i);
                choices.add(i + ": " + hand.get(i).getCardName());
            }
        }
        if (choices.isEmpty()) {
            onGameEvent("Double The Rent must be paired with a rent card.");
            return;
        }
        Optional<String> selected = GameDialogs.showChoice("Double The Rent",
                "Choose a rent card to play with it",
                "Rent",
                choices,
                choices.get(0));
        if (!selected.isPresent()) {
            return;
        }
        int rentCardIndex = rentIndexes.get(choices.indexOf(selected.get()));
        Card rent = hand.get(rentCardIndex);
        if (rent instanceof cards.RentCard && ((cards.RentCard) rent).isMultiColor()) {
            cards.RentCard rentCard = (cards.RentCard) rent;
            enums.PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
            if (selectedColor == null) {
                return;
            }
            rentCard.setSelectedColor(selectedColor);
        } else if (rent instanceof cards.WildRentCard) {
            cards.WildRentCard wildRentCard = (cards.WildRentCard) rent;
            enums.PropertyColor selectedColor = chooseColor(wildRentCard.getAvailableColors());
            if (selectedColor == null) {
                return;
            }
            wildRentCard.setSelectedColor(selectedColor);
        }

        TargetInfo targetInfo = null;
        if (needsTarget(rent)) {
            targetInfo = chooseTarget(rent);
            if (targetInfo == null) {
                onGameEvent("Cancelled " + rent.getCardName());
                return;
            }
        }
        requestDoubleRent(doubleCardIndex, rentCardIndex, targetInfo);
    }

    private List<Card> choosePaymentCardsForPayment(Player payer, Player payee, int amount,
                                                    List<Card> bankCards,
                                                    List<cards.PropertyCard> propertyCards) {
        if (payer.isAI() || modeConfig.isNetwork()) {
            return chooseAutomaticPaymentCards(payer, amount, bankCards, propertyCards);
        }

        java.util.List<Card> options = new java.util.ArrayList<>();
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
        java.util.List<CheckBox> boxes = new java.util.ArrayList<>();

        for (Card card : options) {
            boolean fromBank = bankCards.contains(card);
            CheckBox box = GameDialogs.checkBox((fromBank ? "Bank: " : "Property: ")
                    + card.getCardName() + " (" + card.getMonetaryValue() + "M)");
            boxes.add(box);
            content.getChildren().add(box);
        }
        content.getChildren().add(selectedTotal);
        dialog.getDialogPane().setContent(content);
        ButtonType payButtonType = new ButtonType("Pay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(payButtonType);
        GameDialogs.styleButtons(dialog);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(payButtonType);
        Runnable updateSelection = () -> {
            int selected = 0;
            for (int i = 0; i < boxes.size(); i++) {
                if (boxes.get(i).isSelected()) {
                    selected += options.get(i).getMonetaryValue();
                }
            }
            selectedTotal.setText("Selected: " + selected + "M / Owed: " + amount + "M");
            boolean enough = selected >= amount || (totalAvailable < amount && selected == totalAvailable);
            okButton.setDisable(!enough);
        };
        boxes.forEach(box -> box.selectedProperty().addListener((ignored, oldValue, newValue) -> updateSelection.run()));
        updateSelection.run();

        dialog.setResultConverter(button -> {
            if (button != payButtonType) {
                return Collections.emptyList();
            }
            java.util.List<Card> selected = new java.util.ArrayList<>();
            for (int i = 0; i < boxes.size(); i++) {
                if (boxes.get(i).isSelected()) {
                    selected.add(options.get(i));
                }
            }
            return selected;
        });
        return dialog.showAndWait().orElse(Collections.emptyList());
    }

    private List<Card> chooseAutomaticPaymentCards(Player payer, int amount,
                                                   List<Card> bankCards,
                                                   List<cards.PropertyCard> propertyCards) {
        List<Card> options = new ArrayList<>();
        options.addAll(bankCards);
        options.sort(java.util.Comparator.comparingInt(Card::getMonetaryValue));
        List<Card> selected = selectEnoughCards(options, amount);
        int total = selected.stream().mapToInt(Card::getMonetaryValue).sum();
        if (total >= amount) {
            return selected;
        }

        List<cards.PropertyCard> properties = new ArrayList<>(propertyCards);
        properties.sort(java.util.Comparator.comparingInt(Card::getMonetaryValue));
        selected.addAll(selectEnoughCards(new ArrayList<Card>(properties), amount - total));
        return selected;
    }

    private List<Card> selectEnoughCards(List<Card> candidates, int amount) {
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

    private static class PropertyPick {
        final enums.PropertyColor color;
        final int index;
        final String label;

        PropertyPick(enums.PropertyColor color, int index, String label) {
            this.color = color;
            this.index = index;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private void renderBank(Player current) {
        bankView.getChildren().clear();
        current.getBankArea().getAssets().forEach(card -> bankView.getChildren().add(new CardView(card)));
    }

    private void renderProperties(Player current) {
        propertyView.getChildren().clear();
        PlayerAreaView view = new PlayerAreaView();
        view.render(current, false, game.getActionsRemaining());
        propertyView.getChildren().add(view);
    }

    private void renderTurnStatus(Player current) {
        int completedSets = current.getPropertyArea().countCompletedSets();
        turnStatus.getChildren().setAll(
                statusText("TURN\n", "#f8e7b4", FontWeight.EXTRA_BOLD, 24),
                statusText(current.getPlayerName() + "\n", "#ffffff", FontWeight.EXTRA_BOLD, 26),
                statusText("Actions " + game.getActionsRemaining()
                        + "  |  Sets " + completedSets + "/3", "#bfefff", FontWeight.BOLD, 16));
    }

    private Text statusText(String text, String color, FontWeight weight) {
        return statusText(text, color, weight, 24);
    }

    private Text statusText(String text, String color, FontWeight weight, int size) {
        Text node = new Text(text);
        node.setFill(javafx.scene.paint.Color.web(color));
        node.setFont(Font.font("Segoe UI", weight, size));
        return node;
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web("#f7efe1"));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        return label;
    }

    private Label sectionLabel(String text, String color) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web(color));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        label.setStyle("-fx-padding: 2 0 2 2;");
        return label;
    }

    private void styleInfoLabel(Label label, String color) {
        label.setTextFill(javafx.scene.paint.Color.web(color));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
    }

    private void styleButton(Button button) {
        button.setTextFill(javafx.scene.paint.Color.web("#1b2a31"));
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        button.setStyle("-fx-background-color: #f0c978; -fx-background-radius: 8;"
                + "-fx-border-color: #ffe0a1; -fx-border-radius: 8;"
                + "-fx-padding: 10 24 10 24;");
    }

    @Override
    public void onGameEvent(String message) {
        Platform.runLater(() -> {
            logView.getItems().add(0, message);
            renderAll();

            if (message.contains("[INTERRUPT_REQUEST]")) {
                handleInterruptRequest();
            }
        });
    }

    private void handleInterruptRequest() {
        if (game.getCurrentState() != GameManager.GameState.WAITING_FOR_COUNTER_ACTION) return;

        Player victim = game.getPendingVictim();
        if (victim == null) return;

        if (victim.isAI() || modeConfig.isNetwork()) {
            if (victim.isAI()) {
                aiExecutor.handleInterrupt(victim);
            } else {
                game.resolvePendingAction();
            }
            renderAll();
            return;
        }

        List<Card> hand = victim.getHand().getCards();
        int jsnIdx = -1;
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardName().equals("Just Say No")) {
                jsnIdx = i;
                break;
            }
        }
        final int jsnIndex = jsnIdx;

        if (jsnIndex >= 0) {
            boolean counter = GameDialogs.showConfirmation("Counter Action",
                    victim.getPlayerName() + " is under attack!",
                    "Use Just Say No to counter?");
            if (counter) {
                game.counterAttackWithJustSayNo(jsnIndex);
            } else {
                game.resolvePendingAction();
            }
            renderAll();
        } else {
            GameDialogs.showMessage("Counter Action",
                    victim.getPlayerName() + " is under attack!",
                    "No Just Say No available. The action will proceed.");
            game.resolvePendingAction();
            renderAll();
        }
    }

    @Override
    public void onTurnChanged(String playerName) {
        Platform.runLater(() -> {
            logView.getItems().add(0, "Turn starts: " + playerName);
            renderAll();
            scheduleAiIfNeeded();
        });
    }

    public void dispose() {
        disposed = true;
        aiExecutor.stop();
        player.BankArea.setPaymentResolver(null);
        game.removeObserver(this);
    }

    private void scheduleAiIfNeeded() {
        if (disposed || !modeConfig.hasAi() || game.isGameOver()
                || game.getCurrentState() != GameManager.GameState.NORMAL_TURN) {
            return;
        }
        Player current = game.getCurrentPlayer();
        if (current.isAI()) {
            aiExecutor.startTurn(current);
        }
    }
}
