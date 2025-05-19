package com.awesome.lindabrain.controller;

import com.awesome.lindabrain.annotation.Login;
import com.awesome.lindabrain.commons.R;
import com.awesome.lindabrain.model.dto.ChatInfoDto;
import com.awesome.lindabrain.model.request.ChatRequest;
import com.awesome.lindabrain.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 聊天信息控制器
 * 接收用户的聊天信息并处理
 */
@RequestMapping("/chat")
@RestController
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;


    /**
     * 用户发送消息
     */
    @PostMapping("/message")
    @Login
    public R<ChatInfoDto> sendChat(@RequestBody ChatRequest chatRequest) {
        return R.ok(chatService.processUserMessage(chatRequest));
    }

    /**
     * 根据 session 查找聊天记录
     */
    @GetMapping("/history")
    @Login
    public R<List<ChatInfoDto>> getChatList(@RequestParam("id") String sessionId) {
        return R.ok(chatService.getChatList(Long.valueOf(sessionId)));
    }

}
