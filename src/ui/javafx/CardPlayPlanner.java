package ui.javafx;

import cards.Card;
import cards.DealBreakerCard;
import cards.DoubleTheRentCard;
import cards.ForceDealCard;
import cards.HotelCard;
import cards.HouseCard;
import cards.PropertyCard;
import cards.PropertyWildCard;
import cards.RentCard;
import cards.SlyDealCard;
import cards.SuperWildCard;
import cards.WildRentCard;
import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// Collects user choices needed before a card can be played.
final class CardPlayPlanner {
    private final GameManager game;
    private final Consumer<String> eventSink;

    // Stores dependencies needed for choice dialogs and legality filtering.
    CardPlayPlanner(GameManager game, Consumer<String> eventSink) {
        this.game = game;
        this.eventSink = eventSink == null ? message -> { } : eventSink;
    }

    // Prepares color, build, and target choices for a normal card play.
    PlaySelection prepareNormalPlay(Card card) {
        TargetInfo targetInfo = chooseBuildTarget(card);
        if (targetInfo == null && (card instanceof HouseCard || card instanceof HotelCard)) {
            return PlaySelection.cancelled();
        }
        if (!applySelectedColor(card)) {
            eventSink.accept("Cancelled " + card.getCardName());
            return PlaySelection.cancelled();
        }
        if (card.requiresTarget()) {
            targetInfo = chooseTarget(card);
            if (targetInfo == null) {
                eventSink.accept("Cancelled " + card.getCardName());
                return PlaySelection.cancelled();
            }
        }
        return PlaySelection.proceed(targetInfo);
    }

    // Prepares the rent card, color, and target for Double The Rent.
    DoubleRentSelection prepareDoubleRent(int doubleCardIndex) {
        List<Integer> rentIndexes = findRentCardIndexes(doubleCardIndex);
        Optional<Integer> selectedRentIndex = chooseRentCardIndex(rentIndexes);
        if (!selectedRentIndex.isPresent()) {
            return DoubleRentSelection.cancelled();
        }

        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        int rentCardIndex = selectedRentIndex.get();
        Card rent = hand.get(rentCardIndex);
        if (!applySelectedRentColor(rent)) {
            return DoubleRentSelection.cancelled();
        }

        TargetInfo targetInfo = chooseRentTarget(rent);
        if (targetInfo == null && rent.requiresTarget()) {
            return DoubleRentSelection.cancelled();
        }
        return DoubleRentSelection.proceed(rentCardIndex, targetInfo);
    }

    // Chooses the target set for house and hotel cards.
    private TargetInfo chooseBuildTarget(Card card) {
        if (card instanceof HouseCard) {
            TargetInfo targetInfo = chooseImprovementTarget(
                    game.getCurrentPlayer().getPropertyArea().getHouseEligibleColors());
            if (targetInfo == null) {
                eventSink.accept("No eligible complete set for House.");
            }
            return targetInfo;
        }
        if (card instanceof HotelCard) {
            TargetInfo targetInfo = chooseImprovementTarget(
                    game.getCurrentPlayer().getPropertyArea().getHotelEligibleColors());
            if (targetInfo == null) {
                eventSink.accept("No eligible complete set for Hotel.");
            }
            return targetInfo;
        }
        return null;
    }

    // Applies selected color state for rent and wild property cards.
    private boolean applySelectedColor(Card card) {
        if (card instanceof RentCard && ((RentCard) card).isMultiColor()) {
            RentCard rentCard = (RentCard) card;
            PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
            if (selectedColor == null) {
                return false;
            }
            rentCard.setSelectedColor(selectedColor);
        } else if (card instanceof WildRentCard) {
            WildRentCard wrCard = (WildRentCard) card;
            PropertyColor selectedColor = chooseColor(wrCard.getAvailableColors());
            if (selectedColor == null) {
                return false;
            }
            wrCard.setSelectedColor(selectedColor);
        } else if (card instanceof SuperWildCard || card instanceof PropertyWildCard) {
            return applySelectedWildPropertyColor(card);
        }
        return true;
    }

    // Applies selected color state for property wild cards.
    private boolean applySelectedWildPropertyColor(Card card) {
        PropertyColor[] options = card instanceof SuperWildCard
                ? ((SuperWildCard) card).getAvailableColors()
                : ((PropertyWildCard) card).getAvailableColors();
        PropertyColor selectedColor = chooseColor(options);
        if (selectedColor == null) {
            return false;
        }
        if (card instanceof SuperWildCard) {
            ((SuperWildCard) card).setCurrentColor(selectedColor);
        } else {
            ((PropertyWildCard) card).setCurrentColor(selectedColor);
        }
        return true;
    }

