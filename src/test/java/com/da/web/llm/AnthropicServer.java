package com.da.web.llm;

import com.da.web.core.DApp;
import com.da.web.util.Logger;

/**
 * Anthropic 格式 API 服务启动类
 * 
 * 使用说明：
 * 1. 运行此类启动服务器
 * 2. 访问 http://localhost:8080/v1/models 获取模型列表
 * 3. POST 请求 http://localhost:8080/v1/messages 进行聊天
 * 
 * 示例请求（非流式）:
 * curl -X POST http://localhost:8080/v1/messages \
 *   -H "Content-Type: application/json" \
 *   -H "x-api-key: test-key" \
 *   -d '{"model":"claude-3-sonnet-20240229","messages":[{"role":"user","content":"Hello"}],"max_tokens":100}'
 * 
 * 示例请求（流式）:
 * curl -X POST http://localhost:8080/v1/messages \
 *   -H "Content-Type: application/json" \
 *   -H "x-api-key: test-key" \
 *   -d '{"model":"claude-3-sonnet-20240229","messages":[{"role":"user","content":"Hello"}],"max_tokens":100,"stream":true}'
 */
public class AnthropicServer {
    
    public static void main(String[] args) {
        try {
            // 创建应用实例，自动扫描并注册路由和组件
            DApp app = new DApp(AnthropicServer.class);
            
            // 启动服务器
            Logger.info(AnthropicServer.class, "正在启动 Anthropic 兼容 API 服务...");
            app.listen();
            
            Logger.info(AnthropicServer.class, "Anthropic 兼容 API 服务已启动!");
            Logger.info(AnthropicServer.class, "API 端点:");
            Logger.info(AnthropicServer.class, "  GET  /v1/models - 获取模型列表");
            Logger.info(AnthropicServer.class, "  POST /v1/messages - 聊天补全 (支持流式)");
            
        } catch (Exception e) {
            Logger.error(AnthropicServer.class, "启动失败", e);
        }
    }
}
