package com.da.web.protocol.anthropic;

import com.da.web.annotations.Component;
import com.da.web.model.ChatRequest;
import com.da.web.core.LlmProvider;
import com.da.web.model.Message;
import com.da.web.model.ModelInfo;


import java.util.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anthropic 协议实现
 */
@Component("anthropicProvider")
public class AnthropicProvider implements LlmProvider {
    
    // 支持的模型列表
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229", 
        "claude-3-haiku-20240307",
        "claude-2.1",
        "claude-2.0"
    );
    
    // 模拟的响应模板，基于用户输入生成有意义的回复
    private final Map<String, String> responseTemplates = new ConcurrentHashMap<>();
    
    public AnthropicProvider() {
        initResponseTemplates();
    }
    
    private void initResponseTemplates() {
        responseTemplates.put("greeting", "你好！我是 Claude，由 Anthropic 开发的 AI 助手。我很高兴为你提供帮助！");
        responseTemplates.put("code", "我很乐意帮助你处理代码相关的任务。无论是编写、审查还是调试代码，我都可以协助你。请告诉我具体需求。");
        responseTemplates.put("question", "这是一个很有深度的问题！让我仔细分析一下...");
        responseTemplates.put("thanks", "不用谢！随时欢迎你来提问。");
        responseTemplates.put("default", "感谢你的消息。作为演示系统，我会根据内容智能生成回复。");
    }
    
    @Override
    public List<ModelInfo> getModels() {
        List<ModelInfo> models = new ArrayList<>();
        for (String modelId : SUPPORTED_MODELS) {
            models.add(new ModelInfo(modelId, "anthropic"));
        }
        return models;
    }
    
    @Override
    public String chatComplete(ChatRequest request) {
        // 提取用户最后一条消息
        String userMessage = extractLastUserMessage(request);
        
        // 根据内容类型生成响应
        String response = generateIntelligentResponse(userMessage, request.getSystem());
        
        return response;
    }
    
    @Override
    public Iterator<String> chatCompleteStream(ChatRequest request) {
        String fullResponse = chatComplete(request);
        return new ResponseIterator(fullResponse, 3); // 每块 3 个字符
    }
    
    @Override
    public String getProviderName() {
        return "anthropic";
    }
    
    /**
     * 提取用户最后一条消息
     */
    private String extractLastUserMessage(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "";
        }
        
        // 从后往前找第一条用户消息
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            Message msg = request.getMessages().get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return "";
    }
    
    /**
     * 基于规则生成智能响应（Anthropic 风格）
     */
    private String generateIntelligentResponse(String userMessage, String systemPrompt) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return responseTemplates.get("default");
        }
        
        String lowerMessage = userMessage.toLowerCase();
        
        // 识别问候语
        if (lowerMessage.matches(".*(你好 | 您好|hello|hi|hey|good morning|good afternoon).*")) {
            return responseTemplates.get("greeting");
        }
        
        // 识别代码相关
        if (lowerMessage.matches(".*(代码|编程|写个|函数|类|方法|code|function|class|write).*")) {
            return responseTemplates.get("code") + "\n\n请详细描述你需要什么样的代码帮助？";
        }
        
        // 识别感谢
        if (lowerMessage.matches(".*(谢谢|感谢|thank|thanks).*")) {
            return responseTemplates.get("thanks");
        }
        
        // 识别问题
        if (lowerMessage.contains("?") || lowerMessage.contains("?") || 
            lowerMessage.matches(".*(什么 | 为什么 | 怎么 | 如何 | 多少|who|what|when|where|why|how).*")) {
            return responseTemplates.get("question") + "\n\n针对你的问题 \"" + userMessage + "\"，我的分析如下：";
        }
        
        // 默认响应
        return responseTemplates.get("default") + "\n\n你发送的内容是：" + userMessage;
    }
    
    /**
     * 流式响应迭代器
     */
    private static class ResponseIterator implements Iterator<String> {
        private final String content;
        private final int chunkSize;
        private int position = 0;
        
        public ResponseIterator(String content, int chunkSize) {
            this.content = content;
            this.chunkSize = chunkSize;
        }
        
        @Override
        public boolean hasNext() {
            return position < content.length();
        }
        
        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int end = Math.min(position + chunkSize, content.length());
            String chunk = content.substring(position, end);
            position = end;
            return chunk;
        }
    }
}
