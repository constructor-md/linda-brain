package com.awesome.lindabrain.exception;


import com.awesome.lindabrain.commons.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        log.error("", e);
        return R.error(e);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e){
        log.error("", e);
        return R.error(ErrorCode.SYSTEM_ERROR);
    }


}
