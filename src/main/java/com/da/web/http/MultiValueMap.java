package com.da.web.http;

import java.util.*;

/**
 * 支持多值的 Map 实现，用于存储 HTTP 头部
 */
public class MultiValueMap {
    private final Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * 添加一个值
     */
    public void add(String key, String value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    /**
     * 获取第一个值
     */
    public String get(String key) {
        List<String> values = map.get(key);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     * 获取所有值
     */
    public List<String> getAll(String key) {
        return map.getOrDefault(key, Collections.emptyList());
    }

    /**
     * 检查是否包含某个键
     */
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    /**
     * 获取所有键
     */
    public Set<String> keySet() {
        return map.keySet();
    }

    /**
     * 获取所有条目
     */
    public Set<Map.Entry<String, List<String>>> entrySet() {
        return map.entrySet();
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 大小
     */
    public int size() {
        return map.size();
    }

    /**
     * 转为普通 Map（只取第一个值）
     */
    public Map<String, String> toSingleValueMap() {
        Map<String, String> result = new HashMap<>();
        map.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key, values.get(0));
            }
        });
        return result;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
