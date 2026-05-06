package ui.javafx;

import enums.PropertyColor;
import player.Player;
import player.Rentable;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.Map;

public class PlayerAreaView extends VBox {
    private final Label title = new Label();
    private final FlowPane properties = new FlowPane(6, 6);

    public PlayerAreaView() {
        setSpacing(6);
        setPadding(new Insets(8));
        properties.setPrefWrapLength(360);
        getChildren().addAll(title, properties);
        setStyle("-fx-border-color: #b0bec5; -fx-border-radius: 6;");
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
            set.setPadding(new Insets(6));
            set.setStyle("-fx-background-color: #eceff1; -fx-border-color: #90a4ae; -fx-border-radius: 4;");
            properties.getChildren().add(set);
        }
    }
}
