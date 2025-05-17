package com.awesome.lindabrain.service.impl;

import cn.hutool.crypto.digest.MD5;
import com.awesome.lindabrain.aop.UserInfoContext;
import com.awesome.lindabrain.commons.Constants;
import com.awesome.lindabrain.exception.BusinessException;
import com.awesome.lindabrain.exception.ErrorCode;
import com.awesome.lindabrain.exception.ThrowUtils;
import com.awesome.lindabrain.mapper.UserInfoMapper;
import com.awesome.lindabrain.model.request.LoginRequest;
import com.awesome.lindabrain.service.UserInfoService;
import com.awesome.lindabrain.model.entity.UserInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
* @author 82611
* @description 针对表【user_info】的数据库操作Service实现
* @createDate 2025-04-09 16:15:40
*/
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void register(LoginRequest registerRequest) {
        // 检查用户名是否重复
        boolean exist = this.baseMapper
                .exists(new QueryWrapper<UserInfo>()
                        .lambda().eq(UserInfo::getUsername, registerRequest.getUsername()));
        ThrowUtils.throwIf(exist, new BusinessException(ErrorCode.USERNAME_EXIST));
        // 加密密码
        String password = MD5.create().digestHex(registerRequest.getPassword());
        // 注册
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(registerRequest.getUsername());
        userInfo.setPassword(password);
        userInfo.setCreateTime(new Date());
        userInfo.setUpdateTime(new Date());
        this.baseMapper.insert(userInfo);

    }

    @Override
    @Transactional
    public String login(LoginRequest loginRequest) {
        // 加密密码
        String password = MD5.create().digestHex(loginRequest.getPassword());
        // 检查用户名
        UserInfo check = this.baseMapper
                .selectOne(new QueryWrapper<UserInfo>()
                        .lambda()
                        .eq(UserInfo::getUsername, loginRequest.getUsername()));
        ThrowUtils.throwIf(check == null, new BusinessException(ErrorCode.NOT_REGISTER));
        ThrowUtils.throwIf(!password.equals(check.getPassword()), new BusinessException(ErrorCode.PASSWD_ERROR));
        // 检查用户名密码
        UserInfo userInfo = this.baseMapper
                .selectOne(new QueryWrapper<UserInfo>()
                        .lambda()
                        .eq(UserInfo::getUsername, loginRequest.getUsername())
                        .eq(UserInfo::getPassword, password));
        // 生成token
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(Constants.REDIS_ACCESS_TOKEN_PREFIX + token, userInfo.getId(), 1, TimeUnit.DAYS);
        return token;
    }

    @Override
    public boolean isAdmin() {
        UserInfo userInfo = UserInfoContext.get();
        return "admin".equals(userInfo.getUsername());
    }
}




