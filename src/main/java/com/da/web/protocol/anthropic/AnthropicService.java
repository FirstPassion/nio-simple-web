package com.da.web.protocol.anthropic;

import com.da.web.annotations.Component;
import com.da.web.annotations.Inject;
import com.da.web.model.ChatRequest;
import com.da.web.core.Context;
import com.da.web.core.LlmProvider;
import com.da.web.model.Message;
import com.da.web.function.Handler;
import com.da.web.http.JsonParser;
import com.da.web.util.Logger;

import java.util.*;

/**
 * Anthropic 格式的 API 接口服务
 * 支持 /v1/messages 端点，兼容 Anthropic SDK
 */
@Component("/v1/messages")
public class AnthropicService implements Handler {

    @Inject("anthropicProvider")
    private LlmProvider llmProvider;

    @Override
    public void callback(Context ctx) throws Exception {
        String method = ctx.getMethod();
        
        if ("POST".equalsIgnoreCase(method)) {
            handleChatCompletion(ctx);
        } else {
            ctx.sendJson("{\"error\": {\"type\": \"invalid_request_error\", \"message\": \"Method not allowed\"}}", 405);
        }
    }

    private void handleChatCompletion(Context ctx) throws Exception {
        ChatCompletionRequest request = parseRequest(ctx);
        
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            ctx.sendJson("{\"error\": {\"type\": \"invalid_request_error\", \"message\": \"Invalid request: messages is required\"}}", 400);
            return;
        }

