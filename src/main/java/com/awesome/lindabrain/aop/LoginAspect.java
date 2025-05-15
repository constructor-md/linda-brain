package com.awesome.lindabrain.aop;

import cn.hutool.core.util.StrUtil;
import com.awesome.lindabrain.commons.Constants;
import com.awesome.lindabrain.exception.BusinessException;
import com.awesome.lindabrain.exception.ErrorCode;
import com.awesome.lindabrain.model.entity.UserInfo;
import com.awesome.lindabrain.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(value = 1)
@Slf4j
public class LoginAspect {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserInfoService userInfoService;

    @Pointcut(value = "@annotation(com.awesome.lindabrain.annotation.Login))")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object checkToken(ProceedingJoinPoint joinPoint) throws Throwable {

        try {
            RequestAttributes requestAttribute = RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttribute).getRequest();
            String token = request.getHeader("token");
            if (StrUtil.isBlank(token)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            // 检查有效
            Long userId = (Long) redisTemplate.opsForValue().get(Constants.REDIS_ACCESS_TOKEN_PREFIX + token);
            if (userId == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            } else {
                // 自动续期
                redisTemplate.opsForValue().set(Constants.REDIS_ACCESS_TOKEN_PREFIX + token, userId, 1, TimeUnit.DAYS);
                // 保存用户信息上下文以便使用
                UserInfo userInfo = userInfoService.getById(userId);
                UserInfoContext.set(userInfo);
            }
            return joinPoint.proceed();
        } finally {
            // 清理上下文 避免内存泄漏
            UserInfoContext.clear();
        }
    }


}
