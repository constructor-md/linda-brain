package com.awesome.lindabrain.commons;


import com.awesome.lindabrain.exception.BusinessException;
import com.awesome.lindabrain.exception.ErrorCode;
import lombok.Data;

@Data
public class R<T> {

    private int code;
    private String msg;
    private T data;

    private R() {
        this.code = 200;
        this.msg = "success";
    }

    private R(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static R<?> ok() {
        return new R<>();
    }

    public static <T> R<T> ok(T data) {
        R<T> result = new R<>();
        result.setData(data);
        return result;
    }

    public static R<?> error(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg());
    }

    public static R<?> error(BusinessException e) {
        return new R<>(e.getCode(), e.getMsg());
    }

}
