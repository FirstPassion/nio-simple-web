package com.da.web.llm.openai;

import com.da.web.annotations.Component;
import com.da.web.llm.model.ChatRequest;
import com.da.web.llm.provider.LlmProvider;
import com.da.web.llm.model.Message;
import com.da.web.llm.model.ModelInfo;


import java.util.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI 协议实现
 */
@Component("openAiProvider")
public class OpenAiProvider implements LlmProvider {
    
    // 支持的模型列表
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo", 
        "gpt-4-32k", "gpt-3.5-turbo-16k"
    );
    
    // 模拟的响应模板，基于用户输入生成有意义的回复
    private final Map<String, String> responseTemplates = new ConcurrentHashMap<>();
    
    public OpenAiProvider() {
        initResponseTemplates();
    }
    
    private void initResponseTemplates() {
        responseTemplates.put("greeting", "你好！我是由 OpenAI 驱动的 AI 助手。有什么我可以帮助你的吗？");
        responseTemplates.put("code", "这是一个代码相关的请求。我可以帮你编写、解释或调试代码。请提供具体的需求。");
        responseTemplates.put("question", "这是一个很好的问题！让我来思考一下...");
        responseTemplates.put("thanks", "不客气！如果还有其他问题，随时问我。");
        responseTemplates.put("default", "我收到了你的消息。作为演示系统，我生成的这个响应是基于规则的智能回复。");
    }
    
    @Override
    public List<ModelInfo> getModels() {
        List<ModelInfo> models = new ArrayList<>();
        for (String modelId : SUPPORTED_MODELS) {
            models.add(new ModelInfo(modelId, "openai"));
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
        return "openai";
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
     * 基于规则生成智能响应
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
            return responseTemplates.get("code") + "\n\n请告诉我你需要什么样的代码？";
        }
        
        // 识别感谢
        if (lowerMessage.matches(".*(谢谢|感谢|thank|thanks).*")) {
            return responseTemplates.get("thanks");
        }
        
        // 识别问题
        if (lowerMessage.contains("?") || lowerMessage.contains("？") || 
            lowerMessage.matches(".*(什么|为什么|怎么|如何|多少|who|what|when|where|why|how).*")) {
            return responseTemplates.get("question") + "\n\n基于你的问题：" + userMessage;
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