        Boolean stream = request.getStream();
        if (stream != null && stream) {
            handleStreamingResponse(ctx, request);
        } else {
            handleNonStreamingResponse(ctx, request);
        }
    }

    private ChatCompletionRequest parseRequest(Context ctx) {
        try {
            String jsonBody = ctx.getBodyAsJson();
            if (jsonBody == null || jsonBody.isEmpty()) {
                return null;
            }
            
            Object parsed = JsonParser.parse(jsonBody);
            if (parsed instanceof Map) {
                return mapToRequest((Map<?, ?>) parsed);
            }
            return null;
        } catch (Exception e) {
            Logger.error(AnthropicService.class, "Failed to parse request", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ChatCompletionRequest mapToRequest(Map<?, ?> map) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        
        request.setModel(getString(map, "model"));
        request.setMaxTokens(getInteger(map, "max_tokens"));
        request.setStream(getBoolean(map, "stream"));
        request.setTemperature(getDouble(map, "temperature"));
        request.setTopP(getDouble(map, "top_p"));
        request.setTopK(getInteger(map, "top_k"));
        request.setSystem(getString(map, "system"));
        
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

    private void handleNonStreamingResponse(Context ctx, ChatCompletionRequest request) throws Exception {
        ChatRequest chatRequest = convertToChatRequest(request);
        String reply = llmProvider.chatComplete(chatRequest);
        ChatCompletionResponse response = buildResponse(request, reply);
        String jsonResponse = responseToJson(response);
        ctx.sendJson(jsonResponse);
    }

    private void handleStreamingResponse(Context ctx, ChatCompletionRequest request) throws Exception {
        ctx.getResponse().sendSSEHeaders();
        ChatRequest chatRequest = convertToChatRequest(request);
        streamGeneratedResponse(ctx, request, chatRequest);
    }

    private ChatCompletionResponse buildResponse(ChatCompletionRequest request, String reply) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        response.setType("message");
        response.setRole("assistant");
        response.setModel(request.getModel() != null ? request.getModel() : "claude-3-sonnet-20240229");
        response.setStopReason("end_turn");
        
        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        String lastUserMessage = getLastUserMessage(request.getMessages());
        usage.setInputTokens(lastUserMessage.length());
        usage.setOutputTokens(reply.length());
        response.setUsage(usage);
        
        ChatCompletionResponse.Content content = new ChatCompletionResponse.Content();
        content.setType("text");
        content.setText(reply);
        response.setContent(new ChatCompletionResponse.Content[]{content});
        
        return response;
    }

    private void streamGeneratedResponse(Context ctx, ChatCompletionRequest request, 
                                         ChatRequest chatRequest) throws Exception {
        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String model = request.getModel() != null ? request.getModel() : "claude-3-sonnet-20240229";
        
        sendEvent(ctx, "message_start", createMessageStartEvent(messageId, model));
        sendEvent(ctx, "content_block_start", createContentBlockStartEvent());
        
        Iterator<String> streamIterator = llmProvider.chatCompleteStream(chatRequest);
        
        while (streamIterator.hasNext()) {
            String chunkContent = streamIterator.next();
            sendEvent(ctx, "content_block_delta", createContentBlockDeltaEvent(chunkContent));
            Thread.sleep(30);
        }
        
        sendEvent(ctx, "content_block_stop", createContentBlockStopEvent());
        sendEvent(ctx, "message_delta", createMessageDeltaEvent());
        sendEvent(ctx, "message_stop", createMessageStopEvent());
    }

    private ChatRequest convertToChatRequest(ChatCompletionRequest request) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(request.getModel());
        chatRequest.setStream(request.getStream());
        chatRequest.setMaxTokens(request.getMaxTokens());
        chatRequest.setTemperature(request.getTemperature());
        chatRequest.setSystem(request.getSystem());
        
        if (request.getMessages() != null) {
            List<Message> messages = new ArrayList<>();
            for (ChatCompletionRequest.Message msg : request.getMessages()) {
                messages.add(new Message(msg.getRole(), msg.getContent()));
            }
            chatRequest.setMessages(messages);
        }
        
        return chatRequest;
    }

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

    private Map<String, Object> createMessageStartEvent(String messageId, String model) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "message_start");
        
        Map<String, Object> message = new HashMap<>();
        message.put("id", messageId);
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("content", new ArrayList<>());
        message.put("model", model);
        message.put("stop_reason", null);
        message.put("stop_sequence", null);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("input_tokens", 0);
        usage.put("output_tokens", 1);
        message.put("usage", usage);
        
        event.put("message", message);
        return event;
    }
    
    private Map<String, Object> createContentBlockStartEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "content_block_start");
        event.put("index", 0);
        
        Map<String, Object> contentBlock = new HashMap<>();
        contentBlock.put("type", "text");
        contentBlock.put("text", "");
        event.put("content_block", contentBlock);
        
        return event;
    }
    
    private Map<String, Object> createContentBlockDeltaEvent(String text) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "content_block_delta");
        event.put("index", 0);
        
        Map<String, Object> delta = new HashMap<>();
        delta.put("type", "text_delta");
        delta.put("text", text);
        event.put("delta", delta);
        
        return event;
    }
    
    private Map<String, Object> createContentBlockStopEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "content_block_stop");
        event.put("index", 0);
        return event;
    }
    
    private Map<String, Object> createMessageDeltaEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "message_delta");
        
        Map<String, Object> delta = new HashMap<>();
        delta.put("stop_reason", "end_turn");
        delta.put("stop_sequence", null);
        event.put("delta", delta);
        
        Map<String, Object> usage = new HashMap<>();
        usage.put("output_tokens", 1);
        event.put("usage", usage);
        
        return event;
    }
    
    private Map<String, Object> createMessageStopEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "message_stop");
        return event;
    }

    private void sendEvent(Context ctx, String eventType, Map<String, Object> data) throws Exception {
        String json = mapToJson(data);
        ctx.getResponse().writeDirect("event: " + eventType + "\n");
        ctx.getResponse().writeDirect("data: " + json + "\n\n");
    }

    private String responseToJson(ChatCompletionResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(response.getId())).append("\",");
        sb.append("\"type\":\"").append(response.getType()).append("\",");
        sb.append("\"role\":\"").append(response.getRole()).append("\",");
        sb.append("\"content\":[");
        
        if (response.getContent() != null && response.getContent().length > 0) {
            for (int i = 0; i < response.getContent().length; i++) {
                if (i > 0) sb.append(",");
                ChatCompletionResponse.Content content = response.getContent()[i];
                sb.append("{");
                sb.append("\"type\":\"").append(content.getType()).append("\",");
                sb.append("\"text\":\"").append(escapeJson(content.getText())).append("\"");
                sb.append("}");
            }
        }
        
        sb.append("],");
        sb.append("\"model\":\"").append(escapeJson(response.getModel())).append("\",");
        sb.append("\"stop_reason\":\"").append(response.getStopReason()).append("\",");
        sb.append("\"usage\":{");
        if (response.getUsage() != null) {
            sb.append("\"input_tokens\":").append(response.getUsage().getInputTokens()).append(",");
            sb.append("\"output_tokens\":").append(response.getUsage().getOutputTokens());
        } else {
            sb.append("\"input_tokens\":0,\"output_tokens\":0");
        }
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value.toString());
            } else if (value instanceof Boolean) {
                sb.append(value.toString());
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) value));
            } else if (value instanceof List) {
                sb.append(listToJson((List<Object>) value));
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String listToJson(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            
            if (item == null) {
                sb.append("null");
            } else if (item instanceof String) {
                sb.append("\"").append(escapeJson((String) item)).append("\"");
            } else if (item instanceof Number) {
                sb.append(item.toString());
            } else if (item instanceof Boolean) {
                sb.append(item.toString());
            } else if (item instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) item));
            } else if (item instanceof List) {
                sb.append(listToJson((List<Object>) item));
            } else {
                sb.append("\"").append(escapeJson(item.toString())).append("\"");
            }
        }
        
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Double getDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Integer getInteger(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
