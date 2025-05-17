package com.awesome.lindabrain.service.impl;

import cn.hutool.core.util.StrUtil;
import com.awesome.lindabrain.aop.UserInfoContext;
import com.awesome.lindabrain.commons.Constants;
import com.awesome.lindabrain.mapper.ChatMapper;
import com.awesome.lindabrain.model.dto.ChatInfoDto;
import com.awesome.lindabrain.model.entity.Chat;
import com.awesome.lindabrain.model.entity.Session;
import com.awesome.lindabrain.model.entity.UserInfo;
import com.awesome.lindabrain.model.request.ChatRequest;
import com.awesome.lindabrain.model.request.DeepSeekMessage;
import com.awesome.lindabrain.service.AiService;
import com.awesome.lindabrain.service.ChatService;
import com.awesome.lindabrain.service.SessionService;
import com.awesome.lindabrain.websocket.WebSocketConnectionManager;
import com.awesome.lindabrain.websocket.WebsocketMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author 82611
 * @description 针对表【chat】的数据库操作Service实现
 * @createDate 2025-05-17 17:22:21
 */
@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
        implements ChatService {

    @Resource
    private AiService aiService;

    @Resource
    private SessionService sessionService;

    @Resource
    private WebSocketConnectionManager webSocketConnectionManager;

    @Resource(name = "aiTaskExecutor")
    private ExecutorService aiTaskExecutor;

    private static final String LINDA_PROMPT = "" +
            "你将扮演一位虚拟人，是用户的好朋友。" +
            "你的名字是Linda，性别女，来自欧美某个国家，性格热情开朗，善解人意，心态乐观向上，对事物抱有好奇心。" +
            "你只会英文，你的回复必须全英文。" +
            "你的任务是，使用英语与用户进行日常交流，尽量使用地道的表达，以提升用户的英语对话能力。" +
            "你不需要解释自己的用语含义，除非用户问你。" +
            "你的表达需要比用户的英语水平稍微高一些，但又不至于让他看不懂而产生挫败感。" +
            "聊天时请用 emoji 代替动作和状态描述。";

    private static final String TITLE_PROMPT = "" +
            "请你总结我接下来发给你的信息，生成一个title，作为这段会话的标题。" +
            "这个标题应该是纯英文的，可以带有emoji。" +
            "这个标题的长度不应超过25个字符";

    private static final DeepSeekMessage SYSTEM = DeepSeekMessage.create()
            .setRole(Constants.DEEPSEEK_ROLE_SYSTEM)
            .setContent(LINDA_PROMPT);

    private static final DeepSeekMessage TITLE = DeepSeekMessage.create()
            .setRole(Constants.DEEPSEEK_ROLE_SYSTEM)
            .setContent(TITLE_PROMPT);

    private DeepSeekMessage getUsernamePromptMessage() {
        UserInfo userInfo = UserInfoContext.get();
        return DeepSeekMessage.create()
                .setRole(Constants.DEEPSEEK_ROLE_SYSTEM)
                .setContent("用户的名字是：" + userInfo.getUsername());
    }

    /**
     * 调用DeepSeek流式API并打印每个响应片段
     *
     * @param chatRequest 用户消息
     */
    @Transactional
    public void processUserMessage(ChatRequest chatRequest) {
        boolean newSession = StrUtil.isBlank(chatRequest.getSessionId());

        if (newSession) {
            processNewSession(chatRequest);
        } else {
            processOldSession(chatRequest);
        }
    }

    @Override
    public List<ChatInfoDto> getChatList(Long sessionId) {
        Long userId = UserInfoContext.get().getId();
        return this.lambdaQuery()
                .eq(Chat::getSessionId, sessionId)
                .in(Chat::getSender, Arrays.asList(userId, 0L))
                .list().stream()
                .map(ChatInfoDto::transferDto)
                .sorted(Comparator.comparing(ChatInfoDto::getCreateTime))
                .collect(Collectors.toList());
    }

    private void processNewSession(ChatRequest chatRequest) {
        // 直接调用AI
        Long userId = UserInfoContext.get().getId();
        List<DeepSeekMessage> messages = new ArrayList<>();
        messages.add(SYSTEM);
        messages.add(getUsernamePromptMessage());
        DeepSeekMessage userMessage = DeepSeekMessage.create()
                .setRole(Constants.DEEPSEEK_ROLE_USER)
                .setContent(chatRequest.getContent());
        messages.add(userMessage);
        // 新建会话
        Session session = saveNewSession(userId);

        // 保存用户消息
        saveUserMessage(session.getId(), userId, chatRequest.getContent());
        // 刷新 title
        titleGenerate(session.getId(), userId, Collections.singletonList(userMessage));

        // 与 Linda 沟通
        sendLindaAndReplyUser(session.getId(), userId, messages);
    }


    private void processOldSession(ChatRequest chatRequest) {
        Long userId = UserInfoContext.get().getId();
        List<DeepSeekMessage> messages = new ArrayList<>();
        messages.add(SYSTEM);
        messages.add(getUsernamePromptMessage());
        Long sessionId = Long.valueOf(chatRequest.getSessionId());
        // 查找历史聊天记录
        List<ChatInfoDto> chatInfoDtoList = this.getChatList(sessionId);
        // 构建用户历史聊天记录进去
        chatInfoDtoList.forEach(chatInfoDto -> {
            DeepSeekMessage userMessage = DeepSeekMessage.create()
                    .setRole(Constants.DEEPSEEK_ROLE_USER)
                    .setContent(chatInfoDto.getContent());
            messages.add(userMessage);
        });
        DeepSeekMessage userMessage = DeepSeekMessage.create()
                .setRole(Constants.DEEPSEEK_ROLE_USER)
                .setContent(chatRequest.getContent());
        messages.add(userMessage);

        // 保存用户消息
        saveUserMessage(sessionId, userId, chatRequest.getContent());

        // 与 Linda 沟通
        sendLindaAndReplyUser(sessionId, userId, messages);

    }

    private void sendLindaAndReplyUser(Long sessionId, Long userId, List<DeepSeekMessage> messages) {
        StringBuilder stringBuilder = new StringBuilder();
        String messageId = UUID.randomUUID().toString();
        try {
            // 调用AiService的流式API方法
            aiService.sendMessageToDeepSeekForStream(messages)
                    .subscribe(
                            content -> {
                                // 将AI的回复发给用户
                                WebsocketMessage websocketMessage = WebsocketMessage.createChatMessage();
                                websocketMessage.setMessageId(messageId);
                                websocketMessage.setContent(content);
                                websocketMessage.setSessionId(String.valueOf(sessionId));
                                stringBuilder.append(content);
                                webSocketConnectionManager.sendMessageToUser(userId, websocketMessage);
                            },
                            // 处理错误
                            error -> {
                                log.error("处理流式响应时发生错误", error);
                            },
                            // 处理完成
                            () -> {
                                log.info("流式响应处理完成");
                                // 保存 Linda 回复的消息
                                saveLindaMessage(sessionId, stringBuilder.toString());
                            }
                    );
        } catch (Exception e) {
            log.error("调用流式API失败", e);
        }
    }

    // 根据指定会话内容更新 title
    private void titleGenerate(Long sessionId, Long userId, List<DeepSeekMessage> userMessage) {
        List<DeepSeekMessage> messages = new ArrayList<>();
        messages.add(TITLE);
        messages.addAll(userMessage);
        sendMessageToDeepSeekAsync(messages)
                .thenAccept(result -> {
                    // 更新会话 title
                    sessionService.lambdaUpdate()
                            .eq(Session::getId, sessionId)
                            .eq(Session::getUserId, userId)
                            .set(Session::getTitle, result)
                            .update();
                    // 告知客户端更新指定会话的 title
                    WebsocketMessage websocketMessage = WebsocketMessage.createTitleMessage();
                    websocketMessage.setContent(result);
                    webSocketConnectionManager.sendMessageToUser(userId, websocketMessage);
                })
                .exceptionally(ex -> {
                    // 处理异常
                    return null;
                });
    }

    /**
     * 异步调用DeepSeek API发送消息并获取非流式响应
     * 支持链式调用风格，可以通过CompletableFuture的API处理异步结果
     * 例如：sendMessageToDeepSeekAsync(messages)
     * .thenAccept(result -> { 处理成功结果 })
     * .exceptionally(ex -> { 处理异常 });
     *
     * @param messages 消息列表
     * @return CompletableFuture对象，包含API调用结果
     */
    public CompletableFuture<String> sendMessageToDeepSeekAsync(List<DeepSeekMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 调用AiService的非流式API方法
                return aiService.sendMessageToDeepSeek(messages);
            } catch (Exception e) {
                log.error("异步调用DeepSeek API失败", e);
                // 重新抛出异常，让CompletableFuture处理
                throw new RuntimeException("异步调用DeepSeek API失败: " + e.getMessage(), e);
            }
        }, aiTaskExecutor); // 使用自定义线程池而不是默认的ForkJoinPool
    }


    private Session saveNewSession(Long userId) {
        Session session = new Session();
        session.setTitle("new chat");
        session.setUserId(userId);
        session.setCreateTime(new Date());
        sessionService.save(session);
        return session;
    }

    private void saveLindaMessage(Long sessionId, String message) {
        Chat chat = new Chat();
        chat.setSessionId(sessionId);
        chat.setCreateTime(new Date());
        chat.setMessage(message);
        // 0 表示 Linda
        chat.setSender(0L);
        this.save(chat);
    }

    private void saveUserMessage(Long sessionId, Long userId, String message) {
        Chat chat = new Chat();
        chat.setSessionId(sessionId);
        chat.setSender(userId);
        chat.setMessage(message);
        chat.setCreateTime(new Date());
        this.save(chat);
    }
}




