package com.awesome.lindabrain.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private List<String> slaveDataSources = new ArrayList<>();

    public void setSlaveDataSources(List<String> slaveDataSources) {
        this.slaveDataSources = slaveDataSources;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String dataSource = DataSourceContextHolder.getDataSource();
        if (DataSourceContextHolder.MASTER.equals(dataSource)) {
            log.info("采用master数据源");
            return dataSource;
        }
        // 只要指定非主库，就取从库 并对从库负载均衡
        if (slaveDataSources.size() > 0) {
            int index = counter.getAndIncrement() % slaveDataSources.size();
            log.info("采用数据源：{}", slaveDataSources.get(index));
            return slaveDataSources.get(index);
        }
        log.info("采用数据源：{}", DataSourceContextHolder.MASTER);
        return DataSourceContextHolder.MASTER;
    }
}
