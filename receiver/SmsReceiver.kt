package com.banktracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.banktracker.data.AppDatabase
import com.banktracker.parser.BankSmsParser

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val sender = sms.originatingAddress ?: return@forEach
            val body   = sms.messageBody       ?: return@forEach
            if (BankSmsParser.isBankSms(sender)) {
                BankSmsParser.parse(sender, body)?.let { tx ->
                    AppDatabase.getInstance(context).transactionDao().insert(tx)
                }
            }
        }
    }
}
