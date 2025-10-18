# app.py
import os
import json
import requests
from fastapi import FastAPI, Query, HTTPException
from pymongo import MongoClient
from datetime import datetime, date
from collections import defaultdict
import statistics
import joblib
import numpy as np
import pandas as pd
from bson.objectid import ObjectId
from pydantic import BaseModel
from openai import OpenAI
from dotenv import load_dotenv
import re, random
from fastapi import HTTPException, Header

load_dotenv()

# =======================
# Config
# =======================
MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "test")
MONGODB_COLLECTION = os.getenv("MONGODB_COLLECTION", "expenses")

client = MongoClient(MONGODB_URI)
db = client[MONGODB_DATABASE]
col = db[MONGODB_COLLECTION]

app = FastAPI(title="Expense ML Service")

# =======================
# Load ML model
# =======================
MODEL_PATH = os.path.join(os.path.dirname(__file__), "models", "monthly_spend_model.joblib")
model_bundle = None
model = None
cat_le = None
user_le = None
try:
    if os.path.exists(MODEL_PATH):
        model_bundle = joblib.load(MODEL_PATH)
        model = model_bundle.get("model")
        cat_le = model_bundle.get("cat_le")
        user_le = model_bundle.get("user_le")
        print("Loaded ML model from", MODEL_PATH)
    else:
        print("No ML model found at", MODEL_PATH)
except Exception as e:
    print("Failed loading model:", e)
    model = None

# =======================
# Helpers
# =======================
def parse_date_iso(s):
    try:
        return datetime.fromisoformat(s).date()
    except Exception:
        return None

def month_key(d: date):
    return d.year, d.month

def next_month_key(y, m):
    if m == 12:
        return (y + 1, 1)
    return (y, m + 1)

