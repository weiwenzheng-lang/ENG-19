package ui.javafx;

import cards.Card;
import cards.MoneyCard;
import cards.RentCard;
import core.GameManager;
import core.TargetInfo;
import patterns.observer.GameObserver;
import player.Player;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
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

    public BorderPane createContent() {
        game.addObserver(this);
        game.initializeGame(Arrays.asList("Player A", "Player B", "Player C"));

        opponents.setPadding(new Insets(10));
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

        VBox center = new VBox(12, turnLabel, deckLabel, discardLabel, endTurn);
        center.setPadding(new Insets(16));
        center.setStyle("-fx-background-color: #f6f8f9;");
        return center;
    }

    private VBox createPlayerPanel() {
        Label hand = new Label("Hand");
        Label bank = new Label("Bank");
        Label property = new Label("Properties");
        VBox panel = new VBox(8, hand, handView, bank, bankView, property, propertyView);
        panel.setPadding(new Insets(12));
        return panel;
    }

    private VBox createLogPanel() {
        VBox panel = new VBox(8, new Label("Log"), logView);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(280);
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
            if (player == current) {
                continue;
            }
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
