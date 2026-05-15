package player;

import cards.PropertyCard;
import player.Rentable;

public abstract class SetDecorator implements Rentable {
    protected Rentable wrappedSet;

    public SetDecorator(Rentable wrappedSet) {
        this.wrappedSet = wrappedSet;
    }

    @Override
    public boolean isDecorated() { return true; }

    @Override
    public boolean isComplete() {
        return wrappedSet.isComplete();
    }

    @Override
    public enums.PropertyColor getColor() {
        return wrappedSet.getColor();
    }

    @Override
    public void addProperty(PropertyCard card) {
        wrappedSet.addProperty(card);
    }

    @Override
    public int calculateRent() {
        // 子类（如 HouseDecorator）会在这里加 3M
        return wrappedSet.calculateRent();
    }

    @Override
    public String getDescription() {
        // 子类会在这里加 " + House"
        return wrappedSet.getDescription();
    }

    @Override
    public String toString() {
        // 老师要求：重写 toString。转发给内部对象，保证调试信息不丢失
        return wrappedSet.toString();
    }

    // 递归寻找最底层的原始房产套装
    public PropertySet getRootSet() {
        if (wrappedSet instanceof PropertySet) {
            return (PropertySet) wrappedSet;
        } else if (wrappedSet instanceof SetDecorator) {
            return ((SetDecorator) wrappedSet).getRootSet(); // 继续往里找
        }
        return null;
    }
}