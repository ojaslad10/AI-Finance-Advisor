# train_model.py
import os
import pandas as pd
import numpy as np
from pymongo import MongoClient
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_percentage_error, mean_squared_error, r2_score
import joblib

MONGO_URI = os.getenv("MONGODB_URI", "mongodb+srv://ojasdb15:ojasdb2003@cluster0.jdm8hdl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0")
DB = os.getenv("MONGODB_DATABASE", "expensetracker")
COL = os.getenv("MONGODB_COLLECTION", "expenses")

client = MongoClient(MONGO_URI)
col = client[DB][COL]

print("Loading expenses from MongoDB...")
docs = list(col.find({}))
if not docs:
    print("No data in collection. Run data generator first.")
    raise SystemExit(1)

df = pd.DataFrame(docs)
# ensure columns exist
df = df[[c for c in ["userId", "category", "amount", "date"] if c in df.columns]]
df["date"] = pd.to_datetime(df["date"], errors="coerce")
df = df.dropna(subset=["date", "amount", "userId", "category"])

# Create year-month key
df["year_month"] = df["date"].dt.to_period("M").astype(str)

# Aggregate per user-category-month
agg = df.groupby(["userId", "category", "year_month"]).agg(
    total_spend=("amount", "sum"),
    tx_count=("amount", "count"),
    avg_tx=("amount", "mean"),
).reset_index()

# Sort and create previous month feature
agg = agg.sort_values(["userId", "category", "year_month"])
agg["prev_total"] = agg.groupby(["userId", "category"])["total_spend"].shift(1)

# Drop rows without prev_total (we'll predict current month's total from prev month)
agg = agg.dropna(subset=["prev_total"])

# Feature engineering
# month as cyclical feature
agg["month"] = pd.to_datetime(agg["year_month"] + "-01").dt.month
agg["month_sin"] = np.sin(2 * np.pi * agg["month"] / 12)
agg["month_cos"] = np.cos(2 * np.pi * agg["month"] / 12)

# encode category and user
cat_le = LabelEncoder()
user_le = LabelEncoder()
agg["category_code"] = cat_le.fit_transform(agg["category"])
agg["user_code"] = user_le.fit_transform(agg["userId"])

# Features and target
features = ["prev_total", "tx_count", "avg_tx", "month_sin", "month_cos", "category_code", "user_code"]
X = agg[features].fillna(0)
y = agg["total_spend"]

print(f"Dataset size (samples): {len(X)}")

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

print("Training RandomForestRegressor...")
model = RandomForestRegressor(n_estimators=100, random_state=42, n_jobs=-1)
model.fit(X_train, y_train)

y_pred = model.predict(X_test)
mape = mean_absolute_percentage_error(y_test, y_pred)
from math import sqrt
rmse = sqrt(mean_squared_error(y_test, y_pred))
r2 = r2_score(y_test, y_pred)

print(f"Done. MAPE: {mape:.3f}, RMSE: {rmse:.2f}, R2: {r2:.3f}")

os.makedirs("models", exist_ok=True)
joblib.dump({"model": model, "cat_le": cat_le, "user_le": user_le}, "models/monthly_spend_model.joblib")
print("Saved model to models/monthly_spend_model.joblib")
