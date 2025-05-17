package com.awesome.lindabrain.service.impl;

import com.awesome.lindabrain.mapper.SysConfigMapper;
import com.awesome.lindabrain.model.entity.SysConfig;
import com.awesome.lindabrain.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author 82611
* @description 针对表【sys_config】的数据库操作Service实现
* @createDate 2025-05-09 19:25:40
*/
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
    implements SysConfigService{

    /**
     * 根据键获取系统配置值
     * @param key 配置键
     * @return 配置值
     */
    @Override
    public String getConfigValue(String key) {
        SysConfig config = getConfigByKey(key);
        return config != null ? config.getSysValue() : null;
    }
    
    /**
     * 根据键获取系统配置对象
     * @param key 配置键
     * @return 配置对象
     */
    @Override
    public SysConfig getConfigByKey(String key) {
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getSysKey, key);
        return getOne(queryWrapper);
    }
}




