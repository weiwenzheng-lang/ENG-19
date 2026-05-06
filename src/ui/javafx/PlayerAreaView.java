package ui.javafx;

import enums.PropertyColor;
import player.Player;
import player.Rentable;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

public class PlayerAreaView extends VBox {
    private final Label title = new Label();
    private final FlowPane properties = new FlowPane(6, 6);

    public PlayerAreaView() {
        setSpacing(6);
        setPadding(new Insets(10));
        properties.setPrefWrapLength(360);
        getChildren().addAll(title, properties);
        title.setTextFill(javafx.scene.paint.Color.web("#f7fbff"));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        setStyle("-fx-background-color: rgba(74,94,116,0.72);"
                + "-fx-border-color: rgba(211,228,236,0.5);"
                + "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 12, 0.18, 0, 3);");
    }

    public void render(Player player, boolean compact) {
        title.setText(player.getPlayerName()
                + " | Bank " + player.getBankArea().calculateTotalFunds() + "M"
                + " | Hand " + player.getHand().getSize());
        properties.getChildren().clear();
        for (Map.Entry<PropertyColor, Rentable> entry : player.getPropertyArea().getPropertySets().entrySet()) {
            Label set = new Label(entry.getKey() + "\n" + entry.getValue().getDescription());
            set.setWrapText(true);
            set.setPrefWidth(compact ? 132 : 160);
            set.setPadding(new Insets(7));
            set.setTextFill(javafx.scene.paint.Color.web("#20313d"));
            set.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 11));
            set.setStyle(propertyStyle(entry.getKey()));
            properties.getChildren().add(set);
        }
    }

    private String propertyStyle(PropertyColor color) {
        String fill = "#edf3f7";
        String border = "#a9c0cc";
        if (color == PropertyColor.BROWN) { fill = "#d8b08a"; border = "#8c5c37"; }
        else if (color == PropertyColor.LIGHT_BLUE) { fill = "#ccecff"; border = "#7abbd8"; }
        else if (color == PropertyColor.PINK) { fill = "#ffd6e9"; border = "#d583b0"; }
        else if (color == PropertyColor.ORANGE) { fill = "#ffd9aa"; border = "#d79445"; }
        else if (color == PropertyColor.RED) { fill = "#ffc5c5"; border = "#c96868"; }
        else if (color == PropertyColor.YELLOW) { fill = "#fff1a8"; border = "#c7ad38"; }
        else if (color == PropertyColor.GREEN) { fill = "#cfe8c9"; border = "#6fa865"; }
        else if (color == PropertyColor.DARK_BLUE) { fill = "#cbd8ff"; border = "#5c72bd"; }
        else if (color == PropertyColor.RAILROAD) { fill = "#e1e5e8"; border = "#87939b"; }
        else if (color == PropertyColor.UTILITY) { fill = "#d7efe8"; border = "#72aa98"; }
        return "-fx-background-color: " + fill + "; -fx-border-color: " + border
                + "; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;";
    }
}
