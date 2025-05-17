package com.awesome.lindabrain.controller;

import com.awesome.lindabrain.commons.R;
import com.awesome.lindabrain.model.request.LoginRequest;
import com.awesome.lindabrain.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RequestMapping("/user")
@RestController
@Slf4j
public class UserController {

    @Resource
    private UserInfoService userInfoService;


    @PostMapping("/register")
    public R<?> register(@RequestBody LoginRequest registerRequest) {
        userInfoService.register(registerRequest);
        return R.ok();
    }

    @PostMapping("/login")
    public R<?> login(@RequestBody LoginRequest loginRequest) {
        return R.ok(userInfoService.login(loginRequest));
    }


}