    // Prompts for a color from the card's allowed options.
    private PropertyColor chooseColor(PropertyColor[] colorOptions) {
        List<PropertyColor> options = Arrays.asList(colorOptions);
        return GameDialogs.showChoice("Select Color",
                "Choose the color for this card",
                "Color",
                options,
                options.get(0)).orElse(null);
    }

    // Prompts for the target player and any target property details.
    private TargetInfo chooseTarget(Card card) {
        List<Player> choices = new ArrayList<>(game.getOpponents(game.getCurrentPlayer()));
        filterTargetChoices(card, choices);
        Optional<Player> selected = chooseTargetPlayer(choices);
        return selected.map(player -> buildTargetInfoForCard(card, player)).orElse(null);
    }

    // Removes opponents that cannot legally be targeted by the card.
    private void filterTargetChoices(Card card, List<Player> choices) {
        if (card instanceof SlyDealCard) {
            choices.removeIf(player -> player.getPropertyArea().getStealableIncompleteColors().isEmpty());
        } else if (card instanceof ForceDealCard) {
            if (game.getCurrentPlayer().getPropertyArea().getPropertyColorsWithCards().isEmpty()) {
                choices.clear();
                return;
            }
            choices.removeIf(player -> player.getPropertyArea().getPropertyColorsWithCards().isEmpty());
        } else if (card instanceof DealBreakerCard) {
            choices.removeIf(player -> player.getPropertyArea().countCompletedSets() == 0);
        }
    }

    // Prompts for one target player after legality filtering.
    private Optional<Player> chooseTargetPlayer(List<Player> choices) {
        if (choices.isEmpty()) {
            eventSink.accept("No available opponent.");
            return Optional.empty();
        }
        return GameDialogs.showChoice("Choose Target",
                "Select a player to perform the action on",
                "Target",
                choices,
                choices.get(0));
    }

    // Builds target details for the selected player.
    private TargetInfo buildTargetInfoForCard(Card card, Player target) {
        if (card instanceof SlyDealCard) {
            return chooseSlyDealTarget(target);
        }
        if (card instanceof ForceDealCard) {
            return chooseForceDealTarget(target);
        }
        if (card instanceof DealBreakerCard) {
            return chooseDealBreakerTarget(target);
        }
        return new TargetInfo(target);
    }

    // Builds a Sly Deal target from one incomplete property.
    private TargetInfo chooseSlyDealTarget(Player target) {
        PropertyPick targetCard = choosePropertyCard(target, target.getPropertyArea().getStealableIncompleteColors(),
                "Choose property to steal", false);
        return targetCard == null ? null : new TargetInfo(target, targetCard.color, targetCard.index);
    }

    // Builds a Force Deal target from one owned and one opponent property.
    private TargetInfo chooseForceDealTarget(Player target) {
        PropertyPick mine = choosePropertyCard(game.getCurrentPlayer(),
                game.getCurrentPlayer().getPropertyArea().getPropertyColorsWithCards(),
                "Choose your property to give", false);
        if (mine == null) {
            return null;
        }
        PropertyPick theirs = choosePropertyCard(target, target.getPropertyArea().getPropertyColorsWithCards(),
                "Choose target property to receive", false);
        return theirs == null ? null : new TargetInfo(target, mine.color, mine.index, theirs.color, theirs.index);
    }

    // Builds a Deal Breaker target and optional complete-set color.
    private TargetInfo chooseDealBreakerTarget(Player target) {
        List<PropertyColor> completed = target.getPropertyArea().getCompletedColorsList();
        if (completed.size() > 1) {
            PropertyColor chosen = chooseColor(completed.toArray(new PropertyColor[0]));
            if (chosen == null) return null;
            return TargetInfo.forImprovement(chosen).withTarget(target);
        }
        return new TargetInfo(target);
    }

    // Prompts for the complete set that should receive a house or hotel.
    private TargetInfo chooseImprovementTarget(List<PropertyColor> colors) {
        if (colors.isEmpty()) {
            return null;
        }
        Optional<PropertyColor> selected = GameDialogs.showChoice("Choose Property Set",
                "Select a set",
                "Set",
                colors,
                colors.get(0));
        return selected.map(TargetInfo::forImprovement).orElse(null);
    }

    // Prompts for a property card from selected colors.
    private PropertyPick choosePropertyCard(Player owner, List<PropertyColor> colors, String title,
                                            boolean includeComplete) {
        List<PropertyPick> picks = new ArrayList<>();
        for (PropertyColor color : colors) {
            List<PropertyCard> cards = owner.getPropertyArea().getCards(color, includeComplete);
            for (int i = 0; i < cards.size(); i++) {
                picks.add(new PropertyPick(color, i, color + " - " + cards.get(i).getCardName()));
            }
        }
        if (picks.isEmpty()) {
            return null;
        }
        return GameDialogs.showChoice(title,
                owner.getPlayerName(),
                "Property",
                picks,
                picks.get(0)).orElse(null);
    }

