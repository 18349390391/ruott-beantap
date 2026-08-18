package com.ruott.beantap.server;

import com.ruott.beantap.utils.ConfigurableUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VertxTmpdirPort {

    private static final VertxTmpdirPort INSTANCE = new VertxTmpdirPort();

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String FILE_PREFIX = "vertx_beantap-port";

    private VertxTmpdirPort() {
    }

    public static VertxTmpdirPort getInstance() {
        return INSTANCE;
    }

    public void write(int port) {
        try {
            String appName = ConfigurableUtil.getProperty("spring.application.name");
            Path portFile = Path.of(TMP_DIR, FILE_PREFIX + "_" + appName);
            Files.writeString(portFile, String.valueOf(port));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write beantap port file", e);
        }
    }

    public int read() {
        String appName = ConfigurableUtil.getProperty("spring.application.name");
        Path portFile = Path.of(TMP_DIR, FILE_PREFIX + "_" + appName);
        try {
            return Integer.parseInt(Files.readString(portFile).trim());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read beantap port file: " + portFile
                    + "，请确认被测应用已从 IDE 启动且启动成功（启动日志中应出现 'http server started on port'）", e);
        }
    }

    public void delete() {
        try {
            String appName = ConfigurableUtil.getProperty("spring.application.name");
            Files.deleteIfExists(Path.of(TMP_DIR, FILE_PREFIX + "_" + appName));
        } catch (IOException ignored) {
        }
    }
}