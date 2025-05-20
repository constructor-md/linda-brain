package com.awesome.lindabrain.websocket.kafka;

import com.awesome.lindabrain.websocket.WebsocketMessage;
import lombok.Data;

/**
 * WebSocket Kafka消息模型
 * 用于在Kafka中传递WebSocket消息
 */
@Data
public class WebSocketKafkaMessage {
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 目标用户ID
     */
    private Long userId;
    
    /**
     * WebSocket消息内容
     */
    private String messageContent;
    
    /**
     * 消息时间戳
     */
    private long timestamp;
    
    /**
     * 节点标识
     * 用于标识发送消息的节点
     */
    private String nodeId;
    
    /**
     * Kafka消息类型枚举
     */
    public enum MessageType {
        /**
         * 用户消息：需要发送给用户的WebSocket消息
         */
        USER_MESSAGE,
        
        /**
         * 连接建立：通知其他节点用户在当前节点建立了连接
         */
        CONNECTION_ESTABLISHED,
        
        /**
         * 连接关闭：通知其他节点用户在当前节点关闭了连接
         */
        CONNECTION_CLOSED
    }
    
    /**
     * 创建用户消息
     * 
     * @param userId 用户ID
     * @param message WebSocket消息
     * @param nodeId 节点ID
     * @return Kafka消息对象
     */
    public static WebSocketKafkaMessage createUserMessage(Long userId, WebsocketMessage<?> message, String nodeId) {
        WebSocketKafkaMessage kafkaMessage = new WebSocketKafkaMessage();
        kafkaMessage.setType(MessageType.USER_MESSAGE);
        kafkaMessage.setUserId(userId);
        kafkaMessage.setNodeId(nodeId);
        kafkaMessage.setTimestamp(System.currentTimeMillis());
        // 将WebSocket消息序列化为JSON字符串
        try {
            kafkaMessage.setMessageContent(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message));
        } catch (Exception e) {
            // 序列化失败时设置为空字符串
            kafkaMessage.setMessageContent("");
        }
        return kafkaMessage;
    }
    
    /**
     * 创建连接建立消息
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return Kafka消息对象
     */
    public static WebSocketKafkaMessage createConnectionEstablished(Long userId, String nodeId) {
        WebSocketKafkaMessage kafkaMessage = new WebSocketKafkaMessage();
        kafkaMessage.setType(MessageType.CONNECTION_ESTABLISHED);
        kafkaMessage.setUserId(userId);
        kafkaMessage.setNodeId(nodeId);
        kafkaMessage.setTimestamp(System.currentTimeMillis());
        return kafkaMessage;
    }
    
    /**
     * 创建连接关闭消息
     * 
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @return Kafka消息对象
     */
    public static WebSocketKafkaMessage createConnectionClosed(Long userId, String nodeId) {
        WebSocketKafkaMessage kafkaMessage = new WebSocketKafkaMessage();
        kafkaMessage.setType(MessageType.CONNECTION_CLOSED);
        kafkaMessage.setUserId(userId);
        kafkaMessage.setNodeId(nodeId);
        kafkaMessage.setTimestamp(System.currentTimeMillis());
        return kafkaMessage;
    }
}