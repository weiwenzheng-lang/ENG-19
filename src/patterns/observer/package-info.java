/**
 * 观察者模式接口。
 *
 * <p>核心游戏流程通过 {@link patterns.observer.GameObserver} 通知外部“发生了什么”和
 * “当前轮到谁”。控制台日志、JavaFX 界面和未来可能的网络同步层都可以作为观察者接入，
 * 从而避免核心规则直接依赖具体展示方式。</p>
 */
package patterns.observer;
