from pymongo import MongoClient
from bson.objectid import ObjectId
import os

MONGODB_URI = os.getenv("MONGODB_URI",
    "mongodb+srv://ojasdb15:ojasdb2003@cluster0.jdm8hdl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0")
DB = os.getenv("MONGODB_DATABASE", "expensetracker")
COL = os.getenv("MONGODB_COLLECTION", "expenses")

client = MongoClient(MONGODB_URI)
db = client[DB]
e = db[COL].find_one()
if not e:
    print("No expense docs found.")
else:
    print("sample expense doc keys:", list(e.keys()))
    print("userId value repr:", repr(e.get("userId")))
    print("userId type:", type(e.get("userId")))
