package com.awesome.lindabrain.aop;


import com.awesome.lindabrain.model.entity.UserInfo;

public class UserInfoContext {

    private static final ThreadLocal<UserInfo> currentUser = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        currentUser.set(userInfo);
    }

    public static UserInfo get() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }

}
