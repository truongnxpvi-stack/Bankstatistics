package com.banktracker.parser

import com.banktracker.data.Transaction
import com.banktracker.data.TransactionType

object BankSmsParser {
    private val BANK_SENDERS = setOf(
        "Vietcombank","VCB","BIDV","Agribank","Techcombank",
        "MB","MBBank","VPBank","TPBank","SHB","ACB","Sacombank",
        "HDBank","8149","8150","8900","6088","VIB","OCB"
    )

    private val TRANSACTION_PATTERNS = listOf(
        Regex("""GD\s+moi\s+nhat\s*:\s*[-+]?([\d,.]+)\s*(?:VND|vnd|đ|VNĐ)""", RegexOption.IGNORE_CASE),
        Regex("""(?:so tien|tien giao dich|tien GD|amount)\s*:?\s*[-+]?([\d,.]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:PS No|PS Co|Tien vao|Tien ra)\s*:?\s*([\d,.]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:thanh toan|chi tieu|nap tien|rut tien)\s+([\d,.]+)\s*(?:VND|vnd|đ)?""", RegexOption.IGNORE_CASE),
        Regex("""[-+]?([\d,.]+)\s*(?:VND|VNĐ)\b""")
    )

    private val BALANCE_PATTERN = Regex(
        """(?:SD|so du|balance|du kha dung)\s*:?\s*([\d,.]+)\s*(?:VND|vnd|đ)?""",
        RegexOption.IGNORE_CASE
    )

    private val SHB_PATTERN = Regex(
        """SDTK[^.]+la\s+([\d,.]+)\s*VND[^G]*GD\s+moi\s+nhat\s*:\s*[-+]?([\d,.]+)\s*VND""",
        RegexOption.IGNORE_CASE
    )

    private val DEBIT_WORDS  = listOf("no ","chi ","thanh toan","rut tien","debit","ps no","tien ra","giam","- ")
    private val CREDIT_WORDS = listOf("co ","nhan ","chuyen den","credit","nap tien","ps co","tien vao","tang","+ ")

    fun isBankSms(sender: String) = BANK_SENDERS.any { sender.contains(it, true) }

    fun looksLikeTransaction(text: String): Boolean {
        val lower = text.lowercase()
        return (DEBIT_WORDS + CREDIT_WORDS).any { lower.contains(it) } && extractTransactionAmount(text) != null
    }

    fun parse(sender: String, body: String): Transaction? {
        val amount = extractTransactionAmount(body) ?: return null
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

    private fun extractTransactionAmount(body: String): Long? {
        val shbMatch = SHB_PATTERN.find(body)
        if (shbMatch != null) {
            val txAmt = shbMatch.groupValues[2]
                .replace(",","").replace(".","").trim().toLongOrNull()
            if (txAmt != null && txAmt > 1000) return txAmt
        }
        val balanceAmt = extractBalance(body)
        for (p in TRANSACTION_PATTERNS) {
            val v = p.find(body)?.groupValues?.get(1)
                ?.replace(",","")?.replace(".","")?.trim()?.toLongOrNull()
            if (v != null && v > 1000 && v != balanceAmt) return v
        }
        return null
    }

    private fun extractBalance(body: String): Long? {
        val shbBal = Regex("""SDTK[^l]+la\s+([\d,.]+)\s*VND""", RegexOption.IGNORE_CASE).find(body)
        if (shbBal != null) {
            return shbBal.groupValues[1].replace(",","").replace(".","").toLongOrNull()
        }
        return BALANCE_PATTERN.find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
    }

    private fun detectType(body: String): TransactionType {
        val lower = body.lowercase()
        if (body.contains("GD moi nhat: -", ignoreCase = true)) return TransactionType.DEBIT
        if (body.contains("GD moi nhat: +", ignoreCase = true)) return TransactionType.CREDIT
        return when {
            DEBIT_WORDS.any  { lower.contains(it) } -> TransactionType.DEBIT
            CREDIT_WORDS.any { lower.contains(it) } -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }
    }

    private fun extractDescription(body: String): String {
        val gdDesc = Regex("""GD\s+moi\s+nhat\s*:.*?VND\s*:\s*(.{3,80})""", RegexOption.IGNORE_CASE).find(body)
        if (gdDesc != null) return gdDesc.groupValues[1].trim().take(80)
        val p = Regex("""(?:ND|noi dung|GD|mo ta|ref)\s*:?\s*(.{3,80})""", RegexOption.IGNORE_CASE)
        return p.find(body)?.groupValues?.get(1)?.trim() ?: body.take(100)
    }

    private fun detectBank(sender: String) = when {
        sender.contains("VCB",true)||sender.contains("Vietcombank",true) -> "Vietcombank"
        sender.contains("BIDV",true)   -> "BIDV"
        sender.contains("TCB",true)||sender.contains("Techcombank",true) -> "Techcombank"
        sender.contains("MB",true)     -> "MB Bank"
        sender.contains("VPB",true)||sender.contains("VPBank",true) -> "VPBank"
        sender.contains("TPB",true)    -> "TPBank"
        sender.contains("ACB",true)    -> "ACB"
        sender.contains("SHB",true)    -> "SHB"
        else -> sender.take(20)
    }
}
