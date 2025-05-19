package com.awesome.lindabrain.service;

import com.awesome.lindabrain.model.dto.ChatInfoDto;
import com.awesome.lindabrain.model.entity.Chat;
import com.awesome.lindabrain.model.request.ChatRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author 82611
* @description 针对表【chat】的数据库操作Service
* @createDate 2025-05-17 17:22:21
*/
public interface ChatService extends IService<Chat> {

    ChatInfoDto processUserMessage(ChatRequest chatRequest);

    List<ChatInfoDto> getChatList(Long sessionId);
}
