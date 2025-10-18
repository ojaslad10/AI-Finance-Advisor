FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt .
RUN apt-get update && \
    apt-get install -y build-essential gcc && \
    pip install --no-cache-dir -r requirements.txt && \
    apt-get remove -y build-essential gcc && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

COPY . .

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
