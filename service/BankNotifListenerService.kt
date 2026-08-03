package com.banktracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.banktracker.data.AppDatabase
import com.banktracker.parser.BankSmsParser

class BankNotifListenerService : NotificationListenerService() {
    private val BANK_APPS = setOf(
        "com.VCB","com.mbmobile","com.techcombank.mb.app",
        "vn.com.tpb.smartbanking","com.vnpay.vpbankonline",
        "com.vietinbank.ipay","com.agribank.mobile","com.acb.mobile"
    )
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in BANK_APPS) return
        val extras = sbn.notification.extras
        val title  = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text   = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val full   = "$title $text"
        if (BankSmsParser.looksLikeTransaction(full)) {
            BankSmsParser.parse(sbn.packageName, full)?.let { tx ->
                AppDatabase.getInstance(applicationContext).transactionDao().insert(tx)
            }
        }
    }
}
