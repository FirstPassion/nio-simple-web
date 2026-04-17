package com.da.web.openai;

import com.da.web.annotations.Path;
import com.da.web.core.Context;
import com.da.web.function.Handler;
import com.da.web.util.Logger;

/**
 * OpenAI 格式的模型列表 API
 * 支持 /v1/models 端点
 */
@Path("/v1/models")
public class ModelService implements Handler {

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
        
        // 返回支持的模型列表
        String[] models = {
                "{\"id\":\"gpt-3.5-turbo\",\"object\":\"model\",\"created\":1677649963,\"owned_by\":\"openai\"}",
                "{\"id\":\"gpt-4\",\"object\":\"model\",\"created\":1677649963,\"owned_by\":\"openai\"}",
                "{\"id\":\"gpt-4-turbo\",\"object\":\"model\",\"created\":1677649963,\"owned_by\":\"openai\"}",
                "{\"id\":\"custom-model\",\"object\":\"model\",\"created\":1677649963,\"owned_by\":\"custom\"}"
        };
        
        for (int i = 0; i < models.length; i++) {
            if (i > 0) json.append(",");
            json.append(models[i]);
        }
        
        json.append("]");
        json.append("}");
        
        ctx.sendJson(json.toString());
    }
}
