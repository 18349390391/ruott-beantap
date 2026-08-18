package com.ruott.beantap.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class NativeHttpUtil {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * GET请求
     * @param url 请求地址
     * @return 响应字符串
     * @throws Exception
     */
    public static String doGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.body();
    }

    /**
     * POST JSON 请求
     * @param url 地址
     * @param jsonBody json字符串
     * @return 返回响应体
     * @throws Exception
     */
    public static String doPostJson(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        return response.body();
    }

    // 测试示例
    public static void main(String[] args) throws Exception {
        String getResp = doGet("https://httpbin.org/get");
        System.out.println("GET响应：" + getResp);

        String json = "{\"name\":\"test\"}";
        String postResp = doPostJson("https://httpbin.org/post", json);
        System.out.println("POST响应：" + postResp);
    }
}
