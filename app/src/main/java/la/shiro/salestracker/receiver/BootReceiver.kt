package la.shiro.salestracker.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import la.shiro.salestracker.MainActivity
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.STS_STATUS_SECRET_CODE
import la.shiro.salestracker.config.TAG
import la.shiro.salestracker.config.TRACKED_STATE
import la.shiro.salestracker.service.SalesTrackerService
import la.shiro.salestracker.util.ConfigUtil
import la.shiro.salestracker.util.NvRamUtil

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action: String? = intent?.action ?: return
        Log.d(TAG, "BootReceiver --> onReceive action=$action")
        when (action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_INITIALIZE -> {
                if (NvRamUtil.readNvRamState() == TRACKED_STATE) {
                    Log.d(TAG, "BootReceiver --> already tracked, nothing to do")
                    return
                }
                scheduleBootDelayedTrigger()
            }
            "android.provider.Telephony.SECRET_CODE" -> {
                handleSecretCode(context, intent)
            }
        }
    }

    private fun scheduleBootDelayedTrigger() {
        val appContext: Context = SalesTrackerApplication.getAppContext()
        val delayMinutes: Long = ConfigUtil.getBootTriggerDelayMinutes()
        val serviceIntent: Intent = Intent(appContext, SalesTrackerService::class.java).apply {
            action = SalesTrackerService.ACTION_TRIGGER
        }
        val pending: PendingIntent = PendingIntent.getService(
            appContext,
            0,
            serviceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager: AlarmManager = SalesTrackerApplication.getAlarmManager()
        val triggerAt: Long = SystemClock.elapsedRealtime() + delayMinutes * 60L * 1000L
        alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt] = pending
        Log.d(TAG, "BootReceiver --> scheduled boot trigger in $delayMinutes minutes")
    }

    private fun handleSecretCode(context: Context?, intent: Intent) {
        val host: String? = intent.data?.host
        Log.d(TAG, "BootReceiver --> SECRET_CODE host=$host")
        if (host != STS_STATUS_SECRET_CODE) {
            return
        }
        val appContext: Context = context?.applicationContext
            ?: SalesTrackerApplication.getAppContext()
        val launchIntent: Intent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(launchIntent)
    }
}
