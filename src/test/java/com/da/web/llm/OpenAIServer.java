package com.da.web.llm;

import com.da.web.core.DApp;
import com.da.web.util.Logger;

/**
 * OpenAI 格式 API 服务启动类
 *
 * 使用说明：
 * 1. 运行此类启动服务器
 * 2. 访问 http://localhost:8080/v1/models 获取模型列表
 * 3. POST 请求 http://localhost:8080/v1/chat/completions 进行聊天
 *
 * 示例请求（非流式）:
 * curl -X POST http://localhost:8080/v1/chat/completions \
 *   -H "Content-Type: application/json" \
 *   -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"Hello"}]}'
 *
 * 示例请求（流式）:
 * curl -X POST http://localhost:8080/v1/chat/completions \
 *   -H "Content-Type: application/json" \
 *   -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"Hello"}],"stream":true}'
 */
public class OpenAIServer {
    
    public static void main(String[] args) {
        try {
            // 创建应用实例，自动扫描并注册路由和组件
            DApp app = new DApp(OpenAIServer.class);
            
            // 启动服务器
            Logger.info(OpenAIServer.class, "正在启动 OpenAI 兼容 API 服务...");
            app.listen();
            
            Logger.info(OpenAIServer.class, "OpenAI 兼容 API 服务已启动!");
            Logger.info(OpenAIServer.class, "API 端点:");
            Logger.info(OpenAIServer.class, "  GET  /v1/models - 获取模型列表");
            Logger.info(OpenAIServer.class, "  POST /v1/chat/completions - 聊天补全 (支持流式)");
            
        } catch (Exception e) {
            Logger.error(OpenAIServer.class, "启动失败", e);
        }
    }
}
