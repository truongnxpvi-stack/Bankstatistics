package com.banktracker.parser

object CategoryClassifier {
    private val CATEGORIES = mapOf(
        "🍜 Ăn uống"     to listOf("shopeefood","grabfood","baemin","highlands","kfc","pizza","bun","pho","cafe","com","nha hang"),
        "🛒 Mua sắm"     to listOf("shopee","lazada","tiki","vinmart","coopmart","sieu thi"),
        "🚗 Di chuyển"   to listOf("grab","be app","parking","xang","petrolimex","taxi"),
        "🎬 Giải trí"    to listOf("netflix","spotify","steam","game","cinema","cgv","bhd"),
        "🏥 Y tế"        to listOf("benh vien","pharmacy","nha thuoc","vinmec","thuoc"),
        "📚 Giáo dục"    to listOf("hoc phi","truong","school","university","course"),
        "💡 Tiện ích"    to listOf("evn","dien luc","nuoc","internet","viettel","vnpt","mobifone","fpt"),
        "💸 Chuyển tiền" to listOf("chuyen khoan","transfer","gui tien","ck"),
        "🏠 Nhà ở"       to listOf("thue nha","tien nha","rent","quan ly")
    )
    fun classify(text: String): String {
        val lower = text.lowercase()
        for ((cat, keys) in CATEGORIES) if (keys.any { lower.contains(it) }) return cat
        return "📦 Khác"
    }
}
