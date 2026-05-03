package player;

public class HotelDecorator extends SetDecorator {

    public HotelDecorator(Rentable wrappedSet) {
        super(wrappedSet);
        if (!(wrappedSet instanceof HouseDecorator)) {
            throw new IllegalStateException("必须先盖了房子(House)，才能盖酒店(Hotel)！");
        }
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

    @Override
    public String toString() {
        return getDescription() + " | 总租金: " + calculateRent() + "M";
    }
}