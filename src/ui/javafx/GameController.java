package ui.javafx;

import cards.Card;
import cards.RentCard;
import core.GameManager;
import core.TargetInfo;
import patterns.observer.GameObserver;
import player.Player;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GameController implements GameObserver {
    private final GameManager game = GameManager.getInstance();
    private final BorderPane root = new BorderPane();
    private final HBox opponents = new HBox(10);
    private final FlowPane handView = new FlowPane(8, 8);
    private final FlowPane bankView = new FlowPane(6, 6);
    private final FlowPane propertyView = new FlowPane(8, 8);
    private final ListView<String> logView = new ListView<>();
    private final Label turnLabel = new Label("Turn");
    private final Label deckLabel = new Label("Deck");
    private final Label discardLabel = new Label("Discard");
    private final CardView discardPileView = new CardView("Discard", "");
    private final Button endTurnButton = new Button("End Turn");
    private final HBox gameOverActions = new HBox(10);
    private final Runnable newGameAction;
    private final Runnable exitGameAction;

    public GameController(List<String> playerNames) {
        this(playerNames, () -> {}, Platform::exit);
    }

    public GameController(List<String> playerNames, Runnable newGameAction, Runnable exitGameAction) {
        this.newGameAction = newGameAction;
        this.exitGameAction = exitGameAction;
        game.addObserver(this);
        game.initializeGame(playerNames);
    }

    public GameController() {
        this(Arrays.asList("Player A", "Player B", "Player C"));
    }

    public BorderPane createContent() {
        opponents.setPadding(new Insets(10));
        opponents.setAlignment(Pos.CENTER);
        opponents.setStyle("-fx-background-color: #1d2b33; -fx-border-color: #43545f;"
                + "-fx-border-width: 0 0 1 0;");
        root.setStyle("-fx-background-color: #132127;");
        root.setTop(opponents);
        root.setCenter(createCenter());
        root.setBottom(createPlayerPanel());
        root.setRight(createLogPanel());
        renderAll();
        return root;
    }

    private VBox createCenter() {
        endTurnButton.setOnAction(event -> {
            game.endTurn();
            renderAll();
        });
        styleButton(endTurnButton);

        Button newGame = new Button("开始新游戏");
        newGame.setOnAction(event -> newGameAction.run());
        styleButton(newGame);

        Button exitGame = new Button("退出游戏");
        exitGame.setOnAction(event -> exitGameAction.run());
        styleButton(exitGame);

        gameOverActions.getChildren().setAll(exitGame, newGame);
        gameOverActions.setAlignment(Pos.CENTER);
        gameOverActions.setVisible(false);
        gameOverActions.setManaged(false);

        HBox piles = new HBox(18,
                CardView.back(game.getGameDeck().getDrawPileSize()),
                discardPileView);
        piles.setAlignment(Pos.CENTER);

        VBox center = new VBox(14, title("Table"), turnLabel, deckLabel, discardLabel,
                piles, endTurnButton, gameOverActions);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(18));
        center.setStyle("-fx-background-color: #23343b;"
                + "-fx-border-color: #58716f; -fx-border-width: 0 1 0 1;");
        styleInfoLabel(turnLabel, "#f8fbf6");
        styleInfoLabel(deckLabel, "#cfe7dd");
        styleInfoLabel(discardLabel, "#ead8b3");
        return center;
    }

    private ScrollPane createPlayerPanel() {
        Label hand = sectionLabel("Hand", "#ffd7a8");
        Label bank = sectionLabel("Bank", "#ccecc3");
        Label property = sectionLabel("Properties", "#cbd8ff");

        VBox handColumn = new VBox(8, hand, handView);
        VBox assetColumn = new VBox(8, bank, bankView, property, propertyView);
        HBox content = new HBox(12, handColumn, assetColumn);
        HBox.setHgrow(handColumn, Priority.ALWAYS);
        HBox.setHgrow(assetColumn, Priority.ALWAYS);
        handColumn.setMinWidth(380);
        assetColumn.setMinWidth(320);

        VBox panel = new VBox(content);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #17262d; -fx-border-color: #314a55;"
                + "-fx-border-width: 1 0 0 0;");
        panel.setFillWidth(true);
        handView.setStyle("-fx-background-color: rgba(255,215,168,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        bankView.setStyle("-fx-background-color: rgba(204,236,195,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        propertyView.setStyle("-fx-background-color: rgba(203,216,255,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        VBox.setVgrow(propertyView, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefViewportHeight(300);
        scroll.setMaxHeight(340);
        scroll.setStyle("-fx-background-color: #17262d; -fx-background: #17262d;"
                + "-fx-border-color: #314a55; -fx-border-width: 1 0 0 0;");
        return scroll;
    }

    private VBox createLogPanel() {
        Label logTitle = sectionLabel("Log", "#f7d0d7");
        VBox panel = new VBox(8, logTitle, logView);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: #202b35; -fx-border-color: #3f5262;"
                + "-fx-border-width: 0 0 0 1;");
        logView.setStyle("-fx-control-inner-background: #f4ead8;"
                + "-fx-background-color: #f4ead8; -fx-background-radius: 8;"
                + "-fx-border-color: #c8a96b; -fx-border-radius: 8;"
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");
        VBox.setVgrow(logView, Priority.ALWAYS);
        return panel;
    }

    private boolean winPopupShown = false;

    private void renderAll() {
        Player current = game.getCurrentPlayer();
        turnLabel.setText("Current: " + current.getPlayerName()
                + " | Actions: " + game.getActionsRemaining()
                + " | Sets: " + current.getPropertyArea().countCompletedSets() + "/3");
        deckLabel.setText("Draw pile: " + game.getGameDeck().getDrawPileSize());
        Card discardTop = game.getGameDeck().peekDiscardTop();
        discardLabel.setText("Discard: " + (discardTop == null ? "Empty" : discardTop.getCardName()));

        // Update discard pile visual
        if (discardTop != null) {
            discardPileView.getChildren().clear();
            discardPileView.getChildren().add(new CardView(discardTop));
        }

        boolean gameOver = game.isGameOver();
        endTurnButton.setDisable(gameOver);
        gameOverActions.setVisible(gameOver);
        gameOverActions.setManaged(gameOver);

        if (gameOver && !winPopupShown) {
            winPopupShown = true;
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(current.getPlayerName() + " Wins!");
            alert.setContentText("Collected 3 complete property sets!");
            alert.show();
        }

        renderOpponents(current);
        renderHand(current);
        renderBank(current);
        renderProperties(current);
    }

    private void renderOpponents(Player current) {
        opponents.getChildren().clear();
        for (Player player : game.getActivePlayers()) {
            if (player == current) continue;
            PlayerAreaView view = new PlayerAreaView();
            view.render(player, true);
            opponents.getChildren().add(view);
        }
    }

    private void renderHand(Player current) {
        handView.getChildren().clear();
        List<Card> cards = current.getHand().getCards();
        for (int i = 0; i < cards.size(); i++) {
            int index = i;
            CardView cardView = new CardView(cards.get(i));
            if (!game.isGameOver()) {
                cardView.setOnMouseClicked(event -> showCardMenu(cardView, index, cards.get(index)));
            }
            handView.getChildren().add(cardView);
        }
    }

    private void showCardMenu(CardView owner, int cardIndex, Card card) {
        ContextMenu menu = new ContextMenu();

        MenuItem bank = new MenuItem("Deposit to bank");
        bank.setOnAction(event -> {
            game.depositCardToBank(cardIndex);
            renderAll();
        });

        MenuItem discard = new MenuItem("Discard");
        discard.setOnAction(event -> {
            game.discardCard(cardIndex);
            renderAll();
        });

        MenuItem play = new MenuItem("Play card");
        play.setOnAction(event -> {
            if (card instanceof cards.RentCard && ((cards.RentCard) card).isMultiColor()) {
                cards.RentCard rentCard = (cards.RentCard) card;
                enums.PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
                if (selectedColor == null) {
                    onGameEvent("取消使用 " + card.getCardName());
                    return;
                }
                rentCard.setSelectedColor(selectedColor);
            } else if (card instanceof cards.SuperWildCard || card instanceof cards.PropertyWildCard) {
                // 获取可选颜色
                enums.PropertyColor[] options;
                if (card instanceof cards.SuperWildCard) {
                    options = ((cards.SuperWildCard) card).getAvailableColors();
                } else {
                    options = ((cards.PropertyWildCard) card).getAvailableColors();
                }

                // 弹出选择框
                enums.PropertyColor selectedColor = chooseColor(options);

                if (selectedColor == null) {
                    onGameEvent("取消使用 " + card.getCardName());
                    return;
                }

                // 调用各自的 setCurrentColor
                if (card instanceof cards.SuperWildCard) {
                    ((cards.SuperWildCard) card).setCurrentColor(selectedColor);
                } else {
                    ((cards.PropertyWildCard) card).setCurrentColor(selectedColor);
                }
            }
            // 只有需要目标的卡才弹窗选人，否则直接打出
            if (needsTarget(card)) {
                TargetInfo target = chooseTarget();
                if (target == null) {
                    // 用户取消了选择，不执行任何操作
                    onGameEvent("取消使用 " + card.getCardName());
                    return;
                }
                game.executePlayerAction(cardIndex, target);
            } else {
                game.executePlayerAction(cardIndex, null);
            }
            renderAll();
        });

        menu.getItems().addAll(bank, discard, play);
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    /**
     * 判断卡牌是否需要选择目标（单个对手）
     * 注意：RentCard 不需要选人，它会自动向所有对手收租
     */
    private boolean needsTarget(Card card) {
        return card.requiresTarget();
    }
    /**
     * 支持不同数量的选项（2个或10个）
     * @return 玩家选中的颜色，如果取消则返回 null
     */
    private enums.PropertyColor chooseColor(enums.PropertyColor[] colorOptions) {
        List<enums.PropertyColor> options = java.util.Arrays.asList(colorOptions);
        ChoiceDialog<enums.PropertyColor> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Select Color");
        dialog.setHeaderText("Choose the color for this card");
        dialog.setContentText("Color:");

        Optional<enums.PropertyColor> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private TargetInfo chooseTarget() {
        List<Player> choices = game.getOpponents(game.getCurrentPlayer());
        if (choices.isEmpty()) {
            onGameEvent("没有可选的对手");
            return null;
        }
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(choices.get(0), FXCollections.observableArrayList(choices));
        dialog.setTitle("Choose Target");
        dialog.setHeaderText("Select a player to perform the action on");
        dialog.setContentText("Target:");
        Optional<Player> selected = dialog.showAndWait();
        return selected.map(TargetInfo::new).orElse(null);
    }

    private void renderBank(Player current) {
        bankView.getChildren().clear();
        current.getBankArea().getAssets().forEach(card -> bankView.getChildren().add(new CardView(card)));
    }

    private void renderProperties(Player current) {
        propertyView.getChildren().clear();
        PlayerAreaView view = new PlayerAreaView();
        view.render(current, false);
        propertyView.getChildren().add(view);
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web("#f7efe1"));
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
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
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
    }

    private void styleButton(Button button) {
        button.setTextFill(javafx.scene.paint.Color.web("#1b2a31"));
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setStyle("-fx-background-color: #f0c978; -fx-background-radius: 8;"
                + "-fx-border-color: #ffe0a1; -fx-border-radius: 8;"
                + "-fx-padding: 8 18 8 18;");
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

        List<Card> hand = victim.getHand().getCards();
        int jsnIdx = -1;
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getCardName().equals("Just Say No")) {
                jsnIdx = i;
                break;
            }
        }
        final int jsnIndex = jsnIdx;

        Alert alert = new Alert(
                jsnIndex >= 0
                        ? Alert.AlertType.CONFIRMATION
                        : Alert.AlertType.INFORMATION);
        alert.setTitle("Counter Action");
        alert.setHeaderText(victim.getPlayerName() + " is under attack!");

        if (jsnIndex >= 0) {
            alert.setContentText("Use Just Say No to counter?");
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    game.counterAttackWithJustSayNo(jsnIndex);
                } else {
                    game.resolvePendingAction();
                }
                renderAll();
            });
        } else {
            alert.setContentText("No Just Say No available. The action will proceed.");
            alert.showAndWait();
            game.resolvePendingAction();
            renderAll();
        }
    }

    @Override
    public void onTurnChanged(String playerName) {
        Platform.runLater(() -> {
            logView.getItems().add(0, "Turn starts: " + playerName);
            renderAll();
        });
    }

    public void dispose() {
        game.removeObserver(this);
    }
}
