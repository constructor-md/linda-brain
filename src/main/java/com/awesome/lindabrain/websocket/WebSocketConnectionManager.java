package com.awesome.lindabrain.websocket;

import com.awesome.lindabrain.websocket.kafka.WebSocketKafkaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket连接管理器
 * 管理用户连接和消息发送
 * 支持通过Kafka实现多节点部署时的消息分发
 */
@Slf4j
@Component
public class WebSocketConnectionManager {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 使用ConcurrentHashMap存储用户ID和Channel的映射关系
    private final Map<Long, Channel> userChannelMap = new ConcurrentHashMap<>();
    
    // 注入Kafka服务
    @Resource
    private WebSocketKafkaService kafkaService;

    /**
     * 添加连接
     *
     * @param userId  用户ID
     * @param channel 连接通道
     */
    public void addConnection(Long userId, Channel channel) {
        Channel oldChannel = userChannelMap.get(userId);
        if (oldChannel != null) {
            // 如果用户已经有连接，先关闭旧连接
            oldChannel.close();
            log.info("关闭用户 {} 的旧连接", userId);
        }
        userChannelMap.put(userId, channel);
        log.info("用户 {} 建立WebSocket连接. 当前在线用户数：{}", userId, getOnlineCount());
        
        // 通知其他节点该用户在当前节点建立了连接
        kafkaService.notifyConnectionEstablished(userId);
    }

    /**
     * 移除连接
     *
     * @param userId 用户ID
     */
    public void removeConnection(Long userId) {
        Channel channel = userChannelMap.remove(userId);
        if (channel != null) {
            channel.close();
            log.info("用户 {} 断开WebSocket连接", userId);
            
            // 通知其他节点该用户在当前节点关闭了连接
            kafkaService.notifyConnectionClosed(userId);
        }
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        Channel channel = userChannelMap.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 向指定用户发送消息
     * 如果用户在当前节点连接，则直接发送
     * 如果用户不在当前节点连接，则通过Kafka广播消息
     *
     * @param userId  用户ID
     * @param message 消息对象
     * @return 发送是否成功
     */
    public boolean sendMessageToUser(Long userId, WebsocketMessage<?> message) {
        // 检查用户是否在当前节点连接
        Channel channel = userChannelMap.get(userId);
        if (channel != null && channel.isActive()) {
            try {
                // 用户在当前节点连接，直接发送消息
                String jsonMessage = objectMapper.writeValueAsString(message);
                channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
                return true;
            } catch (Exception e) {
                log.error("向用户 {} 发送消息失败", userId, e);
            }
        } else {
            // 用户不在当前节点连接，通过Kafka广播消息
            // 消息将被所有节点接收，持有该用户连接的节点会处理并发送给用户
            log.debug("用户 {} 不在当前节点连接，通过Kafka广播消息", userId);
            kafkaService.sendWebSocketMessage(userId, message);
            return true; // 返回true表示消息已发送到Kafka
        }
        return false;
    }

    /**
     * 获取当前在线用户数
     *
     * @return 在线用户数
     */
    public int getOnlineCount() {
        return (int) userChannelMap.values().stream()
                .filter(Channel::isActive)
                .count();
    }
}