package com.banktracker.parser

import com.banktracker.data.Transaction
import com.banktracker.data.TransactionType

object BankSmsParser {
    private val BANK_SENDERS = setOf(
        "Vietcombank","VCB","BIDV","Agribank","Techcombank",
        "MB","MBBank","VPBank","TPBank","SHB","ACB","Sacombank",
        "HDBank","8149","8150","8900","6088","VIB","OCB"
    )

    // Ưu tiên bắt "GD moi nhat: -50,000 VND" hoặc "giao dich: 50,000"
    private val TRANSACTION_PATTERNS = listOf(
        // Dạng SHB: "GD moi nhat: -50,000 VND"
        Regex("""GD\s+moi\s+nhat\s*:\s*[-+]?([\d,.]+)\s*(?:VND|vnd|đ|VNĐ)""", RegexOption.IGNORE_CASE),
        // Dạng "so tien GD: 50,000"
        Regex("""(?:so tien|tien giao dich|tien GD|amount)\s*:?\s*[-+]?([\d,.]+)""", RegexOption.IGNORE_CASE),
        // Dạng "PS No/Co 50,000"
        Regex("""(?:PS No|PS Co|Tien vao|Tien ra)\s*:?\s*([\d,.]+)""", RegexOption.IGNORE_CASE),
        // Dạng "thanh toan 50,000 VND"
        Regex("""(?:thanh toan|chi tieu|nap tien|rut tien)\s+([\d,.]+)\s*(?:VND|vnd|đ)?""", RegexOption.IGNORE_CASE),
        // Dạng Vietcombank: "50,000VND"
        Regex("""[-+]?([\d,.]+)\s*(?:VND|VNĐ)\b""")
    )

    // Regex tách riêng số dư — để LOẠI TRỪ khỏi số tiền GD
    private val BALANCE_PATTERN = Regex(
        """(?:SD|so du|balance|du kha dung|so tien hien tai|SDTK.*?la)\s*:?\s*([\d,.]+)\s*(?:VND|vnd|đ)?""",
        RegexOption.IGNORE_CASE
    )

    // Dạng SMS SHB đặc biệt: "SDTK ... la 3,514,763 VND. GD moi nhat: -50,000 VND"
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
        val balance = extractBalance(body)
        return Transaction(
            bank        = detectBank(sender),
            amount      = amount,
            type        = detectType(body),
            description = extractDescription(body),
            category    = CategoryClassifier.classify(body),
            balance     = balance,
            rawMessage  = body
        )
    }

    private fun extractTransactionAmount(body: String): Long? {
        // Thử pattern SHB trước (tách rõ số dư và số tiền GD)
        val shbMatch = SHB_PATTERN.find(body)
        if (shbMatch != null) {
            // Group 2 = số tiền GD (50,000)
            val txAmt = shbMatch.groupValues[2]
                .replace(",","").replace(".","").trim().toLongOrNull()
            if (txAmt != null && txAmt > 1000) return txAmt
        }

        // Lấy số dư để loại trừ
        val balanceAmt = extractBalance(body)

        // Thử từng pattern giao dịch
        for (p in TRANSACTION_PATTERNS) {
            val v = p.find(body)?.groupValues?.get(1)
                ?.replace(",","")?.replace(".","")?.trim()?.toLongOrNull()
            if (v != null && v > 1000 && v != balanceAmt) return v
        }
        return null
    }

    private fun extractBalance(body: String): Long? {
        // Dạng SHB: "SDTK ... la 3,514,763 VND"
        val shbBal = Regex("""SDTK[^l]+la\s+([\d,.]+)\s*VND""", RegexOption.IGNORE_CASE).find(body)
        if (shbBal != null) {
            return shbBal.groupValues[1].replace(",","").replace(".","").toLongOrNull()
        }
        return BALANCE_PATTERN.find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
    }

    private fun detectType(body: String): TransactionType {
        val lower = body.lowercase()
        // SHB dùng dấu - trước số tiền GD
        if (lower.contains("gd moi nhat") && body.contains("GD moi nhat: -", ignoreCase = true))
            return
