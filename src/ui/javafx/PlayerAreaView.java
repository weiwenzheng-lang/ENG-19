package ui.javafx;

import enums.PropertyColor;
import player.Player;
import player.PropertySet;
import player.SetDecorator;
import player.HouseDecorator;
import player.HotelDecorator;
import player.Rentable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import java.util.Map;

public class PlayerAreaView extends VBox {
    private final Label nameLabel = new Label();
    private final Label statsLabel = new Label();
    private final FlowPane properties = new FlowPane(10, 10);

    public PlayerAreaView() {
        setSpacing(10);
        setPadding(new Insets(15));

        nameLabel.setTextFill(Color.web("#00f2ff"));
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BLACK, 18));

        statsLabel.setTextFill(Color.web("#a0aeb2"));
        statsLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));

        properties.setPrefWrapLength(450);
        getChildren().addAll(nameLabel, statsLabel, properties);

        // 玻璃拟态背景
        setStyle("-fx-background-color: rgba(20, 25, 35, 0.85); "
                + "-fx-background-radius: 12; "
                + "-fx-border-color: rgba(0, 242, 255, 0.4); "
                + "-fx-border-radius: 12; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 5);");
    }

    private PropertySet getRoot(Rentable r) {
        if (r instanceof SetDecorator) return ((SetDecorator) r).getRootSet();
        if (r instanceof PropertySet) return (PropertySet) r;
        return null;
    }

    public void render(Player player, boolean compact) {
        render(player, compact, -1);
    }

    public void render(Player player, boolean compact, int actionsRemaining) {
        nameLabel.setText("TERMINAL_ID: " + player.getPlayerName().toUpperCase());

        // 高亮显示关键数据
        String actionText = actionsRemaining >= 0
                ? String.format("ACTIONS: %d  |  ", actionsRemaining)
                : "";
        statsLabel.setText(String.format("%sBANK: %dM  |  ASSET SETS: %d/3  |  HAND: %d",
                actionText,
                player.getBankArea().calculateTotalFunds(),
                player.getPropertyArea().countCompletedSets(),
                player.getHand().getSize()));

        properties.getChildren().clear();
        for (Map.Entry<PropertyColor, Rentable> entry : player.getPropertyArea().getPropertySets().entrySet()) {
            VBox setBox = new VBox(5);
            setBox.setPadding(new Insets(8));
            setBox.setStyle("-fx-background-color: #1c212b; -fx-background-radius: 6; "
                    + "-fx-border-color: " + entry.getKey().getColorHex() + "; -fx-border-width: 0 0 0 5;");

            Label title = new Label(entry.getKey().toString() + " SET");
            title.setTextFill(Color.WHITE);
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

            // Progress bar — StackPane overlay for reliable rendering
            Rentable rentable = entry.getValue();
            PropertySet root = getRoot(rentable);
            int cardCount = root != null ? root.getCardsCount() : 0;
            int required = entry.getKey().getRequiredCount();
            double ratio = Math.min(1.0, (double) cardCount / required);

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
            progressFill.setStyle("-fx-background-color: " + (ratio >= 1.0 ? "#00ff9f" : entry.getKey().getColorHex())
                    + "; -fx-background-radius: 4;");
            StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);
            progressBar.getChildren().addAll(progressBg, progressFill);

            // Building icon
            String building = "";
            if (rentable instanceof HotelDecorator) building = " [Hotel]";
            else if (rentable instanceof HouseDecorator) building = " [House]";

            Label desc = new Label(cardCount + "/" + required + building);
            desc.setTextFill(Color.web("#8da0b3"));
            desc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));

            Label rent = new Label("RENT: " + rentable.calculateRent() + "M");
            rent.setTextFill(Color.web("#ff007f"));
            rent.setFont(Font.font("Consolas", FontWeight.BOLD, 12));

            setBox.getChildren().addAll(title, progressBar, desc, rent);
            properties.getChildren().add(setBox);
        }
    }
}
