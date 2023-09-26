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
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.service.SalesTrackerService
import la.shiro.salestracker.config.TAG

@SuppressLint("UnspecifiedRegisterReceiverFlag")
class SmsUtil(private val context: Context) {

    companion object {
        private const val SMS_SEND_ACTION = "SMS_SENT"
        private const val SMS_DELIVERY_ACTION = "SMS_DELIVERED"
    }

    private var isSMSSend: Boolean = false

    private val sentIntent = PendingIntent.getBroadcast(
        context, 0, Intent(SMS_SEND_ACTION), PendingIntent.FLAG_IMMUTABLE
    )
    private val deliveryIntent = PendingIntent.getBroadcast(
        context, 0, Intent(SMS_DELIVERY_ACTION), PendingIntent.FLAG_IMMUTABLE
    )

    private val sentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "SmsSendReceiver --> onReceive")
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(TAG, "SmsSendReceiver --> RESULT_OK")
                    isSMSSend = true
                    NvRamUtil.writeNvRamState(true)
                    Log.d(TAG, "SmsSendReceiver --> stop service")
                    SalesTrackerService.getInstance().stopSelf()
                }

                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                    Log.d(TAG, "SmsSendReceiver --> RESULT_ERROR_GENERIC_FAILURE")
                    if (ActivityCompat.checkSelfPermission(
                            SalesTrackerApplication.getAppContext(),
                            Manifest.permission.READ_PHONE_STATE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(TAG, "SmsSendReceiver --> No permission")
                        isSMSSend = false
                        return
                    }
                    val subInfoList =
                        SalesTrackerApplication.getSubscriptionManager().activeSubscriptionInfoList
                    Log.d(TAG, "SmsSendReceiver --> subInfoList: $subInfoList")
                    if (subInfoList != null && subInfoList.isNotEmpty()) {
                        setDefaultSmsSubId(
                            SalesTrackerApplication.getAppContext(),
                            SalesTrackerApplication.getSubscriptionManager()
                        )
                        sendTrackingSMS()
                    }
                    isSMSSend = false
                }

                SmsManager.RESULT_ERROR_RADIO_OFF -> {
                    Log.d(TAG, "SmsSendReceiver --> RESULT_ERROR_RADIO_OFF")
                    isSMSSend = false
                }

                SmsManager.RESULT_ERROR_NULL_PDU -> {
                    Log.d(TAG, "SmsSendReceiver --> RESULT_ERROR_NULL_PDU")
                    isSMSSend = false
                }

                else -> {
                    Log.d(TAG, "SmsSendReceiver --> RESULT_ERROR_NO_SERVICE")
                    isSMSSend = false
                }
            }
        }
    }

    private val deliveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "SmsDeliverReceiver --> onReceive")
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(TAG, "SmsDeliverReceiver --> RESULT_OK")
                    if (isSMSSend) {
                        NvRamUtil.writeNvRamState(true)
                        Log.d(TAG, "SmsDeliverReceiver --> RESULT_OK and isSMSSend is true")
                        Log.d(TAG, "SmsDeliverReceiver --> stop service")
                        SalesTrackerService.getInstance().stopSelf()
                    } else {
                        Log.d(TAG, "SmsDeliverReceiver --> RESULT_OK but isSMSSend is false")
                    }
                }

                else -> {
                    Log.d(TAG, "SmsDeliverReceiver --> RESULT_NOT_OK")
                }
            }
        }
    }

    init {
        context.registerReceiver(sentReceiver, IntentFilter(SMS_SEND_ACTION))
        context.registerReceiver(deliveryReceiver, IntentFilter(SMS_DELIVERY_ACTION))
    }

    fun sendTrackingSMS() {
        val messageSubId = SubscriptionManager.getDefaultSmsSubscriptionId()

        if (messageSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            setDefaultSmsSubId(context, SalesTrackerApplication.getSubscriptionManager())
        }
        val serverNumber = ConfigUtil.getSalesTrackerServerNumber()
        val smsContent = CellInfoUtil.getTrackingSMSContent()
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "sendTrackingSMS --> No permission")
            return
        }
        val smsManager = SalesTrackerApplication.getSmsManager()
        // smsManager.sendTextMessage(serverNumber, null, smsContent, sentIntent, deliveryIntent)
        smsManager.sendTextMessageWithoutPersisting(serverNumber, null, smsContent, sentIntent, deliveryIntent)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(sentReceiver)
        context.unregisterReceiver(deliveryReceiver)
    }

    private fun setDefaultSmsSubId(context: Context, subscriptionManager: SubscriptionManager) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "sendTrackingSMS --> No permission")
            return
        }
        val subInfoList = subscriptionManager.activeSubscriptionInfoList
        Log.d(TAG, "sendTrackingSMS --> subInfoList: $subInfoList")
        if (subInfoList != null && subInfoList.isNotEmpty()) {
            val subInfo = subInfoList[0]
            if (subInfo != null) {
                val subId = subInfo.subscriptionId
                subscriptionManager.setDefaultSmsSubId(subId)
                Log.d(TAG, "sendTrackingSMS --> Set default SMS subId: $subId")
            }
        }
    }
}