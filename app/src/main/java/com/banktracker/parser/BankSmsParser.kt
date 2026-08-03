package com.banktracker.parser

import com.banktracker.data.Transaction
import com.banktracker.data.TransactionType

object BankSmsParser {

    private val BANK_SENDERS = setOf(
        "Vietcombank","VCB","BIDV","Agribank","Techcombank",
        "MB","MBBank","VPBank","TPBank","SHB","ACB","Sacombank",
        "HDBank","8149","8150","8900","6088","VIB","OCB"
    )

    fun isBankSms(sender: String) = BANK_SENDERS.any { sender.contains(it, true) }

    fun looksLikeTransaction(text: String): Boolean {
        val lower = text.lowercase()
        val hasAction = listOf("no ","co ","chi ","nhan ","thanh toan","rut tien",
            "chuyen tien","nap tien","ps no","ps co","gd moi nhat").any { lower.contains(it) }
        return hasAction && extractTxAmount(text) != null
    }

    fun parse(sender: String, body: String): Transaction? {
        val amount = extractTxAmount(body) ?: return null
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

    // ── Tách số tiền GIAO DỊCH (không lấy số dư) ──────────────────────────
    private fun extractTxAmount(body: String): Long? {

        // 1. SHB: "GD moi nhat: -50,000 VND: ..."
        Regex("""GD\s+moi\s+nhat\s*:\s*[-+]?([\d,]+)\s*VND""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
            ?.replace(",","")?.toLongOrNull()
            ?.takeIf { it > 1000 }?.let { return it }

        // 2. Vietcombank / BIDV: "PS No/Co <amount>"
        Regex("""PS\s+(?:No|Co)\s+([\d,.]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
            ?.takeIf { it > 1000 }?.let { return it }

        // 3. "so tien: 50,000" hoặc "tien GD: 50,000"
        Regex("""(?:so tien|tien\s*gd|tien giao dich|amount)\s*:?\s*([\d,.]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
            ?.takeIf { it > 1000 }?.let { return it }

        // 4. "thanh toan/nap tien/rut tien 50,000 VND"
        Regex("""(?:thanh toan|nap tien|rut tien|chi tieu)\s+([\d,.]+)\s*(?:VND|vnd|d|đ)?""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
            ?.takeIf { it > 1000 }?.let { return it }

        // 5. Fallback: lấy số đầu tiên có VND — nhưng loại trừ số dư
        val balance = extractBalance(body)
        Regex("""([\d,.]+)\s*(?:VND|VNĐ)\b""")
            .findAll(body)
            .mapNotNull { it.groupValues[1].replace(",","").replace(".","").toLongOrNull() }
            .firstOrNull { it > 1000 && it != balance }
            ?.let { return it }

        return null
    }

    // ── Tách số dư tài khoản ──────────────────────────────────────────────
    private fun extractBalance(body: String): Long? {
        // SHB: "SDTK ... la 3,514,763 VND."
        Regex("""SDTK[^l]{0,60}la\s+([\d,.]+)\s*VND""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
            ?.let { return it }

        // Chung: "So du: 3,514,763"
        Regex("""(?:so du|SD|balance|du kha dung)\s*:?\s*([\d,.]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
            ?.replace(",","")?.replace(".","")?.toLongOrNull()
            ?.let { return it }

        return null
    }

    private fun detectType(body: String): TransactionType {
        val lower = body.lowercase()
        // SHB dấu "-" = chi
        if (Regex("""GD\s+moi\s+nhat\s*:\s*-""", RegexOption.IGNORE_CASE).containsMatchIn(body))
            return TransactionType.DEBIT
        if (Regex("""GD\s+moi\s+nhat\s*:\s*\+""", RegexOption.IGNORE_CASE).containsMatchIn(body))
            return TransactionType.CREDIT

        val debit  = listOf("no ","chi ","thanh toan","rut tien","ps no","tien ra","giam","debit")
        val credit = listOf("co ","nhan ","chuyen den","nap tien","ps co","tien vao","tang","credit")
        return when {
            debit.any  { lower.contains(it) } -> TransactionType.DEBIT
            credit.any { lower.contains(it) } -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }
    }

    private fun extractDescription(body: String): String {
        // SHB: lấy phần sau "VND: <nội dung>"
        Regex("""GD\s+moi\s+nhat\s*:.*?VND\s*:\s*(.{3,80})""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()?.let { return it }

        Regex("""(?:ND|noi dung|mo ta|ref)\s*:?\s*(.{3,80})""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()?.let { return it }

        return body.take(100)
    }

    private fun detectBank(sender: String) = when {
        sender.contains("VCB",true) || sender.contains("Vietcombank",true) -> "Vietcombank"
        sender.contains("BIDV",true)   -> "BIDV"
        sender.contains("TCB",true) || sender.contains("Techcombank",true) -> "Techcombank"
        sender.contains("MB",true)     -> "MB Bank"
        sender.contains("VPB",true) || sender.contains("VPBank",true) -> "VPBank"
        sender.contains("TPB",true)    -> "TPBank"
        sender.contains("ACB",true)    -> "ACB"
        sender.contains("SHB",true)    -> "SHB"
        else -> sender.take(20)
    }
}
