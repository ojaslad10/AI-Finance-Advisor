import os
import random
from datetime import datetime, timedelta
from pymongo import MongoClient

# ==========================
# 🔧 CONFIGURATION
# ==========================
MONGO_URI = os.getenv(
    "MONGODB_URI",
    "mongodb+srv://ojasdb15:ojasdb2003@cluster0.jdm8hdl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"
)
DB_NAME = os.getenv("MONGODB_DATABASE", "expensetracker")
EXPENSES_COLLECTION = os.getenv("MONGODB_COLLECTION", "expenses")
USERS_COLLECTION = "users"

NUM_RECORDS = int(os.getenv("NUM_RECORDS", "3000"))  # how many fake expenses to generate
BATCH_SIZE = 1000  # insert in batches for speed

client = MongoClient(MONGO_URI)
db = client[DB_NAME]
users_col = db[USERS_COLLECTION]
expenses_col = db[EXPENSES_COLLECTION]


users = list(users_col.find({}, {"_id": 1}))
if not users:
    print("⚠️ No users found in MongoDB! Add users via your app or insert one manually.")
    exit(1)

user_ids = [str(u["_id"]) for u in users]
print(f"✅ Found {len(user_ids)} real users in DB.")


categories = [
    "Food", "Shopping", "Bills", "Entertainment",
    "Travel", "Groceries", "Rent", "Fuel", "Healthcare", "Other"
]
banks = ["HDFC", "ICICI", "SBI", "Axis"]
accounts = ["Savings", "Credit", "Wallet"]
receivers = ["Amazon", "Swiggy", "Netflix", "Uber", "Zomato", "Flipkart", "Paytm", "PhonePe", "IRCTC"]

start_date = datetime(2023, 1, 1)
end_date = datetime(2025, 10, 1)


data_batch = []
count = 0

print(f"⚙️ Generating {NUM_RECORDS} fake expenses between {start_date.date()} and {end_date.date()}...")

for i in range(NUM_RECORDS):
    user_id = random.choice(user_ids)
    amount = round(random.uniform(50, 20000), 2)
    category = random.choice(categories)
    bank = random.choice(banks)
    account = random.choice(accounts)
    receiver = random.choice(receivers)

    # Random date within range
    days_diff = (end_date - start_date).days
    date = start_date + timedelta(days=random.randint(0, days_diff))

    doc = {
        "userId": user_id,
        "amount": amount,
        "bank": bank,
        "account": account,
        "receiver": receiver,
        "category": category,
        "date": date.strftime("%Y-%m-%d"),
        "idempotencyKey": f"auto-{i}"
    }

    data_batch.append(doc)
    if len(data_batch) >= BATCH_SIZE:
        expenses_col.insert_many(data_batch)
        count += len(data_batch)
        print(f"✅ Inserted {count} records so far...")
        data_batch = []

# Insert remaining records
if data_batch:
    expenses_col.insert_many(data_batch)
    count += len(data_batch)

print(f"🎉 Done! Inserted total {count} fake expense records linked to real users.")
