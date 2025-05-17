package com.awesome.lindabrain.service.impl;

import com.awesome.lindabrain.commons.Constants;
import com.awesome.lindabrain.model.request.DeepSeekMessage;
import com.awesome.lindabrain.service.AiService;
import com.awesome.lindabrain.service.SysConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    @Autowired
    private SysConfigService sysConfigService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建WebClient实例
     * 
     * @return WebClient实例
     */
    private WebClient createWebClient() {
        String apiKey = sysConfigService.getConfigValue(Constants.DEEPSEEK_API_KEY);
        String apiUrl = sysConfigService.getConfigValue(Constants.DEEPSEEK_API_URL);
        
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
    
    /**
     * 构建请求体
     * 
     * @param messages 消息列表
     * @return 请求体Map
     */
    private Map<String, Object> buildRequestBody(List<DeepSeekMessage> messages) {
        String modelName = sysConfigService.getConfigValue(Constants.DEEPSEEK_MODEL_V3);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        return requestBody;
    }

    /**
     * 调用DeepSeek API发送消息并获取非流式响应
     * 
     * @param messages 消息列表
     * @return 响应内容
     */
    @Override
    public String sendMessageToDeepSeek(List<DeepSeekMessage> messages) {
        try {
            WebClient webClient = createWebClient();
            Map<String, Object> requestBody = buildRequestBody(messages);
            
            // 设置为非流式请求
            requestBody.put("stream", false);
            
            String response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // 解析响应获取内容
            try {
                JsonNode jsonNode = objectMapper.readTree(response);
                return jsonNode.path("choices").path(0).path("message").path("content").asText();
            } catch (JsonProcessingException e) {
                log.error("解析DeepSeek响应失败", e);
                return "解析响应失败: " + e.getMessage();
            }
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            return "调用API失败: " + e.getMessage();
        }
    }
    
    /**
     * 调用DeepSeek API发送消息并获取流式响应
     * 
     * @param messages 消息列表
     * @return 响应流
     */
    @Override
    public Flux<String> sendMessageToDeepSeekForStream(List<DeepSeekMessage> messages) {
        WebClient webClient = createWebClient();
        Map<String, Object> requestBody = buildRequestBody(messages);
        
        // 设置为流式请求
        requestBody.put("stream", true);
        
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    try {
                        // 跳过结束消息
                        if ("[DONE]".equals(chunk)) {
                            return "";
                        }
                        // 解析JSON响应
                        JsonNode jsonNode = objectMapper.readTree(chunk);
                        JsonNode choicesNode = jsonNode.path("choices");

                        if (choicesNode.isArray() && choicesNode.size() > 0) {
                            JsonNode deltaNode = choicesNode.get(0).path("delta");
                            String content = deltaNode.path("content").asText("");
                            if (!content.isEmpty()) {
                                return content;
                            }
                        }
                        return "";
                    } catch (JsonProcessingException e) {
                        log.error("解析DeepSeek流式响应失败", e);
                        return "解析响应失败: " + e.getMessage();
                    }
                })
                .filter(content -> !content.isEmpty());
    }
}
