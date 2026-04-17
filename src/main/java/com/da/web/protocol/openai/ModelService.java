package com.da.web.protocol.openai;

import com.da.web.annotations.Component;
import com.da.web.annotations.Inject;
import com.da.web.core.Context;
import com.da.web.core.LlmProvider;
import com.da.web.model.ModelInfo;
import com.da.web.function.Handler;

import java.util.List;

/**
 * OpenAI 格式的模型列表 API
 * 支持 /v1/models 端点
 */
@Component("/v1/models")
public class ModelService implements Handler {

    @Inject("openAiProvider")
    private LlmProvider llmProvider;

    @Override
    public void callback(Context ctx) throws Exception {
        String method = ctx.getMethod();
        
        if ("GET".equalsIgnoreCase(method)) {
            handleListModels(ctx);
        } else {
            ctx.sendJson("{\"error\": {\"message\": \"Method not allowed\", \"type\": \"invalid_request_error\"}}", 405);
        }
    }

    /**
     * 处理获取模型列表请求
     */
    private void handleListModels(Context ctx) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"object\":\"list\",");
        json.append("\"data\":[");
        
        // 从 LlmProvider 获取模型列表
        List<ModelInfo> models = llmProvider.getModels();
        
        for (int i = 0; i < models.size(); i++) {
            if (i > 0) json.append(",");
            ModelInfo model = models.get(i);
            json.append("{");
            json.append("\"id\":\"").append(escapeJson(model.getId())).append("\",");
            json.append("\"object\":\"").append(model.getObject()).append("\",");
            json.append("\"created\":").append(model.getCreated()).append(",");
            json.append("\"owned_by\":\"").append(escapeJson(model.getOwnedBy())).append("\"");
            json.append("}");
        }
        
        json.append("]");
        json.append("}");
        
        ctx.sendJson(json.toString());
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
}
