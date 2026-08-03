package com.banktracker.parser

object CategoryClassifier {
    private val CATEGORIES = mapOf(
        "🍜 Ăn uống"    to listOf("shopeefood","grabfood","baemin","highlands","phuc long","kfc","lotteria","pizza","bun","pho","cafe","tra sua","com","nha hang","quan an","tien rau"),
        "🛒 Mua sắm"    to listOf("shopee","lazada","tiki","sendo","vinmart","coopmart","sieu thi","supermarket","bach hoa"),
        "🚗 Di chuyển"  to listOf("grab","be app","xanh sm","parking","xang dau","petrolimex","taxi","xe om","goxe"),
        "🎬 Giải trí"   to listOf("netflix","spotify","youtube","steam","game","cinema","cgv","bhd","lotte cinema","ticketbox"),
        "🏥 Y tế"       to listOf("hospital","benh vien","pharmacy","nha thuoc","vinmec","medical","clinic","thuoc"),
        "📚 Giáo dục"   to listOf("hoc phi","tuition","truong","school","university","khoa hoc","course","hoc"),
        "💡 Tiện ích"   to listOf("evn","dien luc","nuoc sach","internet","viettel","vnpt","mobifone","vietnamobile","fpt"),
        "💸 Chuyển tiền" to listOf("chuyen khoan","transfer","gui tien","ck","nop tien"),
        "🏠 Nhà ở"      to listOf("thue nha","tien nha","quan ly","dien","nuoc","gas","rent"),
        "👗 Thời trang"  to listOf("zara","h&m","uniqlo","giay","quan ao","thoi trang","fashion")
    )

    fun classify(text: String): String {
        val lower = text.lowercase()
        for ((cat, keys) in CATEGORIES) {
            if (keys.any { lower.contains(it) }) return cat
        }
        return "📦 Khác"
    }
}
