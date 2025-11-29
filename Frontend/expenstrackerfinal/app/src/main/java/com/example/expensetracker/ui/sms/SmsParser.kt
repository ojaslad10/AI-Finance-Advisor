package com.example.expensetracker.ui.sms

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.security.MessageDigest

// richer parsing result
data class ParsedSmsResult(
    val amount: Double,
    val bank: String,
    val account: String,
    val receiver: String,
    val date: String, // ISO yyyy-MM-dd
    val direction: String, // "credit" | "debit" | "unknown"
    val categorySuggestion: String,
    val confidence: Double, // 0.0..1.0
    val idempotencyKey: String
)

object SmsParser {

    private val patternSent = Regex("""(?i)Sent\s+Rs\.?\s*([\d,]+(?:\.\d+)?)[\s,]+From\s+(.+?)\s+A/C\s+\*?(\d+)[\s,]+To\s+(.+?)[\s,]+On\s+(\d{2}/\d{2}/\d{2,4})""")
    private val patternCredited = Regex("""(?i)(credited|deposited|paid into|cr\s+by|cr:)\D*([₹Rs\.\s]*)([\d,]+(?:\.\d+)?)""")
    private val patternDebited = Regex("""(?i)(debited|withdrawn|spent|paid|purchase|txn)\D*([₹Rs\.\s]*)([\d,]+(?:\.\d+)?)""")
    private val patternGeneric = Regex("""(?i)(?:Rs|INR|₹)\s*([\d,]+(?:\.\d+)?)""")




    private val creditKeywords = listOf("credited", "deposit", "salary", "received", "refund", "cr")
    private val debitKeywords = listOf("debited", "withdrawn", "paid", "purchase", "spent", "txn", "sent", "transfer")

    private val datePatterns = listOf("dd/MM/yy", "dd/MM/yyyy", "yyyy-MM-dd")

    private fun convertDateToIso(dateStr: String?): String? {
        if (dateStr == null) return null
        val s = dateStr.trim()
        for (p in datePatterns) {
            try {
                val dt = LocalDate.parse(s, DateTimeFormatter.ofPattern(p, Locale.ENGLISH))
                return dt.toString()
            } catch (_: DateTimeParseException) { }
        }
        return try {
            LocalDate.parse(s).toString()
        } catch (_: Exception) { null }
    }

    private fun normalizeAmount(raw: String): Double? {
        val cleaned = raw.replace("""[,\s]""".toRegex(), "").replace("₹","").replace("Rs.","").replace("Rs","")
        return cleaned.toDoubleOrNull()
    }

    private fun sha256Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun parse(message: String, sender: String = "unknown", receivedDateIso: String? = null): ParsedSmsResult? {
        val text = message.trim()

        patternSent.find(text)?.let { m ->
            val amountRaw = m.groupValues[1]
            val bankRaw = m.groupValues[2]
            val accountRaw = m.groupValues[3]
            val receiverRaw = m.groupValues[4]
            val dateRaw = m.groupValues[5]

            val amt = normalizeAmount(amountRaw) ?: return null
            val isoDate = convertDateToIso(dateRaw) ?: receivedDateIso ?: LocalDate.now().toString()
            val direction = "debit"
            val category = heuristicCategoryFromText(text, receiverRaw)
            val confidence = 0.95
            val idKey = generateIdempotencyKey(sender, isoDate, amt, text)

            return ParsedSmsResult(
                amount = amt,
                bank = bankRaw.trim(),
                account = accountRaw.trim(),
                receiver = receiverRaw.trim(),
                date = isoDate,
                direction = direction,
                categorySuggestion = category,
                confidence = confidence,
                idempotencyKey = idKey
            )
        }

        patternCredited.find(text)?.let { m ->
            val amountRaw = m.groupValues[3]
            val amt = normalizeAmount(amountRaw) ?: return@let
            val isoDate = parseDateFromText(text) ?: receivedDateIso ?: LocalDate.now().toString()
            val direction = "credit"
            val bank = inferBankFromText(text) ?: sender
            val receiver = inferReceiverFromText(text) ?: sender
            val category = heuristicCategoryFromText(text, receiver)
            val confidence = 0.85
            val idKey = generateIdempotencyKey(sender, isoDate, amt, text)

            return ParsedSmsResult(amt, bank, "", receiver, isoDate, direction, category, confidence, idKey)
        }

        patternDebited.find(text)?.let { m ->
            val amountRaw = m.groupValues[3]
            val amt = normalizeAmount(amountRaw) ?: return@let
            val isoDate = parseDateFromText(text) ?: receivedDateIso ?: LocalDate.now().toString()
            val direction = "debit"
            val bank = inferBankFromText(text) ?: sender
            val receiver = inferReceiverFromText(text) ?: "Merchant"
            val category = heuristicCategoryFromText(text, receiver)
            val confidence = 0.85
            val idKey = generateIdempotencyKey(sender, isoDate, amt, text)

            return ParsedSmsResult(amt, bank, "", receiver, isoDate, direction, category, confidence, idKey)
        }


        patternGeneric.find(text)?.let { m ->
            val amountRaw = m.groupValues[1]
            val amt = normalizeAmount(amountRaw) ?: return null
            val isoDate = parseDateFromText(text) ?: receivedDateIso ?: LocalDate.now().toString()
            val direction = decideDirectionByKeywords(text)
            val bank = inferBankFromText(text) ?: sender
            val receiver = inferReceiverFromText(text) ?: "Unknown"
            val category = heuristicCategoryFromText(text, receiver)
            val confidence = if (direction == "unknown") 0.5 else 0.7
            val idKey = generateIdempotencyKey(sender, isoDate, amt, text)

            return ParsedSmsResult(amt, bank, "", receiver, isoDate, direction, category, confidence, idKey)
        }
        return null
    }

