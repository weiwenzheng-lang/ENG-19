package player;

public class HotelDecorator extends SetDecorator {

    public HotelDecorator(Rentable wrappedSet) {
        super(wrappedSet);
    }

    @Override
    public int calculateRent() {
        // 酒店在已有的基础上再加 4M
        return wrappedSet.calculateRent() + 4;
    }

    @Override
    public String getDescription() {
        return wrappedSet.getDescription() + " + 🏨 Hotel";
    }
}