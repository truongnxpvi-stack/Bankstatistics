package com.banktracker.parser

import com.banktracker.data.Transaction
import com.banktracker.data.TransactionType

object BankSmsParser {
    private val BANK_SENDERS = setOf(
        "Vietcombank","VCB","BIDV","Agribank","Techcombank",
        "MB","MBBank","VPBank","TPBank","SHB","ACB","Sacombank",
        "HDBank","8149","8150","8900","6088","VIB","OCB"
    )
    private val AMOUNT_PATTERNS = listOf(
        Regex("""([\d,.]+)\s*(?:VND|vnd|đ|VNĐ)"""),
        Regex("""(?:so tien|tien giao dich|amount|GD|PS No|PS Co)\s*:?\s*([\d,.]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:thanh toan|chi tieu|nap|rut)\s+([\d,.]+)""", RegexOption.IGNORE_CASE)
    )
    private val DEBIT_WORDS  = listOf("no ","chi ","thanh toan","rut tien","debit","ps no","tien ra")
    private val CREDIT_WORDS = listOf("co ","nhan ","chuyen den","credit","nap tien","ps co","tien vao")

    fun isBankSms(sender: String) = BANK_SENDERS.any { sender.contains(it, true) }
    fun looksLikeTransaction(text: String): Boolean {
        val lower = text.lowercase()
        return (DEBIT_WORDS + CREDIT_WORDS).any { lower.contains(it) } && extractAmount(text) != null
    }
    fun parse(sender: String, body: String): Transaction? {
        val amount = extractAmount(body) ?: return null
        if (amount < 1000) return null
        return Transaction(
            bank        = detectBank(sender),
            amount      = amount,
            type        = detectType(body),
            description = extractDescription(body),
            category    = CategoryClassifier.classify(body),
            balance     = extractBalance(body),
            rawMessage  = body
        )
    }
    private fun extractAmount(body: String): Long? {
        for (p in AMOUNT_PATTERNS) {
            val v = p.find(body)?.groupValues?.get(1)
                ?.replace(",","")?.replace(".","")?.trim()?.toLongOrNull()
            if (v != null && v > 1000) return v
        }
        return null
    }
    private fun detectType(body: String): TransactionType {
        val lower = body.lowercase()
        return when {
            DEBIT_WORDS.any  { lower.contains(it) } -> TransactionType.DEBIT
            CREDIT_WORDS.any { lower.contains(it) } -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }
    }
    private fun extractDescription(body: String): String {
        val p = Regex("""(?:ND|noi dung|GD|mo ta|ref)\s*:?\s*(.{3,80})""", RegexOption.IGNORE_CASE)
        return p.find(body)?.groupValues?.get(1)?.trim() ?: body.take(100)
    }
    private fun extractBalance(body: String): Long? {
        val p = Regex("""(?:SD|so du|balance)\s*:?\s*([\d,.]+)""", RegexOption.IGNORE_CASE)
        return p.find(body)?.groupValues?.get(1)?.replace(",","")?.replace(".","")?.toLongOrNull()
    }
    private fun detectBank(sender: String) = when {
        sender.contains("VCB",true)||sender.contains("Vietcombank",true) -> "Vietcombank"
        sender.contains("BIDV",true)   -> "BIDV"
        sender.contains("TCB",true)||sender.contains("Techcombank",true) -> "Techcombank"
        sender.contains("MB",true)     -> "MB Bank"
        sender.contains("VPB",true)||sender.contains("VPBank",true) -> "VPBank"
        sender.contains("TPB",true)    -> "TPBank"
        sender.contains("ACB",true)    -> "ACB"
        else -> sender.take(20)
    }
}
