package player;

import cards.PropertyCard;
import enums.PropertyColor;

public interface Rentable {
    int calculateRent();      // 计算租金
    String getDescription();  // 获取描述 (方便控制台打印)
    boolean isComplete();     // 👈 新增：让装饰器和原始套装都能判断是否凑齐
    PropertyColor getColor(); // 👈 新增：方便获取颜色
    void addProperty(PropertyCard card);



}