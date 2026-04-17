package com.da.web.model;

import java.util.List;
import java.util.Map;

/**
 * 通用聊天完成请求，屏蔽不同协议的请求格式差异
 */
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private String system; // Anthropic 特有
    private Integer maxTokens;
    private Boolean stream;
    private Double temperature;
    private Map<String, Object> extraParams; // 存储协议特有的参数

    public ChatRequest() {}

    // Getters and Setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Map<String, Object> getExtraParams() { return extraParams; }
    public void setExtraParams(Map<String, Object> extraParams) { this.extraParams = extraParams; }
}
