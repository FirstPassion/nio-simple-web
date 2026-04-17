package com.da.web.openai;

import com.da.web.annotations.Path;
import com.da.web.core.Context;
import com.da.web.function.Handler;
import com.da.web.http.JsonParser;
import com.da.web.sse.SSEManager;
import com.da.web.util.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI 格式的 API 接口服务
 * 支持 /v1/chat/completions 端点，兼容 OpenAI SDK
 */
@Path("/v1/chat/completions")
public class OpenAIService implements Handler {

    // 模拟的模型列表
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
            "gpt-3.5-turbo",
            "gpt-4",
            "gpt-4-turbo",
            "custom-model"
    );

    @Override
    public void callback(Context ctx) throws Exception {
        String method = ctx.getMethod();
        
        if ("POST".equalsIgnoreCase(method)) {
            handleChatCompletion(ctx);
        } else {
            ctx.sendJson("{\"error\": {\"message\": \"Method not allowed\", \"type\": \"invalid_request_error\"}}", 405);
        }
    }

    /**
     * 处理聊天补全请求
     */
    private void handleChatCompletion(Context ctx) throws Exception {
        // 解析请求体
        ChatCompletionRequest request = parseRequest(ctx);
        
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            ctx.sendJson("{\"error\": {\"message\": \"Invalid request: messages is required\", \"type\": \"invalid_request_error\"}}", 400);
            return;
        }

        // 检查是否流式响应
        Boolean stream = request.getStream();
        if (stream != null && stream) {
            handleStreamingResponse(ctx, request);
        } else {
            handleNonStreamingResponse(ctx, request);
        }
    }

    /**
     * 解析请求
     */
    private ChatCompletionRequest parseRequest(Context ctx) {
        try {
            String jsonBody = ctx.getBodyAsJson();
            if (jsonBody == null || jsonBody.isEmpty()) {
                return null;
            }
            
            // 使用项目自带的 JsonParser 解析 JSON
            Object parsed = JsonParser.parse(jsonBody);
            if (parsed instanceof Map) {
                return mapToRequest((Map<?, ?>) parsed);
            }
            return null;
        } catch (Exception e) {
            Logger.error(OpenAIService.class, "Failed to parse request", e);
            return null;
        }
    }

    /**
     * 将 Map 转换为请求对象
     */
    @SuppressWarnings("unchecked")
    private ChatCompletionRequest mapToRequest(Map<?, ?> map) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        
        request.setModel(getString(map, "model"));
        request.setTemperature(getDouble(map, "temperature"));
        request.setTopP(getDouble(map, "top_p"));
        request.setN(getInteger(map, "n"));
        request.setStream(getBoolean(map, "stream"));
        request.setMaxTokens(getInteger(map, "max_tokens"));
        request.setPresencePenalty(getDouble(map, "presence_penalty"));
        request.setFrequencyPenalty(getDouble(map, "frequency_penalty"));
        request.setUser(getString(map, "user"));
        
        // 解析 messages
        Object messagesObj = map.get("messages");
        if (messagesObj instanceof List) {
            List<ChatCompletionRequest.Message> messages = new ArrayList<>();
            for (Object msgObj : (List<?>) messagesObj) {
                if (msgObj instanceof Map) {
                    Map<?, ?> msgMap = (Map<?, ?>) msgObj;
                    ChatCompletionRequest.Message msg = new ChatCompletionRequest.Message();
                    msg.setRole(getString(msgMap, "role"));
                    msg.setContent(getString(msgMap, "content"));
                    messages.add(msg);
                }
            }
            request.setMessages(messages);
        }
        
        return request;
    }

    /**
     * 处理非流式响应
     */
    private void handleNonStreamingResponse(Context ctx, ChatCompletionRequest request) throws Exception {
        // 生成模拟响应
        ChatCompletionResponse response = generateMockResponse(request);
        
        // 转换为 JSON 并发送
        String jsonResponse = responseToJson(response);
        ctx.sendJson(jsonResponse);
    }

    /**
     * 处理流式响应（SSE）
     */
    private void handleStreamingResponse(Context ctx, ChatCompletionRequest request) throws Exception {
        // 发送 SSE 响应头
        ctx.getResponse().sendSSEHeaders();
        
        // 生成并发送流式响应块
        streamMockResponse(ctx, request);
    }

    /**
     * 生成模拟的非流式响应
     */
    private ChatCompletionResponse generateMockResponse(ChatCompletionRequest request) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(request.getModel() != null ? request.getModel() : "gpt-3.5-turbo");
        
        // 获取最后一条用户消息
        String lastUserMessage = "";
        List<ChatCompletionRequest.Message> messages = request.getMessages();
        if (messages != null && !messages.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("user".equals(messages.get(i).getRole())) {
                    lastUserMessage = messages.get(i).getContent();
                    break;
                }
            }
        }
        
        // 生成简单的回复
        String reply = generateSimpleReply(lastUserMessage);
        
        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        ChatCompletionResponse.Choice.Message message = new ChatCompletionResponse.Choice.Message();
        message.setRole("assistant");
        message.setContent(reply);
        choice.setMessage(message);
        
        response.setChoices(new ChatCompletionResponse.Choice[]{choice});
        
        // 设置 usage
        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(lastUserMessage.length());
        usage.setCompletionTokens(reply.length());
        usage.setTotalTokens(lastUserMessage.length() + reply.length());
        response.setUsage(usage);
        
        return response;
    }

    /**
     * 流式发送模拟响应
     */
    private void streamMockResponse(Context ctx, ChatCompletionRequest request) throws Exception {
        String chunkId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        long created = System.currentTimeMillis() / 1000;
        String model = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";
        
        // 获取最后一条用户消息
        String lastUserMessage = "";
        List<ChatCompletionRequest.Message> messages = request.getMessages();
        if (messages != null && !messages.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("user".equals(messages.get(i).getRole())) {
                    lastUserMessage = messages.get(i).getContent();
                    break;
                }
            }
        }
        
        String reply = generateSimpleReply(lastUserMessage);
        
        // 分块发送
        String[] chunks = splitIntoChunks(reply, 5);
        
        for (int i = 0; i < chunks.length; i++) {
            ChatCompletionChunk chunk = new ChatCompletionChunk();
            chunk.setId(chunkId);
            chunk.setObject("chat.completion.chunk");
            chunk.setCreated(created);
            chunk.setModel(model);
            
            ChatCompletionChunk.Choice choice = new ChatCompletionChunk.Choice();
            choice.setIndex(0);
            
            ChatCompletionChunk.Choice.Delta delta = new ChatCompletionChunk.Choice.Delta();
            if (i == 0) {
                delta.setRole("assistant");
                delta.setContent(chunks[i]);
            } else {
                delta.setContent(chunks[i]);
            }
            
            choice.setDelta(delta);
            
            if (i == chunks.length - 1) {
                choice.setFinishReason("stop");
            }
            
            chunk.setChoices(new ChatCompletionChunk.Choice[]{choice});
            
            // 发送 SSE 数据
            String jsonChunk = chunkToJson(chunk);
            ctx.getResponse().writeDirect("data: " + jsonChunk + "\n\n");
            
            // 模拟延迟
            Thread.sleep(50);
        }
        
        // 发送结束标记
        ctx.getResponse().writeDirect("data: [DONE]\n\n");
    }

    /**
     * 生成简单回复
     */
    private String generateSimpleReply(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "Hello! I'm an AI assistant. How can I help you today?";
        }
        
        // 简单的回复逻辑
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Hello! How can I assist you today?";
        } else if (lowerMessage.contains("help")) {
            return "I'm here to help! What do you need assistance with?";
        } else if (lowerMessage.contains("name")) {
            return "I'm an AI assistant powered by this server.";
        } else if (lowerMessage.contains("time")) {
            return "Current time is: " + new Date();
        } else if (lowerMessage.contains("date")) {
            return "Today's date is: " + new Date();
        }
        
        return "I received your message: \"" + userMessage + "\". This is a demo response from the OpenAI-compatible API server.";
    }

    /**
     * 分割字符串为块
     */
    private String[] splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(len, i + chunkSize)));
        }
        return chunks.toArray(new String[0]);
    }

    /**
     * 将响应对象转换为 JSON
     */
    private String responseToJson(ChatCompletionResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(response.getId())).append("\",");
        sb.append("\"object\":\"").append(response.getObject()).append("\",");
        sb.append("\"created\":").append(response.getCreated()).append(",");
        sb.append("\"model\":\"").append(escapeJson(response.getModel())).append("\",");
        sb.append("\"choices\":[");
        
        if (response.getChoices() != null && response.getChoices().length > 0) {
            for (int i = 0; i < response.getChoices().length; i++) {
                if (i > 0) sb.append(",");
                ChatCompletionResponse.Choice choice = response.getChoices()[i];
                sb.append("{");
                sb.append("\"index\":").append(choice.getIndex()).append(",");
                sb.append("\"message\":{");
                sb.append("\"role\":\"").append(escapeJson(choice.getMessage().getRole())).append("\",");
                sb.append("\"content\":\"").append(escapeJson(choice.getMessage().getContent())).append("\"");
                sb.append("},");
                sb.append("\"finish_reason\":\"").append(escapeJson(choice.getFinishReason())).append("\"");
                sb.append("}");
            }
        }
        
        sb.append("],");
        sb.append("\"usage\":{");
        if (response.getUsage() != null) {
            sb.append("\"prompt_tokens\":").append(response.getUsage().getPromptTokens()).append(",");
            sb.append("\"completion_tokens\":").append(response.getUsage().getCompletionTokens()).append(",");
            sb.append("\"total_tokens\":").append(response.getUsage().getTotalTokens());
        } else {
            sb.append("\"prompt_tokens\":0,\"completion_tokens\":0,\"total_tokens\":0");
        }
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 将 Chunk 对象转换为 JSON
     */
    private String chunkToJson(ChatCompletionChunk chunk) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(chunk.getId())).append("\",");
        sb.append("\"object\":\"").append(chunk.getObject()).append("\",");
        sb.append("\"created\":").append(chunk.getCreated()).append(",");
        sb.append("\"model\":\"").append(escapeJson(chunk.getModel())).append("\",");
        sb.append("\"choices\":[");
        
        if (chunk.getChoices() != null && chunk.getChoices().length > 0) {
            for (int i = 0; i < chunk.getChoices().length; i++) {
                if (i > 0) sb.append(",");
                ChatCompletionChunk.Choice choice = chunk.getChoices()[i];
                sb.append("{");
                sb.append("\"index\":").append(choice.getIndex()).append(",");
                sb.append("\"delta\":{");
                if (choice.getDelta() != null) {
                    if (choice.getDelta().getRole() != null) {
                        sb.append("\"role\":\"").append(escapeJson(choice.getDelta().getRole())).append("\",");
                    }
                    if (choice.getDelta().getContent() != null) {
                        sb.append("\"content\":\"").append(escapeJson(choice.getDelta().getContent())).append("\"");
                    }
                }
                sb.append("}");
                if (choice.getFinishReason() != null) {
                    sb.append(",\"finish_reason\":\"").append(escapeJson(choice.getFinishReason())).append("\"");
                }
                sb.append("}");
            }
        }
        
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    /**
     * JSON 转义
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 辅助方法：从 Map 获取字符串
     */
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 辅助方法：从 Map 获取 Double
     */
    private Double getDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 辅助方法：从 Map 获取 Integer
     */
    private Integer getInteger(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 辅助方法：从 Map 获取 Boolean
     */
    private Boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
