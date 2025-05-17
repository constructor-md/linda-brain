package com.awesome.lindabrain.controller;

import com.awesome.lindabrain.annotation.Login;
import com.awesome.lindabrain.commons.R;
import com.awesome.lindabrain.model.dto.SessionInfoDto;
import com.awesome.lindabrain.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 会话信息控制器
 */
@RequestMapping("/session")
@RestController
@Slf4j
public class SessionController {

    @Resource
    private SessionService sessionService;

    /**
     * 分页获取当前用户会话列表
     * 前端滚动查询，每页20条
     */
    @GetMapping("/list")
    @Login
    public R<List<SessionInfoDto>> getSessionList() {
        return R.ok(sessionService.getSessionList());
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/info")
    @Login
    public R<?> deleteSession(@RequestParam("id") String sessionId) {
        sessionService.deleteSession(Long.valueOf(sessionId));
        return R.ok();
    }


}
