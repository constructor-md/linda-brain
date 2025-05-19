package com.awesome.lindabrain.websocket;

import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
public class WebsocketMessage<T> {
    // 消息类型枚举
    private String messageType;
    // 消息内容
    private T content;
    // 消息时间戳
    private long timestamp;

    // 聊天消息
    @Data
    public static class Chat {
        private String sessionId;
        private String messageId;
        private String content;
        private String createTime;
    }

    // 新建会话消息
    @Data
    public static class Title {
        private String sessionId;
        private String title;
    }

    public static WebsocketMessage<Chat> createChatMessage() {
        return new WebsocketMessage<Chat>().setMessageType(MessageType.CHAT.name())
                .setContent(new Chat())
                .setTimestamp(System.currentTimeMillis());
    }

    public static WebsocketMessage<Title> createTitleMessage() {
        return new WebsocketMessage<Title>()
                .setMessageType(MessageType.TITLE.name())
                .setContent(new Title())
                .setTimestamp(System.currentTimeMillis());
    }

    public static WebsocketMessage<?> createDoneMessage() {
        return new WebsocketMessage<Title>()
                .setMessageType(MessageType.DONE.name())
                .setContent(null)
                .setTimestamp(System.currentTimeMillis());
    }
}
