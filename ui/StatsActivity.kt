package com.banktracker.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.R
import com.banktracker.data.AppDatabase
import java.text.NumberFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private val fmt = NumberFormat.getNumberInstance(Locale("vi","VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        title = "Thống kê chi tiêu"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabGroup = findViewById<RadioGroup>(R.id.tabGroup)
        tabGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.tabDay   -> loadStats(period = "day")
                R.id.tabWeek  -> loadStats(period = "week")
                R.id.tabMonth -> loadStats(period = "month")
            }
        }
        loadStats("month")
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadStats(period: String) {
        val cal = Calendar.getInstance()
        val to  = System.currentTimeMillis()
        val from = when (period) {
            "day"  -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            "week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            else -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
        }

        val dao     = AppDatabase.getInstance(applicationContext).transactionDao()
        val expense = dao.getTotalExpense(from, to) ?: 0
        val income  = dao.getTotalIncome(from, to)  ?: 0
        val count   = dao.getCount(from, to)
        val cats    = dao.getCategoryExpense(from, to)
        val daily   = dao.getDailyExpense(from, to)

        // Summary cards
        findViewById<TextView>(R.id.tvStatExpense).text = fmt.format(expense) + " đ"
        findViewById<TextView>(R.id.tvStatIncome).text  = fmt.format(income)  + " đ"
        findViewById<TextView>(R.id.tvStatBalance).text = fmt.format(income - expense) + " đ"
        findViewById<TextView>(R.id.tvStatCount).text   = "$count giao dịch"

        // Bar chart theo ngày
        val chartContainer = findViewById<LinearLayout>(R.id.chartContainer)
        chartContainer.removeAllViews()
        val maxVal = daily.maxOfOrNull { it.total } ?: 1L
        daily.forEach { d ->
            val barWrap = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(2, 0, 2, 0)
            }
            val pct = (d.total.toFloat() / maxVal * 180).toInt().coerceAtLeast(4)
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pct)
                setBackgroundColor(0xFF2563EB.toInt())
                background.also { bg ->
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.setColor(0xFF2563EB.toInt())
                    shape.cornerRadii = floatArrayOf(4f,4f,4f,4f,0f,0f,0f,0f)
                    background = shape
                }
            }
            val lbl = TextView(this).apply {
                text = d.date.takeLast(2)
                textSize = 9f
                setTextColor(0xFF888888.toInt())
                gravity = android.view.Gravity.CENTER
            }
            barWrap.addView(bar)
            barWrap.addView(lbl)
            chartContainer.addView(barWrap)
        }

        // Category list
        val catContainer = findViewById<LinearLayout>(R.id.catContainer)
        catContainer.removeAllViews()
        val totalCat = cats.sumOf { it.total }.coerceAtLeast(1)
        cats.take(8).forEach { cat ->
            val pct = (cat.total * 100 / totalCat).toInt()
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, 6, 0, 6)
            }
            val tvName = TextView(this).apply {
                text = cat.category
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
            }
            val tvPct = TextView(this).apply {
                text = "$pct%"
                textSize = 12f
                setTextColor(0xFF888888.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(8, 0, 8, 0)
            }
            val tvAmt = TextView(this).apply {
                text = fmt.format(cat.total) + "đ"
                textSize = 12f
                setTextColor(0xFFDC2626.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                gravity = android.view.Gravity.END
            }
            row.addView(tvName); row.addView(tvPct); row.addView(tvAmt)
            catContainer.addView(row)

            // Progress bar
            val prog = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8)
                max = 100; progress = pct
                progressDrawable.setColorFilter(0xFF2563EB.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
            }
            catContainer.addView(prog)
        }
    }
}
