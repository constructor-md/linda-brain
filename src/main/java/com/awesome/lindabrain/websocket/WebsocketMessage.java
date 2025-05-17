package com.awesome.lindabrain.websocket;

import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
public class WebsocketMessage {

    // 所属会话ID
    private String sessionId;
    // 消息ID 后端临时生成 表征一批websocket消息属于同一个消息的不同部分
    private String messageId;
    // 消息类型枚举
    private String messageType;
    // 消息内容
    private String content;
    // 消息时间戳
    private long timestamp;

    public static WebsocketMessage createChatMessage() {
        return new WebsocketMessage().setMessageType(MessageType.CHAT.name()).setTimestamp(System.currentTimeMillis());
    }

    public static WebsocketMessage createTitleMessage() {
        return new WebsocketMessage().setMessageType(MessageType.TITLE.name()).setTimestamp(System.currentTimeMillis());
    }
}
