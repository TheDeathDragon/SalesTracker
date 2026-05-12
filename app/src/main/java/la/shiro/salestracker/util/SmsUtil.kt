package la.shiro.salestracker.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.TAG

@SuppressLint("UnspecifiedRegisterReceiverFlag")
class SmsUtil(private val context: Context) {

    companion object {
        private const val SMS_SEND_ACTION: String = "la.shiro.salestracker.SMS_SENT"
        private const val SMS_DELIVERY_ACTION: String = "la.shiro.salestracker.SMS_DELIVERED"
        private const val EXTRA_SLOT_INDEX: String = "slot_index"

        @Volatile
        private var listener: SmsResultListener? = null

        fun setListener(l: SmsResultListener?) {
            listener = l
        }
    }

    interface SmsResultListener {
        fun onSmsSent(slotIndex: Int)
        fun onSmsFailed(slotIndex: Int, resultCode: Int)
        fun onSmsDelivered(slotIndex: Int)
    }

    private val sentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val slotIndex: Int = intent?.getIntExtra(EXTRA_SLOT_INDEX, -1) ?: -1
            Log.d(TAG, "SmsSendReceiver --> onReceive slot=$slotIndex code=$resultCode")
            when (resultCode) {
                Activity.RESULT_OK -> {
                    listener?.onSmsSent(slotIndex)
                }
                else -> {
                    listener?.onSmsFailed(slotIndex, resultCode)
                }
            }
        }
    }

    private val deliveryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val slotIndex: Int = intent?.getIntExtra(EXTRA_SLOT_INDEX, -1) ?: -1
            Log.d(TAG, "SmsDeliverReceiver --> onReceive slot=$slotIndex code=$resultCode")
            if (resultCode == Activity.RESULT_OK) {
                listener?.onSmsDelivered(slotIndex)
            }
        }
    }

    init {
        context.registerReceiver(sentReceiver, IntentFilter(SMS_SEND_ACTION))
        context.registerReceiver(deliveryReceiver, IntentFilter(SMS_DELIVERY_ACTION))
    }

    fun sendOnSlot(slotIndex: Int): Boolean {
        if (!hasSendPermission()) {
            Log.d(TAG, "sendOnSlot --> No SEND_SMS permission")
            return false
        }
        val telephonyManager: TelephonyManager = SalesTrackerApplication.getTelephonyManager()
        if (telephonyManager.getSimState(slotIndex) != TelephonyManager.SIM_STATE_READY) {
            Log.d(TAG, "sendOnSlot --> SIM slot $slotIndex not ready")
            return false
        }
        val subInfo: SubscriptionInfo? = CellInfoUtil.getActiveSubscriptions().firstOrNull {
            it.simSlotIndex == slotIndex
        }
        if (subInfo == null) {
            Log.d(TAG, "sendOnSlot --> No SubscriptionInfo for slot $slotIndex")
            return false
        }
        val serverNumber: String = CellInfoUtil.resolveServerNumberForSlot(slotIndex)
        val smsContent: String = CellInfoUtil.buildTrackingSmsContent()
        Log.d(TAG, "sendOnSlot --> slot=$slotIndex to=$serverNumber content=$smsContent")
        val sentIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            slotIndex,
            Intent(SMS_SEND_ACTION).putExtra(EXTRA_SLOT_INDEX, slotIndex),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deliveryIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            slotIndex + 100,
            Intent(SMS_DELIVERY_ACTION).putExtra(EXTRA_SLOT_INDEX, slotIndex),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            val baseSmsManager: SmsManager =
                context.getSystemService(SmsManager::class.java) as SmsManager
            val smsManager: SmsManager =
                baseSmsManager.createForSubscriptionId(subInfo.subscriptionId)
            smsManager.sendTextMessageWithoutPersisting(
                serverNumber, null, smsContent, sentIntent, deliveryIntent
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendOnSlot --> Exception: $e")
            false
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(sentReceiver)
            context.unregisterReceiver(deliveryReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "unregisterReceiver --> Exception: $e")
        }
    }

    private fun hasSendPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
