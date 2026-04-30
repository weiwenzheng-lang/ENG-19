package player;

import cards.PropertyCard;
import player.Rentable;

public abstract class SetDecorator implements Rentable {
    protected Rentable wrappedSet;

    public SetDecorator(Rentable wrappedSet) {
        this.wrappedSet = wrappedSet;
    }

    @Override
    public boolean isComplete() {
        return wrappedSet.isComplete(); // 转发给内部对象
    }

    @Override
    public enums.PropertyColor getColor() {
        return wrappedSet.getColor();
    }

    @Override public void addProperty(PropertyCard card) { wrappedSet.addProperty(card); }
}