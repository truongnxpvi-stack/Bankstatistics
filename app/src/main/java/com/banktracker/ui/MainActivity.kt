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
        if (!BiometricHelper.canAuthenticate(this)) {
            onAuthSuccess()
            return
        }
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
        val dao = AppDatabase.getInstance(applicationContext).transactionDao()
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis

        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis

        val allTx = dao.getAllSync()

        val expMonth = allTx.filter { it.timestamp in monthStart..now }
            .let { txs ->
                val d = txs.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                if (d > 0) d else txs.sumOf { it.amount }
            }
        val expWeek = allTx.filter { it.timestamp in weekStart..now }
            .let { txs ->
                val d = txs.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                if (d > 0) d else txs.sumOf { it.amount }
            }
        val expDay = allTx.filter { it.timestamp in dayStart..now }
            .let { txs ->
                val d = txs.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                if (d > 0) d else txs.sumOf { it.amount }
            }
        val incMonth = allTx.filter {
            it.timestamp in monthStart..now && it.type == TransactionType.CREDIT
        }.sumOf { it.amount }

        findViewById<TextView>(R.id.tvExpenseMonth).text = fmt.format(expMonth) + " đ"
        findViewById<TextView>(R.id.tvIncomeMonth).text  = fmt.format(incMonth)  + " đ"
        findViewById<TextView>(R.id.tvExpenseWeek).text  = fmt.format(expWeek)   + " đ"
        findViewById<TextView>(R.id.tvExpenseDay).text   = fmt.format(expDay)    + " đ"
        findViewById<TextView>(R.id.tvTxCount).text      = "${allTx.size} giao dịch"

        val recentList = findViewById<LinearLayout>(R.id.recentList)
        recentList.removeAllViews()
        val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        if (allTx.isEmpty()) {
            recentList.addView(TextView(this).apply {
                text = "Chưa có giao dịch. Hãy thực hiện giao dịch ngân hàng."
                setTextColor(0xFF888888.toInt())
                textSize = 13f
                setPadding(16, 16, 16, 16)
            })
            return
        }

        allTx.take(5).forEach { tx ->
            val row = layoutInflater.inflate(R.layout.item_transaction, recentList, false)
            val color = when (tx.type) {
                TransactionType.DEBIT  -> 0xFFDC2626.toInt()
                TransactionType.CREDIT -> 0xFF16A34A.toInt()
                else                   -> 0xFF2563EB.toInt()
            }
            val sign = when (tx.type) {
                TransactionType.DEBIT  -> "▼"
                TransactionType.CREDIT -> "▲"
                else                   -> "●"
            }
            row.findViewById<TextView>(R.id.tvItemBank).text = tx.bank
            row.findViewById<TextView>(R.id.tvItemCat).text  = tx.category
            row.findViewById<TextView>(R.id.tvItemDesc).text = tx.description.take(50)
            row.findViewById<TextView>(R.id.tvItemDate).text = dateFmt.format(Date(tx.timestamp))
            val tvAmt = row.findViewById<TextView>(R.id.tvItemAmount)
            tvAmt.text = "$sign ${fmt.format(tx.amount)} đ"
            tvAmt.setTextColor(color)
            recentList.addView(row)
        }
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat?.contains(packageName) != true) {
            Toast.makeText(
                this,
                "Vui lòng bật Notification Access cho BankTracker",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
            }
        }
    }
}
