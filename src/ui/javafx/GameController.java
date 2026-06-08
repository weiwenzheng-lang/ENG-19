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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
import java.util.List;

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
    private final Label handHelpLabel = new Label("For hand cards: Double-click or right-click a hand card to zoom.");
    private final Label wildColorHintLabel = new Label("For desk cards: Dual-color property: the upper half is the active color.");
    private final Label deckLabel = new Label("Deck");
    private final Label discardLabel = new Label("Discard");
    private final Button endTurnButton = imageButton("end_turn.png", "End Turn", 210, 64);
    private final Button leaveMatchButton = imageButton("leave_match.png", "Leave Match", 180, 48);
    private final HBox gameOverActions = new HBox(10);
    private final HBox quickActions = new HBox(8);
    private final Runnable newGameAction;
    private final Runnable exitGameAction;
    private final GameModeConfig modeConfig;
    private final AITurnExecutor aiExecutor = new AITurnExecutor(new AIPlayerBrain());
    private final CardPlayPlanner playPlanner;
    private final InterruptController interruptController;
    private final NetworkActionApplier networkActionApplier;
    private int playerCount;
    private int localPlayerIndex;
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
        this.localPlayerIndex = modeConfig.localPlayerIndex;
        this.playPlanner = new CardPlayPlanner(game, this::onGameEvent);
        this.interruptController = new InterruptController(game, aiExecutor, modeConfig,
                this::isLocalPlayer, this::sendNetworkAction, this::onGameEvent, this::renderAll);
        this.networkActionApplier = new NetworkActionApplier(game, () -> performEndTurn(false),
                this::renderAll, this::onGameEvent, () -> localPlayerIndex,
                value -> localPlayerIndex = value, value -> playerCount = value, this::createContent);
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
        boardPane.scaleXProperty().unbind();
        boardPane.scaleYProperty().unbind();
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
        for (BoardLayoutConfig.ZoneSpec spec : BoardLayoutConfig.opponentSpecs(playerCount)) {
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
        BoardLayoutConfig.ZoneSpec ownTable = BoardLayoutConfig.ownTableSpec(playerCount);
        configurePane(ownTableView, ownTable.x, ownTable.y, ownTable.width, ownTable.height, ownTable.rotate);
        BoardLayoutConfig.ZoneSpec hand = BoardLayoutConfig.handSpec(playerCount);
        configurePane(handView, hand.x, hand.y, hand.width, hand.height, hand.rotate);
        expandHandClip(handView, hand.width, hand.height);
        configurePane(centerPileView, 545, 352, 582, 190, 0);
        boardPane.getChildren().addAll(ownTableView, handView, centerPileView);
        configureHintLabel(handHelpLabel, hand.x+400, hand.y + 200, Math.min(420, hand.width - 24));
        configureHintLabel(wildColorHintLabel, ownTable.x + 10, ownTable.y - 520, Math.min(460, ownTable.width - 20));

        BoardLayoutConfig.ZoneSpec ownName = BoardLayoutConfig.ownNameSpec(playerCount);
        configureNameLabel(ownNameLabel, ownName.x, ownName.y + 1, ownName.width, 21);
        configureStatsLabel(ownStatsLabel, ownName.x + 4, ownName.y + 23, ownName.width - 48, 17);
        configureSetsProgress(ownSetsProgress, ownName.x + 8,
                ownName.y + ownName.height - SET_PROGRESS_HEIGHT,
                ownName.width - 16, SET_PROGRESS_HEIGHT);
        configureSetsLabel(ownSetsLabel, ownName.x + ownName.width - 42, ownName.y + 23, 38, 17);
        boardPane.getChildren().addAll(ownNameLabel, ownSetsProgress, ownStatsLabel, ownSetsLabel,
                handHelpLabel, wildColorHintLabel);
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
        Button bankInfo = imageButton("bank.png", "Bank", 104, 32);
        bankInfo.setTooltip(new Tooltip("All players' bank totals and card counts"));
        bankInfo.setOnAction(event -> showBankSummary());
        Button propertyInfo = imageButton("properties.png", "Properties", 118, 32);
        propertyInfo.setTooltip(new Tooltip("All players' property progress"));
        propertyInfo.setOnAction(event -> showPropertySummary());
        leaveMatchButton.setTooltip(new Tooltip("Leave the current match"));
        leaveMatchButton.setOnAction(event -> requestLeaveMatch());
        quickActions.getChildren().setAll(bankInfo, propertyInfo);
        quickActions.setLayoutX(1137);
        quickActions.setLayoutY(35);
        quickActions.setAlignment(Pos.CENTER);
        leaveMatchButton.setLayoutX(1390);
        leaveMatchButton.setLayoutY(27);
        boardPane.getChildren().addAll(quickActions, leaveMatchButton);
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

        Button newGame = imageButton("confirm.png", "New Game", 156, 52);
        newGame.setOnAction(event -> newGameAction.run());
        Button exitGame = imageButton("cancel.png", "Exit Game", 156, 52);
        exitGame.setOnAction(event -> exitGameAction.run());
        gameOverActions.getChildren().setAll(exitGame, newGame);
        gameOverActions.setSpacing(24);
        gameOverActions.setLayoutX(1194);
        gameOverActions.setLayoutY(842);
        gameOverActions.setAlignment(Pos.CENTER);
        gameOverActions.setVisible(false);
        gameOverActions.setManaged(false);
        boardPane.getChildren().addAll(endTurnButton, gameOverActions);
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

    // Places short in-game help text near the relevant frame.
    private void configureHintLabel(Label label, double x, double y, double width) {
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefWidth(width);
        label.setTextFill(javafx.scene.paint.Color.web("rgba(255, 235, 180, 0.82)"));
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        label.setStyle("-fx-background-color: rgba(0,0,0,0.38); -fx-background-radius: 7;"
                + "-fx-padding: 2 7 2 7;");
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

    // Groups all UI nodes associated with one opponent slot.
    private static class PlayerZone {
        final BoardLayoutConfig.ZoneSpec spec;
        final Pane cards = new Pane();
        final Label name = new Label();
        final Label stats = new Label();
        final ProgressBar setsProgress = new ProgressBar(0);
        final Label setsLabel = new Label();

        PlayerZone(BoardLayoutConfig.ZoneSpec spec) {
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
            handleGameOverOnce();
            return;
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
            TableCardRenderer.render(zone.cards, player, OPPONENT_CARD_WIDTH, OPPONENT_CARD_HEIGHT, false);
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
            cardView.setOnMouseClicked(event -> handleHandCardClick(event, cardView, index, cards));
            handView.getChildren().add(cardView);
        }
    }

    // Records the winner once and returns to the correct post-game screen.
    private void handleGameOverOnce() {
        winPopupShown = true;
        Player winner = game.getWinner();
        String winnerName = winner == null ? "No winner" : winner.getPlayerName();
        String recordMessage;
        try {
            Path path = MatchHistoryRecorder.recordVictory(winner, game.getActivePlayers(), modeConfig.mode);
            recordMessage = "Match record exported to:\n" + path;
        } catch (Exception ex) {
            recordMessage = "Could not export match record: " + ex.getMessage();
        }
        GameDialogs.showMessage("Game Over",
                winnerName + " Wins!",
                (winner == null ? "The game ended." : "Collected 3 complete property sets.")
                        + "\n\n" + recordMessage);
        newGameAction.run();
    }

    // Renders the local table area for the visible player.
    private void renderOwnTable(Player current) {
        TableCardRenderer.render(ownTableView, current, OWN_TABLE_CARD_WIDTH, OWN_TABLE_CARD_HEIGHT, true);
    }

    // Routes hand-card clicks to preview or action menu behavior.
    private void handleHandCardClick(javafx.scene.input.MouseEvent event, CardView cardView,
                                     int index, List<Card> cards) {
        if (index < 0 || index >= cards.size()) {
            return;
        }
        cardView.toFront();
        if (event.getButton() == MouseButton.SECONDARY || event.getClickCount() >= 2) {
            CardPreviewDialog.show(cards.get(index));
            event.consume();
            return;
        }
        if (!game.isGameOver() && canControlCurrentPlayer() && getViewPlayer() == game.getCurrentPlayer()) {
            showCardMenu(cardView, index, cards.get(index));
            event.consume();
        }
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

    // Chooses which player's hand/table this machine is allowed to see.
    private Player getViewPlayer() {
        if (modeConfig.isNetwork()) {
            List<Player> players = game.getActivePlayers();
            int index = Math.max(0, Math.min(localPlayerIndex, players.size() - 1));
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
        return index == localPlayerIndex;
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
        String payload = "index=" + cardIndex + ";card="
                + NetworkActionCodec.safe(card == null ? "" : card.getCardName());
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
        String payload = "index=" + cardIndex + ";card="
                + NetworkActionCodec.safe(card == null ? "" : card.getCardName());
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
        String payload = NetworkActionCodec.encodePlayPayload(cardIndex, card, targetInfo,
                game.getActivePlayers());
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
        String payload = NetworkActionCodec.encodeDoubleRentPayload(doubleCardIndex, doubleCard,
                rentCardIndex, rentCard, targetInfo, game.getActivePlayers());
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

    // Handles the leave-match image button.
    private void requestLeaveMatch() {
        if (modeConfig.isNetwork()) {
            boolean leave = GameDialogs.showConfirmation("Leave Match",
                    "Leave this online match?",
                    "Other online players will be notified and can continue with the remaining seats.");
            if (!leave) {
                return;
            }
            Player local = getViewPlayer();
            sendNetworkAction("LEAVE_MATCH",
                    "index=" + localPlayerIndex + ";name=" + NetworkActionCodec.safe(local.getPlayerName()));
            newGameAction.run();
            return;
        }

        boolean leave = GameDialogs.showConfirmation("Leave Match",
                "Leave this local match?",
                "This ends the current match for all local players and returns to the start menu.");
        if (leave) {
            newGameAction.run();
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
            networkActionApplier.apply(message);
        } catch (Exception ex) {
            onGameEvent("[Network] Could not apply remote action: " + ex.getMessage());
        } finally {
            applyingRemoteAction = false;
        }
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

    // Shows every player's banked card counts and money total.
    private void showBankSummary() {
        GameDialogs.showMessage("Bank", "All players' bank assets",
                TableSummaryFormatter.bankSummary(game.getActivePlayers()));
    }

    // Shows every player's property counts and set progress.
    private void showPropertySummary() {
        GameDialogs.showMessage("Properties", "All players' property progress",
                TableSummaryFormatter.propertySummary(game.getActivePlayers()));
    }

    // Opens the per-card action menu for the current player's hand.
    private void showCardMenu(CardView owner, int cardIndex, Card card) {
        ContextMenu menu = HandCardMenuFactory.create(cardIndex, card, new HandCardMenuFactory.Handler() {
            @Override
            public void deposit(int selectedIndex) {
                requestDeposit(selectedIndex);
            }

            @Override
            public void play(int selectedIndex, Card selectedCard) {
                handlePlayMenuAction(selectedIndex, selectedCard);
            }

            @Override
            public void discard(int selectedIndex) {
                requestDiscard(selectedIndex);
            }

            @Override
            public boolean discardRequired() {
                return game.getCurrentPlayer().getHand().requiresDiscard();
            }
        });
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    // Runs the pre-play dialogs and then executes the selected card.
    private void handlePlayMenuAction(int cardIndex, Card card) {
        if (card instanceof cards.DoubleTheRentCard) {
            CardPlayPlanner.DoubleRentSelection selection = playPlanner.prepareDoubleRent(cardIndex);
            if (!selection.isCancelled()) {
                requestDoubleRent(cardIndex, selection.getRentCardIndex(), selection.getTargetInfo());
            }
            return;
        }

        CardPlayPlanner.PlaySelection selection = playPlanner.prepareNormalPlay(card);
        if (!selection.isCancelled()) {
            requestPlayCard(cardIndex, selection.getTargetInfo());
        }
    }

    // Lets a human choose payment cards, while AI and network seats pay automatically.
    private List<Card> choosePaymentCardsForPayment(Player payer, Player payee, int amount,
                                                    List<Card> bankCards,
                                                    List<cards.PropertyCard> propertyCards) {
        return PaymentSelectionDialog.choose(payer, payee, amount, bankCards, propertyCards,
                payer.isAI() || modeConfig.isNetwork());
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
                interruptController.handleInterruptRequest();
            }
        });
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
