/**
 * AI 自动出牌模块。
 *
 * <p>该包把“思考”和“执行”拆开：{@link ai.AIPlayerBrain} 只读取当前游戏状态并返回
 * {@link ai.AIAction}，不直接修改牌局；{@link ai.AITurnExecutor} 负责把决策转换成
 * {@code GameManager} 可执行的动作，并用定时器模拟玩家出牌节奏。</p>
 *
 * <p>维护 AI 策略时应优先保证动作合法性，再考虑收益高低。尤其是租金、强制交换、偷牌、
 * Deal Breaker 和 Just Say No 这类动作都依赖目标信息，目标缺失时宁可跳过该策略，也不要
 * 让执行层进入非法状态。</p>
 */
package ai;
