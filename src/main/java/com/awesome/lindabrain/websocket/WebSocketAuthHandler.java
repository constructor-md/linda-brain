package com.awesome.lindabrain.websocket;

import cn.hutool.core.util.StrUtil;
import com.awesome.lindabrain.commons.Constants;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WebSocket认证处理器
 * 处理WebSocket连接的认证逻辑
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private WebSocketConnectionManager connectionManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            // 解析请求参数
            String uri = request.uri();
            String token = extractToken(uri);
            if (StrUtil.isBlank(token)) {
                log.warn("WebSocket连接认证失败：token为空");
                ctx.close();
                return;
            }

            // 验证token并获取用户ID
            Long userId = (Long) redisTemplate.opsForValue().get(Constants.REDIS_ACCESS_TOKEN_PREFIX + token);
            if (userId == null) {
                log.warn("WebSocket连接认证失败：无效的token");
                ctx.close();
                return;
            }

            // 将用户ID保存到Channel的属性中
            ctx.channel().attr(Constants.CHANNEL_ATTR_USER_ID).set(userId);
            // 添加连接到管理器
            connectionManager.addConnection(userId, ctx.channel());
            log.info("用户 {} WebSocket连接认证成功", userId);
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接断开时，移除连接
        Long userId = ctx.channel().attr(Constants.CHANNEL_ATTR_USER_ID).get();
        if (userId != null) {
            connectionManager.removeConnection(userId);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket连接异常", cause);
        ctx.close();
    }

    private String extractToken(String url) {
        if (url == null) return null;

        int tokenIndex = url.indexOf("token=");
        if (tokenIndex == -1) return null;

        return url.substring(tokenIndex + 6); // 直接截取"token="之后的所有字符
    }
}