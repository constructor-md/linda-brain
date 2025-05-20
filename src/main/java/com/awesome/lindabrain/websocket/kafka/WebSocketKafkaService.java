package com.awesome.lindabrain.websocket.kafka;

import com.awesome.lindabrain.config.KafkaConfig;
import com.awesome.lindabrain.websocket.WebSocketConnectionManager;
import com.awesome.lindabrain.websocket.WebsocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * WebSocket Kafka服务
 * 负责处理WebSocket消息的跨节点分发
 */
@Slf4j
@Service
public class WebSocketKafkaService {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    @Lazy
    private WebSocketConnectionManager connectionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 当前节点ID，用于标识消息来源
     * 可以使用主机名、IP地址或随机生成的UUID
     */
    @Value("${spring.application.name:linda-brain}-${server.port:8080}-${random.uuid}")
    private String nodeId;

    /**
     * 发送WebSocket消息到Kafka
     * 
     * @param userId 目标用户ID
     * @param message WebSocket消息
     */
    public void sendWebSocketMessage(Long userId, WebsocketMessage<?> message) {
        try {
            // 创建Kafka消息
            WebSocketKafkaMessage kafkaMessage = WebSocketKafkaMessage.createUserMessage(userId, message, nodeId);
            // 序列化为JSON
            String jsonMessage = objectMapper.writeValueAsString(kafkaMessage);
            // 发送到Kafka消息主题
            kafkaTemplate.send(KafkaConfig.WEBSOCKET_MESSAGE_TOPIC, userId.toString(), jsonMessage);
            log.debug("发送WebSocket消息到Kafka: userId={}, messageType={}", userId, message.getMessageType());
        } catch (JsonProcessingException e) {
            log.error("序列化WebSocket消息失败", e);
        } catch (Exception e) {
            log.error("发送WebSocket消息到Kafka失败", e);
        }
    }

    /**
     * 通知其他节点用户连接已建立
     * 
     * @param userId 用户ID
     */
    public void notifyConnectionEstablished(Long userId) {
        try {
            // 创建连接建立消息
            WebSocketKafkaMessage kafkaMessage = WebSocketKafkaMessage.createConnectionEstablished(userId, nodeId);
            // 序列化为JSON
            String jsonMessage = objectMapper.writeValueAsString(kafkaMessage);
            // 发送到Kafka连接主题
            kafkaTemplate.send(KafkaConfig.WEBSOCKET_CONNECTION_TOPIC, userId.toString(), jsonMessage);
            log.info("通知其他节点用户 {} 在节点 {} 建立连接", userId, nodeId);
        } catch (JsonProcessingException e) {
            log.error("序列化连接建立消息失败", e);
        } catch (Exception e) {
            log.error("发送连接建立消息到Kafka失败", e);
        }
    }

    /**
     * 通知其他节点用户连接已关闭
     * 
     * @param userId 用户ID
     */
    public void notifyConnectionClosed(Long userId) {
        try {
            // 创建连接关闭消息
            WebSocketKafkaMessage kafkaMessage = WebSocketKafkaMessage.createConnectionClosed(userId, nodeId);
            // 序列化为JSON
            String jsonMessage = objectMapper.writeValueAsString(kafkaMessage);
            // 发送到Kafka连接主题
            kafkaTemplate.send(KafkaConfig.WEBSOCKET_CONNECTION_TOPIC, userId.toString(), jsonMessage);
            log.info("通知其他节点用户 {} 在节点 {} 关闭连接", userId, nodeId);
        } catch (JsonProcessingException e) {
            log.error("序列化连接关闭消息失败", e);
        } catch (Exception e) {
            log.error("发送连接关闭消息到Kafka失败", e);
        }
    }

    /**
     * 监听WebSocket消息主题
     * 接收其他节点发送的WebSocket消息并转发给用户
     * 
     * @param message Kafka消息内容
     */
    @KafkaListener(topics = KafkaConfig.WEBSOCKET_MESSAGE_TOPIC)
    public void listenWebSocketMessages(String message) {
        try {
            // 反序列化Kafka消息
            WebSocketKafkaMessage kafkaMessage = objectMapper.readValue(message, WebSocketKafkaMessage.class);
            
            // 如果消息来自当前节点，则忽略（避免消息循环）
            if (nodeId.equals(kafkaMessage.getNodeId())) {
                return;
            }
            
            // 只处理用户消息类型
            if (kafkaMessage.getType() == WebSocketKafkaMessage.MessageType.USER_MESSAGE) {
                Long userId = kafkaMessage.getUserId();
                
                // 检查用户是否在当前节点连接
                if (connectionManager.isUserOnline(userId)) {
                    try {
                        // 反序列化WebSocket消息
                        WebsocketMessage<?> websocketMessage = objectMapper.readValue(
                                kafkaMessage.getMessageContent(), 
                                WebsocketMessage.class
                        );
                        
                        // 发送消息给用户
                        boolean sent = connectionManager.sendMessageToUser(userId, websocketMessage);
                        if (sent) {
                            log.debug("转发来自节点 {} 的WebSocket消息到用户 {}", kafkaMessage.getNodeId(), userId);
                        }
                    } catch (Exception e) {
                        log.error("处理Kafka WebSocket消息失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析Kafka WebSocket消息失败: {}", message, e);
        }
    }

    /**
     * 监听WebSocket连接主题
     * 处理其他节点的连接建立和关闭通知
     * 
     * @param message Kafka消息内容
     */
    @KafkaListener(topics = KafkaConfig.WEBSOCKET_CONNECTION_TOPIC)
    public void listenConnectionEvents(String message) {
        try {
            // 反序列化Kafka消息
            WebSocketKafkaMessage kafkaMessage = objectMapper.readValue(message, WebSocketKafkaMessage.class);
            
            // 如果消息来自当前节点，则忽略（避免处理循环）
            if (nodeId.equals(kafkaMessage.getNodeId())) {
                return;
            }
            
            Long userId = kafkaMessage.getUserId();
            
            // 处理连接建立消息
            if (kafkaMessage.getType() == WebSocketKafkaMessage.MessageType.CONNECTION_ESTABLISHED) {
                // 如果其他节点建立了连接，当前节点应该关闭该用户的连接
                if (connectionManager.isUserOnline(userId)) {
                    connectionManager.removeConnection(userId);
                    log.info("用户 {} 在节点 {} 建立连接，关闭当前节点的连接", userId, kafkaMessage.getNodeId());
                }
            }
            // 处理连接关闭消息（可以用于统计或日志记录）
            else if (kafkaMessage.getType() == WebSocketKafkaMessage.MessageType.CONNECTION_CLOSED) {
                log.info("收到用户 {} 在节点 {} 关闭连接的通知", userId, kafkaMessage.getNodeId());
            }
        } catch (Exception e) {
            log.error("解析Kafka连接消息失败: {}", message, e);
        }
    }
}