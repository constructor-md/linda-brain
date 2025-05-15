package com.awesome.lindabrain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SYSTEM_ERROR(600500, "系统内部异常"),
    PARAMS_ERROR(600501, "请求参数错误"),
    NOT_LOGIN_ERROR(600502, "未登录"),
    OPERATOR_ERROR(600503, "操作失败"),
    REQUEST_LIMITED(600504, "请求频率限制中"),
    USERNAME_EXIST(600505, "用户名重复"),
    NOT_REGISTER(600506, "未注册"),
    FORBIDDEN_ERROR(600507, "权限不足，禁止访问"),
    PASSWD_ERROR(600508, "密码错误"),
    NOT_FOUND(600509, "找不到数据"),

    ;

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }






}
