package com.ruott.beantap.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.ruott.beantap.constant.UriConstant;
import com.ruott.beantap.client.VertHttpClient;
import com.ruott.beantap.model.BeantapRequest;
import com.ruott.beantap.model.BeantapResponse;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxHttpServerHandler implements Handler<HttpServerRequest> {


    private final VertHttpClient endpoint;

    public VertxHttpServerHandler(VertHttpClient endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void handle(HttpServerRequest request) {
        String uri = request.uri();
        String method = request.method().name();

        if (!"POST".equalsIgnoreCase(method) || !uri.equals(UriConstant._BEANTAP_URI)) {
            request.response().setStatusCode(404).end();
            return;
        }

        HttpServerResponse response = request.response();
        response.putHeader("Content-Type", "application/json;charset=utf-8");

        request.body().onSuccess(body -> {
            try {
                BeantapRequest tapRequest = JSON.parseObject(body.toString(), BeantapRequest.class, JSONReader.Feature.SupportClassForName);
                String result = endpoint.invoke(tapRequest);
                response.end(result);
            } catch (Exception e) {
                log.error("Beantap invoke error", e);
                BeantapResponse errResp = new BeantapResponse();
                errResp.setCode(500);
                errResp.setMessage("BeantapServer error: " + e.getMessage());
                response.end(JSON.toJSONString(errResp));
            }
        }).onFailure(err -> {
            log.error("Beantap read request body error", err);
            response.setStatusCode(500).end("{\"code\":500,\"message\":\"read body failed\"}");
        });
    }
}