# =======================
# API
# =======================
@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/analyze/{user_id}")
def analyze(user_id: str, window: int = Query(3, ge=1, le=12)):
    # try ObjectId then string
    try:
        oid = ObjectId(user_id)
        query = {"$or": [{"userId": oid}, {"userId": user_id}]}
    except Exception:
        query = {"userId": user_id}

    docs = list(col.find(query))
    print(f"MONGO DEBUG -> using query={query} found {len(docs)} docs")
    if not docs:
        return {
            "user_id": user_id,
            "analysis_date": date.today().isoformat(),
            "category_summaries": [],
            "anomalies": [],
            "overall_advice_hint": "No transactions found for this user."
        }

    # Parse transactions
    txs = []
    for d in docs:
        amt = d.get("amount", 0)
        try:
            amt_f = float(amt)
            spend = abs(amt_f)
        except Exception:
            amt_f = 0.0
            spend = 0.0
        cat = d.get("category") or "Other"
        parsed = parse_date_iso(d.get("date")) if d.get("date") else None
        txs.append({"id": str(d.get("_id","")), "amount": amt_f, "spend": spend, "category": cat, "date": parsed})

    # build per-category per-month aggregates
    cat_month = defaultdict(lambda: defaultdict(float))
    cat_tx_counts = defaultdict(lambda: defaultdict(int))
    cat_tx_sums = defaultdict(lambda: defaultdict(float))
    all_spends = []

    for t in txs:
        if t["date"] is None:
            continue
        k = month_key(t["date"])
        cat_month[t["category"]][k] += t["spend"]
        cat_tx_counts[t["category"]][k] += 1
        cat_tx_sums[t["category"]][k] += t["spend"]
        all_spends.append(t["spend"])

    # Anchor month selection: prefer calendar month if it has transactions, else most recent month
    today = date.today()
    calendar_key = (today.year, today.month)
    all_dates = [t["date"] for t in txs if t["date"]]
    if any(d.year == calendar_key[0] and d.month == calendar_key[1] for d in all_dates):
        anchor = calendar_key
    elif all_dates:
        latest = max(all_dates)
        anchor = (latest.year, latest.month)
    else:
        anchor = calendar_key

    # helper: yield N months backward from key (inclusive)
    def months_ago(key, n):
        y, m = key
        for _ in range(n):
            yield (y, m)
            if m == 1:
                y -= 1
                m = 12
            else:
                m -= 1

    # Rolling window (N months) for current period
    N = window

    category_summaries = []
    for cat, months in cat_month.items():
        # current = sum of last N months ending at anchor
        curr = 0.0
        for k in months_ago(anchor, N):
            curr += months.get(k, 0.0)

        # previous window (N months immediately before current window)
        py, pm = anchor
        for _ in range(N):
            if pm == 1:
                py -= 1
                pm = 12
            else:
                pm -= 1
        prev_anchor = (py, pm)
        prev = 0.0
        for k in months_ago(prev_anchor, N):
            prev += months.get(k, 0.0)

        # mom percentage
        mom = None if prev == 0 else round(((curr - prev) / prev) * 100, 2)

        # fallback forecast (avg of last up-to-3 months present in months dict)
        month_vals = sorted(months.items(), key=lambda x: x[0])
        last_vals = [v for _, v in month_vals[-3:]]
        fallback_forecast = round(sum(last_vals) / len(last_vals), 2) if last_vals else round(curr, 2)

        # ML forecast (use DataFrame to preserve feature names)
        forecast = float(fallback_forecast)
        if model is not None:
            try:
                prev_total = float(curr)
                tx_count = sum(cat_tx_counts[cat].get(k, 0) for k in months_ago(anchor, N))
                avg_tx = (sum(cat_tx_sums[cat].get(k, 0.0) for k in months_ago(anchor, N)) / tx_count) if tx_count > 0 else 0.0

                nm_year, nm_month = next_month_key(*anchor)
                month_sin = np.sin(2 * np.pi * nm_month / 12)
                month_cos = np.cos(2 * np.pi * nm_month / 12)

                if cat_le is not None and hasattr(cat_le, "classes_"):
                    try:
                        category_code = int(np.where(cat_le.classes_ == cat)[0][0])
                    except Exception:
                        category_code = -1
                else:
                    category_code = -1

                if user_le is not None and hasattr(user_le, "classes_"):
                    try:
                        user_code = int(np.where(user_le.classes_ == user_id)[0][0])
                    except Exception:
                        user_code = -1
                else:
                    user_code = -1

                feat_row = {
                    "prev_total": prev_total,
                    "tx_count": tx_count,
                    "avg_tx": avg_tx,
                    "month_sin": month_sin,
                    "month_cos": month_cos,
                    "category_code": category_code,
                    "user_code": user_code
                }

                Xdf = pd.DataFrame([feat_row], columns=[
                    "prev_total", "tx_count", "avg_tx",
                    "month_sin", "month_cos", "category_code", "user_code"
                ])
                ypred = model.predict(Xdf)
                forecast = float(round(float(ypred[0]), 2))

            except Exception as e:
                print("ML predict failed:", e)
                forecast = float(fallback_forecast)

        # over-budget heuristic (applies to aggregated curr/prev)
        is_over_budget = False
        if prev > 0:
            is_over_budget = curr > prev * 1.10
        else:
            is_over_budget = curr > 0 and prev == 0 and len(last_vals) > 1 and curr > sum(last_vals[:-1])

        recommendation = "Try reducing recurring buys or weekly takeout; set a category budget."
        if cat.lower() in ("food", "eating", "dining", "restaurant"):
            recommendation = "Cook at home more often; try meal-prep for 1–2 days."
        elif cat.lower() in ("entertainment", "movies", "subscriptions", "ott"):
            recommendation = "Review and cancel unused subscriptions; choose 1 streaming plan."

        category_summaries.append({
            "category": cat,
            "current_month_spend": round(curr, 2),
            "forecast_next_month": forecast,
            "mom_change_pct": mom,
            "is_over_budget": bool(is_over_budget),
            "recommendation_short": recommendation
        })

    # anomaly detection
    anomalies = []
    try:
        if len(all_spends) >= 3:
            mean_ = statistics.mean(all_spends)
            stdev = statistics.pstdev(all_spends)
            threshold = mean_ + 3 * stdev
            for tx in txs:
                if tx["spend"] > threshold:
                    anomalies.append({
                        "transaction_id": tx["id"],
                        "amount": tx["amount"],
                        "reason": "one-off large payment (statistical outlier)",
                    })
        else:
            sorted_txs = sorted(txs, key=lambda x: x["spend"], reverse=True)[:2]
            for tx in sorted_txs:
                if tx["spend"] > 0:
                    anomalies.append({
                        "transaction_id": tx["id"],
                        "amount": tx["amount"],
                        "reason": "large transaction (few data points)"
                    })
    except Exception:
        anomalies = []

    overall_hint = "No significant spending detected"
    sorted_by_curr = sorted(category_summaries, key=lambda x: x["current_month_spend"], reverse=True)
    top_cats = [c["category"] for c in sorted_by_curr[:2]]
    if top_cats and sum([c["current_month_spend"] for c in sorted_by_curr[:2]]) > 0:
        overall_hint = f"Overspending in {', '.join(top_cats)}"

    return {
        "user_id": user_id,
        "analysis_date": today.isoformat(),
        "category_summaries": category_summaries,
        "anomalies": anomalies,
        "overall_advice_hint": overall_hint
    }

