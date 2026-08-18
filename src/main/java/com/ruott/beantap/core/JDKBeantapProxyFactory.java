package com.ruott.beantap.core;

import java.lang.reflect.Proxy;

public class JDKBeantapProxyFactory {

    public static <T> T getProxy(Class<T> clazz, String beanName) {
        System.out.println("正在创建代理对象：" + clazz);

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                new JDKBeantapProxy(beanName)
        );
    }
}
