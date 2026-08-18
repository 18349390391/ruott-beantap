package com.ruott.beantap.core;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;

public class BeantapExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        System.out.println("enter beantap extension beforeEach……parse beantapInit");
        Object testInstance = extensionContext.getRequiredTestInstance();
        Class<?> testClass = extensionContext.getRequiredTestClass();

        if (!testClass.isAnnotationPresent(BeantapInit.class)) {
            return;
        }

        //获取所有字段
        Field[] fields = testClass.getDeclaredFields();

        for (Field field : fields) {
            boolean annotationPresent = field.isAnnotationPresent(TestBeantap.class);
            if (!annotationPresent)
                continue;
            field.setAccessible(true);

            TestBeantap testBeantap = field.getAnnotation(TestBeantap.class);
            String name = testBeantap.name();

            Class<?> fieldType = field.getType();

            Object proxy = null;
            // 判断字段类型是接口还是正常的class
            if (fieldType.isInterface()) {
                proxy = JDKBeantapProxyFactory.getProxy(fieldType, name);
            } else {
                proxy = ByteBuddyBeantapProxyFactory.getProxy(fieldType, name);
            }
            field.set(testInstance, proxy);
        }


    }

}
