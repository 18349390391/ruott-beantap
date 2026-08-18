package com.ruott.beantap.utils;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 配置加载工具：复用 Spring Boot 启动时的配置加载流程，
 * 只加载 application.yml / profile / spring.config.import 等配置，不启动容器。
 */
public class ConfigurableUtil {

    private static final ConfigurableEnvironment ENV = loadEnvironment();

    private ConfigurableUtil() {
    }

    private static ConfigurableEnvironment loadEnvironment() {
        Object originalLevel = silenceAllLogs();
        try {
            ConfigurableEnvironment env = new StandardEnvironment();
            ConfigDataEnvironmentPostProcessor.applyTo(env);
            return env;
        } finally {
            restoreLogs(originalLevel);
        }
    }

    /**
     * 配置加载前临时把根 logger 关掉，避免配置加载过程（yml 解析、Nacos 拉取等）刷屏。
     * 只影响测试 JVM 且仅在加载配置期间生效，不影响被测应用的日志。
     *
     * @return 原来的根 logger 级别，用于加载后恢复；非 logback 环境返回 null
     */
    private static Object silenceAllLogs() {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                    org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            ch.qos.logback.classic.Level original = root.getLevel();
            root.setLevel(ch.qos.logback.classic.Level.OFF);
            return original;
        } catch (Throwable ignore) {
            // 非 logback 环境直接忽略
            return null;
        }
    }

    private static void restoreLogs(Object originalLevel) {
        if (originalLevel == null) {
            return;
        }
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                    org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel((ch.qos.logback.classic.Level) originalLevel);
        } catch (Throwable ignore) {
            // 非 logback 环境直接忽略
        }
    }

    /**
     * 获取配置属性，不存在返回 null
     */
    public static String getProperty(String key) {
        return ENV.getProperty(key);
    }

    /**
     * 获取配置属性，不存在返回默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return ENV.getProperty(key, defaultValue);
    }
}
