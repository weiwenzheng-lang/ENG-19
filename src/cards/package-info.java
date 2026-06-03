/**
 * 卡牌模型与卡牌效果模块。
 *
 * <p>所有卡牌都继承自 {@link cards.Card}，通过 {@link cards.Card#executePlayLogic(player.Player)}
 * 实现“打出该牌时”的规则。资产牌、货币牌和行动牌在出牌后去向不同：资产和货币通常留在
 * 玩家桌面，行动牌结算后进入弃牌堆。</p>
 *
 * <p>需要目标、颜色或额外上下文的卡牌会从 {@code GameManager} 当前保存的
 * {@code TargetInfo} 中读取信息，因此 UI、AI 或网络同步层在调用出牌接口前必须先准备好
 * 对应目标。</p>
 */
package cards;
