package com.da.web.enums;

import com.da.web.constant.ContentTypes;

/**
 * 内容类型枚举
 * @deprecated 请使用 {@link ContentTypes} 常量类
 */
@Deprecated
public enum ContentType {
    CONTENT_TYPE_HTML(ContentTypes.TEXT_HTML),
    CONTENT_TYPE_TEXT(ContentTypes.TEXT_PLAIN),
    CONTENT_TYPE_XML(ContentTypes.TEXT_XML),
    CONTENT_TYPE_GIF(ContentTypes.IMAGE_GIF),
    CONTENT_TYPE_JPG(ContentTypes.IMAGE_JPEG),
    CONTENT_TYPE_PNG(ContentTypes.IMAGE_PNG),
    CONTENT_TYPE_JSON(ContentTypes.APPLICATION_JSON);
    
    private final String type;
    
    ContentType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return type;
    }
}
