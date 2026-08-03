package com.banktracker.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.R
import com.banktracker.data.AppDatabase
import com.banktracker.data.Transaction
import com.banktracker.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private val fmt     = NumberFormat.getNumberInstance(Locale("vi","VN"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dayFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val lblFmt  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var allTx   = listOf<Transaction>()
    private var selectedDate: String? = null  // null = hiện tất cả

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        title = "Lịch sử giao dịch"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        allTx = AppDatabase.getInstance(applicationContext).transactionDao().getAllSync()

        buildDateFilter()
        renderList(allTx)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // ── Bộ lọc ngày ──────────────────────────────────────────────
    private fun buildDateFilter() {
        val filterScroll = findViewById<HorizontalScrollView>(R.id.filterScroll)
        val filterRow    = findViewById<LinearLayout>(R.id.filterRow)
        filterRow.removeAllViews()

        // Lấy danh sách ngày có giao dịch (mới → cũ)
        val dates = allTx.map { dayFmt.format(Date(it.timestamp)) }
            .distinct().sorted().reversed()

        if (dates.isEmpty()) { filterScroll.visibility = View.GONE; return }
        filterScroll.visibility = View.VISIBLE

        // Nút "Tất cả"
        filterRow.addView(makeChip("Tất cả", selected = true) {
            selectedDate = null
            refreshChips(filterRow, null)
            renderList(allTx)
        }.also { it.tag = "all" })

        // Một chip cho mỗi ngày
        dates.forEach { date ->
            val cal = Calendar.getInstance()
            cal.time = dayFmt.parse(date)!!
            val lbl = when (date) {
                dayFmt.format(Date()) -> "Hôm nay"
                else -> lblFmt.format(cal.time)
            }
            filterRow.addView(makeChip(lbl, selected = false) {
                selectedDate = date
                refreshChips(filterRow, date)
                val filtered = allTx.filter { dayFmt.format(Date(it.timestamp)) == date }
                renderList(filtered)
            }.also { it.tag = date })
        }
    }

    private fun makeChip(label: String, selected: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text  = label
            textSize = 12f
            isAllCaps = false
            setPadding(24, 0, 24, 0)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                72
            )
            lp.marginEnd = 8
            layoutParams = lp
            setChipStyle(this, selected)
            setOnClickListener { onClick() }
        }
    }

    private fun setChipStyle(btn: Button, selected: Boolean) {
        if (selected) {
            btn.setBackgroundColor(0xFF2563EB.toInt())
            btn.setTextColor(0xFFFFFFFF.toInt())
        } else {
            btn.setBackgroundColor(0xFFE2E8F0.toInt())
            btn.setTextColor(0xFF334155.toInt())
        }
    }

    private fun refreshChips(row: LinearLayout, activeDate: String?) {
        for (i in 0 until row.childCount) {
            val chip = row.getChildAt(i) as? Button ?: continue
            val isActive = when {
                activeDate == null -> chip.tag == "all"
                else -> chip.tag == activeDate
            }
            setChipStyle(chip, isActive)
        }
    }

    // ── Render danh sách ──────────────────────────────────────────
    private fun renderList(list: List<Transaction>) {
        val container = findViewById<LinearLayout>(R.id.historyContainer)
        val tvEmpty   = findViewById<TextView>(R.id.tvEmpty)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)
        container.removeAllViews()

        if (list.isEmpty()) {
            tvEmpty.visibility   = View.VISIBLE
            tvSummary.visibility = View.GONE
            return
        }

        tvEmpty.visibility = View.GONE

        // Tóm tắt kỳ đang xem
        val totalDebit  = list.filter { it.type == TransactionType.DEBIT  }.sumOf { it.amount }
        val totalCredit = list.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalAll    = list.sumOf { it.amount }
        val summaryExp  = if (totalDebit  > 0) totalDebit  else totalAll
        val summaryInc  = if (totalCredit > 0) totalCredit else 0L

        tvSummary.visibility = View.VISIBLE
        tvSummary.text = buildString {
            append("${list.size} giao dịch")
            if (summaryExp > 0) append("  |  Chi: ${fmt.format(summaryExp)}đ")
            if (summaryInc > 0) append("  |  Thu: ${fmt.format(summaryInc)}đ")
        }

        // Nhóm giao dịch theo ngày
        val grouped = list.groupBy { dayFmt.format(Date(it.timestamp)) }
            .toSortedMap(compareByDescending { it })

        grouped.forEach { (date, txList) ->
            // Header ngày
            val cal = Calendar.getInstance()
            cal.time = dayFmt.parse(date)!!
            val dayLabel = when (date) {
                dayFmt.format(Date()) -> "Hôm nay — ${lblFmt.format(cal.time)}"
                else -> lblFmt.format(cal.time)
            }
            val dayTotal = txList.sumOf { it.amount }

            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 20, 16, 8)
                setBackgroundColor(0xFFF0F4FF.toInt())
            }
            header.addView(TextView(this).apply {
                text = dayLabel
                textSize = 13f
                setTextColor(0xFF2563EB.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            header.addView(TextView(this).apply {
                text = fmt.format(dayTotal) + "đ"
                textSize = 12f
                setTextColor(0xFF64748B.toInt())
                gravity = Gravity.END
            })
            container.addView(header)

            // Các giao dịch trong ngày
            txList.forEach { tx ->
                val row   = layoutInflater.inflate(R.layout.item_transaction, container, false)
                val isDebit = tx.type == TransactionType.DEBIT
                val color = when (tx.type) {
                    TransactionType.DEBIT   -> 0xFFDC2626.toInt()
                    TransactionType.CREDIT  -> 0xFF16A34A.toInt()
                    TransactionType.UNKNOWN -> 0xFF2563EB.toInt()
                }
                val sign = when (tx.type) {
                    TransactionType.DEBIT   -> "▼"
                    TransactionType.CREDIT  -> "▲"
                    TransactionType.UNKNOWN -> "●"
                }
                row.findViewById<TextView>(R.id.tvItemBank).text  = tx.bank
                row.findViewById<TextView>(R.id.tvItemCat).text   = tx.category
                row.findViewById<TextView>(R.id.tvItemDesc).text  = tx.description.take(60)
                row.findViewById<TextView>(R.id.tvItemDate).text  = dateFmt.format(Date(tx.timestamp))
                val tvAmt = row.findViewById<TextView>(R.id.tvItemAmount)
                tvAmt.text = "$sign ${fmt.format(tx.amount)} đ"
                tvAmt.setTextColor(color)

                // Nếu có số dư → hiện thêm
                if (tx.balance != null && tx.balance > 0) {
                    row.findViewById<TextView>(R.id.tvItemDesc).text =
                        "${tx.description.take(40)}\nSố dư: ${fmt.format(tx.balance)}đ"
                }
                container.addView(row)

                // Đường kẻ phân cách
                container.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.marginStart = 16; it.marginEnd = 16 }
                    setBackgroundColor(0xFFE2E8F0.toInt())
                })
            }
        }
    }
}
