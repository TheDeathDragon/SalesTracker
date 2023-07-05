package la.shiro.salestracker.util

import android.provider.Settings
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.DEFAULT_SEND_SMS_DELAY
import la.shiro.salestracker.config.DEFAULT_SERVER_NUMBER
import la.shiro.salestracker.config.SALES_TRACKER_SEND_SMS_DELAY
import la.shiro.salestracker.config.SALES_TRACKER_SERVER_NUMBER


object ConfigUtil {
    fun getSalesTrackerServerNumber(): String {
        return Settings.System.getLong(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_SERVER_NUMBER,
            DEFAULT_SERVER_NUMBER
        ).toString()
    }

    fun setSalesTrackerServerNumber(serverNumber: String) {
        Settings.System.putLong(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_SERVER_NUMBER,
            serverNumber.trim().toLong()
        )
    }

    fun getSalesTrackerSendSMSDelay(): String {
        return Settings.System.getInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_SEND_SMS_DELAY,
            DEFAULT_SEND_SMS_DELAY
        ).toString()
    }

    fun setSalesTrackerSendSMSDelay(delay: String) {
        Settings.System.putInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_SEND_SMS_DELAY,
            delay.trim().toInt()
        )
    }
}