package com.ruott.beantap.client;


import com.alibaba.fastjson.JSON;
import com.ruott.beantap.exception.BeanTapIllegalStateException;
import com.ruott.beantap.model.BeantapRequest;
import com.ruott.beantap.model.BeantapResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;


@Slf4j
public class VertHttpClient {

    private final ApplicationContext context;

    public VertHttpClient(ApplicationContext context) {
        this.context = context;
    }


    public String invoke(BeantapRequest request) throws Exception {
        Object bean = this.resolveBean(request);
        // 获取原始目标类，规避CGLIB代理
        Class<?> targetClazz = AopUtils.getTargetClass(bean);

        Method method = targetClazz.getMethod(request.getMethodName(), request.getParameterTypes());
        method.setAccessible(true);

        // 遍历参数数组，按泛型类型转换 防止JSON转换时类型擦除问题（如 List<RpaTaskReportReq> 元素残留 JSONObject）
        Object[] args = this.convertArgs(request.getArgs(), method.getGenericParameterTypes());


        BeantapResponse response = new BeantapResponse();
        Object invoke = null;
        try {
            invoke = method.invoke(bean, args);
            response.setCode(200);
            response.setData(invoke);
            if (invoke != null) {
                Class<?> aClass = invoke.getClass();
                response.setDataType(aClass);
            }
            response.setMessage("success");
        } catch (Exception e) {
            Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
            log.error("Beantap invoke error", cause);
            response.setCode(500);
            response.setMessage(cause.toString());
            response.setException(cause instanceof Exception ? (Exception) cause : e);
        }
        return JSON.toJSONString(response);
    }

    private Object[] convertArgs(Object[] rawArgs, Type[] paramTypes) {
        if (rawArgs == null || rawArgs.length == 0) {
            return rawArgs;
        }
        Object[] converted = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = rawArgs[i];
            if (arg == null) {
                converted[i] = null;
            } else if (paramTypes[i] instanceof Class && ((Class<?>) paramTypes[i]).isInstance(arg)) {
                // 普通类型直接复用；泛型容器(List<T>/Map<K,V>)不能仅凭原始类型判断，需按下述分支按泛型转换元素
                converted[i] = arg;
            } else {
                converted[i] = JSON.parseObject(JSON.toJSONString(arg), paramTypes[i]);
            }
        }
        return converted;
    }

    private Object resolveBean(BeantapRequest request) throws Exception {
        String beanName = request.getBeanName();

        // 1. 显式指定了 name：严格按名字找，找不到直接抛 NoSuchBeanDefinitionException，不兜底
        if (beanName != null && !beanName.trim().isEmpty()) {
            return context.getBean(beanName);
        }

        // 2. 未指定：先按默认名（声明类型首字母小写）试
        Class<?> serviceType = Class.forName(request.getServiceName());
        String defaultName = generateDefaultBeanName(serviceType);
        try {
            return context.getBean(defaultName);
        } catch (NoSuchBeanDefinitionException ignored) {
            System.out.println("default bean name is null: " + defaultName);
        }

        // 3. 按类型找，多个实现时报错引导用户指定 name
        try {
            return context.getBean(serviceType);
        } catch (NoUniqueBeanDefinitionException e) {
            throw new BeanTapIllegalStateException(
                    "类型 [" + serviceType.getName() + "] 存在多个实现 bean" + e.getBeanNamesFound()
                            + "，请在 @TestBeantap(name = ...) 中显式指定", e);
        }
    }

    private String generateDefaultBeanName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        if (className.length() > 1
                && Character.isUpperCase(className.charAt(0))
                && Character.isUpperCase(className.charAt(1))) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

}
