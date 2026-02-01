package io.github.timemachinelab.util;

import okhttp3.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类，基于OkHttp封装
 * 
 * @author TimeMachineLab
 * @since 1.0
 */
public class HttpUtils {

    private static final int DEFAULT_TIMEOUT = 60;
    private static final String DEFAULT_CHARSET = "UTF-8";

    // 默认客户端和代理客户端
    private static final OkHttpClient DEFAULT_CLIENT;
    private static volatile OkHttpClient PROXY_CLIENT;

    // 代理配置
    private static volatile ProxyConfig proxyConfig;

    static {
        DEFAULT_CLIENT = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    /**
     * 代理配置类
     */
    public static class ProxyConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final Proxy.Type type;

        public ProxyConfig(String host, int port) {
            this(host, port, null, null, Proxy.Type.HTTP);
        }

        public ProxyConfig(String host, int port, String username, String password) {
            this(host, port, username, password, Proxy.Type.HTTP);
        }

        public ProxyConfig(String host, int port, String username, String password, Proxy.Type type) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.type = type;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public Proxy.Type getType() {
            return type;
        }

        public boolean hasAuth() {
            return username != null && !username.isEmpty() && password != null;
        }
    }

    /**
     * 设置代理配置
     * 
     * @param config 代理配置
     */
    public static void setProxyConfig(ProxyConfig config) {
        proxyConfig = config;
        if (config != null) {
            PROXY_CLIENT = createProxyClient(config);
        } else {
            PROXY_CLIENT = null;
        }
    }

    /**
     * 清除代理配置
     */
    public static void clearProxyConfig() {
        proxyConfig = null;
        PROXY_CLIENT = null;
    }

    /**
     * 获取当前代理配置
     * 
     * @return 代理配置
     */
    public static ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    /**
     * 创建代理客户端
     * 
     * @param config 代理配置
     * @return OkHttpClient
     */
    private static OkHttpClient createProxyClient(ProxyConfig config) {
        Proxy proxy = new Proxy(config.getType(), new InetSocketAddress(config.getHost(), config.getPort()));

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(true)
                .proxy(proxy);

        if (config.hasAuth()) {
            builder.proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic(config.getUsername(), config.getPassword());
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            });
        }

        return builder.build();
    }

    /**
     * 获取当前使用的客户端
     * 
     * @return OkHttpClient
     */
    private static OkHttpClient getCurrentClient() {
        return PROXY_CLIENT != null ? PROXY_CLIENT : DEFAULT_CLIENT;
    }

    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * 发送GET请求
     * 
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        Request request = requestBuilder.build();
        try (Response response = getCurrentClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body() != null ? response.body().string() : "";
            } else {
                throw new IOException("HTTP请求失败，状态码: " + response.code());
            }
        }
    }

    /**
     * 发送POST请求
     * 
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String post(String url, String body) throws IOException {
        return post(url, body, "application/json", null);
    }

    /**
     * 发送POST请求
     * 
     * @param url         请求URL
     * @param body        请求体
     * @param contentType 内容类型
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String post(String url, String body, String contentType) throws IOException {
        return post(url, body, contentType, null);
    }

    /**
     * 发送POST请求
     * 
     * @param url         请求URL
     * @param body        请求体
     * @param contentType 内容类型
     * @param headers     请求头
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String post(String url, String body, String contentType, Map<String, String> headers)
            throws IOException {
        RequestBody requestBody = body != null
                ? RequestBody.create(body, MediaType.parse(contentType != null ? contentType : "application/json"))
                : RequestBody.create("", MediaType.parse("application/json"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (contentType != null) {
            requestBuilder.header("Content-Type", contentType);
        }

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        Request request = requestBuilder.build();
        try (Response response = getCurrentClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body() != null ? response.body().string() : "";
            } else {
                throw new IOException("HTTP请求失败，状态码: " + response.code());
            }
        }
    }

    /**
     * 发送PUT请求
     * 
     * @param url  请求URL
     * @param body 请求体
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String put(String url, String body) throws IOException {
        return put(url, body, "application/json", null);
    }

    /**
     * 发送PUT请求
     * 
     * @param url         请求URL
     * @param body        请求体
     * @param contentType 内容类型
     * @param headers     请求头
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String put(String url, String body, String contentType, Map<String, String> headers)
            throws IOException {
        RequestBody requestBody = body != null
                ? RequestBody.create(body, MediaType.parse(contentType != null ? contentType : "application/json"))
                : RequestBody.create("", MediaType.parse("application/json"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .put(requestBody);

        if (contentType != null) {
            requestBuilder.header("Content-Type", contentType);
        }

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        Request request = requestBuilder.build();
        try (Response response = getCurrentClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body() != null ? response.body().string() : "";
            } else {
                throw new IOException("HTTP请求失败，状态码: " + response.code());
            }
        }
    }

    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String delete(String url) throws IOException {
        return delete(url, null);
    }

    /**
     * 发送DELETE请求
     * 
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public static String delete(String url, Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .delete();

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        Request request = requestBuilder.build();
        try (Response response = getCurrentClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body() != null ? response.body().string() : "";
            } else {
                throw new IOException("HTTP请求失败，状态码: " + response.code());
            }
        }
    }

    /**
     * 获取默认的OkHttpClient实例
     * 
     * @return OkHttpClient实例
     */
    public static OkHttpClient getDefaultClient() {
        return DEFAULT_CLIENT;
    }

    /**
     * 创建新的OkHttpClient构建器
     * 
     * @return OkHttpClient.Builder实例
     */
    public static OkHttpClient.Builder newClientBuilder() {
        return new OkHttpClient.Builder();
    }
}