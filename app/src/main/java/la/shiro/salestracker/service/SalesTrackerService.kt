package la.shiro.salestracker.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.util.CellInfoUtil
import la.shiro.salestracker.util.NvRamUtil
import la.shiro.salestracker.util.SmsUtil
import la.shiro.salestracker.config.DEFAULT_SEND_SMS_RETRY_DELAY
import la.shiro.salestracker.config.TAG
import java.util.Timer
import java.util.TimerTask

class SalesTrackerService : Service() {

    companion object {
        private lateinit var instance: SalesTrackerService
        fun getInstance(): SalesTrackerService = instance
    }

    private lateinit var smsUtils: SmsUtil
    private lateinit var timer: Timer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        smsUtils = SmsUtil(SalesTrackerApplication.getAppContext())
        Log.d(TAG, "SalesTrackerService --> onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SalesTrackerService --> onStartCommand")
        startTimer()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SalesTrackerService --> onDestroy")
        stopTimer()
        smsUtils.unregisterReceiver()
    }

    private fun startTimer() {
        Log.d(TAG, "SalesTrackerService --> startTimer")
        timer = Timer()
        val handler = Handler(Looper.getMainLooper())
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    checkTrackingStateTask()
                }
            }
        }, 0, DEFAULT_SEND_SMS_RETRY_DELAY * 1000 * 60)
    }

    private fun stopTimer() {
        Log.d(TAG, "SalesTrackerService --> stopTimer")
        timer.cancel()
    }

    private fun checkTrackingStateTask() {
        Log.d(TAG, "SalesTrackerService --> checkTrackingStateTask")
        val isTracking = NvRamUtil.readNvRamState() == 1
        if (!isTracking) {
            if (CellInfoUtil.getSimState()) {
                smsUtils.sendTrackingSMS()
                Log.d(TAG, "SalesTrackerService onStartCommand --> sendTrackingSMS")
            } else {
                Log.d(TAG, "SalesTrackerService onStartCommand --> No SIM card")
            }
        } else {
            stopSelf()
            Log.d(TAG, "SalesTrackerService onStartCommand --> stopSelf")
        }
    }

}