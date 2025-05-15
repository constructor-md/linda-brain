package com.awesome.lindabrain.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket服务器配置类
 */
@Slf4j
@Component
public class WebSocketServer {

    @Value("${websocket.port:8335}")
    private int port;

    @Resource
    private WebSocketMessageHandler webSocketMessageHandler;

    @Resource
    private WebSocketAuthHandler webSocketAuthHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 启动WebSocket服务器
     */
    @PostConstruct
    public void start() {
        // 使用新线程启动服务器，避免阻塞主线程
        new Thread(() -> {
            try {
                bossGroup = new NioEventLoopGroup(1);
                workerGroup = new NioEventLoopGroup();
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        // 设置30秒没有读写操作就触发心跳检测
                                        .addLast(new IdleStateHandler(30, 30, 60, TimeUnit.SECONDS))
                                        .addLast(new HttpServerCodec())
                                        .addLast(new ChunkedWriteHandler())
                                        .addLast(new HttpObjectAggregator(8192))
                                        .addLast(webSocketAuthHandler)
                                        .addLast(new WebSocketServerProtocolHandler("/api/linda/ws", null, true, 65536, false, true))
                                        .addLast(webSocketMessageHandler);
                            }
                        });

                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("WebSocket服务器启动成功，监听端口: {}...", port);
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("WebSocket服务器启动失败", e);
            }
        }, "websocket-server").start();
    }

    /**
     * 关闭WebSocket服务器
     */
    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("WebSocket服务器已关闭");
    }
}