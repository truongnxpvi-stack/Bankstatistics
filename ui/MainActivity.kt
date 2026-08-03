package com.banktracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.banktracker.R
import com.banktracker.data.AppDatabase
import com.banktracker.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val fmt = NumberFormat.getNumberInstance(Locale("vi","VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.mainContent).visibility = View.INVISIBLE

        if (!BiometricHelper.canAuthenticate(this)) { onAuthSuccess(); return }

        BiometricHelper.showPrompt(this,
            onSuccess = { runOnUiThread { onAuthSuccess() } },
            onFailed  = {},
            onError   = { runOnUiThread { finish() } }
        )
    }

    private fun onAuthSuccess() {
        findViewById<View>(R.id.mainContent).visibility = View.VISIBLE
        checkPermissions()
        loadDashboard()

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.btnStats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }

    private fun loadDashboard() {
        val db   = AppDatabase.getInstance(applicationContext)
        val dao  = db.transactionDao()
        val cal  = Calendar.getInstance()

        // Tháng này
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val monthStart = cal.timeInMillis
        val monthEnd   = System.currentTimeMillis()

        // Tuần này
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val weekStart = cal.timeInMillis

        // Hôm nay
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val dayStart = cal.timeInMillis

        val expMonth = dao.getTotalExpense(monthStart, monthEnd) ?: 0
        val incMonth = dao.getTotalIncome(monthStart, monthEnd)  ?: 0
        val expWeek  = dao.getTotalExpense(weekStart, monthEnd)  ?: 0
        val expDay   = dao.getTotalExpense(dayStart, monthEnd)   ?: 0
        val txCount  = dao.getCount(monthStart, monthEnd)
        val allTx    = dao.getAllSync()

        findViewById<TextView>(R.id.tvExpenseMonth).text = fmt.format(expMonth) + " đ"
        findViewById<TextView>(R.id.tvIncomeMonth).text  = fmt.format(incMonth) + " đ"
        findViewById<TextView>(R.id.tvExpenseWeek).text  = fmt.format(expWeek)  + " đ"
        findViewById<TextView>(R.id.tvExpenseDay).text   = fmt.format(expDay)   + " đ"
        findViewById<TextView>(R.id.tvTxCount).text      = "$txCount giao dịch"

        // Giao dịch gần nhất
        val recentList = findViewById<LinearLayout>(R.id.recentList)
        recentList.removeAllViews()
        val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        allTx.take(5).forEach { tx ->
            val row = layoutInflater.inflate(R.layout.item_transaction, recentList, false)
            val sign  = if (tx.type == TransactionType.DEBIT) "▼" else "▲"
            val color = if (tx.type == TransactionType.DEBIT) 0xFFDC2626.toInt() else 0xFF16A34A.toInt()
            row.findViewById<TextView>(R.id.tvItemBank).text   = tx.bank
            row.findViewById<TextView>(R.id.tvItemCat).text    = tx.category
            row.findViewById<TextView>(R.id.tvItemDesc).text   = tx.description.take(50)
            row.findViewById<TextView>(R.id.tvItemDate).text   = dateFmt.format(Date(tx.timestamp))
            val tvAmt = row.findViewById<TextView>(R.id.tvItemAmount)
            tvAmt.text = "$sign ${fmt.format(tx.amount)} đ"
            tvAmt.setTextColor(color)
            recentList.addView(row)
        }

        if (allTx.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Chưa có giao dịch. Hãy thực hiện giao dịch ngân hàng."
            tv.setTextColor(0xFF888888.toInt())
            tv.textSize = 13f
            tv.setPadding(0, 8, 0, 0)
            recentList.addView(tv)
        }
    }

    private fun checkPermissions() {
        val perms = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        val missing = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)

        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat?.contains(packageName) != true) {
            Toast.makeText(this, "Vui lòng bật Notification Access cho BankTracker", Toast.LENGTH_LONG).show()
            try { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (e: Exception) {}
        }
    }
}
