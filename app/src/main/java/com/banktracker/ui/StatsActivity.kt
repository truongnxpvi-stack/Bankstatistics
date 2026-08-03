package com.banktracker.ui

import android.os.Bundle
import android.view.Gravity
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

        findViewById<TextView>(R.id.tvStatExpense).text = fmt.format(expense) + " đ"
        findViewById<TextView>(R.id.tvStatIncome).text  = fmt.format(income)  + " đ"
        findViewById<TextView>(R.id.tvStatBalance).text = fmt.format(income - expense) + " đ"
        findViewById<TextView>(R.id.tvStatCount).text   = "$count giao dịch"

        // Biểu đồ bar
        val chartContainer = findViewById<LinearLayout>(R.id.chartContainer)
        chartContainer.removeAllViews()
        val maxVal = daily.maxOfOrNull { it.total } ?: 1L
        if (daily.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Không có dữ liệu"
            tv.setTextColor(0xFF888888.toInt()); tv.gravity = Gravity.CENTER
            tv.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            chartContainer.addView(tv)
        } else {
            daily.forEach { d ->
                val barWrap = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    setPadding(2, 8, 2, 0); gravity = Gravity.BOTTOM
                }
                val pct = (d.total.toFloat() / maxVal * 160).toInt().coerceAtLeast(4)
                val shape = android.graphics.drawable.GradientDrawable()
                shape.setColor(0xFF2563EB.toInt())
                shape.cornerRadii = floatArrayOf(4f,4f,4f,4f,0f,0f,0f,0f)
                val bar = android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pct)
                    background = shape
                }
                val lbl = TextView(this).apply {
                    text = d.date.takeLast(2)
                    textSize = 9f; setTextColor(0xFF888888.toInt()); gravity = Gravity.CENTER
                }
                barWrap.addView(bar); barWrap.addView(lbl)
                chartContainer.addView(barWrap)
            }
        }

        // Danh mục
        val catContainer = findViewById<LinearLayout>(R.id.catContainer)
        catContainer.removeAllViews()
        val totalCat = cats.sumOf { it.total }.coerceAtLeast(1)
        cats.take(8).forEach { cat ->
            val pct = (cat.total * 100 / totalCat).toInt()
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(0, 8, 0, 4)
            }
            row.addView(TextView(this).apply {
                text = cat.category; textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
            })
            row.addView(TextView(this).apply {
                text = "$pct%"; textSize = 12f; setTextColor(0xFF64748B.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(8,0,8,0)
            })
            row.addView(TextView(this).apply {
                text = fmt.format(cat.total) + "đ"; textSize = 12f; setTextColor(0xFFDC2626.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                gravity = Gravity.END
            })
            catContainer.addView(row)
            catContainer.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8)
                max = 100; progress = pct
                progressDrawable.setColorFilter(0xFF2563EB.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
            })
        }
    }
}
