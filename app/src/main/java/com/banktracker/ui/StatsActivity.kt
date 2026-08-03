package com.banktracker.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.R
import com.banktracker.data.AppDatabase
import com.banktracker.data.TransactionType
import java.text.NumberFormat
import java.util.*

class StatsActivity : AppCompatActivity() {
    private val fmt = NumberFormat.getNumberInstance(Locale("vi","VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        title = "Thống kê chi tiêu"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<RadioGroup>(R.id.tabGroup).setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.tabDay   -> loadStats("day")
                R.id.tabWeek  -> loadStats("week")
                R.id.tabMonth -> loadStats("month")
            }
        }
        loadStats("month")
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadStats(period: String) {
        val cal = Calendar.getInstance()
        val to  = System.currentTimeMillis()
        val from = when (period) {
            "day" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }

        val dao    = AppDatabase.getInstance(applicationContext).transactionDao()
        val allTx  = dao.getAllSync().filter { it.timestamp in from..to }
        val count  = allTx.size

        // Tính chi/thu theo type — nếu UNKNOWN thì phân loại theo dấu
        val expense = allTx.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            .let { if (it > 0) it else allTx.filter { tx -> tx.type == TransactionType.UNKNOWN }.sumOf { tx -> tx.amount } }
        val income  = allTx.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val balance = income - expense

        // Cập nhật summary cards
        findViewById<TextView>(R.id.tvStatExpense).text = fmt.format(expense) + " đ"
        findViewById<TextView>(R.id.tvStatIncome).text  = fmt.format(income)  + " đ"
        val tvBalance = findViewById<TextView>(R.id.tvStatBalance)
        tvBalance.text = fmt.format(balance) + " đ"
        tvBalance.setTextColor(if (balance >= 0) 0xFF16A34A.toInt() else 0xFFDC2626.toInt())
        findViewById<TextView>(R.id.tvStatCount).text = "$count giao dịch"

        // ── Biểu đồ bar theo ngày ──
        val chartContainer = findViewById<LinearLayout>(R.id.chartContainer)
        chartContainer.removeAllViews()

        // Nhóm theo ngày
        val dailyMap = allTx.groupBy { it.date }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toSortedMap()

        if (dailyMap.isEmpty()) {
            chartContainer.addView(TextView(this).apply {
                text = "Không có dữ liệu trong kỳ này"
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            })
        } else {
            val maxVal = dailyMap.values.maxOrNull() ?: 1L
            dailyMap.forEach { (date, total) ->
                val barHeight = (total.toFloat() / maxVal * 160).toInt().coerceAtLeast(6)
                val barWrap = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                    )
                    setPadding(3, 8, 3, 0)
                }

                // Tooltip số tiền
                val tvAmt = TextView(this).apply {
                    text = if (total >= 1_000_000)
                        "${fmt.format(total / 1_000_000)}M"
                    else
                        "${fmt.format(total / 1_000)}K"
                    textSize = 8f
                    setTextColor(0xFF2563EB.toInt())
                    gravity = Gravity.CENTER
                }

                // Bar
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF2563EB.toInt())
                    cornerRadii = floatArrayOf(4f,4f,4f,4f,0f,0f,0f,0f)
                }
                val bar = android.view.View(this).apply {
                    background = shape
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, barHeight
                    )
                }

                // Label ngày
                val lbl = TextView(this).apply {
                    text = date.takeLast(2)
                    textSize = 9f
                    setTextColor(0xFF64748B.toInt())
                    gravity = Gravity.CENTER
                }

                barWrap.addView(tvAmt)
                barWrap.addView(bar)
                barWrap.addView(lbl)
                chartContainer.addView(barWrap)
            }
        }

        // ── Danh mục chi tiêu ──
        val catContainer = findViewById<LinearLayout>(R.id.catContainer)
        catContainer.removeAllViews()

        val catMap = allTx.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        if (catMap.isEmpty()) {
            catContainer.addView(TextView(this).apply {
                text = "Không có dữ liệu"
                setTextColor(0xFF888888.toInt())
                textSize = 13f
                setPadding(0, 16, 0, 16)
            })
        } else {
            val totalAll = catMap.sumOf { it.second }.coerceAtLeast(1)
            catMap.take(8).forEach { (category, total) ->
                val pct = (total * 100 / totalAll).toInt()

                // Row tên + % + số tiền
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 10, 0, 4)
                }
                row.addView(TextView(this).apply {
                    text = category
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
                })
                row.addView(TextView(this).apply {
                    text = "$pct%"
                    textSize = 12f
                    setTextColor(0xFF64748B.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(8, 0, 8, 0)
                })
                row.addView(TextView(this).apply {
                    text = fmt.format(total) + "đ"
                    textSize = 12f
                    setTextColor(0xFFDC2626.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                    gravity = Gravity.END
                })
                catContainer.addView(row)

                // Progress bar
                catContainer.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 10
                    ).also { it.bottomMargin = 4 }
                    max = 100
                    progress = pct
                    progressDrawable.setColorFilter(
                        0xFF2563EB.toInt(),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                })
            }
        }
    }
}
