package com.awesome.lindabrain.service;

import com.awesome.lindabrain.model.entity.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 82611
* @description 针对表【sys_config】的数据库操作Service
* @createDate 2025-05-09 19:25:40
*/
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据键获取系统配置值
     * @param key 配置键
     * @return 配置值
     */
    String getConfigValue(String key);
    
    /**
     * 根据键获取系统配置对象
     * @param key 配置键
     * @return 配置对象
     */
    SysConfig getConfigByKey(String key);
}
