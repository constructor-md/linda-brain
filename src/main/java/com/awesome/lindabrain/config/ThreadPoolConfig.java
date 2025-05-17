package com.awesome.lindabrain.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 * 用于管理系统中的异步任务执行，特别是AI相关的异步调用
 * 
 * @author 82611
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {

    /**
     * AI服务线程池
     * 用于处理AI相关的异步请求，如调用DeepSeek API等
     * 
     * @return 配置好的线程池执行器
     */
    @Bean(name = "aiTaskExecutor")
    public ExecutorService aiTaskExecutor() {
        // 获取CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        // IO密集型应用，核心线程数设为CPU核心数的2倍
        int corePoolSize = cpuCores * 2;
        // 最大线程数设为CPU核心数的4倍
        int maxPoolSize = cpuCores * 4;
        long keepAliveTime = 60L; // 空闲线程存活时间
        int queueCapacity = 100; // 队列容量
        
        log.info("初始化AI任务线程池 - CPU核心数: {}, 核心线程数: {}, 最大线程数: {}", cpuCores, corePoolSize, maxPoolSize);
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程处理
        );
        
        // 添加钩子，在JVM关闭时优雅地关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭AI任务线程池...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("AI任务线程池在60秒内未完全终止，将强制关闭");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("关闭AI任务线程池时被中断", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("AI任务线程池已关闭");
        }));
        
        return executor;
    }
}