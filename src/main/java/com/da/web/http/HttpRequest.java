package com.da.web.http;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求模型
 */
public class HttpRequest {
    private String method;
    private String url;
    private String path;
    private String httpVersion = "HTTP/1.1";
    private MultiValueMap headers;
    private Map<String, Object> queryParams;
    private Map<String, Object> bodyParams;
    private byte[] rawBody;
    private String contentType;
    private String charset = "UTF-8";

    public HttpRequest() {
        this.headers = new MultiValueMap();
        this.queryParams = new HashMap<>();
        this.bodyParams = new HashMap<>();
    }

    // Getters and Setters
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public MultiValueMap getHeaders() {
        return headers;
    }

    public void setHeaders(MultiValueMap headers) {
        this.headers = headers;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, Object> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, Object> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, Object> getBodyParams() {
        return bodyParams;
    }

    public void setBodyParams(Map<String, Object> bodyParams) {
        this.bodyParams = bodyParams;
    }

    public byte[] getRawBody() {
        return rawBody;
    }

    public void setRawBody(byte[] rawBody) {
        this.rawBody = rawBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * 获取 URL 参数
     */
    public Object getQueryParam(String name) {
        return queryParams.get(name);
    }

    /**
     * 获取请求体参数
     */
    public Object getBodyParam(String name) {
        return bodyParams.get(name);
    }

    /**
     * 获取请求体参数为字符串
     */
    public String getBodyParamAsString(String name) {
        Object value = bodyParams.get(name);
        return value != null ? value.toString() : null;
    }

    /**
     * 检查是否有请求体
     */
    public boolean hasBody() {
        return rawBody != null && rawBody.length > 0;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", httpVersion='" + httpVersion + '\'' +
                ", headers=" + headers.toSingleValueMap() +
                ", queryParams=" + queryParams +
                ", bodyParams=" + bodyParams +
                '}';
    }
}
