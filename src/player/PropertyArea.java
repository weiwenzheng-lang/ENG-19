package player;

import cards.PropertyCard;
import enums.PropertyColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PropertyArea {
    private final Map<PropertyColor, List<Rentable>> propertySets;

    // Identifies one visible property set inside a color bucket.
    public static class PropertySetEntry {
        private final PropertyColor color;
        private final int setIndex;
        private final Rentable rentable;

        // Stores the color, position, and rentable wrapper for one set.
        public PropertySetEntry(PropertyColor color, int setIndex, Rentable rentable) {
            this.color = color;
            this.setIndex = setIndex;
            this.rentable = rentable;
        }

        // Returns the color bucket that owns this set.
        public PropertyColor getColor() {
            return color;
        }

        // Returns the index inside the color bucket.
        public int getSetIndex() {
            return setIndex;
        }

        // Returns the property set, including any house or hotel wrapper.
        public Rentable getRentable() {
            return rentable;
        }
    }

    // Creates an empty board-side property area.
    public PropertyArea() {
        propertySets = new HashMap<>();
    }

    // Houses and hotels are not allowed on railroads or utilities.
    private boolean canBuildOn(PropertyColor color) {
        return color != PropertyColor.RAILROAD && color != PropertyColor.UTILITY;
    }

    // Adds a house to the first complete set that can accept one.
    public boolean addHouseToCompleteSet() {
        for (PropertySetEntry entry : getPropertySetEntries()) {
            if (addHouseToCompleteSet(entry.getColor())) {
                return true;
            }
        }
        return false;
    }

    // Adds a house to a complete set of the requested color.
    public boolean addHouseToCompleteSet(PropertyColor color) {
        if (!canBuildOn(color)) return false;
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return false;
        for (int i = 0; i < sets.size(); i++) {
            Rentable current = sets.get(i);
            if (current.isComplete() && !current.isDecorated()) {
                sets.set(i, new HouseDecorator(current));
                return true;
            }
        }
        return false;
    }

    // Adds a hotel to the first complete housed set that can accept one.
    public boolean addHotelToCompleteSet() {
        for (PropertySetEntry entry : getPropertySetEntries()) {
            if (addHotelToCompleteSet(entry.getColor())) {
                return true;
            }
        }
        return false;
    }

    // Adds a hotel to a complete housed set of the requested color.
    public boolean addHotelToCompleteSet(PropertyColor color) {
        if (!canBuildOn(color)) return false;
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return false;
        for (int i = 0; i < sets.size(); i++) {
            Rentable current = sets.get(i);
            if (current.isComplete()
                    && current instanceof HouseDecorator
                    && !(current instanceof HotelDecorator)) {
                sets.set(i, new HotelDecorator(current));
                return true;
            }
        }
        return false;
    }

    // Finds the first complete undecorated set that can be improved.
    public Optional<PropertyColor> findSetToImprove() {
        for (PropertySetEntry entry : getPropertySetEntries()) {
            Rentable set = entry.getRentable();
            if (canBuildOn(entry.getColor()) && !set.isDecorated() && set.isComplete()) {
                return Optional.of(entry.getColor());
            }
        }
        return Optional.empty();
    }

    // Lists colors that can receive a house.
    public List<PropertyColor> getHouseEligibleColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (PropertySetEntry entry : getPropertySetEntries()) {
            Rentable set = entry.getRentable();
            if (canBuildOn(entry.getColor()) && set.isComplete() && !set.isDecorated()
                    && !colors.contains(entry.getColor())) {
                colors.add(entry.getColor());
            }
        }
        return colors;
    }

    // Lists colors that can receive a hotel.
    public List<PropertyColor> getHotelEligibleColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (PropertySetEntry entry : getPropertySetEntries()) {
            Rentable set = entry.getRentable();
            if (canBuildOn(entry.getColor())
                    && set.isComplete()
                    && set instanceof HouseDecorator
                    && !(set instanceof HotelDecorator)
                    && !colors.contains(entry.getColor())) {
                colors.add(entry.getColor());
            }
        }
        return colors;
    }

    // Lists incomplete colors that can be targeted by Sly Deal.
    public List<PropertyColor> getStealableIncompleteColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (PropertySetEntry entry : getPropertySetEntries()) {
            Rentable set = entry.getRentable();
            PropertySet root = unwrap(set);
            if (!set.isComplete() && root != null && !root.getCards().isEmpty()
                    && !colors.contains(entry.getColor())) {
                colors.add(entry.getColor());
            }
        }
        return colors;
    }

    // Lists incomplete colors that still contain at least one property card.
    public List<PropertyColor> getPropertyColorsWithCards() {
        List<PropertyColor> colors = new ArrayList<>();
        for (PropertySetEntry entry : getPropertySetEntries()) {
            Rentable set = entry.getRentable();
            PropertySet root = unwrap(set);
            if (!set.isComplete() && root != null && !root.getCards().isEmpty()
                    && !colors.contains(entry.getColor())) {
                colors.add(entry.getColor());
            }
        }
        return colors;
    }

    // Returns all cards of a color, including complete sets.
    public List<PropertyCard> getCards(PropertyColor color) {
        return getCards(color, true);
    }

    // Returns all cards of a color with optional complete-set filtering.
    public List<PropertyCard> getCards(PropertyColor color, boolean includeComplete) {
        List<PropertyCard> cards = new ArrayList<>();
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) {
            return Collections.emptyList();
        }
        for (Rentable rentable : sets) {
            if (!includeComplete && rentable.isComplete()) {
                continue;
            }
            PropertySet root = unwrap(rentable);
            if (root != null) {
                cards.addAll(root.getCards());
            }
        }
        return Collections.unmodifiableList(cards);
    }

    // Returns every property card currently on this player's table.
    public List<PropertyCard> getAllPropertyCards() {
        List<PropertyCard> cards = new ArrayList<>();
        for (PropertySetEntry entry : getPropertySetEntries()) {
            PropertySet root = unwrap(entry.getRentable());
            if (root != null) {
                cards.addAll(root.getCards());
            }
        }
        return Collections.unmodifiableList(cards);
    }

    // Returns the highest-value visible set for a color.
    public Rentable getPropertySet(PropertyColor color) {
        return getBestSet(propertySets.get(color));
    }

    // Returns one best display set per color for legacy callers.
    public Map<PropertyColor, Rentable> getPropertySets() {
        Map<PropertyColor, Rentable> bestByColor = new HashMap<>();
        for (PropertyColor color : sortedColors()) {
            Rentable best = getPropertySet(color);
            if (best != null) {
                bestByColor.put(color, best);
            }
        }
        return Collections.unmodifiableMap(bestByColor);
    }

    // Flattens all color buckets into stable display entries.
    public List<PropertySetEntry> getPropertySetEntries() {
        List<PropertySetEntry> entries = new ArrayList<>();
        for (PropertyColor color : sortedColors()) {
            List<Rentable> sets = propertySets.get(color);
            if (sets == null) continue;
            for (int i = 0; i < sets.size(); i++) {
                entries.add(new PropertySetEntry(color, i, sets.get(i)));
            }
        }
        return Collections.unmodifiableList(entries);
    }

    // Replaces the best set for a color, usually after decoration.
    public void updatePropertySet(PropertyColor color, Rentable decoratedSet) {
        List<Rentable> sets = mutableSets(color);
        int bestIndex = getBestSetIndex(sets);
        if (bestIndex >= 0) {
            sets.set(bestIndex, decoratedSet);
        } else {
            sets.add(decoratedSet);
        }
    }

    // Moves the first stealable incomplete property to another area.
    public boolean stealFirstIncompletePropertyTo(PropertyArea recipient) {
        PropertyCard card = detachFirstIncompleteProperty();
        if (card == null) {
            return false;
        }
        recipient.addPropertyCard(card);
        return true;
    }

    // Moves a specific stealable incomplete property to another area.
    public boolean stealIncompletePropertyTo(PropertyArea recipient, PropertyColor color, int cardIndex) {
        PropertyCard card = detachProperty(color, cardIndex, false);
        if (card == null) {
            return false;
        }
        recipient.addPropertyCard(card);
        return true;
    }

    // Transfers a concrete property card after validating the source set.
    public boolean transferPropertyCardTo(PropertyArea recipient, PropertyCard card, boolean allowComplete) {
        if (recipient == null || card == null) {
            return false;
        }
        PropertyCard detached = detachSpecificProperty(card, allowComplete);
        if (detached == null) {
            return false;
        }
        recipient.addPropertyCard(detached);
        return true;
    }

    // Transfers the first complete set to another player.
    public boolean transferFirstCompletedSetTo(PropertyArea recipient) {
        for (PropertySetEntry entry : getPropertySetEntries()) {
            if (entry.getRentable().isComplete()) {
                removeRentable(entry.getColor(), entry.getSetIndex());
                recipient.receiveTransferredSet(entry.getColor(), entry.getRentable());
                return true;
            }
        }
        return false;
    }

    // Transfers one complete set of the requested color.
    public boolean transferCompletedSet(PropertyArea recipient, PropertyColor color) {
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return false;
        for (int i = 0; i < sets.size(); i++) {
            Rentable set = sets.get(i);
            if (set.isComplete()) {
                removeRentable(color, i);
                recipient.receiveTransferredSet(color, set);
                return true;
            }
        }
        return false;
    }

    // Accepts a full set transfer while preserving houses and hotels.
    public void receiveTransferredSet(PropertyColor color, Rentable transferredSet) {
        if (transferredSet == null) return;
        PropertySet root = unwrap(transferredSet);
        if (root == null || root.getCards().isEmpty()) return;

        // Complete or decorated sets must remain grouped after Deal Breaker.
        if (transferredSet.isComplete() || transferredSet.isDecorated()) {
            mutableSets(color).add(transferredSet);
            return;
        }

        // Incomplete transfers can merge into the recipient's best partial set.
        for (PropertyCard card : new ArrayList<>(root.getCards())) {
            addPropertyCard(card);
        }
    }

    // Returns completed colors in enum order for UI summaries.
    public List<PropertyColor> getCompletedColorsList() {
        List<PropertyColor> colors = new ArrayList<>(getCompletedColors());
        colors.sort(Comparator.comparingInt(Enum::ordinal));
        return colors;
    }

    // Swaps the first available incomplete properties between two players.
    public boolean forceSwapFirstAvailableProperty(PropertyArea other) {
        PropertyCard mine = detachFirstIncompleteProperty();
        if (mine == null) {
            return false;
        }

        // Roll back the source card if the opponent has no legal property.
        PropertyCard theirs = other.detachFirstIncompleteProperty();
        if (theirs == null) {
            addPropertyCard(mine);
            return false;
        }

        addPropertyCard(theirs);
        other.addPropertyCard(mine);
        return true;
    }

    // Swaps selected incomplete properties between two players.
    public boolean forceSwapProperty(PropertyArea other, PropertyColor myColor, int myIndex,
                                     PropertyColor theirColor, int theirIndex) {
        PropertyCard mine = detachProperty(myColor, myIndex, false);
        if (mine == null) {
            return false;
        }

        // Preserve the source card if the opponent selection is no longer valid.
        PropertyCard theirs = other.detachProperty(theirColor, theirIndex, false);
        if (theirs == null) {
            addPropertyCard(mine);
            return false;
        }

        addPropertyCard(theirs);
        other.addPropertyCard(mine);
        return true;
    }

    // Removes and returns the first incomplete property in display order.
    private PropertyCard detachFirstIncompleteProperty() {
        for (PropertyColor color : sortedColors()) {
            PropertyCard card = detachProperty(color, 0, false);
            if (card != null) {
                return card;
            }
        }
        return null;
    }

    // Detaches a flattened card index from one color bucket.
    private PropertyCard detachProperty(PropertyColor color, int cardIndex, boolean allowComplete) {
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return null;

        int flattenedIndex = 0;
        for (int setIndex = 0; setIndex < sets.size(); setIndex++) {
            Rentable rentable = sets.get(setIndex);
            if (!allowComplete && rentable.isComplete()) {
                continue;
            }

            PropertySet root = unwrap(rentable);
            if (root == null) continue;
            List<PropertyCard> cards = root.getCards();
            if (cardIndex < flattenedIndex + cards.size()) {
                PropertyCard selectedCard = cards.get(cardIndex - flattenedIndex);
                root.removeProperty(selectedCard);
                // Dropping below complete removes any house or hotel wrapper.
                if (rentable.isDecorated() && !root.isComplete()) {
                    sets.set(setIndex, root);
                }
                removeSetIfEmpty(color, root);
                return selectedCard;
            }
            flattenedIndex += cards.size();
        }
        return null;
    }

    // Detaches a concrete card object from its current color bucket.
    private PropertyCard detachSpecificProperty(PropertyCard card, boolean allowComplete) {
        PropertyColor color = card.getColorGroup();
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return null;

        for (int i = 0; i < sets.size(); i++) {
            Rentable rentable = sets.get(i);
            if (!allowComplete && rentable.isComplete()) {
                continue;
            }

            PropertySet root = unwrap(rentable);
            if (root == null || !root.getCards().contains(card)) {
                continue;
            }

            root.removeProperty(card);
            if (rentable.isDecorated() && !root.isComplete()) {
                sets.set(i, root);
            }
            removeSetIfEmpty(color, root);
            return card;
        }
        return null;
    }

    // Sells properties until enough value has been raised for payment.
    public List<PropertyCard> forceSellProperties(int amount) {
        List<PropertyCard> sold = new ArrayList<>();
        if (amount <= 0) return sold;

        // Official play protects full sets unless partial sets cannot pay.
        int raised = 0;
        raised = sellFromSets(sold, raised, amount, false);
        if (raised < amount) {
            sellFromSets(sold, raised, amount, true);
        }

        return sold;
    }

    // Sells cards from either incomplete or complete sets.
    private int sellFromSets(List<PropertyCard> sold, int raised, int target, boolean includeComplete) {
        for (PropertySetEntry entry : new ArrayList<>(getPropertySetEntries())) {
            if (raised >= target) break;
            Rentable rentable = entry.getRentable();
            if (rentable.isComplete() != includeComplete) continue;

            PropertySet root = unwrap(rentable);
            if (root == null || root.getCards().isEmpty()) continue;

            List<Rentable> sets = propertySets.get(entry.getColor());
            if (sets == null || entry.getSetIndex() >= sets.size()) continue;
            if (rentable.isDecorated()) {
                // Selling from a decorated set strips the decoration first.
                sets.set(entry.getSetIndex(), root);
            }

            List<PropertyCard> cards = new ArrayList<>(root.getCards());
            for (PropertyCard card : cards) {
                if (raised >= target) break;
                if (card.getMonetaryValue() <= 0) continue;
                root.removeProperty(card);
                sold.add(card);
                raised += card.getMonetaryValue();
            }

            removeSetIfEmpty(entry.getColor(), root);
        }
        return raised;
    }

    // Adds a property card into the best matching partial set.
    public void addPropertyCard(PropertyCard card) {
        PropertyColor color = card.getColorGroup();
        if (color == PropertyColor.WILD) {
            throw new IllegalStateException("Super wild card must have a color set before playing.");
        }

        PropertySet target = findBestIncompleteSetFor(color);
        if (target == null) {
            target = new PropertySet(color);
            mutableSets(color).add(target);
        }
        target.addProperty(card);
    }

    // Counts colors with at least one complete set.
    public int countCompletedSets() {
        return getCompletedColors().size();
    }

    // Returns the unique colors that currently have complete sets.
    public Set<PropertyColor> getCompletedColors() {
        Set<PropertyColor> colors = new HashSet<>();
        for (PropertySetEntry entry : getPropertySetEntries()) {
            if (entry.getRentable().isComplete()) {
                colors.add(entry.getColor());
            }
        }
        return colors;
    }

    // Moves a wild card to a new color and rejoins it to the best set.
    public void swapWildCardColor(PropertyCard card, PropertyColor newColor) {
        PropertyColor oldColor = card.getColorGroup();
        if (oldColor == newColor) return;

        // Remove before mutating color so the old bucket stays consistent.
        removeCardFromColor(card, oldColor);

        if (card instanceof cards.SuperWildCard) {
            ((cards.SuperWildCard) card).setCurrentColor(newColor);
        } else if (card instanceof cards.PropertyWildCard) {
            ((cards.PropertyWildCard) card).setCurrentColor(newColor);
        }

        addPropertyCard(card);
    }

    // Removes a concrete card from the requested color bucket.
    private void removeCardFromColor(PropertyCard card, PropertyColor color) {
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return;
        for (int i = 0; i < sets.size(); i++) {
            Rentable rentable = sets.get(i);
            PropertySet root = unwrap(rentable);
            if (root == null || !root.getCards().contains(card)) continue;
            root.removeProperty(card);
            if (rentable.isDecorated() && !root.isComplete()) {
                sets.set(i, root);
            }
            removeSetIfEmpty(color, root);
            return;
        }
    }

    // Finds the fullest incomplete set that can still accept this color.
    private PropertySet findBestIncompleteSetFor(PropertyColor color) {
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return null;
        PropertySet best = null;
        int bestCount = -1;
        for (Rentable rentable : sets) {
            if (rentable.isComplete()) continue;
            PropertySet root = unwrap(rentable);
            if (root == null) continue;
            int count = root.getCardsCount();
            if (count < color.getRequiredCount() && count > bestCount) {
                best = root;
                bestCount = count;
            }
        }
        return best;
    }

    // Returns the preferred set for display or decoration.
    private Rentable getBestSet(List<Rentable> sets) {
        int index = getBestSetIndex(sets);
        return index < 0 ? null : sets.get(index);
    }

    // Selects complete sets first, then highest-rent sets.
    private int getBestSetIndex(List<Rentable> sets) {
        if (sets == null || sets.isEmpty()) return -1;
        int bestIndex = 0;
        for (int i = 1; i < sets.size(); i++) {
            Rentable best = sets.get(bestIndex);
            Rentable candidate = sets.get(i);
            if (isBetterSet(candidate, best)) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    // Compares two candidate sets for display priority.
    private boolean isBetterSet(Rentable candidate, Rentable current) {
        if (candidate.isComplete() != current.isComplete()) {
            return candidate.isComplete();
        }
        return candidate.calculateRent() > current.calculateRent();
    }

    // Returns the mutable list for one color bucket.
    private List<Rentable> mutableSets(PropertyColor color) {
        return propertySets.computeIfAbsent(color, ignored -> new ArrayList<>());
    }

    // Removes a set wrapper by color and index.
    private void removeRentable(PropertyColor color, int setIndex) {
        List<Rentable> sets = propertySets.get(color);
        if (sets == null || setIndex < 0 || setIndex >= sets.size()) return;
        sets.remove(setIndex);
        if (sets.isEmpty()) {
            propertySets.remove(color);
        }
    }

    // Drops the set bucket when its root has no cards left.
    private void removeSetIfEmpty(PropertyColor color, PropertySet root) {
        if (root.getCardsCount() > 0) return;
        removeSetByRoot(color, root);
    }

    // Removes any wrapper whose root object matches the supplied set.
    private void removeSetByRoot(PropertyColor color, PropertySet root) {
        List<Rentable> sets = propertySets.get(color);
        if (sets == null) return;
        sets.removeIf(rentable -> unwrap(rentable) == root);
        if (sets.isEmpty()) {
            propertySets.remove(color);
        }
    }

    // Returns the root property set under house or hotel decorators.
    private PropertySet unwrap(Rentable rentable) {
        if (rentable == null) return null;
        if (rentable.isDecorated()) {
            return ((SetDecorator) rentable).getRootSet();
        }
        return (PropertySet) rentable;
    }

    // Keeps UI and AI iteration deterministic.
    private List<PropertyColor> sortedColors() {
        List<PropertyColor> colors = new ArrayList<>(propertySets.keySet());
        colors.sort(Comparator.comparingInt(Enum::ordinal));
        return colors;
    }
}
