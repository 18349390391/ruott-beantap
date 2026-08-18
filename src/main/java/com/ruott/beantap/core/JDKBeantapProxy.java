package com.ruott.beantap.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public final class JDKBeantapProxy implements InvocationHandler {

    private final String beanName;

    public JDKBeantapProxy(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("enter jdk proxy……");
        return BeantapProxy.invoke(method,args,beanName);
    }

}
