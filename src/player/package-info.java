/**
 * 玩家状态与桌面资产模块。
 *
 * <p>{@link player.Player} 聚合手牌、银行区和地产区；{@link player.Hand} 管理手牌上限；
 * {@link player.BankArea} 处理收款和付款；{@link player.PropertyArea} 维护所有地产组、
 * 万能地产换色、偷牌、换牌、完整套装转移和破产式支付。</p>
 *
 * <p>地产组实现了 {@link player.Rentable}，房子和酒店通过装饰器叠加在完整套装上。这样租金
 * 计算可以从基础套装一路委托到装饰器，保持扩展卡效果时的结构清晰。</p>
 */
package player;
