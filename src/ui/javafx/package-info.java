/**
 * JavaFX 用户界面模块。
 *
 * <p>{@link ui.javafx.MainApp} 负责应用入口、菜单和场景切换；
 * {@link ui.javafx.GameController} 把核心牌局渲染成桌面、手牌、按钮和弹窗；
 * {@link ui.javafx.CardView} 根据卡牌图片或兜底样式生成卡牌节点；
 * {@link ui.javafx.NetworkLobbyController} 管理局域网大厅。</p>
 *
 * <p>界面层负责收集用户选择并组装 {@code TargetInfo}，真正的规则结算仍交给核心层。更新 UI
 * 时要注意 JavaFX 线程限制，来自网络线程或 AI 定时器的回调应通过 {@code Platform.runLater}
 * 回到界面线程。</p>
 */
package ui.javafx;
