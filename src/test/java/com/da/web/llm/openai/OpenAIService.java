package com.da.web.llm.openai;

import com.da.web.annotations.Component;
import com.da.web.annotations.Inject;
import com.da.web.llm.model.ChatRequest;
import com.da.web.core.Context;
import com.da.web.llm.provider.LlmProvider;
import com.da.web.llm.model.Message;
import com.da.web.function.Handler;
import com.da.web.http.JsonParser;
import com.da.web.util.Logger;

import java.util.*;

/**
 * OpenAI 格式的 API 接口服务
 * 支持 /v1/chat/completions 端点，兼容 OpenAI SDK
 */
@Component("/v1/chat/completions")
public class OpenAIService implements Handler {

    // 注入 LlmProvider 实现
    @Inject("openAiProvider")
    private LlmProvider llmProvider;

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
        // 转换为通用 ChatRequest
        ChatRequest chatRequest = convertToChatRequest(request);
        
        // 调用 LlmProvider 生成响应
        String reply = llmProvider.chatComplete(chatRequest);
        
        // 生成响应对象
        ChatCompletionResponse response = buildResponse(request, reply);
        
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
        
        // 转换为通用 ChatRequest
        ChatRequest chatRequest = convertToChatRequest(request);
        
        streamGeneratedResponse(ctx, request, chatRequest);
    }

    /**
     * 构建响应对象
     */
    private ChatCompletionResponse buildResponse(ChatCompletionRequest request, String reply) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(request.getModel() != null ? request.getModel() : "gpt-3.5-turbo");
        
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
        String lastUserMessage = getLastUserMessage(request.getMessages());
        usage.setPromptTokens(lastUserMessage.length());
        usage.setCompletionTokens(reply.length());
        usage.setTotalTokens(lastUserMessage.length() + reply.length());
        response.setUsage(usage);
        
        return response;
    }

    /**
     * 流式发送生成的响应
     */
    private void streamGeneratedResponse(Context ctx, ChatCompletionRequest request, 
                                         ChatRequest chatRequest) throws Exception {
        String chunkId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        long created = System.currentTimeMillis() / 1000;
        String model = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";
        
        // 使用 LlmProvider 的流式迭代
        Iterator<String> streamIterator = llmProvider.chatCompleteStream(chatRequest);
        
        int index = 0;
        while (streamIterator.hasNext()) {
            String chunkContent = streamIterator.next();
            
            ChatCompletionChunk chunk = new ChatCompletionChunk();
            chunk.setId(chunkId);
            chunk.setObject("chat.completion.chunk");
            chunk.setCreated(created);
            chunk.setModel(model);
            
            ChatCompletionChunk.Choice choice = new ChatCompletionChunk.Choice();
            choice.setIndex(0);
            
            ChatCompletionChunk.Choice.Delta delta = new ChatCompletionChunk.Choice.Delta();
            if (index == 0) {
                delta.setRole("assistant");
            }
            delta.setContent(chunkContent);
            
            choice.setDelta(delta);
            choice.setFinishReason(null); // 中间块不设置
            
            chunk.setChoices(new ChatCompletionChunk.Choice[]{choice});
            
            // 发送 SSE 数据
            String jsonChunk = chunkToJson(chunk);
            ctx.getResponse().writeDirect("data: " + jsonChunk + "\n\n");
            
            // 模拟延迟
            Thread.sleep(30);
            index++;
        }
        
        // 发送结束标记块
        ChatCompletionChunk finalChunk = new ChatCompletionChunk();
        finalChunk.setId(chunkId);
        finalChunk.setObject("chat.completion.chunk");
        finalChunk.setCreated(created);
        finalChunk.setModel(model);
        
        ChatCompletionChunk.Choice finalChoice = new ChatCompletionChunk.Choice();
        finalChoice.setIndex(0);
        finalChoice.setFinishReason("stop");
        finalChoice.setDelta(new ChatCompletionChunk.Choice.Delta());
        
        finalChunk.setChoices(new ChatCompletionChunk.Choice[]{finalChoice});
        
        String jsonFinalChunk = chunkToJson(finalChunk);
        ctx.getResponse().writeDirect("data: " + jsonFinalChunk + "\n\n");
        
        // 发送结束标记
        ctx.getResponse().writeDirect("data: [DONE]\n\n");
    }

    /**
     * 转换为通用 ChatRequest
     */
    private ChatRequest convertToChatRequest(ChatCompletionRequest request) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(request.getModel());
        chatRequest.setStream(request.getStream());
        chatRequest.setMaxTokens(request.getMaxTokens());
        chatRequest.setTemperature(request.getTemperature());
        
        // 转换 messages
        if (request.getMessages() != null) {
            List<Message> messages = new ArrayList<>();
            for (ChatCompletionRequest.Message msg : request.getMessages()) {
                messages.add(new Message(msg.getRole(), msg.getContent()));
            }
            chatRequest.setMessages(messages);
        }
        
        return chatRequest;
    }

    /**
     * 获取最后一条用户消息
     */
    private String getLastUserMessage(List<ChatCompletionRequest.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                return messages.get(i).getContent() != null ? messages.get(i).getContent() : "";
            }
        }
        return "";
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
