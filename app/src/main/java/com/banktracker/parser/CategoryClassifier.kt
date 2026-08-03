package com.banktracker.parser

object CategoryClassifier {
    private val CATEGORIES = mapOf(
        "An uong"     to listOf("shopeefood","grabfood","baemin","highlands","kfc","pizza","bun","pho","cafe","com","nha hang","quan an","tien rau"),
        "Mua sam"     to listOf("shopee","lazada","tiki","vinmart","coopmart","sieu thi","supermarket"),
        "Di chuyen"   to listOf("grab","be app","parking","xang","petrolimex","taxi","xe om"),
        "Giai tri"    to listOf("netflix","spotify","steam","game","cinema","cgv","bhd","lotte cinema"),
        "Y te"        to listOf("benh vien","pharmacy","nha thuoc","vinmec","thuoc","kham"),
        "Giao duc"    to listOf("hoc phi","truong","school","university","course","hoc"),
        "Tien ich"    to listOf("evn","dien luc","nuoc","internet","viettel","vnpt","mobifone","fpt"),
        "Chuyen tien" to listOf("chuyen khoan","transfer","gui tien","ck","nop tien"),
        "Nha o"       to listOf("thue nha","tien nha","rent","quan ly chung cu"),
        "Vietlott"    to listOf("vietlott","xo so","lottery","hmdt")
    )

    fun classify(text: String): String {
        val lower = text.lowercase()
            .replace("đ","d").replace("ă","a").replace("ắ","a")
            .replace("ổ","o").replace("ổ","o").replace("ề","e")
        for ((cat, keys) in CATEGORIES) {
            if (keys.any { lower.contains(it) }) return cat
        }
        return "Khac"
    }
}
