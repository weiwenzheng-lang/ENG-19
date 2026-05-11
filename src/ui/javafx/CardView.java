package ui.javafx;

import cards.Card;
import cards.MoneyCard;
import cards.PropertyCard;
import cards.SuperWildCard;
import cards.HouseCard;
import cards.ActionCard;
import cards.RentCard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;


public class CardView extends StackPane {
    public CardView(Card card) {
        this(card == null ? "Empty" : card.getCardName(),
                card == null ? "" : card.getMonetaryValue() + "M",
                styleFor(card));
    }

    public CardView(String title, String subtitle) {
        this(title, subtitle, "-fx-background-color: #f7efe1; -fx-border-color: #d8b46f;");
    }

    private CardView(String title, String subtitle, String style) {
        Label label = new Label(title + (subtitle == null || subtitle.trim().isEmpty() ? "" : "\n" + subtitle));
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextFill(javafx.scene.paint.Color.web("#21313c"));
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        setAlignment(Pos.CENTER);
        setPadding(new Insets(8));
        setPrefSize(96, 136);
        setMinSize(96, 136);
        setMaxSize(96, 136);
        getChildren().add(label);
        setStyle(style + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 10, 0.18, 0, 3);");
    }

    public static CardView back(int count) {
        return new CardView("Cards", String.valueOf(count),
                "-fx-background-color: #315a70; -fx-border-color: #b9d8e6;");
    }

    private static String styleFor(Card card) {
        if (card == null) return "-fx-background-color: #ffffff; -fx-border-color: #cccccc;";

        // 1. 特殊万能牌
        if (card instanceof cards.SuperWildCard) {
            return "-fx-background-color: linear-gradient(to bottom right, #ff9a9e, #fad0c4); -fx-border-color: #ff4d4d;";
        }
        if (card instanceof cards.PropertyWildCard) {
            return "-fx-background-color: #ffebcc; -fx-border-color: #ff9900; -fx-border-style: dashed;";
        }

        // 2. 房产卡 (加入颜色区分逻辑)
        if (card instanceof cards.PropertyCard) {
            String colorHex = "#c99f4f"; // 默认
            try {
                enums.PropertyColor color = ((cards.PropertyCard) card).getColorGroup();
                switch (color) {
                    case DARK_BLUE:
                        colorHex = "#0d47a1";
                        break;
                    case GREEN:
                        colorHex = "#2e7d32";
                        break;
                    case RED:
                        colorHex = "#c62828";
                        break;
                    case YELLOW:
                        colorHex = "#f9a825";
                        break;
                    case PINK:
                        colorHex = "#ad1457";
                        break;
                    case ORANGE:
                        colorHex = "#ef6c00";
                        break;
                    case LIGHT_BLUE:
                        colorHex = "#0288d1";
                        break;
                    case BROWN:
                        colorHex = "#4e342e";
                        break;
                    case RAILROAD:
                        colorHex = "#37474f";
                        break;
                    case UTILITY:
                        colorHex = "#558b2f";
                        break;
                    default:
                        colorHex = "#c99f4f";
                        break;
                }
            } catch (Exception e) {}
            return "-fx-background-color: #fdfaf0; -fx-border-color: " + colorHex + "; -fx-border-width: 5 0 0 0;";
        }

        // 3. 基础卡
        if (card instanceof cards.MoneyCard) {
            return "-fx-background-color: #e8f5e9; -fx-border-color: #4caf50;";
        }
        if (card instanceof cards.RentCard) {
            return "-fx-background-color: #e3f2fd; -fx-border-color: #1e88e5;";
        }

        // 4. 建筑与加倍 (新加的部分)
        if (card instanceof cards.HouseCard) {
            return "-fx-background-color: #e0f2f1; -fx-border-color: #009688;";
        }
        if (card instanceof cards.HotelCard) {
            return "-fx-background-color: #ffebee; -fx-border-color: #d32f2f; -fx-border-width: 3;";
        }
        if (card instanceof cards.DoubleTheRentCard) {
            return "-fx-background-color: #fffde7; -fx-border-color: #fbc02d; -fx-border-style: dashed;";
        }

        // 5. 具体行动卡
        if (card instanceof cards.JustSayNoCard) return "-fx-background-color: #fce4ec; -fx-border-color: #e91e63;";
        if (card instanceof cards.DealBreakerCard) return "-fx-background-color: #f3e5f5; -fx-border-color: #7b1fa2; -fx-border-width: 3;";
        if (card instanceof cards.SlyDealCard || card instanceof cards.ForceDealCard) return "-fx-background-color: #ede7f6; -fx-border-color: #5e35b1;";
        if (card instanceof cards.PassGoCard) return "-fx-background-color: #e0f7fa; -fx-border-color: #00acc1;";
        if (card instanceof cards.DebtCollectorCard || card instanceof cards.BirthdayCard) return "-fx-background-color: #fff3e0; -fx-border-color: #fb8c00;";

        // 6. 兜底
        return "-fx-background-color: #f5f5f5; -fx-border-color: #9e9e9e;";
    }
}
