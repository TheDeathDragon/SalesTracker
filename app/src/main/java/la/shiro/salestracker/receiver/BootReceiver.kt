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
import la.shiro.salestracker.util.ContactUtil.addContact


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
            Log.d(TAG, "SalesTracker BootReceiver --> Add emergency contacts")
            addContact("Police", "15")
            addContact("Fire Brigade", "16")
            addContact("Ambulance", "1122")
            addContact("Counter Terrorism", "1717")
            addContact("Motorway Police", "130")
            addContact("Edhi", "115")
            addContact("Emergency", "911")
        }
    }
}