    // Finds rent cards that can pair with Double The Rent.
    private List<Integer> findRentCardIndexes(int doubleCardIndex) {
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        List<Integer> rentIndexes = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (i != doubleCardIndex && (hand.get(i) instanceof RentCard || hand.get(i) instanceof WildRentCard)) {
                rentIndexes.add(i);
            }
        }
        return rentIndexes;
    }

    // Prompts for the rent card to pair with Double The Rent.
    private Optional<Integer> chooseRentCardIndex(List<Integer> rentIndexes) {
        if (rentIndexes.isEmpty()) {
            eventSink.accept("Double The Rent must be paired with a rent card.");
            return Optional.empty();
        }
        List<Card> hand = game.getCurrentPlayer().getHand().getCards();
        List<String> choices = new ArrayList<>();
        for (Integer index : rentIndexes) {
            choices.add(index + ": " + hand.get(index).getCardName());
        }
        Optional<String> selected = GameDialogs.showChoice("Double The Rent",
                "Choose a rent card to play with it",
                "Rent",
                choices,
                choices.get(0));
        if (!selected.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(rentIndexes.get(choices.indexOf(selected.get())));
    }

    // Applies the selected color for the paired rent card.
    private boolean applySelectedRentColor(Card rent) {
        if (rent instanceof RentCard && ((RentCard) rent).isMultiColor()) {
            RentCard rentCard = (RentCard) rent;
            PropertyColor selectedColor = chooseColor(rentCard.getColorOptions());
            if (selectedColor == null) {
                return false;
            }
            rentCard.setSelectedColor(selectedColor);
        } else if (rent instanceof WildRentCard) {
            WildRentCard wildRentCard = (WildRentCard) rent;
            PropertyColor selectedColor = chooseColor(wildRentCard.getAvailableColors());
            if (selectedColor == null) {
                return false;
            }
            wildRentCard.setSelectedColor(selectedColor);
        }
        return true;
    }

    // Prompts for the paired rent card target if required.
    private TargetInfo chooseRentTarget(Card rent) {
        TargetInfo targetInfo = null;
        if (rent.requiresTarget()) {
            targetInfo = chooseTarget(rent);
            if (targetInfo == null) {
                eventSink.accept("Cancelled " + rent.getCardName());
                return null;
            }
        }
        return targetInfo;
    }

    // Displays one property option inside selection dialogs.
    private static final class PropertyPick {
        private final PropertyColor color;
        private final int index;
        private final String label;

        // Stores the property color, flattened index, and label.
        private PropertyPick(PropertyColor color, int index, String label) {
            this.color = color;
            this.index = index;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // Result of preparing a normal card play.
    static final class PlaySelection {
        private final boolean cancelled;
        private final TargetInfo targetInfo;

        // Stores whether preparation was cancelled and the selected target.
        private PlaySelection(boolean cancelled, TargetInfo targetInfo) {
            this.cancelled = cancelled;
            this.targetInfo = targetInfo;
        }

        // Creates a cancelled selection.
        static PlaySelection cancelled() {
            return new PlaySelection(true, null);
        }

        // Creates a playable selection.
        static PlaySelection proceed(TargetInfo targetInfo) {
            return new PlaySelection(false, targetInfo);
        }

        // Reports whether the user cancelled or no legal option existed.
        boolean isCancelled() {
            return cancelled;
        }

        // Returns the selected target, or null when none is required.
        TargetInfo getTargetInfo() {
            return targetInfo;
        }
    }

    // Result of preparing a Double The Rent combo.
    static final class DoubleRentSelection {
        private final boolean cancelled;
        private final int rentCardIndex;
        private final TargetInfo targetInfo;

        // Stores the chosen rent card and target.
        private DoubleRentSelection(boolean cancelled, int rentCardIndex, TargetInfo targetInfo) {
            this.cancelled = cancelled;
            this.rentCardIndex = rentCardIndex;
            this.targetInfo = targetInfo;
        }

        // Creates a cancelled selection.
        static DoubleRentSelection cancelled() {
            return new DoubleRentSelection(true, -1, null);
        }

        // Creates a playable selection.
        static DoubleRentSelection proceed(int rentCardIndex, TargetInfo targetInfo) {
            return new DoubleRentSelection(false, rentCardIndex, targetInfo);
        }

        // Reports whether preparation was cancelled.
        boolean isCancelled() {
            return cancelled;
        }

        // Returns the selected rent card index.
        int getRentCardIndex() {
            return rentCardIndex;
        }

        // Returns the selected target, or null when none is required.
        TargetInfo getTargetInfo() {
            return targetInfo;
        }
    }
}
