package ui.javafx;

import cards.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

public class CardView extends StackPane {

    public CardView(Card card) {
        // 优化1：尺寸适当缩小，防止撑出界面的滚动条
        setPrefSize(100, 140);
        setMinSize(100, 140);
        setMaxSize(100, 140);

        // 卡牌主面板：毛玻璃+暗金质感
        VBox cardBody = new VBox();
        cardBody.setAlignment(Pos.TOP_CENTER);
        cardBody.setStyle("-fx-background-color: linear-gradient(to bottom right, #2a2d34, #141518); "
                + "-fx-background-radius: 10; "
                + "-fx-border-color: " + getAccentColor(card) + "; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 10;");

        // 增加阴影发光特效
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(getAccentColor(card)).deriveColor(1, 1, 1, 0.6));
        glow.setRadius(10);
        glow.setSpread(0.2);
        cardBody.setEffect(glow);

        // 顶部高亮条
        Region headerBar = new Region();
        headerBar.setPrefHeight(6); // 略微调细
        headerBar.setStyle("-fx-background-color: " + getAccentColor(card) + "; "
                + "-fx-background-radius: 8 8 0 0;");

        // 卡牌名称
        Label nameLabel = new Label(card.getCardName());
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); // 字体微调
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setPadding(new Insets(8, 3, 2, 3));

        // 卡牌类型标签
        Label typeLabel = new Label(getCardTypeShortName(card));
        typeLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        typeLabel.setTextFill(Color.web("#a0aab5"));

        // 优化2：动态弹簧！把价值文本永远“挤”到最下面，不用死板的 padding
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 卡牌价值 (底部发光的货币价值)
        Label valueLabel = new Label("¥ " + card.getMonetaryValue() + "M");
        valueLabel.setFont(Font.font("Consolas", FontWeight.BLACK, 14));
        valueLabel.setTextFill(Color.web("#00ff9f")); // 荧光绿
        valueLabel.setPadding(new Insets(0, 0, 8, 0)); // 只有底部留白

        // 注意这里加入了 spacer
        cardBody.getChildren().addAll(headerBar, nameLabel, typeLabel, spacer, valueLabel);

        // 鼠标悬停动画效果
        this.setOnMouseEntered(e -> {
            this.setTranslateY(-10);
            glow.setRadius(20);
        });
        this.setOnMouseExited(e -> {
            this.setTranslateY(0);
            glow.setRadius(10);
        });

        getChildren().add(cardBody);
    }

    // 重载用于背面/牌堆
    public CardView(String title, String subtitle) {
        setPrefSize(100, 140);
        setMinSize(100, 140);
        setMaxSize(100, 140);

        VBox cardBody = new VBox(8);
        cardBody.setAlignment(Pos.CENTER);
        cardBody.setStyle("-fx-background-color: repeating-linear-gradient(45deg, #111, #111 10px, #222 10px, #222 20px); "
                + "-fx-background-radius: 10; -fx-border-color: #00f2ff; -fx-border-width: 2; -fx-border-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#00f2ff"));
        titleLabel.setFont(Font.font("Consolas", FontWeight.BLACK, 14));

        // 优化3：修复了之前你代码里漏掉的 subtitle（剩余卡牌数）
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setTextFill(Color.web("#a0aab5"));
        subtitleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));

        cardBody.getChildren().addAll(titleLabel, subtitleLabel);
        getChildren().add(cardBody);
    }

    private String getAccentColor(Card card) {
        if (card instanceof PropertyCard) return ((PropertyCard) card).getColorGroup().getColorHex();
        if (card instanceof MoneyCard) return "#00ff9f"; // 财富绿
        if (card instanceof ActionCard) return "#ff007f"; // 赛博粉红
        return "#00f2ff";
    }

    private String getCardTypeShortName(Card card) {
        if (card instanceof PropertyWildCard) return "[WILD ASSET]";
        if (card instanceof PropertyCard) return "[ASSET]";
        if (card instanceof MoneyCard) return "[FUNDS]";
        if (card instanceof RentCard) return "[RENTAL]";
        if (card instanceof ActionCard) return "[ACTION]";
        return "[SYS]";
    }

    public static CardView back(int count) {
        return new CardView("HUB_DECK", count + " CARDS");
    }
}