    private fun parseDateFromText(text: String): String? {
        val re1 = Regex("""(\b\d{2}/\d{2}/\d{2,4}\b)""")
        re1.find(text)?.let { return convertDateToIso(it.groupValues[1]) }
        val re2 = Regex("""(\b\d{4}-\d{2}-\d{2}\b)""")
        re2.find(text)?.let { return convertDateToIso(it.groupValues[1]) }

        return null
    }

    private fun inferBankFromText(text: String): String? {
        val banks = listOf("HDFC", "ICICI", "SBI", "Axis", "Kotak", "Bank", "State Bank")
        banks.forEach { b ->
            if (text.contains(b, ignoreCase = true)) return b
        }
        return null
    }

    private fun inferReceiverFromText(text: String): String? {
        Regex("""\bto\s+([A-Za-z0-9&\.\- ]{2,40})""", RegexOption.IGNORE_CASE).find(text)?.let { return it.groupValues[1].trim() }
        Regex("""\bfrom\s+([A-Za-z0-9&\.\- ]{2,40})""", RegexOption.IGNORE_CASE).find(text)?.let { return it.groupValues[1].trim() }
        return null
    }

    private fun decideDirectionByKeywords(text: String): String {
        val low = text.lowercase()
        if (creditKeywords.any { low.contains(it) }) return "credit"
        if (debitKeywords.any { low.contains(it) }) return "debit"
        return "unknown"
    }

    private fun heuristicCategoryFromText(text: String, receiverHint: String): String {
        val low = (text + " " + receiverHint).lowercase()
        return when {
            low.contains("uber") || low.contains("ola") || low.contains("taxi") -> "Transport"
            low.contains("swiggy") || low.contains("zomato") || low.contains("restaurant") -> "Food"
            low.contains("amazon") || low.contains("flipkart") || low.contains("shopping") -> "Shopping"
            low.contains("netflix") || low.contains("ott") || low.contains("subscription") -> "Entertainment"
            low.contains("salary") || low.contains("payroll") -> "Income"
            low.contains("emi") || low.contains("loan") -> "EMI"
            low.contains("grocery") || low.contains("bigbazaar") -> "Grocery"
            else -> "Other"
        }
    }

    private fun generateIdempotencyKey(sender: String, dateIso: String, amount: Double, snippet: String): String {
        val raw = "${sender.trim().lowercase()}|${dateIso}|${String.format("%.2f", amount)}|${snippet.trim().take(120)}"
        return sha256Hex(raw)
    }

    // Public helper: guess category from edited title or receiver
    fun guessCategoryFromText(text: String, receiverHint: String? = null): String {
        // Reuse the same heuristic keywords as parse()
        val low = (text + " " + (receiverHint ?: "")).lowercase()
        return when {
            low.contains("uber") || low.contains("ola") || low.contains("taxi") -> "Transport"
            low.contains("swiggy") || low.contains("zomato") || low.contains("restaurant") -> "Food"
            low.contains("amazon") || low.contains("flipkart") || low.contains("shopping") -> "Shopping"
            low.contains("netflix") || low.contains("ott") || low.contains("subscription") -> "Entertainment"
            low.contains("salary") || low.contains("payroll") -> "Income"
            low.contains("emi") || low.contains("loan") -> "EMI"
            low.contains("grocery") || low.contains("bigbazaar") -> "Grocery"
            else -> "Other"
        }
    }

}
