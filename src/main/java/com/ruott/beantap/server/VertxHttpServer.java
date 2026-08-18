package com.ruott.beantap.server;

import com.ruott.beantap.client.VertHttpClient;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class VertxHttpServer implements Server {

    private final VertHttpClient vertHttpClient;

    private volatile Vertx vertx;

    public VertxHttpServer(VertHttpClient vertHttpClient) {
        this.vertHttpClient = vertHttpClient;
    }

    @Override
    public void doService(int port) {
        //获取实例
        VertxOptions options = new VertxOptions()
                .setMaxEventLoopExecuteTime(10 * 60 * 1000)
                .setBlockedThreadCheckInterval(10 * 60 * 1000);
        this.vertx = Vertx.vertx(options);

        //获取服务端
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(new VertxHttpServerHandler(vertHttpClient));

        //启动http监听端口
        server.listen(port, result -> {
            if (result.succeeded()) {
                int actualPort = result.result().actualPort();
                VertxTmpdirPort.getInstance().write(actualPort);
                log.info("http server started on port {}", actualPort);
            } else {
                log.error("http server start failed", result.cause());
            }
        });
    }

    /**
     * 关闭 Vert.x，回收事件循环线程，等待最多 10 秒。
     */
    public void shutdown() {
        Vertx v = this.vertx;
        if (v != null) {
            try {
                v.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
                log.info("beantap http server closed");
            } catch (Exception e) {
                log.warn("beantap http server close error", e);
            }
        }
    }
}
