package com.ruott.beantap.model;

import lombok.Data;

@Data
public class BeantapRequest {

    //Spring Bean Name
    private String beanName;

    //全限定类名
    private String serviceName;

    //方法名称
    private String methodName;

    //请求参数类型列表
    private Class<?>[] parameterTypes;

    //请求参数
    private Object[] args;
}
