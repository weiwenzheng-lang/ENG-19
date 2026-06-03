/**
 * 游戏核心流程模块。
 *
 * <p>{@link core.GameManager} 是牌局状态的唯一协调者，负责初始化、回合切换、行动次数、
 * 出牌结算、胜负检测、反制链和观察者通知；{@link core.Deck} 管理抽牌堆与弃牌堆；
 * {@link core.CardFactory} 构建标准初始牌库；{@link core.TargetInfo} 承载一次出牌所需的
 * 目标、颜色和索引信息。</p>
 *
 * <p>核心层不关心 JavaFX 控件或网络 socket，只暴露稳定的游戏操作接口。这样 UI、AI 和
 * 局域网模块都可以复用同一套规则，减少“同一张牌在不同入口表现不同”的风险。</p>
 */
package core;
