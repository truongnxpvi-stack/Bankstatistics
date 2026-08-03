package com.banktracker.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.R
import com.banktracker.data.AppDatabase
import java.text.NumberFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        title = "Thống kê chi tiêu"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabGroup = findViewById<RadioGroup>(R.id.tabGroup)
        tabGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.tabDay   -> loadStats("day")
                R.id.tabWeek  -> loadStats("week")
                R.id.tabMonth -> loadStats("month")
            }
        }
        // Mặc định chọn tháng
        findViewById<RadioButton>(R.id.tabMonth).isChecked = true
        loadStats("month")
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadStats(period: String) {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        val from = when (period) {
            "day" -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "week" -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }

        val dao     = AppDatabase.getInstance(applicationContext).transactionDao()
        val expense = dao.getTotalExpense(from, now) ?: 0
        val income  = dao.getTotalIncome(from, now)  ?: 0
        val count   = dao.getCount(from, now)
        val cats    = dao.getCategoryExpense(from, now)
        val daily   = dao.getDailyExpense(from, now)

        // ── Summary cards ──────────────────────────────────────────────────
        findViewById<TextView>(R.id.tvStatExpense).text = fmt.format(expense) + " đ"
        findViewById<TextView>(R.id.tvStatIncome).text  = fmt.format(income)  + " đ"
        val balance = income - expense
        val balTV = findViewById<TextView>(R.id.tvStatBalance)
        balTV.text = fmt.format(balance) + " đ"
        balTV.setTextColor(if (balance >= 0) 0xFF16A34A.toInt() else 0xFFDC2626.toInt())
        findViewById<TextView>(R.id.tvStatCount).text = "$count giao dịch"

        // ── Biểu đồ cột theo ngày ─────────────────────────────────────────
        val chartContainer = findViewById<LinearLayout>(R.id.chartContainer)
        chartContainer.removeAllViews()

        if (daily.isEmpty()) {
            chartContainer.addView(TextView(this).apply {
                text = "Không có dữ liệu chi tiêu"
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 200
                )
            })
        } else {
            val maxVal = daily.maxOf { it.total }.coerceAtLeast(1)
            daily.forEach { d ->
                val colWrap = LinearLayout(this).apply {
                    orientation  = LinearLayout.VERTICAL
                    gravity      = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    setPadding(3, 0, 3, 0)
                }

                // Số tiền nhỏ phía trên cột
                colWrap.addView(TextView(this).apply {
                    val k = d.total / 1000
                    text = if (k >= 1000) "${k/1000}tr" else "${k}k"
                    textSize = 8f
                    setTextColor(0xFF64748B.toInt())
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })

                // Cột màu
                val barH = ((d.total.toFloat() / maxVal) * 140).toInt().coerceAtLeast(6)
                val shape = GradientDrawable().apply {
                    setColor(0xFF2563EB.toInt())
                    cornerRadii = floatArrayOf(4f, 4f, 4f, 4f, 0f, 0f, 0f, 0f)
                }
                colWrap.addView(View(this).apply {
                    background   = shape
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barH)
                })

                // Nhãn ngày
                colWrap.addView(TextView(this).apply {
                    text     = d.date.substring(8) // dd
                    textSize = 9f
                    setTextColor(0xFF64748B.toInt())
                    gravity  = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
                chartContainer.addView(colWrap)
            }
        }

        // ── Chi tiêu theo danh mục ─────────────────────────────────────────
        val catContainer = findViewById<LinearLayout>(R.id.catContainer)
        catContainer.removeAllViews()

        if (cats.isEmpty()) {
            catContainer.addView(TextView(this).apply {
                text = "Không có dữ liệu danh mục"
                setTextColor(0xFF888888.toInt())
                setPadding(0, 16, 0, 16)
            })
        } else {
            val totalCat = cats.sumOf { it.total }.coerceAtLeast(1)
            cats.take(8).forEach { cat ->
                val pct = ((cat.total * 100f) / totalCat).toInt()

                // Hàng tên + số tiền + %
                val row = LinearLayout(this).apply {
                    orientation  = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 10, 0, 4)
                }
                row.addView(TextView(this).apply {
                    text = cat.category
                    textSize = 13f
                    setTextColor(0xFF1E293B.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
                })
                row.addView(TextView(this).apply {
                    text = "$pct%"
                    textSize = 12f
                    setTextColor(0xFF64748B.toInt())
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = fmt.format(cat.total) + " đ"
                    textSize = 12f
                    setTextColor(0xFFDC2626.toInt())
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                })
                catContainer.addView(row)

                // Progress bar
                val progBg = GradientDrawable().apply {
                    setColor(0xFFE2E8F0.toInt())
                    cornerRadius = 4f
                }
                val progFill = GradientDrawable().apply {
                    setColor(0xFF2563EB.toInt())
                    cornerRadius = 4f
                }
                val progBar = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 10
                    ).apply { bottomMargin = 4 }
                    background = progBg
                }
                val fillView = View(this).apply {
                    background = progFill
                    layoutParams = FrameLayout.LayoutParams(
                        (resources.displayMetrics.widthPixels * pct / 100).coerceAtLeast(8), 10
                    )
                }
                progBar.addView(fillView)
                catContainer.addView(progBar)
            }
        }
    }
}
