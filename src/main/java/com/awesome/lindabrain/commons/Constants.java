package com.awesome.lindabrain.commons;

import io.netty.util.AttributeKey;

public class Constants {

    public static final String SERVICE_NAME = "Linda";

    // TOKEN Redis Key前缀
    public static final String REDIS_ACCESS_TOKEN_PREFIX = "ACCESS_TOKEN::" + SERVICE_NAME + "::";

    // 硅基流动文本对话URL
    public static final String SILICON_FLOW_CHAT_URL = "SILICON_FLOW_CHAT_URL";
    // 硅基流动API_KEY
    public static final String SILICON_FLOW_API_KEY = "SILICON_FLOW_API_KEY";

    // 请求频率限制锁前缀
    public static final String REDIS_REQUEST_LIMITED_PREFIX = "REDIS_REQUEST_LIMITED_PREFIX::";

    // WebSocket Channel属性键
    public static final AttributeKey<Long> CHANNEL_ATTR_USER_ID = AttributeKey.valueOf("userId");

}
