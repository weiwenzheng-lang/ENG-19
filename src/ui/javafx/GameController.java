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
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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

    // 新构造方法：接收动态玩家名称列表
    public GameController(List<String> playerNames) {
        game.addObserver(this);
        game.initializeGame(playerNames);
    }

    // 保留无参构造（兼容性，默认 3 人）
    public GameController() {
        this(List.of("Player A", "Player B", "Player C"));
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
        Button endTurn = new Button("End Turn");
        endTurn.setOnAction(event -> {
            game.endTurn();
            renderAll();
        });
        styleButton(endTurn);

        HBox piles = new HBox(18,
                CardView.back(game.getGameDeck().getDrawPileSize()),
                new CardView("Discard", "Top card"));
        piles.setAlignment(Pos.CENTER);

        VBox center = new VBox(14, title("Table"), turnLabel, deckLabel, discardLabel, piles, endTurn);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(18));
        center.setStyle("-fx-background-color: #23343b;"
                + "-fx-border-color: #58716f; -fx-border-width: 0 1 0 1;");
        styleInfoLabel(turnLabel, "#f8fbf6");
        styleInfoLabel(deckLabel, "#cfe7dd");
        styleInfoLabel(discardLabel, "#ead8b3");
        return center;
    }

    private VBox createPlayerPanel() {
        Label hand = sectionLabel("Hand", "#ffd7a8");
        Label bank = sectionLabel("Bank", "#ccecc3");
        Label property = sectionLabel("Properties", "#cbd8ff");
        VBox panel = new VBox(8, hand, handView, bank, bankView, property, propertyView);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #17262d; -fx-border-color: #314a55;"
                + "-fx-border-width: 1 0 0 0;");
        handView.setStyle("-fx-background-color: rgba(255,215,168,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        bankView.setStyle("-fx-background-color: rgba(204,236,195,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        propertyView.setStyle("-fx-background-color: rgba(203,216,255,0.08);"
                + "-fx-background-radius: 10; -fx-padding: 10;");
        return panel;
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

    private void renderAll() {
        Player current = game.getCurrentPlayer();
        turnLabel.setText("Current: " + current.getPlayerName()
                + " | Actions: " + game.getActionsRemaining());
        deckLabel.setText("Draw pile: " + game.getGameDeck().getDrawPileSize());
        Card discardTop = game.getGameDeck().peekDiscardTop();
        discardLabel.setText("Discard: " + (discardTop == null ? "Empty" : discardTop.getCardName()));

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
            cardView.setOnMouseClicked(event -> showCardMenu(cardView, index, cards.get(index)));
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

        MenuItem play = new MenuItem("Play card");
        play.setOnAction(event -> {
            TargetInfo target = needsTarget(card) ? chooseTarget() : null;
            game.executePlayerAction(cardIndex, target);
            renderAll();
        });

        menu.getItems().addAll(bank, play);
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    private boolean needsTarget(Card card) {
        return card instanceof RentCard
                || card.getCardName().equals("Sly Deal")
                || card.getCardName().equals("Forced Deal")
                || card.getCardName().equals("Deal Breaker")
                || card.getCardName().equals("Debt Collector");
    }

    private TargetInfo chooseTarget() {
        List<Player> choices = game.getOpponents(game.getCurrentPlayer());
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(choices.get(0), FXCollections.observableArrayList(choices));
        dialog.setTitle("Choose Target");
        dialog.setHeaderText(null);
        dialog.setContentText("Target player");
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
        });
    }

    @Override
    public void onTurnChanged(String playerName) {
        Platform.runLater(() -> {
            logView.getItems().add(0, "Turn starts: " + playerName);
            renderAll();
        });
    }
}