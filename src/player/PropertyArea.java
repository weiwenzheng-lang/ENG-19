package player;

import cards.PropertyCard;
import enums.PropertyColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PropertyArea {
    private Map<PropertyColor, Rentable> propertySets;

    public PropertyArea() {
        propertySets = new HashMap<>();
    }

    private boolean canBuildOn(PropertyColor color) {
        return color != PropertyColor.RAILROAD && color != PropertyColor.UTILITY;
    }

    public boolean addHouseToCompleteSet() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            PropertyColor color = entry.getKey();
            if (!canBuildOn(color)) continue;
            Rentable current = entry.getValue();
            if (current.isComplete() && !current.isDecorated()) {
                Rentable houseDecorated = new HouseDecorator(current);
                propertySets.put(color, houseDecorated);
                System.out.println("System: Added a House to " + color + " set.");
                return true;
            }
        }
        return false;
    }

    public boolean addHouseToCompleteSet(PropertyColor color) {
        if (!canBuildOn(color)) return false;
        Rentable current = propertySets.get(color);
        if (current != null && current.isComplete() && !current.isDecorated()) {
            propertySets.put(color, new HouseDecorator(current));
            return true;
        }
        return false;
    }

    public boolean addHotelToCompleteSet() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            PropertyColor color = entry.getKey();
            if (!canBuildOn(color)) continue;
            Rentable current = entry.getValue();
            if (current.isComplete() &&
                    (current instanceof HouseDecorator) &&
                    !(current instanceof HotelDecorator)) {

                Rentable hotelDecorated = new HotelDecorator(current);
                propertySets.put(color, hotelDecorated);
                System.out.println("System: Added a Hotel to " + color + " set.");
                return true;
            }
        }
        return false;
    }

    public boolean addHotelToCompleteSet(PropertyColor color) {
        if (!canBuildOn(color)) return false;
        Rentable current = propertySets.get(color);
        if (current != null && current.isComplete()
                && (current instanceof HouseDecorator)
                && !(current instanceof HotelDecorator)) {
            propertySets.put(color, new HotelDecorator(current));
            return true;
        }
        return false;
    }

    public Optional<PropertyColor> findSetToImprove() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable set = entry.getValue();
            if (!set.isDecorated() && set.isComplete()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public List<PropertyColor> getHouseEligibleColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable set = entry.getValue();
            if (set.isComplete() && !set.isDecorated()) {
                colors.add(entry.getKey());
            }
        }
        return colors;
    }

    public List<PropertyColor> getHotelEligibleColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable set = entry.getValue();
            if (set.isComplete() && set instanceof HouseDecorator && !(set instanceof HotelDecorator)) {
                colors.add(entry.getKey());
            }
        }
        return colors;
    }

    public List<PropertyColor> getStealableIncompleteColors() {
        List<PropertyColor> colors = new ArrayList<>();
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable set = entry.getValue();
            PropertySet root = unwrap(set);
            if (!set.isComplete() && root != null && !root.getCards().isEmpty()) {
                colors.add(entry.getKey());
            }
        }
        return colors;
    }

    public List<PropertyColor> getPropertyColorsWithCards() {
        List<PropertyColor> colors = new ArrayList<>();
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            PropertySet root = unwrap(entry.getValue());
            if (root != null && !root.getCards().isEmpty()) {
                colors.add(entry.getKey());
            }
        }
        return colors;
    }

    public List<PropertyCard> getCards(PropertyColor color) {
        Rentable rentable = propertySets.get(color);
        PropertySet root = rentable == null ? null : unwrap(rentable);
        if (root == null) {
            return Collections.emptyList();
        }
        return root.getCards();
    }

    public Rentable getPropertySet(PropertyColor color) {
        return propertySets.get(color);
    }

    public Map<PropertyColor, Rentable> getPropertySets() {
        return Collections.unmodifiableMap(propertySets);
    }

    public void updatePropertySet(PropertyColor color, Rentable decoratedSet) {
        propertySets.put(color, decoratedSet);
    }

    public boolean stealFirstIncompletePropertyTo(PropertyArea recipient) {
        PropertyCard card = detachFirstIncompleteProperty();
        if (card == null) {
            return false;
        }
        recipient.addPropertyCard(card);
        return true;
    }

    public boolean stealIncompletePropertyTo(PropertyArea recipient, PropertyColor color, int cardIndex) {
        PropertyCard card = detachProperty(color, cardIndex, false);
        if (card == null) {
            return false;
        }
        recipient.addPropertyCard(card);
        return true;
    }

    public boolean transferFirstCompletedSetTo(PropertyArea recipient) {
        PropertyColor completedColor = null;
        Rentable completedSet = null;
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable rentable = entry.getValue();
            if (rentable.isComplete()) {
                completedColor = entry.getKey();
                completedSet = rentable;
                break;
            }
        }
        if (completedColor == null) {
            return false;
        }
        propertySets.remove(completedColor);
        recipient.updatePropertySet(completedColor, completedSet);
        return true;
    }

    /** Transfers a specific completed set by color. Returns false if no such completed set exists. */
    public boolean transferCompletedSet(PropertyArea recipient, PropertyColor color) {
        Rentable set = propertySets.get(color);
        if (set == null || !set.isComplete()) return false;
        propertySets.remove(color);
        recipient.updatePropertySet(color, set);
        return true;
    }

    /** Returns the list of colors that have completed sets (for Deal Breaker target selection). */
    public List<PropertyColor> getCompletedColorsList() {
        List<PropertyColor> list = new java.util.ArrayList<>();
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            if (entry.getValue().isComplete()) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public boolean forceSwapFirstAvailableProperty(PropertyArea other) {
        PropertyCard mine = detachFirstIncompleteProperty();
        if (mine == null) {
            return false;
        }

        PropertyCard theirs = other.detachFirstIncompleteProperty();
        if (theirs == null) {
            addPropertyCard(mine);
            return false;
        }

        addPropertyCard(theirs);
        other.addPropertyCard(mine);
        return true;
    }

    public boolean forceSwapProperty(PropertyArea other, PropertyColor myColor, int myIndex,
                                     PropertyColor theirColor, int theirIndex) {
        PropertyCard mine = detachProperty(myColor, myIndex, false);
        if (mine == null) {
            return false;
        }

        PropertyCard theirs = other.detachProperty(theirColor, theirIndex, false);
        if (theirs == null) {
            addPropertyCard(mine);
            return false;
        }

        addPropertyCard(theirs);
        other.addPropertyCard(mine);
        return true;
    }

    private PropertyCard detachFirstIncompleteProperty() {
        PropertyColor selectedColor = null;
        PropertySet selectedRoot = null;
        PropertyCard selectedCard = null;
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable rentable = entry.getValue();
            if (rentable.isComplete()) {
                continue;
            }

            PropertySet root = unwrap(rentable);
            if (root != null && !root.getCards().isEmpty()) {
                selectedColor = entry.getKey();
                selectedRoot = root;
                selectedCard = root.getCards().get(0);
                break;
            }
        }
        if (selectedCard == null) {
            return null;
        }
        selectedRoot.removeProperty(selectedCard);
        removeColorIfEmpty(selectedColor, selectedRoot);
        return selectedCard;
    }

    private PropertyCard detachProperty(PropertyColor color, int cardIndex, boolean allowComplete) {
        Rentable rentable = propertySets.get(color);
        if (rentable == null || (!allowComplete && rentable.isComplete())) {
            return null;
        }

        PropertySet root = unwrap(rentable);
        if (root == null || cardIndex < 0 || cardIndex >= root.getCards().size()) {
            return null;
        }

        PropertyCard selectedCard = root.getCards().get(cardIndex);
        root.removeProperty(selectedCard);
        if (rentable.isDecorated() && !root.isComplete()) {
            propertySets.put(color, root);
        }
        removeColorIfEmpty(color, root);
        return selectedCard;
    }

    private PropertySet unwrap(Rentable rentable) {
        if (rentable.isDecorated()) {
            return ((SetDecorator) rentable).getRootSet();
        }
        return (PropertySet) rentable;
    }

    private void removeColorIfEmpty(PropertyColor color, PropertySet root) {
        if (root.getCardsCount() == 0) {
            propertySets.remove(color);
        }
    }

    /**
     * 强制变卖房产以抵债。
     * 优先变卖非完整套装的牌，不够时再拆完整套装（已装修的先拆装修）。
     * @param amount 需要筹集的金额
     * @return 变卖的牌列表（调用方应将其放入弃牌堆）
     */
    public List<PropertyCard> forceSellProperties(int amount) {
        List<PropertyCard> sold = new ArrayList<>();
        if (amount <= 0) return sold;

        int raised = 0;

        raised = sellFromSets(sold, raised, amount, false);
        if (raised < amount) {
            raised = sellFromSets(sold, raised, amount, true);
        }

        return sold;
    }

    private int sellFromSets(List<PropertyCard> sold, int raised, int target, boolean includeComplete) {
        for (Map.Entry<PropertyColor, Rentable> entry : new ArrayList<>(propertySets.entrySet())) {
            if (raised >= target) break;

            Rentable rentable = entry.getValue();
            boolean isComplete = rentable.isComplete();

            if (isComplete != includeComplete) continue;

            PropertySet root = unwrap(rentable);
            if (root == null || root.getCards().isEmpty()) continue;

            // 拆除装饰器（房子/酒店）
            if (rentable.isDecorated()) {
                propertySets.put(entry.getKey(), root);
                rentable = root;
            }

            // 从该套装中逐一卖牌
            List<PropertyCard> cards = new ArrayList<>(root.getCards());
            for (PropertyCard card : cards) {
                if (raised >= target) break;
                if (card.getMonetaryValue() <= 0) continue;
                root.removeProperty(card);
                sold.add(card);
                raised += card.getMonetaryValue();
            }

            if (root.getCardsCount() == 0) {
                propertySets.remove(entry.getKey());
            }
        }
        return raised;
    }

    public void addPropertyCard(cards.PropertyCard card) {
        PropertyColor color = card.getColorGroup();
        if (color == PropertyColor.WILD) {
            throw new IllegalStateException("Super wild card must have a color set before playing.");
        }

        propertySets.computeIfAbsent(color, k -> new PropertySet(color));
        Rentable current = propertySets.get(color);

        PropertySet root = unwrap(current);
        if (root != null) {
            root.addProperty(card);
        }
    }

    public int countCompletedSets() {
        int count = 0;
        for (Rentable set : propertySets.values()) {
            if (set.isComplete()) {
                count++;
            }
        }
        return count;
    }

    /** Returns the set of distinct colors that have completed sets (for win condition check). */
    public java.util.Set<PropertyColor> getCompletedColors() {
        java.util.Set<PropertyColor> colors = new java.util.HashSet<>();
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            if (entry.getValue().isComplete()) {
                colors.add(entry.getKey());
            }
        }
        return colors;
    }

    public void swapWildCardColor(cards.PropertyCard card, PropertyColor newColor) {
        PropertyColor oldColor = card.getColorGroup();
        if (oldColor == newColor) return;

        Rentable oldRentable = propertySets.get(oldColor);
        if (oldRentable != null) {
            PropertySet oldRoot = unwrap(oldRentable);
            oldRoot.removeProperty(card);

            removeColorIfEmpty(oldColor, oldRoot);
            if (oldRentable.isDecorated() && !oldRoot.isComplete()) {
                System.out.println("System: " + oldColor + " set is no longer complete. Buildings removed.");
                propertySets.put(oldColor, oldRoot);
            }
        }

        if (card instanceof cards.SuperWildCard) {
            ((cards.SuperWildCard) card).setCurrentColor(newColor);
        } else if (card instanceof cards.PropertyWildCard) {
            ((cards.PropertyWildCard) card).setCurrentColor(newColor);
        }

        this.addPropertyCard(card);
    }

}
