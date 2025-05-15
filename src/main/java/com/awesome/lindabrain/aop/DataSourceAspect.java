package com.awesome.lindabrain.aop;

import com.awesome.lindabrain.annotation.DataSource;
import com.awesome.lindabrain.config.datasource.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class DataSourceAspect {


    @Before("@annotation(com.awesome.lindabrain.annotation.DataSource)")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataSource dataSource = method.getAnnotation(DataSource.class);
        // 将注解上配置的数据源名称设置到holder
        if (dataSource != null) {
            DataSourceContextHolder.setDataSource(dataSource.value());
        }
    }

    @After("@annotation(com.awesome.lindabrain.annotation.DataSource)")
    public void after() {
        DataSourceContextHolder.clearDataSource();
    }

}
