package la.shiro.salestracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import la.shiro.salestracker.config.INCOMING_CALL_TRIGGER_COUNT
import la.shiro.salestracker.config.TAG
import la.shiro.salestracker.config.TRACKED_STATE
import la.shiro.salestracker.service.SalesTrackerService
import la.shiro.salestracker.util.ConfigUtil
import la.shiro.salestracker.util.NvRamUtil

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }
        val state: String? = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) {
            return
        }
        if (NvRamUtil.readNvRamState() == TRACKED_STATE) {
            return
        }
        val count: Int = ConfigUtil.getIncomingCallCount() + 1
        ConfigUtil.setIncomingCallCount(count)
        Log.d(TAG, "IncomingCallReceiver --> count=$count / $INCOMING_CALL_TRIGGER_COUNT")
        if (count >= INCOMING_CALL_TRIGGER_COUNT) {
            val serviceIntent: Intent = Intent(context, SalesTrackerService::class.java).apply {
                action = SalesTrackerService.ACTION_TRIGGER
            }
            context?.startService(serviceIntent)
        }
    }
}
