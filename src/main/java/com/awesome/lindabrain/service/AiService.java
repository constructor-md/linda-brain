package com.awesome.lindabrain.service;

import com.awesome.lindabrain.model.request.DeepSeekMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiService {
    
    /**
     * 调用DeepSeek API发送消息并获取非流式响应
     * 
     * @param messages 消息列表
     * @return 响应内容
     */
    String sendMessageToDeepSeek(List<DeepSeekMessage> messages);
    
    /**
     * 调用DeepSeek API发送消息并获取流式响应
     * 
     * @param messages 消息列表
     * @return 响应流
     */
    Flux<String> sendMessageToDeepSeekForStream(List<DeepSeekMessage> messages);
}
