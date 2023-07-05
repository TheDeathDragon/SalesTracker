package la.shiro.salestracker.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.service.SalesTrackerService
import la.shiro.salestracker.util.ConfigUtil
import la.shiro.salestracker.util.NvRamUtil
import la.shiro.salestracker.config.TAG


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "SalesTracker BootReceiver --> onReceive")
            val isTracked = NvRamUtil.readNvRamState()
            val salesTrackerApplicationContext = SalesTrackerApplication.getAppContext()
            val delayTime = ConfigUtil.getSalesTrackerSendSMSDelay().toLong() * 1000
            Log.d(TAG, "SalesTracker BootReceiver isTracked --> $isTracked")
            if (isTracked != 1) {
                val salesTrackerServiceIntent = Intent(
                    salesTrackerApplicationContext, SalesTrackerService::class.java
                )
                val pending = PendingIntent.getService(
                    salesTrackerApplicationContext,
                    0,
                    salesTrackerServiceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = SalesTrackerApplication.getAlarmManager()
                Log.d(TAG, "SalesTracker BootReceiver --> Set alarm to send tracking SMS")
                alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayTime] =
                    pending
            } else {
                Log.d(TAG, "SalesTracker BootReceiver --> No need to send tracking SMS")
            }
        }
    }
}