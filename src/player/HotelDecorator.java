package player;

public class HotelDecorator extends SetDecorator {

    // Wraps a house set with a hotel bonus.
    public HotelDecorator(Rentable wrappedSet) {
        super(wrappedSet);
        if (!(wrappedSet instanceof HouseDecorator)) {
            throw new IllegalStateException("A hotel can only be added after a house.");
        }
    }

    @Override
    public int calculateRent() {
        return wrappedSet.calculateRent() + 4;
    }

    @Override
    public String getDescription() {
        return wrappedSet.getDescription() + " + Hotel";
    }

    @Override
    public String toString() {
        return getDescription() + " | Total rent: " + calculateRent() + "M";
    }
}
