package com.awesome.lindabrain.service;

import com.awesome.lindabrain.model.request.LoginRequest;
import com.awesome.lindabrain.model.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 82611
* @description 针对表【user_info】的数据库操作Service
* @createDate 2025-04-09 16:15:40
*/
public interface UserInfoService extends IService<UserInfo> {

    void register(LoginRequest registerRequest);

    String login(LoginRequest loginRequest);

    boolean isAdmin();


}
