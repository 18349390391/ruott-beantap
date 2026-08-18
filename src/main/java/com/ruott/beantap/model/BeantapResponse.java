package com.ruott.beantap.model;

import lombok.Data;

@Data
public class BeantapResponse {

    //响应数据
    private Object data;

    //响应数据类型
    private Class<?> dataType;

    //响应状态码
    private Integer code;

    //响应信息
    private String message;

    //异常异常
    private Exception exception;
}
