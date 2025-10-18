# migrate_userid_to_objectid.py
from pymongo import MongoClient
from bson.objectid import ObjectId
import os, re

MONGODB_URI = os.getenv("MONGODB_URI", "mongodb+srv://ojasdb15:ojasdb2003@cluster0.jdm8hdl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0")
DB = os.getenv("MONGODB_DATABASE", "expensetracker")
COL = os.getenv("MONGODB_COLLECTION", "expenses")

client = MongoClient(MONGODB_URI)
db = client[DB]
col = db[COL]

# regex to detect 24-hex chars (ObjectId hex)
oid_re = re.compile(r"^[0-9a-fA-F]{24}$")

count = 0
for doc in col.find({"userId": {"$type": "string"}}):
    uid = doc.get("userId")
    if uid and oid_re.match(uid):
        try:
            col.update_one({"_id": doc["_id"]}, {"$set": {"userId": ObjectId(uid)}})
            count += 1
        except Exception as e:
            print("Failed updating doc", doc.get("_id"), "error:", e)

print(f"Converted {count} documents to use ObjectId for userId.")
