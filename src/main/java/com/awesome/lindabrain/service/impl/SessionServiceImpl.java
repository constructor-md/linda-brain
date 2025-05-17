package com.awesome.lindabrain.service.impl;

import com.awesome.lindabrain.aop.UserInfoContext;
import com.awesome.lindabrain.mapper.SessionMapper;
import com.awesome.lindabrain.model.dto.SessionInfoDto;
import com.awesome.lindabrain.model.entity.Chat;
import com.awesome.lindabrain.model.entity.Session;
import com.awesome.lindabrain.service.ChatService;
import com.awesome.lindabrain.service.SessionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 82611
* @description 针对表【session】的数据库操作Service实现
* @createDate 2025-05-17 17:22:21
*/
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session>
    implements SessionService{

    @Resource
    @Lazy
    private ChatService chatService;

    @Override
    public List<SessionInfoDto> getSessionList() {
        Long userId = UserInfoContext.get().getId();
        return this.lambdaQuery()
                .eq(Session::getUserId, userId)
                .list().stream()
                .map(SessionInfoDto::transferDto)
                .sorted(Comparator.comparing(SessionInfoDto::getCreateTime))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        this.removeById(sessionId);
        chatService.remove(new QueryWrapper<Chat>().lambda().eq(Chat::getSessionId, sessionId));
    }
}




