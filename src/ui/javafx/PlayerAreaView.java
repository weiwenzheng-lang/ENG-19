package ui.javafx;

import player.HotelDecorator;
import player.HouseDecorator;
import player.Player;
import player.PropertySet;
import player.Rentable;
import player.SetDecorator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PlayerAreaView extends VBox {
    private final Label nameLabel = new Label();
    private final Label statsLabel = new Label();
    private final FlowPane properties = new FlowPane(10, 10);

    // Creates the reusable player summary panel.
    public PlayerAreaView() {
        setSpacing(10);
        setPadding(new Insets(15));

        nameLabel.setTextFill(Color.web("#00f2ff"));
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BLACK, 18));

        statsLabel.setTextFill(Color.web("#a0aeb2"));
        statsLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));

        properties.setPrefWrapLength(450);
        getChildren().addAll(nameLabel, statsLabel, properties);

        setStyle("-fx-background-color: rgba(20, 25, 35, 0.85); "
                + "-fx-background-radius: 12; "
                + "-fx-border-color: rgba(0, 242, 255, 0.4); "
                + "-fx-border-radius: 12; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 5);");
    }

    // Returns the undecorated property set.
    private PropertySet getRoot(Rentable rentable) {
        if (rentable instanceof SetDecorator) {
            return ((SetDecorator) rentable).getRootSet();
        }
        if (rentable instanceof PropertySet) {
            return (PropertySet) rentable;
        }
        return null;
    }

    // Renders a player summary without an action counter.
    public void render(Player player, boolean compact) {
        render(player, compact, -1);
    }

    // Renders player bank, hand, and property-set progress.
    public void render(Player player, boolean compact, int actionsRemaining) {
        renderHeader(player, actionsRemaining);
        renderPropertySets(player);
    }

    // Updates player name and compact stats text.
    private void renderHeader(Player player, int actionsRemaining) {
        nameLabel.setText("TERMINAL_ID: " + player.getPlayerName().toUpperCase());

        String actionText = actionsRemaining >= 0
                ? String.format("ACTIONS: %d  |  ", actionsRemaining)
                : "";
        statsLabel.setText(String.format("%sBANK: %dM  |  ASSET SETS: %d/3  |  HAND: %d",
                actionText,
                player.getBankArea().calculateTotalFunds(),
                player.getPropertyArea().countCompletedSets(),
                player.getHand().getSize()));
    }

    // Rebuilds every visible property-set summary.
    private void renderPropertySets(Player player) {
        properties.getChildren().clear();
        for (player.PropertyArea.PropertySetEntry entry : player.getPropertyArea().getPropertySetEntries()) {
            properties.getChildren().add(createPropertySetBox(entry));
        }
    }

    // Creates one property-set summary box.
    private VBox createPropertySetBox(player.PropertyArea.PropertySetEntry entry) {
        VBox setBox = new VBox(5);
        setBox.setPadding(new Insets(8));
        setBox.setStyle("-fx-background-color: #1c212b; -fx-background-radius: 6; "
                + "-fx-border-color: " + entry.getColor().getColorHex() + "; -fx-border-width: 0 0 0 5;");

        Rentable rentable = entry.getRentable();
        PropertySet root = getRoot(rentable);
        int cardCount = root != null ? root.getCardsCount() : 0;
        int required = entry.getColor().getRequiredCount();
        double ratio = Math.min(1.0, (double) cardCount / required);

        setBox.getChildren().addAll(
                createSetTitle(entry),
                createProgressBar(entry.getColor().getColorHex(), ratio),
                createSetDescription(rentable, cardCount, required),
                createRentLabel(rentable));
        return setBox;
    }

    // Creates the property-set title label.
    private Label createSetTitle(player.PropertyArea.PropertySetEntry entry) {
        Label title = new Label(entry.getColor().toString() + " SET");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        return title;
    }

    // Creates the set size and building label.
    private Label createSetDescription(Rentable rentable, int cardCount, int required) {
        Label desc = new Label(cardCount + "/" + required + buildingLabel(rentable));
        desc.setTextFill(Color.web("#8da0b3"));
        desc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        return desc;
    }

    // Returns the building suffix for decorated sets.
    private String buildingLabel(Rentable rentable) {
        if (rentable instanceof HotelDecorator) {
            return " [Hotel]";
        }
        if (rentable instanceof HouseDecorator) {
            return " [House]";
        }
        return "";
    }

    // Creates the current rent label.
    private Label createRentLabel(Rentable rentable) {
        Label rent = new Label("RENT: " + rentable.calculateRent() + "M");
        rent.setTextFill(Color.web("#ff007f"));
        rent.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        return rent;
    }

    // Creates a fixed-size set progress bar.
    private StackPane createProgressBar(String color, double ratio) {
        StackPane progressBar = new StackPane();
        progressBar.setPrefSize(120, 8);
        progressBar.setMinSize(120, 8);
        progressBar.setMaxSize(120, 8);

        Region progressBg = new Region();
        progressBg.setMinSize(120, 8);
        progressBg.setMaxSize(120, 8);
        progressBg.setStyle("-fx-background-color: #2a3040; -fx-background-radius: 4;");

        Region progressFill = new Region();
        double fillW = Math.max(0, 120 * ratio);
        progressFill.setMinSize(fillW, 8);
        progressFill.setMaxSize(fillW, 8);
        progressFill.setStyle("-fx-background-color: " + (ratio >= 1.0 ? "#00ff9f" : color)
                + "; -fx-background-radius: 4;");
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);
        progressBar.getChildren().addAll(progressBg, progressFill);
        return progressBar;
    }
}
