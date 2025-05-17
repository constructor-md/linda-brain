package com.awesome.lindabrain.websocket;

import com.awesome.lindabrain.commons.Constants;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket消息处理器
 * 处理WebSocket消息和心跳检测
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketMessageHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 处理不同类型的WebSocket帧
        if (frame instanceof TextWebSocketFrame) {
            // 处理文本消息
            handleTextMessage(ctx, (TextWebSocketFrame) frame);
        } else if (frame instanceof PingWebSocketFrame) {
            // 处理Ping消息，回复Pong
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        }
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String message = frame.text();
        Long userId = ctx.channel().attr(Constants.CHANNEL_ATTR_USER_ID).get();
        log.debug("收到用户 {} 的消息: {}", userId, message);

        // 这里可以添加具体的消息处理逻辑
        // 例如：解析JSON消息，处理不同类型的业务消息等
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.debug("读空闲超时，发送心跳检测");
                ctx.writeAndFlush(new PingWebSocketFrame());
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.debug("写空闲超时，发送心跳");
                ctx.writeAndFlush(new PingWebSocketFrame());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket消息处理异常", cause);
        ctx.close();
    }
}