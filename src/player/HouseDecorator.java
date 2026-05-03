package player;

public class HouseDecorator extends SetDecorator {

    public HouseDecorator(Rentable wrappedSet) {
        super(wrappedSet);
        if (!wrappedSet.isComplete()) {
            throw new IllegalStateException("这套房产还没凑齐，不能盖房子！");
        }
    }

    @Override
    public int calculateRent() {
        // 核心逻辑：原有租金 + 房子增加的 3M 租金
        return wrappedSet.calculateRent() + 3;
    }

    @Override
    public String getDescription() {
        // 打印时，在原有的描述后面加上 🏠 图标
        return wrappedSet.getDescription() + " + 🏠 House";
    }

    @Override
    public String toString() {
        return getDescription() + " | 总租金: " + calculateRent() + "M";
    }
}