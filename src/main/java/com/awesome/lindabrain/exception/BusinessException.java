package com.awesome.lindabrain.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private int code;
    private String msg;

    private BusinessException(){}

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public BusinessException(ErrorCode errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
        this.msg = msg;
    }
}
