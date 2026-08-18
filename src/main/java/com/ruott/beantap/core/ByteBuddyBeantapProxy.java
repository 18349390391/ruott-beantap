package com.ruott.beantap.core;


import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;

public final class ByteBuddyBeantapProxy {

    private final String beanName;

    public ByteBuddyBeantapProxy(String beanName) {
        this.beanName = beanName;
    }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Exception {
        System.out.println("enter byte buddy proxy……");
        return BeantapProxy.invoke(method, args, beanName);
    }
}
