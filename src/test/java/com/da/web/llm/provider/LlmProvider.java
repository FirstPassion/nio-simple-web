package com.da.web.llm.provider;

import java.util.List;
import java.util.Iterator;
import com.da.web.llm.model.ChatRequest;
import com.da.web.llm.model.ModelInfo;

/**
 * LLM 提供者接口 - 定义协议实现的核心契约
 * 所有协议实现（OpenAI、Anthropic 等）都需要实现此接口
 */
public interface LlmProvider {

    /**
     * 获取支持的模型列表
     * @return 模型信息列表
     */
    List<ModelInfo> getModels();

    /**
     * 执行聊天完成请求（非流式）
     * @param request 通用聊天请求
     * @return 响应内容
     */
    String chatComplete(ChatRequest request);

    /**
     * 执行聊天完成请求（流式）
     * @param request 通用聊天请求
     * @return 响应内容迭代器，逐块返回
     */
    Iterator<String> chatCompleteStream(ChatRequest request);

    /**
     * 获取提供者名称
     * @return 提供者标识
     */
    String getProviderName();
}
