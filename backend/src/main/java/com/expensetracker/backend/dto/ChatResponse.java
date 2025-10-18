package com.expensetracker.backend.dto;

public class ChatResponse {
    private boolean success;
    private String reply;
    private Object debug; // optional - pass ML analysis for debugging

    public ChatResponse() {}
    public ChatResponse(boolean success, String reply, Object debug) {
        this.success = success;
        this.reply = reply;
        this.debug = debug;
    }
    // getters & setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public Object getDebug() { return debug; }
    public void setDebug(Object debug) { this.debug = debug; }
}
