package com.da.web.http;

import java.util.*;

/**
 * 简单的 JSON 解析器 - 使用递归下降法实现
 */
public class JsonParser {
    private final String json;
    private int pos = 0;

    public JsonParser(String json) {
        this.json = json;
    }

    /**
     * 解析 JSON，返回对应的 Java 对象
     */
    @SuppressWarnings("unchecked")
    public Object parse() {
        skipWhitespace();
        if (pos >= json.length()) {
            throw new RuntimeException("Empty JSON");
        }
        return parseValue();
    }

    /**
     * 解析任意类型的值
     */
    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length()) {
            throw new RuntimeException("Unexpected end of JSON");
        }

        char c = json.charAt(pos);
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
            case 'f':
                return parseBoolean();
            case 'n':
                return parseNull();
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parseNumber();
            default:
                throw new RuntimeException("Unexpected character: " + c + " at position " + pos);
        }
    }

    /**
     * 解析 JSON 对象
     */
    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        
        // 消耗 '{'
        pos++;
        skipWhitespace();

        // 空对象
        if (match('}')) {
            return map;
        }

        while (true) {
            skipWhitespace();
            
            // 解析键
            if (pos >= json.length() || json.charAt(pos) != '"') {
                throw new RuntimeException("Expected string key at position " + pos);
            }
            String key = parseString();
            
            skipWhitespace();
            
            // 消耗 ':'
            if (!match(':')) {
                throw new RuntimeException("Expected ':' at position " + pos);
            }
            
            skipWhitespace();
            
            // 解析值
            Object value = parseValue();
            map.put(key, value);
            
            skipWhitespace();
            
            // 检查是否有更多键值对
            if (match('}')) {
                break;
            }
            
            if (!match(',')) {
                throw new RuntimeException("Expected ',' or '}' at position " + pos);
            }
        }

        return map;
    }

    /**
     * 解析 JSON 数组
     */
    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        
        // 消耗 '['
        pos++;
        skipWhitespace();

        // 空数组
        if (match(']')) {
            return list;
        }

        while (true) {
            // 解析值
            Object value = parseValue();
            list.add(value);
            
            skipWhitespace();
            
            // 检查是否有更多元素
            if (match(']')) {
                break;
            }
            
            if (!match(',')) {
                throw new RuntimeException("Expected ',' or ']' at position " + pos);
            }
        }

        return list;
    }

    /**
     * 解析字符串
     */
    private String parseString() {
        // 消耗开头的 '"'
        pos++;
        StringBuilder sb = new StringBuilder();

        while (pos < json.length()) {
            char c = json.charAt(pos);
            
            if (c == '"') {
                pos++; // 消耗结尾的 '"'
                return sb.toString();
            }
            
            if (c == '\\') {
                pos++;
                if (pos >= json.length()) {
                    throw new RuntimeException("Unexpected end of JSON in string escape");
                }
                char escaped = json.charAt(pos);
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        // Unicode 转义
                        if (pos + 4 >= json.length()) {
                            throw new RuntimeException("Invalid unicode escape");
                        }
                        String hex = json.substring(pos + 1, pos + 5);
                        char unicodeChar = (char) Integer.parseInt(hex, 16);
                        sb.append(unicodeChar);
                        pos += 4;
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
            
            pos++;
        }

        throw new RuntimeException("Unterminated string");
    }

    /**
     * 解析数字（整数或浮点数）
     */
    private Number parseNumber() {
        int start = pos;
        
        // 处理负号
        if (json.charAt(pos) == '-') {
            pos++;
        }
        
        // 处理整数部分
        if (pos < json.length() && json.charAt(pos) == '0') {
            pos++;
        } else {
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        
        // 处理小数部分
        boolean isDouble = false;
        if (pos < json.length() && json.charAt(pos) == '.') {
            isDouble = true;
            pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        
        // 处理指数部分
        if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            isDouble = true;
            pos++;
            if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }
        }
        
        String numStr = json.substring(start, pos);
        if (isDouble) {
            return Double.parseDouble(numStr);
        } else {
            try {
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                return Double.parseDouble(numStr);
            }
        }
    }

    /**
     * 解析布尔值
     */
    private Boolean parseBoolean() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return true;
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return false;
        } else {
            throw new RuntimeException("Invalid boolean at position " + pos);
        }
    }

    /**
     * 解析 null
     */
    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        } else {
            throw new RuntimeException("Invalid null at position " + pos);
        }
    }

    /**
     * 跳过空白字符
     */
    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }

    /**
     * 匹配并消耗指定字符
     */
    private boolean match(char expected) {
        if (pos < json.length() && json.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }
}
