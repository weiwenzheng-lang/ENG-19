package player;

import cards.PropertyCard;
import enums.PropertyColor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PropertyArea {
    //  注意：这里改为 Rentable 接口，这是多态的精髓
    private Map<PropertyColor, Rentable> propertySets;

    public PropertyArea() {
        propertySets = new HashMap<>();
    }

    public boolean addHouseToCompleteSet() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            PropertyColor color = entry.getKey();
            Rentable current = entry.getValue();

            // 1. 必须是完整套装
            // 2. 检查是否已经盖了房子（避免重复盖房）
            if (current.isComplete() && !(current instanceof HouseDecorator)) {
                // 使用装饰器模式包裹原有的套装
                Rentable houseDecorated = new HouseDecorator(current);
                propertySets.put(color, houseDecorated);
                System.out.println("System: Added a House to " + color + " set.");
                return true;
            }
        }
        return false;
    }

    public boolean addHotelToCompleteSet() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            PropertyColor color = entry.getKey();
            Rentable current = entry.getValue();

            // 1. 必须是完整套装
            // 2. 必须已经有 House (HouseDecorator)
            // 3. 必须还没有 Hotel (HotelDecorator)
            if (current.isComplete() &&
                    (current instanceof HouseDecorator) &&
                    !(current instanceof HotelDecorator)) {

                // 进一步包裹，在房子之上盖酒店
                Rentable hotelDecorated = new HotelDecorator(current);
                propertySets.put(color, hotelDecorated);
                System.out.println("System: Added a Hotel to " + color + " set.");
                return true;
            }
        }
        return false;
    }

    // 辅助方法：找到可以盖房子的颜色（已凑齐且不是装饰器或特定逻辑）
    public Optional<PropertyColor> findSetToImprove() {
        for (Map.Entry<PropertyColor, Rentable> entry : propertySets.entrySet()) {
            Rentable set = entry.getValue();
            // 逻辑：如果是完整的 PropertySet 且目前还没被装饰（或者根据你的规则判断）
            if (set instanceof PropertySet && ((PropertySet) set).isComplete()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
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

    private PropertySet unwrap(Rentable rentable) {
        if (rentable instanceof SetDecorator) {
            return ((SetDecorator) rentable).getRootSet();
        }
        return (PropertySet) rentable;
    }

    private void removeColorIfEmpty(PropertyColor color, PropertySet root) {
        if (root.getCardsCount() == 0) {
            propertySets.remove(color);
        }
    }

    // 原有的 addPropertyCard 需要兼容处理
    public void addPropertyCard(cards.PropertyCard card) {
        PropertyColor color = card.getColorGroup();
        propertySets.computeIfAbsent(color, k -> new PropertySet(color));
        Rentable current = propertySets.get(color);
        // 👈 修正逻辑：无论有没有房子，都找到最底层的套装加牌
        PropertySet root = (current instanceof SetDecorator) ?
                ((SetDecorator) current).getRootSet() :
                (PropertySet) current;

        if (root != null) {
            root.addProperty(card);
        }
    }

    //判断胜利条件
    public int countCompletedSets() {
        int count = 0;
        for (Rentable set : propertySets.values()) {
            if (set.isComplete()) {
                count++;
            }
        }
        return count;
    }

    // 队员3核心逻辑：变色/挪动万能牌
    public void swapWildCardColor(PropertyCard card, PropertyColor newColor) {
        PropertyColor oldColor = card.getColorGroup();
        if (oldColor == newColor) return;

        Rentable oldRentable = propertySets.get(oldColor);
        if (oldRentable != null) {
            // 1. 无论外面包了多少层，先找到最底层的 PropertySet 把牌拔掉
            // 找到那个套装（钱包），然后命令它拔牌
            PropertySet oldRoot ;
            if (oldRentable instanceof SetDecorator) {
                oldRoot = ((SetDecorator) oldRentable).getRootSet();
            } else {
                oldRoot = (PropertySet) oldRentable;
            }

            oldRoot.removeProperty(card);

            // 2. 检查旧套装的“房子”是否还合法
            if (!oldRentable.isComplete() && oldRentable instanceof SetDecorator) {
                System.out.println("注意：" + oldColor + " 套装不再完整，房子/酒店已被拆除！");
                // 拆掉所有装饰器，变回最原始的 PropertySet
                propertySets.put(oldColor, oldRoot);
            }
        }

        // 3. 将牌加入新颜色的套装
        propertySets.computeIfAbsent(newColor, k -> new PropertySet(newColor));
        Rentable targetRentable = propertySets.get(newColor);

        PropertySet targetRoot = (targetRentable instanceof SetDecorator) ?
                ((SetDecorator) targetRentable).getRootSet() :
                (PropertySet) targetRentable;
        targetRoot.addProperty(card);
    }

}
