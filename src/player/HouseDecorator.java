package player;

public class HouseDecorator extends SetDecorator {

    // Wraps a complete set with a house bonus.
    public HouseDecorator(Rentable wrappedSet) {
        super(wrappedSet);
        if (!wrappedSet.isComplete()) {
            throw new IllegalStateException("A house can only be added to a complete set.");
        }
    }

    @Override
    public int calculateRent() {
        return wrappedSet.calculateRent() + 3;
    }

    @Override
    public String getDescription() {
        return wrappedSet.getDescription() + " + House";
    }

    @Override
    public String toString() {
        return getDescription() + " | Total rent: " + calculateRent() + "M";
    }
}
