package com.expensetracker.backend.dto;

public class ChatRequest {
    private String userId;
    private String message;
    private Integer window; // optional - months window

    public ChatRequest() {}
    public ChatRequest(String userId, String message, Integer window) {
        this.userId = userId;
        this.message = message;
        this.window = window;
    }
    // getters & setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getWindow() { return window; }
    public void setWindow(Integer window) { this.window = window; }
}
