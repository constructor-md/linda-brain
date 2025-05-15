package com.awesome.lindabrain.config.datasource;

/**
 * 保存当前指定的数据源的标识
 * 未指定则返回master
 */
public class DataSourceContextHolder {

    public static final String MASTER = "master";
    // 只要指定非主库，就取从库
    public static final String SALVE = "slave";
    public static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setDataSource(String dataSource) {
        contextHolder.set(dataSource);
    }

    public static String getDataSource() {
        String datasource = contextHolder.get();
        // 没有设置数据源时指定为master
        return datasource == null ? MASTER : datasource;
    }

    public static void clearDataSource() {
        contextHolder.remove();
    }

}
