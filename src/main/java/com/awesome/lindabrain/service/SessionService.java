package com.awesome.lindabrain.service;

import com.awesome.lindabrain.model.dto.SessionInfoDto;
import com.awesome.lindabrain.model.entity.Session;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 82611
* @description 针对表【session】的数据库操作Service
* @createDate 2025-05-17 17:22:21
*/
public interface SessionService extends IService<Session> {

    List<SessionInfoDto> getSessionList();

    void deleteSession(Long sessionId);
}
