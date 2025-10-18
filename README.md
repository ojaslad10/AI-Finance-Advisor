# AI-Finance-Advisor
An intelligent expense tracker that auto-detects expenses from transaction SMS and analyzes spending patterns. Its AI chatbot, powered by a machine learning model and OpenAI API, offers friendly financial advice on saving and investing, while timely notifications keep users updated on category-wise spending.


# 💰 AI-Powered Expense Tracker

An **intelligent personal finance management app** built with **Kotlin (Jetpack Compose)** for Android and **Spring Boot + MongoDB** backend.  
The app helps users **track their daily expenses, analyze spending patterns, and get AI-based insights** to improve their financial habits.

---

## 🚀 Overview

**AI Expense Tracker** allows users to record transactions (income & expenses), visualize category-wise spending with charts, and automatically classify their expenses using AI.  
It integrates features like **EMI management**, **daily summaries**, and **AI chat assistant** to provide personalized insights on how to manage money effectively.

---

## 🧠 Key Features

### 💸 Expense Management
- Add, edit, and delete transactions easily.  
- Categorize expenses (Food, Transport, Bills, Medical, etc.) automatically.  
- View daily, weekly, and monthly summaries.

### 🧩 Category Analysis
- Pie chart visualization for category-wise breakdown.  
- Percentage-based comparison of spending across all categories.  

### 📅 EMI Tracker
- Add and manage EMIs dynamically.  
- Mark EMIs as paid — they reappear automatically in the next billing cycle.  
- Stores details like EMI name, amount, due date, and tenure.

### 🤖 AI Assistant
- Integrated AI chatbot to analyze expenses and provide financial advice.  
- Detects unusual transactions and spending patterns.  
- Suggests saving strategies based on expense history.

### 👤 User Management
- Secure login and signup with JWT-based authentication.  
- Each user has their own independent data synced with MongoDB.

---

## 🧰 Tech Stack

| Layer | Technologies Used |
|:------|:------------------|
| **Frontend** | Kotlin, Jetpack Compose, Material 3, Retrofit |
| **Backend** | Spring Boot (Java), REST APIs, JWT Authentication |
| **Database** | MongoDB (Atlas) |
| **AI Integration** | Python microservice (for expense analysis) |
| **Tools & Libraries** | Coroutines, ViewModel, LiveData, Gson, Docker |

---


## ⚙️ Setup Instructions

### 🖥 Backend (Spring Boot)
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/AI-Expense-Tracker.git
   cd backend

## Folder Structure

   AI-Expense-Tracker/
│
├── backend/                    # Spring Boot Backend
│   ├── src/main/java/...       # Controllers, Models, Services
│   └── application.properties
│
├── frontend/                   # Android App (Jetpack Compose)
│   ├── ui/                     # Composables and Screens
│   ├── data/                   # Retrofit, Models, Repositories
│   └── viewmodel/              # MainViewModel, States
│
├── ml_services/                # AI Microservice for analysis (Python)
│   ├── app.py
│   └── generate_fake_data.py
│
└── README.md

