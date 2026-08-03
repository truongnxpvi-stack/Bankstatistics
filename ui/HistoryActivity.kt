package com.banktracker.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.R
import com.banktracker.data.AppDatabase
import com.banktracker.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private val fmt     = NumberFormat.getNumberInstance(Locale("vi","VN"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        title = "Lịch sử giao dịch"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loadHistory()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadHistory() {
        val db    = AppDatabase.getInstance(applicationContext)
        val allTx = db.transactionDao().getAllSync()
        val container = findViewById<LinearLayout>(R.id.historyContainer)
        val tvEmpty   = findViewById<TextView>(R.id.tvEmpty)
        container.removeAllViews()

        if (allTx.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        tvEmpty.visibility = View.GONE
        allTx.forEach { tx ->
            val row = layoutInflater.inflate(R.layout.item_transaction, container, false)
            val sign  = if (tx.type == TransactionType.DEBIT) "▼" else "▲"
            val color = if (tx.type == TransactionType.DEBIT) 0xFFDC2626.toInt() else 0xFF16A34A.toInt()
            row.findViewById<TextView>(R.id.tvItemBank).text  = tx.bank
            row.findViewById<TextView>(R.id.tvItemCat).text   = tx.category
            row.findViewById<TextView>(R.id.tvItemDesc).text  = tx.description.take(60)
            row.findViewById<TextView>(R.id.tvItemDate).text  = dateFmt.format(Date(tx.timestamp))
            val tvAmt = row.findViewById<TextView>(R.id.tvItemAmount)
            tvAmt.text = "$sign ${fmt.format(tx.amount)} đ"
            tvAmt.setTextColor(color)
            container.addView(row)
        }
    }
}
