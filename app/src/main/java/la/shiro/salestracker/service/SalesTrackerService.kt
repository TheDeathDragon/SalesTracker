package la.shiro.salestracker.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.MAX_RETRY_TIMES
import la.shiro.salestracker.config.TAG
import la.shiro.salestracker.config.TRACKED_STATE
import la.shiro.salestracker.util.CellInfoUtil
import la.shiro.salestracker.util.ConfigUtil
import la.shiro.salestracker.util.NvRamUtil
import la.shiro.salestracker.util.SmsUtil

class SalesTrackerService : Service(), SmsUtil.SmsResultListener {

    companion object {
        const val ACTION_TRIGGER: String = "la.shiro.salestracker.action.TRIGGER"
        const val ACTION_RETRY: String = "la.shiro.salestracker.action.RETRY"
        const val ACTION_FORCE_SEND: String = "la.shiro.salestracker.action.FORCE_SEND"

        fun scheduleRetry(intervalMinutes: Long) {
            val context: Context = SalesTrackerApplication.getAppContext()
            val intent: Intent = Intent(context, SalesTrackerService::class.java).apply {
                action = ACTION_RETRY
            }
            val pending: PendingIntent = PendingIntent.getService(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager: AlarmManager = SalesTrackerApplication.getAlarmManager()
            val triggerAt: Long = SystemClock.elapsedRealtime() + intervalMinutes * 60L * 1000L
            alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt] = pending
            Log.d(TAG, "scheduleRetry --> in $intervalMinutes minutes")
        }

        fun cancelRetry() {
            val context: Context = SalesTrackerApplication.getAppContext()
            val intent: Intent = Intent(context, SalesTrackerService::class.java).apply {
                action = ACTION_RETRY
            }
            val pending: PendingIntent = PendingIntent.getService(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            SalesTrackerApplication.getAlarmManager().cancel(pending)
        }
    }

    private lateinit var smsUtil: SmsUtil
    private var sim1Failed: Boolean = false
    private var forceMode: Boolean = false
    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        smsUtil = SmsUtil(SalesTrackerApplication.getAppContext())
        SmsUtil.setListener(this)
        Log.d(TAG, "SalesTrackerService --> onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action: String? = intent?.action
        forceMode = action == ACTION_FORCE_SEND
        Log.d(TAG, "SalesTrackerService --> onStartCommand action=$action forceMode=$forceMode")
        if (!forceMode && NvRamUtil.readNvRamState() == TRACKED_STATE) {
            Log.d(TAG, "SalesTrackerService --> already tracked, stopping")
            cancelRetry()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!CellInfoUtil.hasAnySim()) {
            Log.d(TAG, "SalesTrackerService --> no SIM, will rely on next trigger")
            if (forceMode) {
                toast("No SIM ready")
            }
            stopSelf()
            return START_NOT_STICKY
        }
        sim1Failed = false
        trySendOnSlot(0)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        SmsUtil.setListener(null)
        smsUtil.unregisterReceiver()
        Log.d(TAG, "SalesTrackerService --> onDestroy")
    }

    private fun trySendOnSlot(slotIndex: Int) {
        Log.d(TAG, "trySendOnSlot --> slot=$slotIndex")
        val started: Boolean = smsUtil.sendOnSlot(slotIndex)
        if (!started) {
            onSmsFailed(slotIndex, -1)
        }
    }

    override fun onSmsSent(slotIndex: Int) {
        Log.d(TAG, "onSmsSent --> slot=$slotIndex forceMode=$forceMode")
        if (forceMode) {
            toast("Test SMS sent on SIM${slotIndex + 1}")
            stopSelf()
            return
        }
        NvRamUtil.writeNvRamState(true)
        cancelRetry()
        ConfigUtil.setRetryTimes(0)
        stopSelf()
    }

    override fun onSmsFailed(slotIndex: Int, resultCode: Int) {
        Log.d(TAG, "onSmsFailed --> slot=$slotIndex code=$resultCode forceMode=$forceMode")
        if (slotIndex == 0 && !sim1Failed) {
            sim1Failed = true
            trySendOnSlot(1)
            return
        }
        if (forceMode) {
            toast("Test SMS FAILED on both SIMs (last code=$resultCode)")
            stopSelf()
            return
        }
        val retryTimes: Int = ConfigUtil.getRetryTimes() + 1
        ConfigUtil.setRetryTimes(retryTimes)
        Log.d(TAG, "onSmsFailed --> retryTimes=$retryTimes / $MAX_RETRY_TIMES")
        if (retryTimes >= MAX_RETRY_TIMES) {
            Log.d(TAG, "onSmsFailed --> reached MAX_RETRY_TIMES, stopping")
            cancelRetry()
            stopSelf()
            return
        }
        scheduleRetry(ConfigUtil.getRetryIntervalMinutes())
        stopSelf()
    }

    override fun onSmsDelivered(slotIndex: Int) {
        Log.d(TAG, "onSmsDelivered --> slot=$slotIndex")
        if (forceMode) {
            toast("Test SMS delivered on SIM${slotIndex + 1}")
        }
    }

    private fun toast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
