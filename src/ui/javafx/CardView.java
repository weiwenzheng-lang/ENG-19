package ui.javafx;

import cards.*;
import enums.PropertyColor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CardView extends StackPane {
    public static final double DEFAULT_CARD_WIDTH = 86;
    public static final double DEFAULT_CARD_HEIGHT = 142;
    private static final String CARD_IMAGE_RESOURCE_DIR = "/Card Library/Card Library/";
    private static final String CARD_IMAGE_FILE_DIR = "src/Card Library/Card Library";
    private final double cardWidth;
    private final double cardHeight;

    public CardView(Card card) {
        this(card, DEFAULT_CARD_WIDTH, DEFAULT_CARD_HEIGHT);
    }

    public CardView(Card card, double width, double height) {
        this.cardWidth = width;
        this.cardHeight = height;
        applyFixedCardSize();

        Image cardImage = loadCardImage(card);
        if (cardImage != null) {
            getChildren().add(createImageCard(cardImage, card));
            return;
        }

        getChildren().add(createFallbackCard(card));
    }

    private StackPane createImageCard(Image cardImage, Card card) {
        ImageView imageView = new ImageView(cardImage);
        imageView.setFitWidth(cardWidth);
        imageView.setFitHeight(cardHeight);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        StackPane imageCard = new StackPane(imageView);
        imageCard.setPrefSize(cardWidth, cardHeight);
        imageCard.setMinSize(cardWidth, cardHeight);
        imageCard.setMaxSize(cardWidth, cardHeight);

        Rectangle clip = new Rectangle(cardWidth, cardHeight);
        clip.setArcWidth(Math.max(10, cardWidth * 0.12));
        clip.setArcHeight(Math.max(10, cardWidth * 0.12));
        imageCard.setClip(clip);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.32));
        shadow.setRadius(7);
        shadow.setSpread(0.05);
        imageCard.setEffect(shadow);

        this.setOnMouseEntered(e -> {
            this.setTranslateY(-6);
            shadow.setRadius(12);
        });
        this.setOnMouseExited(e -> {
            this.setTranslateY(0);
            shadow.setRadius(7);
        });

        return imageCard;
    }

    private VBox createFallbackCard(Card card) {

        VBox cardBody = new VBox();
        cardBody.setAlignment(Pos.TOP_CENTER);
        cardBody.setStyle("-fx-background-color: " + getCardBackground(card) + "; "
                + "-fx-background-radius: 10; "
                + "-fx-border-color: " + getBorderColor(card) + "; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 10;");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(0, 0, 0, 0.42));
        glow.setRadius(8);
        glow.setSpread(0.06);
        cardBody.setEffect(glow);

        Region headerBar = new Region();
        headerBar.setPrefHeight(10);
        headerBar.setStyle("-fx-background-color: " + getHeaderColor(card) + "; "
                + "-fx-background-radius: 8 8 0 0;");

        Label nameLabel = new Label(card.getCardName());
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web(getPrimaryTextColor(card)));
        nameLabel.setPadding(new Insets(8, 3, 2, 3));

        Label typeLabel = new Label(getCardTypeShortName(card));
        typeLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
        typeLabel.setTextFill(Color.web(getSecondaryTextColor(card)));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label("¥ " + card.getMonetaryValue() + "M");
        valueLabel.setFont(Font.font("Consolas", FontWeight.BLACK, 14));
        valueLabel.setTextFill(Color.web(getValueTextColor(card)));
        valueLabel.setPadding(new Insets(0, 0, 8, 0));

        cardBody.getChildren().addAll(headerBar, nameLabel, typeLabel, spacer, valueLabel);

        this.setOnMouseEntered(e -> {
            this.setTranslateY(-10);
            glow.setRadius(13);
        });
        this.setOnMouseExited(e -> {
            this.setTranslateY(0);
            glow.setRadius(8);
        });

        return cardBody;
    }

    public CardView(String title, String subtitle) {
        this(title, subtitle, DEFAULT_CARD_WIDTH, DEFAULT_CARD_HEIGHT);
    }

    public CardView(String title, String subtitle, double width, double height) {
        this.cardWidth = width;
        this.cardHeight = height;
        applyFixedCardSize();

        VBox cardBody = new VBox(8);
        cardBody.setAlignment(Pos.CENTER);
        cardBody.setStyle("-fx-background-color: repeating-linear-gradient(45deg, #111, #111 10px, #222 10px, #222 20px); "
                + "-fx-background-radius: 10; -fx-border-color: #d4ad55; -fx-border-width: 2; -fx-border-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#f8e7b4"));
        titleLabel.setFont(Font.font("Consolas", FontWeight.BLACK, 14));

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setTextFill(Color.web("#a0aab5"));
        subtitleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));

        cardBody.getChildren().addAll(titleLabel, subtitleLabel);
        getChildren().add(cardBody);
    }

    private void applyFixedCardSize() {
        setPrefSize(cardWidth, cardHeight);
        setMinSize(cardWidth, cardHeight);
        setMaxSize(cardWidth, cardHeight);
    }

    private Image loadCardImage(Card card) {
        for (String baseName : getImageBaseNameCandidates(card)) {
            for (String extension : new String[]{".png", ".jpg", ".jpeg"}) {
                Image image = loadImage(baseName + extension);
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    private Image loadImage(String fileName) {
        URL resource = CardView.class.getResource(CARD_IMAGE_RESOURCE_DIR + fileName);
        if (resource != null) {
            Image image = new Image(resource.toExternalForm(), 0, 0, true, true);
            return image.isError() ? null : image;
        }

        Path filePath = Paths.get(System.getProperty("user.dir"), CARD_IMAGE_FILE_DIR, fileName);
        if (Files.isRegularFile(filePath)) {
            Image image = new Image(filePath.toUri().toString(), 0, 0, true, true);
            return image.isError() ? null : image;
        }

        return null;
    }

    private List<String> getImageBaseNameCandidates(Card card) {
        List<String> names = new ArrayList<>();
        names.add(card.getCardName());

        String mappedName = getMappedImageBaseName(card);
        if (mappedName != null && !names.contains(mappedName)) {
            names.add(mappedName);
        }

        addSanitizedNames(names, card.getCardName());
        if (mappedName != null) {
            addSanitizedNames(names, mappedName);
        }

        return names;
    }

    private void addSanitizedNames(List<String> names, String baseName) {
        String hyphen = baseName.replace("/", "-");
        String underscore = baseName.replace("/", "_");
        String space = baseName.replace("/", " ");
        for (String candidate : new String[]{hyphen, underscore, space}) {
            if (!names.contains(candidate)) {
                names.add(candidate);
            }
        }
    }

    private String getMappedImageBaseName(Card card) {
        if (card instanceof SuperWildCard) {
            return "Property Wild Card";
        }

        if (card instanceof WildRentCard) {
            return "Rent_Rainbow";
        }

        if (card instanceof RentCard) {
            RentCard rentCard = (RentCard) card;
            PropertyColor[] colors = rentCard.getColorOptions();
            if (hasColors(colors, PropertyColor.BROWN, PropertyColor.LIGHT_BLUE)) {
                return "Brown-Light Blue Rent";
            }
            if (hasColors(colors, PropertyColor.PINK, PropertyColor.ORANGE)) {
                return "Pink-Orange Rent";
            }
            if (hasColors(colors, PropertyColor.RED, PropertyColor.YELLOW)) {
                return "Red-Yellow Rent";
            }
            if (hasColors(colors, PropertyColor.GREEN, PropertyColor.DARK_BLUE)) {
                return "Rent_GreenDeepblue";
            }
            if (hasColors(colors, PropertyColor.RAILROAD, PropertyColor.UTILITY)) {
                return "Railroad-Utility Rent";
            }
        }

        if (card instanceof PropertyWildCard) {
            PropertyWildCard wildCard = (PropertyWildCard) card;
            PropertyColor[] colors = wildCard.getAvailableColors();
            if (hasColors(colors, PropertyColor.BROWN, PropertyColor.LIGHT_BLUE)) return "Property Wild card_BlueBrown";
            if (hasColors(colors, PropertyColor.PINK, PropertyColor.ORANGE)) return "Property Wild Card_OrangePink";
            if (hasColors(colors, PropertyColor.RED, PropertyColor.YELLOW)) return "Property Wild Card_YellowRed";
            if (hasColors(colors, PropertyColor.GREEN, PropertyColor.DARK_BLUE)) return "Property Wild card_GreenDeepblue";
            if (hasColors(colors, PropertyColor.RAILROAD, PropertyColor.UTILITY)) return "Property Wild Card_EnterpriseRailroad";
            if (hasColors(colors, PropertyColor.LIGHT_BLUE, PropertyColor.RAILROAD)) return "Property Wild Card_BlueRailroad";
            if (hasColors(colors, PropertyColor.RAILROAD, PropertyColor.GREEN)) return "Property Wild Card_RailroadGreen";
        }

        return null;
    }

    private boolean hasColors(PropertyColor[] colors, PropertyColor first, PropertyColor second) {
        return colors.length == 2
                && ((colors[0] == first && colors[1] == second)
                || (colors[0] == second && colors[1] == first));
    }

    private String getAccentColor(Card card) {
        if (card instanceof PropertyWildCard) return ((PropertyWildCard) card).getColorA().getColorHex();
        if (card instanceof PropertyCard) return ((PropertyCard) card).getColorGroup().getColorHex();
        if (card instanceof MoneyCard) return "#c9a44b";
        if (card instanceof DealBreakerCard) return "#9b59b6";
        if (card instanceof JustSayNoCard) return "#e91e63";
        if (card instanceof SlyDealCard || card instanceof ForceDealCard) return "#5e35b1";
        if (card instanceof DebtCollectorCard || card instanceof BirthdayCard) return "#fb8c00";
        if (card instanceof PassGoCard) return "#00acc1";
        if (card instanceof DoubleTheRentCard) return "#c9a44b";
        if (card instanceof HouseCard) return "#009688";
        if (card instanceof HotelCard) return "#d32f2f";
        if (card instanceof RentCard || card instanceof WildRentCard) return "#1e88e5";
        if (card instanceof ActionCard) return "#ff007f";
        return "#00f2ff";
    }

    private String getHeaderColor(Card card) {
        PropertyColor[] colors = getCardColors(card);
        if (colors != null) {
            return getColorGradient(colors);
        }
        return getAccentColor(card);
    }

    private String getBorderColor(Card card) {
        PropertyColor[] colors = getCardColors(card);
        if (colors != null) {
            return getColorGradient(colors);
        }
        return getAccentColor(card);
    }

    private String getCardBackground(Card card) {
        if (card instanceof MoneyCard)
            return "linear-gradient(to bottom, #1a1608, #0d0a04)";
        return "linear-gradient(to bottom right, #1e2230, #10131c)";
    }

    private String getPrimaryTextColor(Card card) {
        return "#ffffff";
    }

    private String getSecondaryTextColor(Card card) {
        return "#a0aab5";
    }

    private String getValueTextColor(Card card) {
        if (card instanceof MoneyCard) return "#ffd700";
        if (card instanceof ActionCard) return "#00f2ff";
        return "#00ff9f";
    }

    private PropertyColor[] getCardColors(Card card) {
        if (card instanceof SuperWildCard) {
            return ((SuperWildCard) card).getAvailableColors();
        }
        if (card instanceof PropertyWildCard) {
            PropertyWildCard wildCard = (PropertyWildCard) card;
            return new PropertyColor[]{wildCard.getColorA(), wildCard.getColorB()};
        }
        if (card instanceof RentCard && ((RentCard) card).isMultiColor()) {
            return ((RentCard) card).getColorOptions();
        }
        return null;
    }

    private String getColorGradient(PropertyColor[] colors) {
        StringBuilder gradient = new StringBuilder("linear-gradient(to right");
        double segment = 100.0 / colors.length;
        for (int i = 0; i < colors.length; i++) {
            double start = i * segment;
            double end = (i + 1) * segment;
            gradient.append(", ")
                    .append(colors[i].getColorHex())
                    .append(" ")
                    .append(formatPercent(start))
                    .append("%, ")
                    .append(colors[i].getColorHex())
                    .append(" ")
                    .append(formatPercent(end))
                    .append("%");
        }
        gradient.append(")");
        return gradient.toString();
    }

    private String formatPercent(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private String getCardTypeShortName(Card card) {
        if (card instanceof SuperWildCard) return "[WILD ASSET]";
        if (card instanceof PropertyWildCard) return "[WILD ASSET]";
        if (card instanceof PropertyCard) return "[ASSET]";
        if (card instanceof MoneyCard) return "[FUNDS]";
        if (card instanceof RentCard || card instanceof WildRentCard) return "[RENTAL]";
        if (card instanceof ActionCard) return "[ACTION]";
        return "[SYS]";
    }

    public static CardView back(int count) {
        return back(count, DEFAULT_CARD_WIDTH, DEFAULT_CARD_HEIGHT);
    }

    public static CardView back(int count, double width, double height) {
        return new CardView("DRAW", count + " CARDS", width, height);
    }
}
