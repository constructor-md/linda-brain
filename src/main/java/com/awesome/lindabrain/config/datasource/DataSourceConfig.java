package com.awesome.lindabrain.config.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源配置读取和数据源Bean生成
 */
@Configuration
public class DataSourceConfig {


    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    @Bean(name = "slave1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave1")
    public DataSource slave1Source() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    @Bean(name = "slave2DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave2")
    public DataSource slave2DataSource() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(@Qualifier("masterDataSource") DataSource masterDataSource,
                                        @Qualifier("slave1DataSource") DataSource slave1DataSource,
                                        @Qualifier("slave2DataSource") DataSource slave2DataSource) {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("slave1", slave1DataSource);
        targetDataSources.put("slave2", slave2DataSource);

        List<String> slaveDataSources = new ArrayList<>();
        slaveDataSources.add("slave1");
        slaveDataSources.add("slave2");

        dynamicDataSource.setSlaveDataSources(slaveDataSources);

        // 设置默认数据源
        // 当AbstractRoutingDataSource 的determineCurrentLookupKey
        // 返回null或者没有匹配到具体数据源，就使用默认数据源
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource);
        // 设置所有可用数据源 key是数据源的标识，值是DataSource
        // 由AbstractRoutingDataSource 的determineCurrentLookupKey方法
        // 确定要使用的数据源的标识
        dynamicDataSource.setTargetDataSources(targetDataSources);
        return dynamicDataSource;

    }

}
