package com.ruott.beantap.core;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.ruott.beantap.constant.UriConstant;
import com.ruott.beantap.exception.BeanTapIllegalStateException;
import com.ruott.beantap.exception.BeanTapRunTimeException;
import com.ruott.beantap.model.BeantapRequest;
import com.ruott.beantap.model.BeantapResponse;
import com.ruott.beantap.server.VertxTmpdirPort;
import com.ruott.beantap.utils.NativeHttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.logging.Logger;

public class BeantapProxy {

    private final static Logger logger = Logger.getLogger(BeantapProxy.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Object invoke(Method method, Object[] args, String beanName) throws Exception {
        int port = VertxTmpdirPort.getInstance().read();

        //构建请求参数
        BeantapRequest request = new BeantapRequest();
        request.setBeanName(beanName);
        request.setServiceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setArgs(args);

        String url = UriConstant.HOST + ":" + port + UriConstant._BEANTAP_URI;

        String data;
        try {
            data = NativeHttpUtil.doPostJson(url, JSON.toJSONString(request));
        } catch (Exception e) {
            throw new BeanTapRunTimeException("无法连接 beantap 服务端（" + url + "），"
                    + "请确认被测应用已从 IDE 启动且启动成功"
                    + "（启动日志中应出现 'http server started on port'）: " + e.getMessage(), e);
        }
        BeantapResponse response = null;

        try {
            response = JSON.parseObject(data, BeantapResponse.class);
        } catch (Exception e) {
            throw new BeanTapIllegalStateException("BeantapResponse 序列化失败, 服务端原始返回：" + data, e);
        }

        if (response.getCode() == 200) {
            return convertResult(response.getData(), method);
        }
        throw new BeanTapRunTimeException("Beantap执行失败: " + response.getMessage());
    }

    /**
     * 按方法声明的返回类型反序列化结果，避免代理方法返回时 ClassCastException。
     */
    private static Object convertResult(Object data, Method method) {
        logger.info("convertResult parse data: [" + data + "]");
        Type returnGenericType = method.getGenericReturnType();
        try {
            return OBJECT_MAPPER.convertValue(data, OBJECT_MAPPER.constructType(returnGenericType));
        } catch (Exception e) {
            throw new BeanTapIllegalStateException("Beantap 远程调用返回结果反序列化失败：期望类型 [" + method.getGenericReturnType().getTypeName() + "]，服务端原始返回：" + data, e);
        }
    }
}
