package com.da.web.llm.model;

/**
 * 通用消息结构，屏蔽不同协议的消息格式差异
 */
public class Message {
    private String role; // user, assistant, system
    private String content;

    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
