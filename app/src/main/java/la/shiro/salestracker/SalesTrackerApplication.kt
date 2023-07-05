package la.shiro.salestracker

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import la.shiro.salestracker.config.TAG


class SalesTrackerApplication : Application() {

    companion object {
        private lateinit var instance: SalesTrackerApplication
        private lateinit var telephonyManager: TelephonyManager
        private lateinit var powerManager: PowerManager
        private lateinit var alarmManager: AlarmManager
        private lateinit var smsManager: SmsManager
        private lateinit var subscriptionManager: SubscriptionManager
        fun getAppContext(): Context = instance.applicationContext
        fun getTelephonyManager(): TelephonyManager = telephonyManager
        fun getPowerManager(): PowerManager = powerManager
        fun getAlarmManager(): AlarmManager = alarmManager
        fun getSmsManager(): SmsManager = smsManager
        fun getSubscriptionManager(): SubscriptionManager = subscriptionManager
    }

    override fun onCreate() {
        instance = this
        telephonyManager = getSystemService(TelephonyManager::class.java) as TelephonyManager
        powerManager = getSystemService(PowerManager::class.java) as PowerManager
        alarmManager = getSystemService(AlarmManager::class.java) as AlarmManager
        smsManager = getSystemService(SmsManager::class.java) as SmsManager
        subscriptionManager = getSystemService(SubscriptionManager::class.java) as SubscriptionManager
        super.onCreate()
        Log.d(TAG, "SalesTrackerApplication --> onCreate")
    }

}