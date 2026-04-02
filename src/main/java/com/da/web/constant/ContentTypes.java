package com.da.web.constant;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 内容类型常量
 */
public final class ContentTypes {
    
    public static final String TEXT_HTML = "text/html;charset=";
    public static final String TEXT_PLAIN = "text/plain;charset=";
    public static final String APPLICATION_JSON = "application/json;charset=";
    public static final String TEXT_XML = "text/xml;charset=";
    public static final String IMAGE_GIF = "image/gif;charset=";
    public static final String IMAGE_JPEG = "image/jpeg;charset=";
    public static final String IMAGE_PNG = "image/png;charset=";
    
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    
    private ContentTypes() {
        // 私有构造，防止实例化
    }
    
    /**
     * 获取带默认编码的内容类型
     */
    public static String withDefaultCharset(String contentType) {
        return contentType + DEFAULT_CHARSET.name();
    }
}
