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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
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
import java.util.Collections;
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
    private final CardView drawPileView = CardView.back(0);
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
        player.BankArea.setPaymentResolver(this::choosePaymentCardsForPayment);
        game.addObserver(this);
        game.initializeGame(playerNames);
    }

    public BorderPane createContent() {
        opponents.setPadding(new Insets(10));
        opponents.setAlignment(Pos.CENTER);
        opponents.setStyle("-fx-background-color: #111827; "
                + "-fx-border-color: transparent transparent rgba(0,242,255,0.12) transparent;"
                + "-fx-border-width: 0 0 1 0;");
        root.setStyle("-fx-background-color: #0d0f12;"); // 璋冩暣涓哄拰 css 涓€鑷寸殑鏆楅粦鑳屾櫙
        ScrollPane opponentScroll = new ScrollPane(opponents);
        opponentScroll.setFitToHeight(true);
        opponentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        opponentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        opponentScroll.setMaxHeight(190);
        opponentScroll.setStyle("-fx-background-color: #1d2b33; -fx-background: #1d2b33;");
        root.setTop(opponentScroll);
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

        VBox center = new VBox(14, title("Table"), turnLabel, deckLabel, discardLabel,
                piles, endTurnButton, gameOverActions);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(18));
        center.setStyle("-fx-background-color: #0f1722;"
                + "-fx-border-color: #2a3040; -fx-border-width: 0 1 0 1;");
        styleInfoLabel(turnLabel, "#f8fbf6");
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
        turnLabel.setText("Current: " + current.getPlayerName()
                + " | Actions: " + game.getActionsRemaining()
                + " | Sets: " + current.getPropertyArea().countCompletedSets() + "/3");
        deckLabel.setText("Draw pile: " + game.getGameDeck().getDrawPileSize());
        drawPileView.getChildren().clear();
        drawPileView.getChildren().add(CardView.back(game.getGameDeck().getDrawPileSize()));
        Card discardTop = game.getGameDeck().peekDiscardTop();
        discardLabel.setText("Discard: " + (discardTop == null ? "Empty" : discardTop.getCardName()));

        // Update discard pile visual
        discardPileView.getChildren().clear();
        if (discardTop != null) {
            discardPileView.getChildren().add(new CardView(discardTop));
        } else {
            discardPileView.getChildren().add(new CardView("Discard", ""));
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
            if (card instanceof cards.DoubleTheRentCard) {
                playDoubleRent(cardIndex);
                renderAll();
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
            game.executePlayerAction(cardIndex, targetInfo);
            renderAll();
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
        ChoiceDialog<enums.PropertyColor> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Select Color");
        dialog.setHeaderText("Choose the color for this card");
        dialog.setContentText("Color:");

        Optional<enums.PropertyColor> result = dialog.showAndWait();
        return result.orElse(null);
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
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(choices.get(0), FXCollections.observableArrayList(choices));
        dialog.setTitle("Choose Target");
        dialog.setHeaderText("Select a player to perform the action on");
        dialog.setContentText("Target:");
        Optional<Player> selected = dialog.showAndWait();
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
        ChoiceDialog<enums.PropertyColor> dialog = new ChoiceDialog<>(colors.get(0), colors);
        dialog.setTitle("Choose Property Set");
        dialog.setHeaderText("Select a set");
        dialog.setContentText("Set:");
        Optional<enums.PropertyColor> selected = dialog.showAndWait();
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
        ChoiceDialog<PropertyPick> dialog = new ChoiceDialog<>(picks.get(0), picks);
        dialog.setTitle(title);
        dialog.setHeaderText(owner.getPlayerName());
        dialog.setContentText("Property:");
        return dialog.showAndWait().orElse(null);
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
        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Double The Rent");
        dialog.setHeaderText("Choose a rent card to play with it");
        dialog.setContentText("Rent:");
        Optional<String> selected = dialog.showAndWait();
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
        game.executeDoubleRentAction(doubleCardIndex, rentCardIndex, targetInfo);
    }

    private List<Card> choosePaymentCardsForPayment(Player payer, Player payee, int amount,
                                                    List<Card> bankCards,
                                                    List<cards.PropertyCard> propertyCards) {
        java.util.List<Card> options = new java.util.ArrayList<>();
        options.addAll(bankCards);
        options.addAll(propertyCards);
        if (options.isEmpty()) {
            return Collections.emptyList();
        }

        int totalAvailable = options.stream().mapToInt(Card::getMonetaryValue).sum();
        Dialog<List<Card>> dialog = new Dialog<>();
        dialog.setTitle("Payment");
        dialog.setHeaderText(payer.getPlayerName() + " owes " + payee.getPlayerName() + " " + amount + "M");

        VBox content = new VBox(8);
        content.setPadding(new Insets(8));
        Label selectedTotal = new Label();
        java.util.List<CheckBox> boxes = new java.util.ArrayList<>();

        for (Card card : options) {
            boolean fromBank = bankCards.contains(card);
            CheckBox box = new CheckBox((fromBank ? "Bank: " : "Property: ")
                    + card.getCardName() + " (" + card.getMonetaryValue() + "M)");
            box.setWrapText(true);
            boxes.add(box);
            content.getChildren().add(box);
        }
        content.getChildren().add(selectedTotal);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
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
            if (button != ButtonType.OK) {
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
        player.BankArea.setPaymentResolver(null);
        game.removeObserver(this);
    }
}
