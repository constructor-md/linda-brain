package com.awesome.lindabrain.websocket;

import cn.hutool.core.util.StrUtil;
import com.awesome.lindabrain.commons.Constants;
import com.awesome.lindabrain.model.entity.UserInfo;
import com.awesome.lindabrain.service.UserInfoService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * WebSocket认证处理器
 * 处理WebSocket连接的认证逻辑
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserInfoService userInfoService;

    @Autowired
    private WebSocketConnectionManager connectionManager;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            // 解析请求参数
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            Map<String, List<String>> parameters = decoder.parameters();
            List<String> tokens = parameters.get("token");

            if (tokens == null || tokens.isEmpty() || StrUtil.isBlank(tokens.get(0))) {
                log.warn("WebSocket连接认证失败：token为空");
                ctx.close();
                return;
            }

            String token = tokens.get(0);
            // 验证token并获取用户ID
            Long userId = (Long) redisTemplate.opsForValue().get(Constants.REDIS_ACCESS_TOKEN_PREFIX + token);
            if (userId == null) {
                log.warn("WebSocket连接认证失败：无效的token");
                ctx.close();
                return;
            }

            // 获取用户信息
            UserInfo userInfo = userInfoService.getById(userId);
            if (userInfo == null) {
                log.warn("WebSocket连接认证失败：用户不存在");
                ctx.close();
                return;
            }

            // 将用户ID保存到Channel的属性中
            ctx.channel().attr(USER_ID_KEY).set(userId);
            // 添加连接到管理器
            connectionManager.addConnection(userId, ctx.channel());
            log.info("用户 {} WebSocket连接认证成功", userId);
            
            // 修改请求URI，移除查询参数
            String uri = request.uri();
            int queryIndex = uri.indexOf('?');
            if (queryIndex != -1) {
                String newUri = uri.substring(0, queryIndex);
                request.setUri(newUri);
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 连接断开时，移除连接
        Long userId = ctx.channel().attr(USER_ID_KEY).get();
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
}