# =======================
# AI Chat Route
# =======================
class ChatRequest(BaseModel):
    userId: str
    message: str
    window: int = 3

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    print("⚠️ Warning: OPENAI_API_KEY not set in environment!")
    openai_client = None
else:
    openai_client = OpenAI(api_key=OPENAI_API_KEY)


@app.post("/api/chat")
def chat_with_ai(req: ChatRequest, authorization: str = Header(None)):
    import random
    import requests

    user_message = (req.message or "").strip().lower()

    # --- improved greeting / small-chitchat handling (fast path) ---
    if user_message:
        # detect casual greetings and short chit-chat
        if re.search(r"\b(hi|hello|hey|yo|sup|gm|gn|good\s*(morning|evening|afternoon)|howdy|hiya|heyo)\b", user_message):
            user_name = None
            # try best-effort name lookup from your Spring /me endpoint if Authorization header forwarded
            if authorization:
                try:
                    r = requests.get("http://127.0.0.1:8080/api/users/me",
                                     headers={"Authorization": authorization}, timeout=3)
                    if r.status_code == 200:
                        j = r.json()
                        user_name = j.get("name") or j.get("id")
                except Exception:
                    user_name = None

            # styles: friendly, playful, helpful
            friendly = [
                "Hey there! 👋 How are you doing today?",
                "Hi! 😊 Hope your day’s going well — want me to check your spending?",
                "Hello! 👋 Great to see you. Shall I summarize your recent expenses?",
                "Hey! 😄 Need help with budgeting or a quick spending overview?"
            ]
            casual = [
                "Yo! What’s up? Want a quick spending snapshot?",
                "Sup! 👋 Want me to look at your recent transactions?",
                "Heyo! Need a budget tip or a quick summary?"
            ]
            warm = [
                "Good to see you! Would you like me to summarize your month?",
                "Nice to see you! I can give you a quick spending overview — shall I?",
            ]

            # if message is very short (one word) prefer casual/friendly
            tokens = user_message.split()
            if len(tokens) == 1:
                pool = friendly + casual
            else:
                pool = friendly + warm

            greeting = random.choice(pool)
            if user_name:
                # inject short name politely: "Hey Ojas! How are you..."
                greeting = f"{greeting.split('!')[0]} {user_name}! " + " ".join(greeting.split('!')[1:]).strip()

            return {"success": True, "reply": greeting, "debug": {"note": "greeting detected", "user_name": user_name}}
    # --- end greeting block ---

    # --- fallback: local ML analysis + OpenAI (same behavior as before) ---
    try:
        # call the analyze() helper directly (window=3)
        analysis = analyze(req.userId, 3)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Local analysis failed: {e}")

    hint = analysis.get("overall_advice_hint", "No transactions found.")

    system_prompt = (
        "You are a friendly and insightful financial assistant chatbot for Indian users. "
        "Always use Indian Rupees (Rs) as the currency symbol instead of $. "
        "Never mention dollars. Format numbers with commas, like Rs 12,345.67. "
        "If you mention amounts from data, prefix them with Rs."
    )


    user_prompt = f"""
    The user's spending analysis data:
    {json.dumps(analysis, ensure_ascii=False, indent=2)}

    The user says: "{req.message}"

    Provide a concise, helpful, and conversational response referencing their spending categories.
    """

    # If OpenAI not configured, send a short ML-only fallback summary
    if openai_client is None:
        top_hint = hint
        short_lines = [f"Here's a quick tip: {top_hint}"]
        cats = analysis.get("category_summaries", [])[:2]
        for c in cats:
            f"- {c.get('category')}: ₹{c.get('current_month_spend')} (next: ₹{c.get('forecast_next_month')})"
        return {"success": True, "reply": "\n".join(short_lines), "debug": {"analysis_hint": top_hint, "note": "openai not configured"}}

    # Call OpenAI
    try:
        completion = openai_client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            max_tokens=512,
        )
        # defensive parsing
        reply = ""
        if hasattr(completion, "choices") and len(completion.choices) > 0:
            choice = completion.choices[0]
            if hasattr(choice, "message") and choice.message is not None:
                reply = getattr(choice.message, "content", "") or ""
            else:
                reply = getattr(choice, "text", "") or ""
        else:
            reply = str(completion)

        reply = reply.strip() or "Sorry — I couldn't generate a response just now."
        reply = reply.replace("$", "₹")
        return {"success": True, "reply": reply, "debug": {"analysis_hint": hint}}

    except Exception as e:
        print(" OpenAI call failed:", e)
        return {"success": True, "reply": f"I couldn't call the AI service right now. Quick tip: {hint}", "debug": {"error": str(e)}}