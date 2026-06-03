/**
 * 局域网联机模块。
 *
 * <p>该包提供轻量级 TCP 文本协议：服务端 {@link network.LanGameServer} 接收玩家连接并广播
 * 房间状态，客户端 {@link network.LanGameClient} 负责连接、心跳、重连和消息分发，
 * {@link network.LanGameProtocol} 统一处理命令行的编码与解析。</p>
 *
 * <p>协议字段使用转义后的单行文本传输，便于调试，也避免玩家名称、聊天内容中的分隔符破坏
 * 消息结构。网络线程收到消息后只做解析与通知，具体 UI 更新应切回 JavaFX 线程。</p>
 */
package network;
