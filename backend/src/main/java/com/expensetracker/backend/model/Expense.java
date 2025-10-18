package com.expensetracker.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;

    private String userId;
    private double amount;
    private String bank;
    private String account;
    private String receiver;
    private String category;
    private String date;
    private String idempotencyKey;

    public Expense() {}

    public Expense(String userId, double amount, String bank, String account, String receiver, String category, String date) {
        this.userId = userId;
        this.amount = amount;
        this.bank = bank;
        this.account = account;
        this.receiver = receiver;
        this.category = category;
        this.date = date;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
