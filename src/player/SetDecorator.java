package player;

import cards.PropertyCard;

public abstract class SetDecorator implements Rentable {
    protected final Rentable wrappedSet;

    // Wraps a rentable set with an added improvement.
    public SetDecorator(Rentable wrappedSet) {
        this.wrappedSet = wrappedSet;
    }

    @Override
    public boolean isDecorated() {
        return true;
    }

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
        return wrappedSet.calculateRent();
    }

    @Override
    public String getDescription() {
        return wrappedSet.getDescription();
    }

    @Override
    public String toString() {
        return wrappedSet.toString();
    }

    // Returns the original undecorated property set.
    public PropertySet getRootSet() {
        if (wrappedSet instanceof PropertySet) {
            return (PropertySet) wrappedSet;
        } else if (wrappedSet instanceof SetDecorator) {
            return ((SetDecorator) wrappedSet).getRootSet();
        }
        return null;
    }
}
