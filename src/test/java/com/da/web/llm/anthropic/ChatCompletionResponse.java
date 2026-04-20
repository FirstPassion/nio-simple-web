package com.da.web.llm.anthropic;

/**
 * Anthropic 聊天响应模型（非流式）
 */
public class ChatCompletionResponse {
    private String id;
    private String type;
    private String role;
    private Content[] content;
    private String model;
    private String stopReason;
    private Usage usage;

    public static class Content {
        private String type;
        private String text;

        public Content() {}

        public Content(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class Usage {
        private Integer inputTokens;
        private Integer outputTokens;

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Content[] getContent() {
        return content;
    }

    public void setContent(Content[] content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }
}
