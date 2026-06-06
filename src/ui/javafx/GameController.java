package ui.javafx;

import ai.AIPlayerBrain;
import ai.AITurnExecutor;
import cards.Card;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameController implements GameObserver {
    // The board art is authored at this fixed coordinate system.
    private static final double BOARD_WIDTH = 1672;
    private static final double BOARD_HEIGHT = 941;
    private static final double HAND_CARD_WIDTH = 96;
    private static final double HAND_CARD_HEIGHT = 159;
    private static final double OWN_TABLE_CARD_WIDTH = 58;
    private static final double OWN_TABLE_CARD_HEIGHT = 96;
    private static final double OPPONENT_CARD_WIDTH = 74;
    private static final double OPPONENT_CARD_HEIGHT = 122;
    private static final double PILE_CARD_WIDTH = 70;
    private static final double PILE_CARD_HEIGHT = 116;
    private static final double SET_PROGRESS_HEIGHT = 11;
    private static final double HAND_TOP_PADDING = 14;
    private static final double HAND_CLIP_PADDING = 22;

    private final GameManager game = GameManager.getInstance();
    private final StackPane root = new StackPane();
    private final Pane boardPane = new Pane();
    private final ImageView boardBackground = new ImageView();
    private final Pane handView = new Pane();
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
    private final Button endTurnButton = imageButton("end_turn.png", "End Turn", 210, 64);
    private final HBox gameOverActions = new HBox(10);
    private final HBox quickActions = new HBox(8);
    private final Runnable newGameAction;
    private final Runnable exitGameAction;
    private final GameModeConfig modeConfig;
    private final AITurnExecutor aiExecutor = new AITurnExecutor(new AIPlayerBrain());
    private final int playerCount;
    // Prevents rebroadcasting a network action while applying one.
    private boolean applyingRemoteAction;
    private boolean disposed;

    // Creates a local human-only controller.
    public GameController(List<String> playerNames) {
        this(playerNames, () -> {}, Platform::exit);
    }

    // Creates a local human-only controller with custom navigation actions.
    public GameController(List<String> playerNames, Runnable newGameAction, Runnable exitGameAction) {
        this(GameModeConfig.local(toHumanSetups(playerNames)), newGameAction, exitGameAction);
    }

    // Creates a controller from the selected local, AI, or network mode.
    public GameController(GameModeConfig modeConfig, Runnable newGameAction, Runnable exitGameAction) {
        this.newGameAction = newGameAction;
        this.exitGameAction = exitGameAction;
        this.modeConfig = modeConfig;
        this.playerCount = modeConfig.players.size();
        // Payment choices need UI context, so the bank calls back into this controller.
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

    // Converts old name-only setup calls into explicit human seats.
    private static List<GameManager.PlayerSetup> toHumanSetups(List<String> playerNames) {
        List<GameManager.PlayerSetup> setups = new ArrayList<>();
        for (String name : playerNames) {
            setups.add(new GameManager.PlayerSetup(name, PlayerType.HUMAN));
        }
        return setups;
    }

    // Builds the responsive table view around the fixed background art.
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

        // Scale the authored board uniformly to the available window.
        NumberBinding scale = Bindings.min(root.widthProperty().divide(BOARD_WIDTH),
                root.heightProperty().divide(BOARD_HEIGHT));
        boardPane.scaleXProperty().bind(scale);
        boardPane.scaleYProperty().bind(scale);
        renderAll();
        scheduleAiIfNeeded();
        return root;
    }

    // Creates all overlays that must align with the background metal frames.
    private void createBoardOverlay() {
        createOpponentZones();
        createOwnZones();
        createCenterTurnStatus();
        createQuickActionButtons();
        createLogView();
        createEndTurnAndGameOverActions();
    }

    // Creates opponent card panes and nameplate overlays.
    private void createOpponentZones() {
        opponentZones.clear();
        for (ZoneSpec spec : opponentSpecs(playerCount)) {
            PlayerZone zone = new PlayerZone(spec);
            // Opponent cards and status labels use independent background coordinates.
            configurePane(zone.cards, spec.x, spec.y, spec.width, spec.height, spec.rotate);
            configureNameLabel(zone.name, spec.nameX, spec.nameY + 1, spec.nameWidth, 21);
            configureStatsLabel(zone.stats, spec.nameX + 4, spec.nameY + 23,
                    spec.nameWidth - 48, 17);
            configureSetsProgress(zone.setsProgress, spec.nameX + 8,
                    spec.nameY + spec.nameHeight - SET_PROGRESS_HEIGHT,
                    spec.nameWidth - 16, SET_PROGRESS_HEIGHT);
            configureSetsLabel(zone.setsLabel, spec.nameX + spec.nameWidth - 42, spec.nameY + 23,
                    38, 17);
            opponentZones.add(zone);
            boardPane.getChildren().addAll(zone.cards, zone.name, zone.setsProgress, zone.stats, zone.setsLabel);
        }
    }

    // Creates the local table, hand, piles, and player name overlays.
    private void createOwnZones() {
        // The own table, hand, and center piles are clipped to their frame areas.
        ZoneSpec ownTable = ownTableSpec(playerCount);
        configurePane(ownTableView, ownTable.x, ownTable.y, ownTable.width, ownTable.height, ownTable.rotate);
        ZoneSpec hand = handSpec(playerCount);
        configurePane(handView, hand.x, hand.y, hand.width, hand.height, hand.rotate);
        expandHandClip(handView, hand.width, hand.height);
        configurePane(centerPileView, 545, 352, 582, 190, 0);
        boardPane.getChildren().addAll(ownTableView, handView, centerPileView);

        ZoneSpec ownName = ownNameSpec(playerCount);
        configureNameLabel(ownNameLabel, ownName.x, ownName.y + 1, ownName.width, 21);
        configureStatsLabel(ownStatsLabel, ownName.x + 4, ownName.y + 23, ownName.width - 48, 17);
        configureSetsProgress(ownSetsProgress, ownName.x + 8,
                ownName.y + ownName.height - SET_PROGRESS_HEIGHT,
                ownName.width - 16, SET_PROGRESS_HEIGHT);
        configureSetsLabel(ownSetsLabel, ownName.x + ownName.width - 42, ownName.y + 23, 38, 17);
        boardPane.getChildren().addAll(ownNameLabel, ownSetsProgress, ownStatsLabel, ownSetsLabel);
    }

    // Creates the center turn owner and action counter label.
    private void createCenterTurnStatus() {
        // The circle in the center shows turn ownership and action count.
        turnStatus.setLayoutX(686);
        turnStatus.setLayoutY(382);
        turnStatus.setPrefSize(300, 110);
        turnStatus.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        turnStatus.setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
        boardPane.getChildren().add(turnStatus);
    }

    // Creates the summary buttons beside the local table area.
    private void createQuickActionButtons() {
        // Quick action buttons expose summaries without crowding the table.
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
        quickActions.setLayoutY(35);
        quickActions.setAlignment(Pos.CENTER);
        boardPane.getChildren().add(quickActions);
    }

    // Creates the semi-transparent game log overlay.
    private void createLogView() {
        // The log stays semi-transparent so the board remains visible under it.
        logView.setLayoutX(24);
        logView.setLayoutY(22);
        logView.setPrefSize(365, 150);
        logView.setStyle("-fx-control-inner-background: rgba(5,8,12,0.62); "
                + "-fx-background-color: rgba(5,8,12,0.50); -fx-background-radius: 12;"
                + "-fx-border-color: rgba(255,218,142,0.45); -fx-border-radius: 12;"
                + "-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        boardPane.getChildren().add(logView);
    }

    // Creates end-turn and game-over navigation buttons.
    private void createEndTurnAndGameOverActions() {
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

    // Returns opponent table and name-frame coordinates for each player count.
    private ZoneSpec[] opponentSpecs(int count) {
        switch (count) {
            case 2:
                return new ZoneSpec[]{
                        new ZoneSpec(488, 145, 705, 164, 0, 770, 93, 160, 44)
                };
            case 3:
                return new ZoneSpec[]{
                        new ZoneSpec(158, 162, 470, 220, -28, 226, 154, 186, 45),
                        new ZoneSpec(1044, 162, 470, 220, 28, 1342, 154, 188, 45)
                };
            case 4:
                return new ZoneSpec[]{
                        new ZoneSpec(130, 215, 410, 224, -32, 226, 200, 150, 44),
                        new ZoneSpec(512, 142, 640, 150, 0, 800, 86, 150, 44),
                        new ZoneSpec(1140, 218, 410, 224, 35, 1420, 213, 152, 44)
                };
            default:
                return new ZoneSpec[]{
                        new ZoneSpec(110, 230, 380, 212, -28, 258, 195, 148, 44),
                        new ZoneSpec(485, 174, 360, 122, -8, 640, 120, 138, 44),
                        new ZoneSpec(865, 174, 360, 122, 8, 1085, 120, 142, 44),
                        new ZoneSpec(1182, 230, 380, 212, 28, 1454, 220, 132, 44)
                };
        }
    }

    // Returns the local player's table-frame coordinates.
    private ZoneSpec ownTableSpec(int count) {
        switch (count) {
            case 2:
                return area(410, 555, 852, 98, 0);
            case 3:
                return area(421, 548, 828, 96, 0);
            case 4:
                return area(389, 582, 893, 96, 0);
            default:
                return area(478, 570, 730, 98, 0);
        }
    }

    // Returns the local hand-frame coordinates.
    private ZoneSpec handSpec(int count) {
        switch (count) {
            case 2:
                return area(145, 700, 1385, 176, 0);
            case 3:
                return area(260, 700, 1155, 178, 0);
            case 4:
                return area(252, 700, 1203, 180, 0);
            default:
                return area(255, 700, 1175, 160, 0);
        }
    }

    // Returns the local name and stats-frame coordinates.
    private ZoneSpec ownNameSpec(int count) {
        switch (count) {
            case 2:
                return area(206, 628, 162, 48, 0);
            case 4:
                return area(210, 685, 150, 48, 0);
            case 5:
                return area(286, 660, 150, 48, 0);
            default:
                return area(264, 642, 150, 48, 0);
        }
    }

    // Creates a simple zone spec without a name frame.
    private ZoneSpec area(double x, double y, double width, double height, double rotate) {
        return new ZoneSpec(x, y, width, height, rotate, 0, 0, 0, 0);
    }

    // Positions and clips a card pane to one background frame.
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

    // Gives the hand room for rotated cards, shadows, and hover lift above the painted frame.
    private void expandHandClip(Pane pane, double width, double height) {
        pane.setClip(new Rectangle(-HAND_CLIP_PADDING, -HAND_CLIP_PADDING,
                width + HAND_CLIP_PADDING * 2, height + HAND_CLIP_PADDING * 2));
    }

    // Positions a player name inside the painted nameplate.
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

    // Positions compact money and hand-count text.
    private void configureStatsLabel(Label label, double x, double y, double width, double height) {
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefSize(width, height);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#f8e7b4"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
    }

    // Positions the 0-to-3 complete-set progress bar.
    private void configureSetsProgress(ProgressBar progress, double x, double y, double width, double height) {
        progress.setLayoutX(x);
        progress.setLayoutY(y);
        progress.setPrefSize(width, height);
        progress.setMinSize(width, height);
        progress.setMaxSize(width, height);
        progress.setStyle("-fx-accent: #ffd66b; -fx-control-inner-background: rgba(0,0,0,0.42);"
                + "-fx-background-insets: 0; -fx-background-radius: 5; -fx-padding: 0;");
    }

    // Positions the numeric complete-set label next to player stats.
    private void configureSetsLabel(Label label, double x, double y, double width, double height) {
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefSize(width, height);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#ffe7a6"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        label.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 4, 0.2, 0, 1);");
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

    // Creates a textured button and keeps fallback text usable.
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
            // All textured buttons share the same small hover response.
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

    // Immutable coordinates for one card zone and its optional name frame.
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

    // Groups all UI nodes associated with one opponent slot.
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

    private boolean winPopupShown = false;

    // Repaints the full board from the current game state.
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

    // Renders all visible opponent table zones.
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

    // Renders the hand for the player whose cards are visible locally.
    private void renderHand(Player current) {
        handView.getChildren().clear();
        List<Card> cards = current.getHand().getCards();
        double step = computeCardStep(cards.size(), handView.getPrefWidth(), HAND_CARD_WIDTH, 12, false);
        double rowWidth = cards.isEmpty() ? 0 : HAND_CARD_WIDTH + step * (cards.size() - 1);
        double startX = Math.max(10, (handView.getPrefWidth() - rowWidth) / 2.0);
        double baseY = Math.max(HAND_TOP_PADDING, (handView.getPrefHeight() - HAND_CARD_HEIGHT) / 2.0);
        double curveDepth = CardArcLayout.computeHandCurveDepth(handView.getPrefHeight(),
                HAND_CARD_HEIGHT, SET_PROGRESS_HEIGHT);
        for (int i = 0; i < cards.size(); i++) {
            int index = i;
            CardView cardView = new CardView(cards.get(i), HAND_CARD_WIDTH, HAND_CARD_HEIGHT);
            double normalized = CardArcLayout.normalizeCardPosition(i, cards.size());
            cardView.setLayoutX(startX + i * step);
            cardView.setLayoutY(baseY + CardArcLayout.computeArcOffset(normalized, curveDepth));
            cardView.setRotate(normalized * CardArcLayout.computeHandRotationDepth(curveDepth, SET_PROGRESS_HEIGHT));
            if (!game.isGameOver() && canControlCurrentPlayer() && current == game.getCurrentPlayer()) {
                cardView.setOnMouseClicked(event -> {
                    cardView.toFront();
                    showCardMenu(cardView, index, cards.get(index));
                });
            }
            handView.getChildren().add(cardView);
        }
    }

    // Renders the local table area for the visible player.
    private void renderOwnTable(Player current) {
        renderTableCards(ownTableView, current, OWN_TABLE_CARD_WIDTH, OWN_TABLE_CARD_HEIGHT, true);
    }

    // Renders name, bank/hand stats, and set progress for the visible player.
    private void renderOwnInfo(Player current) {
        ownNameLabel.setText(current.getPlayerName());
        ownStatsLabel.setText(playerStats(current));
        updateSetsProgress(ownSetsProgress, ownSetsLabel, current);
    }

    // Renders draw and discard piles inside the center circle.
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

    // Renders bank and property cards inside a table zone.
    private void renderTableCards(Pane target, Player player, double cardWidth, double cardHeight,
                                  boolean currentPlayerArea) {
        target.getChildren().clear();
        List<Card> cards = getTableCards(player);
        if (cards.isEmpty()) {
            // Empty text is centered inside the same clipped frame as cards.
            Label empty = new Label(currentPlayerArea ? "No table cards" : "No cards on table");
            empty.setTextFill(javafx.scene.paint.Color.web("rgba(255,255,255,0.70)"));
            empty.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            empty.setAlignment(Pos.CENTER);
            empty.setLayoutX(0);
            empty.setLayoutY(0);
            empty.setPrefSize(target.getPrefWidth(), target.getPrefHeight());
            empty.setMinSize(target.getPrefWidth(), target.getPrefHeight());
            empty.setMaxSize(target.getPrefWidth(), target.getPrefHeight());
            target.getChildren().add(empty);
            return;
        }

        int rows = cards.size() > 7 && target.getPrefHeight() >= cardHeight * 1.65 ? 2 : 1;
        int perRow = (int) Math.ceil(cards.size() / (double) rows);
        double step = computeCardStep(perRow, target.getPrefWidth(), cardWidth, 8, !currentPlayerArea);
        double rowGap = rows == 1 ? 0 : Math.min(cardHeight * 0.62,
                (target.getPrefHeight() - cardHeight) / (rows - 1));
        double curveDepth = cards.size() <= 1 ? 0 : (currentPlayerArea ? 6 : 12);
        double usedHeight = cardHeight + rowGap * (rows - 1) + curveDepth;
        double startY = Math.max(0, (target.getPrefHeight() - usedHeight) / 2.0);

        // Opponent table cards arc slightly more to match the tilted frames.
        for (int i = 0; i < cards.size(); i++) {
            int row = i / perRow;
            int column = i % perRow;
            int rowCount = Math.min(perRow, cards.size() - row * perRow);
            double rowWidth = cardWidth + step * Math.max(0, rowCount - 1);
            double rowStartX = Math.max(8, (target.getPrefWidth() - rowWidth) / 2.0);
            Card tableCard = cards.get(i);
            CardView cardView = new CardView(tableCard, cardWidth, cardHeight);
            double centerOffset = column - (rowCount - 1) / 2.0;
            double normalized = rowCount <= 1 ? 0 : centerOffset / ((rowCount - 1) / 2.0);
            double arcY = Math.abs(normalized) * curveDepth;
            cardView.setLayoutX(rowStartX + column * step);
            cardView.setLayoutY(startY + row * rowGap + arcY);
            cardView.setRotate(normalized * (currentPlayerArea ? 3 : 6));
            target.getChildren().add(cardView);
        }
    }

    // Computes horizontal card spacing while respecting each frame width.
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

    // Combines banked money and properties for table rendering.
    private List<Card> getTableCards(Player player) {
        List<Card> cards = new ArrayList<>();
        cards.addAll(player.getBankArea().getAssets());
        cards.addAll(player.getPropertyArea().getAllPropertyCards());
        return cards;
    }

    // Chooses which player's hand/table this machine is allowed to see.
    private Player getViewPlayer() {
        if (modeConfig.isNetwork()) {
            List<Player> players = game.getActivePlayers();
            int index = Math.max(0, Math.min(modeConfig.localPlayerIndex, players.size() - 1));
            return players.get(index);
        }
        Player current = game.getCurrentPlayer();
        if (current.isAI()) {
            for (Player player : game.getActivePlayers()) {
                if (!player.isAI()) {
                    return player;
                }
            }
        }
        return current;
    }

    // Checks whether a player is controlled by this machine in network games.
    private boolean isLocalPlayer(Player player) {
        if (!modeConfig.isNetwork()) {
            return true;
        }
        List<Player> players = game.getActivePlayers();
        int index = players.indexOf(player);
        return index == modeConfig.localPlayerIndex;
    }

    // Determines whether the current player can perform UI actions now.
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

    // Ends the local player's turn after permission checks.
    private void requestEndTurn() {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        performEndTurn(true);
    }

    // Ends the turn and optionally broadcasts it to LAN peers.
    private void performEndTurn(boolean broadcast) {
        game.endTurn();
        if (broadcast) {
            sendNetworkAction("END_TURN", "");
        }
        renderAll();
    }

    // Banks a selected hand card and broadcasts the same hand identity.
    private void requestDeposit(int cardIndex) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        Card card = game.getCurrentPlayer().getHand().getCard(cardIndex);
        String payload = "index=" + cardIndex + ";card=" + safe(card == null ? "" : card.getCardName());
        game.depositCardToBank(cardIndex);
        sendNetworkAction("BANK", payload);
        renderAll();
    }

    // Discards a selected excess hand card and broadcasts the same hand identity.
    private void requestDiscard(int cardIndex) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        Card card = game.getCurrentPlayer().getHand().getCard(cardIndex);
        String payload = "index=" + cardIndex + ";card=" + safe(card == null ? "" : card.getCardName());
        game.discardCard(cardIndex);
        sendNetworkAction("DISCARD", payload);
        renderAll();
    }

    // Plays a selected card with its chosen target information.
    private void requestPlayCard(int cardIndex, TargetInfo targetInfo) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        Card card = game.getCurrentPlayer().getHand().getCard(cardIndex);
        String payload = encodePlayPayload(cardIndex, card, targetInfo);
        game.executePlayerAction(cardIndex, targetInfo);
        sendNetworkAction("PLAY", payload);
        renderAll();
    }

    // Plays Double The Rent with the chosen rent card.
    private void requestDoubleRent(int doubleCardIndex, int rentCardIndex, TargetInfo targetInfo) {
        if (!canControlCurrentPlayer()) {
            onGameEvent("Wait for " + game.getCurrentPlayer().getPlayerName() + "'s turn.");
            return;
        }
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        Card doubleCard = doubleCardIndex >= 0 && doubleCardIndex < hand.size() ? hand.get(doubleCardIndex) : null;
        Card rentCard = rentCardIndex >= 0 && rentCardIndex < hand.size() ? hand.get(rentCardIndex) : null;
        String payload = encodeDoubleRentPayload(doubleCardIndex, doubleCard, rentCardIndex, rentCard, targetInfo);
        game.executeDoubleRentAction(doubleCardIndex, rentCardIndex, targetInfo);
        sendNetworkAction("DOUBLE_RENT", payload);
        renderAll();
    }

    // Sends a network action unless it came from the network already.
    private void sendNetworkAction(String type, String payload) {
        if (applyingRemoteAction || modeConfig.networkBridge == null) {
            return;
        }
        modeConfig.networkBridge.sendGameAction(type, payload);
    }

    // Encodes a normal card play for LAN replay.
    private String encodePlayPayload(int cardIndex, Card card, TargetInfo targetInfo) {
        List<String> parts = new ArrayList<>();
        parts.add("index=" + cardIndex);
        parts.add("card=" + safe(card == null ? "" : card.getCardName()));
        appendCardState(parts, card);
        appendTarget(parts, targetInfo);
        return String.join(";", parts);
    }

    // Encodes a Double The Rent action for LAN replay.
    private String encodeDoubleRentPayload(int doubleCardIndex, Card doubleCard,
                                           int rentCardIndex, Card rentCard,
                                           TargetInfo targetInfo) {
        List<String> parts = new ArrayList<>();
        parts.add("doubleIndex=" + doubleCardIndex);
        parts.add("double=" + safe(doubleCard == null ? "" : doubleCard.getCardName()));
        parts.add("rentIndex=" + rentCardIndex);
        parts.add("rent=" + safe(rentCard == null ? "" : rentCard.getCardName()));
        appendCardState(parts, rentCard);
        appendTarget(parts, targetInfo);
        return String.join(";", parts);
    }

    // Adds mutable card color state to a network payload.
    private void appendCardState(List<String> parts, Card card) {
        if (card instanceof cards.RentCard) {
            parts.add("color=" + ((cards.RentCard) card).getSelectedColor());
        } else if (card instanceof cards.WildRentCard) {
            parts.add("color=" + ((cards.WildRentCard) card).getSelectedColor());
        } else if (card instanceof cards.PropertyCard) {
            parts.add("color=" + ((cards.PropertyCard) card).getColorGroup());
        }
    }

    // Adds target player and selected property data to a network payload.
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

    // Escapes simple key-value payload separators.
    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace(";", "\\s")
                .replace("=", "\\e");
    }

    // Parses a LAN action payload into key-value fields.
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

    // Reverses key-value payload escaping.
    private String unsafe(String value) {
        return value == null ? "" : value.replace("\\e", "=")
                .replace("\\s", ";")
                .replace("\\\\", "\\");
    }

    // Reads an integer payload field with a fallback.
    private int readInt(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Reads a property color payload field with a fallback.
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

    // Applies incoming LAN messages from other clients on the UI thread.
    private void handleNetworkMessage(LanGameMessage message) {
        if (!modeConfig.isNetwork() || message == null || modeConfig.networkBridge == null) {
            return;
        }
        if (message.getSenderId() == modeConfig.networkBridge.getLocalPlayerId()) {
            return;
        }
        Platform.runLater(() -> applyRemoteAction(message));
    }

    // Replays one remote action against this client's local game state.
    private void applyRemoteAction(LanGameMessage message) {
        applyingRemoteAction = true;
        try {
            String type = message.getType();
            Map<String, String> payload = parsePayload(message.getPayload());
            if ("END_TURN".equals(type)) {
                performEndTurn(false);
            } else if ("BANK".equals(type)) {
                int index = findCardIndex(game.getCurrentPlayer(), payload, "index", "card");
                game.depositCardToBank(index);
                renderAll();
            } else if ("DISCARD".equals(type)) {
                int index = findCardIndex(game.getCurrentPlayer(), payload, "index", "card");
                game.discardCard(index);
                renderAll();
            } else if ("PLAY".equals(type)) {
                applyRemotePlay(payload);
            } else if ("DOUBLE_RENT".equals(type)) {
                applyRemoteDoubleRent(payload);
            } else if ("JUST_SAY_NO".equals(type)) {
                applyRemoteJustSayNo(payload);
            } else if ("RESOLVE_PENDING".equals(type)) {
                game.resolvePendingAction();
                renderAll();
            }
        } catch (Exception ex) {
            onGameEvent("[Network] Could not apply remote action: " + ex.getMessage());
        } finally {
            applyingRemoteAction = false;
        }
    }

    // Replays a normal remote card play.
    private void applyRemotePlay(Map<String, String> payload) {
        Player current = game.getCurrentPlayer();
        int cardIndex = findCardIndex(current, payload, "index", "card");
        Card card = current.getHand().getCard(cardIndex);
        if (card == null) {
            onGameEvent("[Network] Remote card not found: " + payload.getOrDefault("card", ""));
            return;
        }
        applyRemoteCardState(card, payload);
        game.executePlayerAction(cardIndex, buildTargetInfo(payload));
        renderAll();
    }

    // Replays a remote Double The Rent combo.
    private void applyRemoteDoubleRent(Map<String, String> payload) {
        Player current = game.getCurrentPlayer();
        int doubleIndex = findCardIndex(current, payload, "doubleIndex", "double");
        int rentIndex = findCardIndex(current, payload, "rentIndex", "rent");
        Card rent = current.getHand().getCard(rentIndex);
        applyRemoteCardState(rent, payload);
        game.executeDoubleRentAction(doubleIndex, rentIndex, buildTargetInfo(payload));
        renderAll();
    }

    // Replays a remote Just Say No counter.
    private void applyRemoteJustSayNo(Map<String, String> payload) {
        Player victim = game.getPendingVictim();
        int index = findCardIndex(victim, payload, "index", "card");
        game.counterAttackWithJustSayNo(index);
        renderAll();
    }

    // Restores selected color state before replaying a remote card.
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

    // Rebuilds target information from a network payload.
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

    // Locates a card by original index, then by name for duplicate-safe replay.
    private int findCardIndex(Player player, Map<String, String> payload, String indexKey, String nameKey) {
        if (player == null || payload == null) {
            return -1;
        }
        List<Card> hand = player.getHand().getCards();
        int index = readInt(payload, indexKey, -1);
        String cardName = payload.getOrDefault(nameKey, "");
        if (index >= 0 && index < hand.size()
                && (cardName.isEmpty() || cardName.equals(hand.get(index).getCardName()))) {
            return index;
        }
        for (int i = 0; i < hand.size(); i++) {
            if (cardName.equals(hand.get(i).getCardName())) {
                return i;
            }
        }
        return -1;
    }

    // Builds compact player stats for nameplate display.
    private String playerStats(Player player) {
        return String.format("Bank %dM Hand %d",
                player.getBankArea().calculateTotalFunds(),
                player.getHand().getSize());
    }

    // Updates the progress bar based on complete sets only.
    private void updateSetsProgress(ProgressBar progress, Label label, Player player) {
        int completed = player.getPropertyArea().countCompletedSets();
        progress.setProgress(calculateSetProgress(player));
        label.setText(completed + "/3");
    }

    // Converts completed sets into a 0.0 to 1.0 win progress value.
    private double calculateSetProgress(Player player) {
        int completed = player.getPropertyArea().countCompletedSets();
        return Math.min(1.0, completed / 3.0);
    }

    // Styles labels under the draw and discard piles.
    private void stylePileLabel(Label label) {
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#f8e7b4"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        label.setStyle("-fx-background-color: rgba(0,0,0,0.52); -fx-background-radius: 8;"
                + "-fx-padding: 3 6 3 6;");
    }

    // Shows draw and discard pile counts.
    private void showDeckSummary() {
        Card discardTop = game.getGameDeck().peekDiscardTop();
        GameDialogs.showMessage("Deck",
                "Draw and discard",
                "Draw pile: " + game.getGameDeck().getDrawPileSize() + "\n"
                        + "Discard pile: " + game.getGameDeck().getDiscardPileSize() + "\n"
                        + "Discard top: " + (discardTop == null ? "Empty" : discardTop.getCardName()));
    }

    // Shows the current player's banked cards.
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

    // Shows property sets, set sizes, and current rent values.
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

    // Shows action cards currently in the visible player's hand.
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

    // Shows compact opponent progress and money summaries.
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

    // Returns the base set behind a house or hotel decorator.
    private player.PropertySet getRootSet(player.Rentable rentable) {
        if (rentable instanceof player.SetDecorator) {
            return ((player.SetDecorator) rentable).getRootSet();
        }
        if (rentable instanceof player.PropertySet) {
            return (player.PropertySet) rentable;
        }
        return null;
    }

    // Opens the per-card action menu for the current player's hand.
    private void showCardMenu(CardView owner, int cardIndex, Card card) {
        ContextMenu menu = new ContextMenu();
        MenuItem bank = createBankMenuItem(cardIndex);
        MenuItem discard = createDiscardMenuItem(cardIndex);

        if (card instanceof cards.MoneyCard) {
            menu.getItems().addAll(bank, discard);
        } else {
            menu.getItems().addAll(bank, discard, createPlayMenuItem(cardIndex, card));
        }
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    // Creates the bank menu action.
    private MenuItem createBankMenuItem(int cardIndex) {
        MenuItem bank = new MenuItem("Deposit to bank");
        bank.setOnAction(event -> {
            requestDeposit(cardIndex);
        });
        return bank;
    }

    // Creates the discard menu action.
    private MenuItem createDiscardMenuItem(int cardIndex) {
        MenuItem discard = new MenuItem("Discard");
        discard.setDisable(!game.getCurrentPlayer().getHand().requiresDiscard());
        discard.setOnAction(event -> {
            requestDiscard(cardIndex);
        });
        return discard;
    }

    // Creates the play menu action.
    private MenuItem createPlayMenuItem(int cardIndex, Card card) {
        MenuItem play = new MenuItem("Play card");
        play.setOnAction(event -> {
            handlePlayMenuAction(cardIndex, card);
        });
        return play;
    }

    // Runs the pre-play dialogs and then executes the selected card.
    private void handlePlayMenuAction(int cardIndex, Card card) {
        if (card instanceof cards.DoubleTheRentCard) {
            playDoubleRent(cardIndex);
            return;
        }

        TargetInfo targetInfo = chooseBuildTarget(card);
        if (targetInfo == null && (card instanceof cards.HouseCard || card instanceof cards.HotelCard)) {
            return;
        }
        if (!applySelectedColor(card)) {
            onGameEvent("Cancelled " + card.getCardName());
            return;
        }
        if (needsTarget(card)) {
            targetInfo = chooseTarget(card);
            if (targetInfo == null) {
                onGameEvent("Cancelled " + card.getCardName());
                return;
            }
        }
        requestPlayCard(cardIndex, targetInfo);
    }

    // Chooses the target set for house and hotel cards.
    private TargetInfo chooseBuildTarget(Card card) {
        if (card instanceof cards.HouseCard) {
            TargetInfo targetInfo = chooseImprovementTarget(
                    game.getCurrentPlayer().getPropertyArea().getHouseEligibleColors());
            if (targetInfo == null) {
                onGameEvent("No eligible complete set for House.");
            }
            return targetInfo;
        }
        if (card instanceof cards.HotelCard) {
            TargetInfo targetInfo = chooseImprovementTarget(
                    game.getCurrentPlayer().getPropertyArea().getHotelEligibleColors());
            if (targetInfo == null) {
                onGameEvent("No eligible complete set for Hotel.");
            }
            return targetInfo;
        }
        return null;
    }

    // Applies selected color state for rent and wild property cards.
    private boolean applySelectedColor(Card card) {
        if (card instanceof cards.RentCard && ((cards.RentCard) card).isMultiColor()) {
            cards.RentCard rentCard = (cards.RentCard) card;
            enums.PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
            if (selectedColor == null) {
                return false;
            }
            rentCard.setSelectedColor(selectedColor);
        } else if (card instanceof cards.WildRentCard) {
            cards.WildRentCard wrCard = (cards.WildRentCard) card;
            enums.PropertyColor selectedColor = chooseColor(wrCard.getAvailableColors());
            if (selectedColor == null) {
                return false;
            }
            wrCard.setSelectedColor(selectedColor);
        } else if (card instanceof cards.SuperWildCard || card instanceof cards.PropertyWildCard) {
            return applySelectedWildPropertyColor(card);
        }
        return true;
    }

    // Applies selected color state for property wild cards.
    private boolean applySelectedWildPropertyColor(Card card) {
        enums.PropertyColor[] options = card instanceof cards.SuperWildCard
                ? ((cards.SuperWildCard) card).getAvailableColors()
                : ((cards.PropertyWildCard) card).getAvailableColors();
        enums.PropertyColor selectedColor = chooseColor(options);
        if (selectedColor == null) {
            return false;
        }
        if (card instanceof cards.SuperWildCard) {
            ((cards.SuperWildCard) card).setCurrentColor(selectedColor);
        } else {
            ((cards.PropertyWildCard) card).setCurrentColor(selectedColor);
        }
        return true;
    }

    // Reports whether this card needs a target before play.
    private boolean needsTarget(Card card) {
        return card.requiresTarget();
    }

    // Prompts for a color from the card's allowed options.
    private enums.PropertyColor chooseColor(enums.PropertyColor[] colorOptions) {
        List<enums.PropertyColor> options = java.util.Arrays.asList(colorOptions);
        return GameDialogs.showChoice("Select Color",
                "Choose the color for this card",
                "Color",
                options,
                options.get(0)).orElse(null);
    }

    // Prompts for the target player and any target property details.
    private TargetInfo chooseTarget(Card card) {
        java.util.List<Player> choices = new java.util.ArrayList<>(game.getOpponents(game.getCurrentPlayer()));
        filterTargetChoices(card, choices);
        Optional<Player> selected = chooseTargetPlayer(choices);
        return selected.map(player -> buildTargetInfoForCard(card, player)).orElse(null);
    }

    // Removes opponents that cannot legally be targeted by the card.
    private void filterTargetChoices(Card card, List<Player> choices) {
        if (card instanceof cards.SlyDealCard) {
            // Sly Deal can only target opponents with incomplete properties.
            choices.removeIf(player -> player.getPropertyArea().getStealableIncompleteColors().isEmpty());
        } else if (card instanceof cards.ForceDealCard) {
            if (game.getCurrentPlayer().getPropertyArea().getPropertyColorsWithCards().isEmpty()) {
                choices.clear();
                return;
            }
            // Force Deal needs a legal property on both sides.
            choices.removeIf(player -> player.getPropertyArea().getPropertyColorsWithCards().isEmpty());
        } else if (card instanceof cards.DealBreakerCard) {
            // Deal Breaker can only target complete sets.
            choices.removeIf(player -> player.getPropertyArea().countCompletedSets() == 0);
        }
    }

    // Prompts for one target player after legality filtering.
    private Optional<Player> chooseTargetPlayer(List<Player> choices) {
        if (choices.isEmpty()) {
            onGameEvent("No available opponent.");
            return Optional.empty();
        }
        return GameDialogs.showChoice("Choose Target",
                "Select a player to perform the action on",
                "Target",
                FXCollections.observableArrayList(choices),
                choices.get(0));
    }

    // Builds target details for the selected player.
    private TargetInfo buildTargetInfoForCard(Card card, Player target) {
        if (card instanceof cards.SlyDealCard) {
            return chooseSlyDealTarget(target);
        }
        if (card instanceof cards.ForceDealCard) {
            return chooseForceDealTarget(target);
        }
        if (card instanceof cards.DealBreakerCard) {
            return chooseDealBreakerTarget(target);
        }
        return new TargetInfo(target);
    }

    // Builds a Sly Deal target from one incomplete property.
    private TargetInfo chooseSlyDealTarget(Player target) {
        PropertyPick targetCard = choosePropertyCard(target, target.getPropertyArea().getStealableIncompleteColors(),
                "Choose property to steal", false);
        return targetCard == null ? null : new TargetInfo(target, targetCard.color, targetCard.index);
    }

    // Builds a Force Deal target from one owned and one opponent property.
    private TargetInfo chooseForceDealTarget(Player target) {
        PropertyPick mine = choosePropertyCard(game.getCurrentPlayer(),
                game.getCurrentPlayer().getPropertyArea().getPropertyColorsWithCards(),
                "Choose your property to give", false);
        if (mine == null) {
            return null;
        }
        PropertyPick theirs = choosePropertyCard(target, target.getPropertyArea().getPropertyColorsWithCards(),
                "Choose target property to receive", false);
        return theirs == null ? null : new TargetInfo(target, mine.color, mine.index, theirs.color, theirs.index);
    }

    // Builds a Deal Breaker target and optional complete-set color.
    private TargetInfo chooseDealBreakerTarget(Player target) {
        java.util.List<enums.PropertyColor> completed = target.getPropertyArea().getCompletedColorsList();
        if (completed.size() > 1) {
            enums.PropertyColor chosen = chooseColor(completed.toArray(new enums.PropertyColor[0]));
            if (chosen == null) return null;
            return TargetInfo.forImprovement(chosen).withTarget(target);
        }
        return new TargetInfo(target);
    }

    // Prompts for the complete set that should receive a house or hotel.
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

    // Prompts for a property card and includes complete sets by default.
    private PropertyPick choosePropertyCard(Player owner, List<enums.PropertyColor> colors, String title) {
        return choosePropertyCard(owner, colors, title, true);
    }

    // Prompts for a property card from selected colors.
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

    // Plays Double The Rent by pairing it with a selected rent card.
    private void playDoubleRent(int doubleCardIndex) {
        java.util.List<Integer> rentIndexes = findRentCardIndexes(doubleCardIndex);
        Optional<Integer> selectedRentIndex = chooseRentCardIndex(rentIndexes);
        if (!selectedRentIndex.isPresent()) {
            return;
        }

        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        int rentCardIndex = selectedRentIndex.get();
        Card rent = hand.get(rentCardIndex);
        if (!applySelectedRentColor(rent)) {
            return;
        }

        TargetInfo targetInfo = chooseRentTarget(rent);
        if (targetInfo == null && needsTarget(rent)) {
            return;
        }
        requestDoubleRent(doubleCardIndex, rentCardIndex, targetInfo);
    }

    // Finds rent cards that can pair with Double The Rent.
    private java.util.List<Integer> findRentCardIndexes(int doubleCardIndex) {
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        java.util.List<Integer> rentIndexes = new java.util.ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (i != doubleCardIndex
                    && (hand.get(i) instanceof cards.RentCard || hand.get(i) instanceof cards.WildRentCard)) {
                rentIndexes.add(i);
            }
        }
        return rentIndexes;
    }

    // Prompts for the rent card to pair with Double The Rent.
    private Optional<Integer> chooseRentCardIndex(List<Integer> rentIndexes) {
        if (rentIndexes.isEmpty()) {
            onGameEvent("Double The Rent must be paired with a rent card.");
            return Optional.empty();
        }
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        java.util.List<String> choices = new java.util.ArrayList<>();
        for (Integer index : rentIndexes) {
            choices.add(index + ": " + hand.get(index).getCardName());
        }
        Optional<String> selected = GameDialogs.showChoice("Double The Rent",
                "Choose a rent card to play with it",
                "Rent",
                choices,
                choices.get(0));
        if (!selected.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(rentIndexes.get(choices.indexOf(selected.get())));
    }

    // Applies the selected color for the paired rent card.
    private boolean applySelectedRentColor(Card rent) {
        if (rent instanceof cards.RentCard && ((cards.RentCard) rent).isMultiColor()) {
            cards.RentCard rentCard = (cards.RentCard) rent;
            enums.PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
            if (selectedColor == null) {
                return false;
            }
            rentCard.setSelectedColor(selectedColor);
        } else if (rent instanceof cards.WildRentCard) {
            cards.WildRentCard wildRentCard = (cards.WildRentCard) rent;
            enums.PropertyColor selectedColor = chooseColor(wildRentCard.getAvailableColors());
            if (selectedColor == null) {
                return false;
            }
            wildRentCard.setSelectedColor(selectedColor);
        }
        return true;
    }

    // Prompts for the paired rent card target if required.
    private TargetInfo chooseRentTarget(Card rent) {
        TargetInfo targetInfo = null;
        if (needsTarget(rent)) {
            targetInfo = chooseTarget(rent);
            if (targetInfo == null) {
                onGameEvent("Cancelled " + rent.getCardName());
                return null;
            }
        }
        return targetInfo;
    }

    // Lets a human choose payment cards, while AI and network seats pay automatically.
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

        // Bank and property cards are shown together but retain their source labels.
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
            // If the player cannot fully pay, all available assets are acceptable.
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

    // Chooses low-value cards first for AI and remote automatic payment.
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

    // Selects cards until the requested amount is reached or assets run out.
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

    // Displays one property option inside selection dialogs.
    private static class PropertyPick {
        final enums.PropertyColor color;
        final int index;
        final String label;

        // Stores the property color, flattened index, and label.
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

    // Renders the center turn status text.
    private void renderTurnStatus(Player current) {
        int completedSets = current.getPropertyArea().countCompletedSets();
        turnStatus.getChildren().setAll(
                statusText("TURN\n", "#f8e7b4", FontWeight.EXTRA_BOLD, 24),
                statusText(current.getPlayerName() + "\n", "#ffffff", FontWeight.EXTRA_BOLD, 26),
                statusText("Actions " + game.getActionsRemaining()
                        + "  |  Sets " + completedSets + "/3", "#bfefff", FontWeight.BOLD, 16));
    }

    // Creates default-size status text.
    private Text statusText(String text, String color, FontWeight weight) {
        return statusText(text, color, weight, 24);
    }

    // Creates styled status text for the center turn label.
    private Text statusText(String text, String color, FontWeight weight, int size) {
        Text node = new Text(text);
        node.setFill(javafx.scene.paint.Color.web(color));
        node.setFont(Font.font("Segoe UI", weight, size));
        return node;
    }

    // Styles fallback text buttons when an image asset is missing.
    private void styleButton(Button button) {
        button.setTextFill(javafx.scene.paint.Color.web("#1b2a31"));
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        button.setStyle("-fx-background-color: #f0c978; -fx-background-radius: 8;"
                + "-fx-border-color: #ffe0a1; -fx-border-radius: 8;"
                + "-fx-padding: 10 24 10 24;");
    }

    @Override
    // Receives game log messages and interrupt markers.
    public void onGameEvent(String message) {
        Platform.runLater(() -> {
            logView.getItems().add(0, message);
            renderAll();

            if (message.contains("[INTERRUPT_REQUEST]")) {
                handleInterruptRequest();
            }
        });
    }

    // Handles Just Say No prompts for AI, local humans, and remote humans.
    private void handleInterruptRequest() {
        if (game.getCurrentState() != GameManager.GameState.WAITING_FOR_COUNTER_ACTION) return;

        Player victim = game.getPendingVictim();
        if (victim == null) return;
        if (victim.isAI()) {
            handleAiInterrupt(victim);
            return;
        }
        if (isRemoteHumanVictim(victim)) {
            waitForRemoteInterrupt(victim);
            return;
        }
        handleLocalHumanInterrupt(victim);
    }

    // Lets the AI executor decide whether to counter.
    private void handleAiInterrupt(Player victim) {
        aiExecutor.handleInterrupt(victim);
        renderAll();
    }

    // Reports whether the pending victim belongs to another LAN client.
    private boolean isRemoteHumanVictim(Player victim) {
        return modeConfig.isNetwork() && !isLocalPlayer(victim);
    }

    // Logs that a remote human must respond from their own machine.
    private void waitForRemoteInterrupt(Player victim) {
        onGameEvent("[Network] Waiting for " + victim.getPlayerName() + " to answer Just Say No.");
    }

    // Prompts the local human victim for Just Say No.
    private void handleLocalHumanInterrupt(Player victim) {
        int jsnIndex = findJustSayNoIndex(victim);
        if (jsnIndex >= 0) {
            resolveLocalCounterChoice(victim, jsnIndex);
        } else {
            resolveWithoutCounter(victim);
        }
        renderAll();
    }

    // Finds the first Just Say No card in the victim hand.
    private int findJustSayNoIndex(Player victim) {
        List<Card> hand = victim.getHand().getCards();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardName().equals("Just Say No")) {
                return i;
            }
        }
        return -1;
    }

    // Applies the local human's counter choice.
    private void resolveLocalCounterChoice(Player victim, int jsnIndex) {
        boolean counter = GameDialogs.showConfirmation("Counter Action",
                victim.getPlayerName() + " is under attack!",
                "Use Just Say No to counter?");
        if (counter) {
            game.counterAttackWithJustSayNo(jsnIndex);
            sendNetworkAction("JUST_SAY_NO", "index=" + jsnIndex + ";card=Just Say No");
        } else {
            game.resolvePendingAction();
            sendNetworkAction("RESOLVE_PENDING", "");
        }
    }

    // Resolves the pending action when no counter card exists.
    private void resolveWithoutCounter(Player victim) {
        GameDialogs.showMessage("Counter Action",
                victim.getPlayerName() + " is under attack!",
                "No Just Say No available. The action will proceed.");
        game.resolvePendingAction();
        sendNetworkAction("RESOLVE_PENDING", "");
    }

    @Override
    // Re-renders the table and starts AI turns after turn changes.
    public void onTurnChanged(String playerName) {
        Platform.runLater(() -> {
            logView.getItems().add(0, "Turn starts: " + playerName);
            renderAll();
            scheduleAiIfNeeded();
        });
    }

    // Removes callbacks and stops AI work when the view is closed.
    public void dispose() {
        disposed = true;
        aiExecutor.stop();
        player.BankArea.setPaymentResolver(null);
        game.removeObserver(this);
    }

    // Starts an AI turn only when this controller owns the active table.
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
