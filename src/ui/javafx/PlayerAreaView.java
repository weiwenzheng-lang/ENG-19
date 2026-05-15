package ui.javafx;

import enums.PropertyColor;
import player.Player;
import player.Rentable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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

    public void render(Player player, boolean compact) {
        nameLabel.setText("TERMINAL_ID: " + player.getPlayerName().toUpperCase());

        // 高亮显示关键数据
        statsLabel.setText(String.format("BANK: %dM  |  ASSET SETS: %d/3  |  HAND: %d",
                player.getBankArea().calculateTotalFunds(),
                player.getPropertyArea().countCompletedSets(),
                player.getHand().getSize()));

        properties.getChildren().clear();
        for (Map.Entry<PropertyColor, Rentable> entry : player.getPropertyArea().getPropertySets().entrySet()) {
            VBox setBox = new VBox(5);
            setBox.setPadding(new Insets(8));
            // 资产包样式：左侧带颜色指示条的深色卡片
            setBox.setStyle("-fx-background-color: #1c212b; -fx-background-radius: 6; "
                    + "-fx-border-color: " + entry.getKey().getColorHex() + "; -fx-border-width: 0 0 0 5;");

            Label title = new Label(entry.getKey().toString() + " SET");
            title.setTextFill(Color.WHITE);
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

            Label desc = new Label(entry.getValue().getDescription());
            desc.setTextFill(Color.web("#8da0b3"));
            desc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));

            Label rent = new Label("RENT: ¥" + entry.getValue().calculateRent() + "M");
            rent.setTextFill(Color.web("#ff007f"));
            rent.setFont(Font.font("Consolas", FontWeight.BOLD, 12));

            setBox.getChildren().addAll(title, desc, rent);
            properties.getChildren().add(setBox);
        }
    }
}