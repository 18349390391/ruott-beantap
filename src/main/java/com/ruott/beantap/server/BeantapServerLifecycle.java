package com.ruott.beantap.server;

import com.ruott.beantap.client.VertHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * beantap 服务端生命周期管理。
 *
 * <p>等 Spring 容器完全启动（{@link ApplicationReadyEvent}）后才拉起 HTTP 端点，
 * 避免启动窗口期内的调用打到尚未 refresh 的容器（has not been refreshed yet）。</p>
 *
 * <p>容器关闭（含启动失败回滚、进程正常退出）时回收 Vert.x 线程并删除端口文件，
 * 避免端点变僵尸、脏端口文件把测试端导向死端口。</p>
 */
public class BeantapServerLifecycle implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private final VertxHttpServer server;
    private final AtomicBoolean started = new AtomicBoolean();

    public BeantapServerLifecycle(VertHttpClient vertHttpClient) {
        this.server = new VertxHttpServer(vertHttpClient);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (started.compareAndSet(false, true)) {
            server.doService(0);
        }
    }

    @Override
    public void destroy() {
        if (started.get()) {
            server.shutdown();
            VertxTmpdirPort.getInstance().delete();
        }
    }
}
