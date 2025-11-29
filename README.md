# 💰 Finance Advisor App

A **smart personal finance tracking and advisory application** that helps users monitor their expenses, analyze spending patterns, and receive personalized financial insights through an **AI-powered chatbot**.

---

## 🧠 Overview

Finance Advisor automatically reads your **SMS-based transaction messages**, categorizes your expenses, and stores them securely in **MongoDB**.  
It also features an **AI Advisor chatbot** powered by the **OpenAI API** and a **Python-based ML model** that helps users with:

- 💸 Expense Analysis  
- 💰 Saving and Investment Suggestions  
- 📊 Spending Predictions  
- 🤖 Interactive Financial Advice  

---

## 🧩 Tech Stack

| Layer | Technology |
|:------|:------------|
| **Frontend** | Kotlin (Android) |
| **Backend** | Spring Boot (Java) |
| **Machine Learning / AI** | Python (FastAPI + OpenAI API) |
| **Database** | MongoDB |
| **Model Serving** | Uvicorn (FastAPI server) |

---

## ⚙️ Features

✅ **Automatic Expense Tracking** – Extracts expense data from SMS and updates your financial records.  
✅ **Real-Time Analytics** – Tracks spending, savings, and bank balance visually.  
✅ **AI-Powered Chatbot** – Interacts naturally with users for financial advice.  
✅ **Expense Categorization** – Categorizes expenses (Food, Transport, Shopping, etc.).  
✅ **Investment Insights** – Helps users identify opportunities for saving and investing.  
✅ **Cross-Service Integration** – Kotlin app connects seamlessly with Spring Boot + FastAPI services.  

---

## 📲 Screenshots
![Dashboard Screenshot](https://github.com/ojaslad10/AI-Finance-Advisor/blob/main/screechot.png?raw=true)

---

## 🚀 Setup & Run Instructions

### 🖥️ Backend (Spring Boot)

1. Open the Spring Boot project in your IDE (IntelliJ / Eclipse).  
2. Update the MongoDB connection details in `application.properties`.  
3. Run the project using:
   ```bash
   mvn spring-boot:run

#### Navigate to ML Service Directory:
```bash
cd Desktop/personal/Collegeproject/Application/backend/src/main/java/com/expensetracker/ml_services
source venv/bin/activate
uvicorn app:app --reload --host 0.0.0.0 --port 8000 --log-level info
The Python service will start on http://localhost:8000


---

## 🧩 AI Chatbot Capabilities

The AI Advisor can answer queries such as:

- 🧾 “Where did I spend the most this month?”  
- 💸 “How can I save more based on my spending habits?”  
- 📈 “What percentage of my income goes to food or transport?”  
- 💬 “Give me some smart investment tips.”  

The responses are **friendly, personalized, and insight-driven**, thanks to the **OpenAI GPT API** integration.

---



