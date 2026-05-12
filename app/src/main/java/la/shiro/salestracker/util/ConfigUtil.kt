package la.shiro.salestracker.util

import android.provider.Settings
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.BOOT_TRIGGER_DELAY_MINUTES_DEFAULT
import la.shiro.salestracker.config.DEFAULT_SERVER_NUMBER
import la.shiro.salestracker.config.DOMESTIC_TEST_BOOT_DELAY_MINUTES
import la.shiro.salestracker.config.DOMESTIC_TEST_RETRY_INTERVAL_MINUTES
import la.shiro.salestracker.config.RETRY_INTERVAL_MINUTES_DEFAULT
import la.shiro.salestracker.config.SALES_TRACKER_AUTO_SERVER_NUMBER
import la.shiro.salestracker.config.SALES_TRACKER_BOOT_TRIGGER_DELAY
import la.shiro.salestracker.config.SALES_TRACKER_DOMESTIC_TEST_MODE
import la.shiro.salestracker.config.SALES_TRACKER_INCOMING_CALL_COUNT
import la.shiro.salestracker.config.SALES_TRACKER_RETRY_INTERVAL
import la.shiro.salestracker.config.SALES_TRACKER_RETRY_TIMES
import la.shiro.salestracker.config.SALES_TRACKER_SERVER_NUMBER

object ConfigUtil {

    fun getServerNumberOverride(): String {
        return Settings.System.getString(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_SERVER_NUMBER
        ) ?: DEFAULT_SERVER_NUMBER
    }

    fun setServerNumberOverride(serverNumber: String) {
        Settings.System.putString(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_SERVER_NUMBER,
            serverNumber.trim()
        )
    }

    fun isAutoServerNumber(): Boolean {
        return Settings.System.getInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_AUTO_SERVER_NUMBER,
            1
        ) == 1
    }

    fun setAutoServerNumber(auto: Boolean) {
        Settings.System.putInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_AUTO_SERVER_NUMBER,
            if (auto) 1 else 0
        )
    }

    fun getBootTriggerDelayMinutes(): Long {
        return Settings.System.getLong(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_BOOT_TRIGGER_DELAY,
            BOOT_TRIGGER_DELAY_MINUTES_DEFAULT
        )
    }

    fun setBootTriggerDelayMinutes(minutes: Long) {
        Settings.System.putLong(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_BOOT_TRIGGER_DELAY,
            minutes
        )
    }

    fun getRetryIntervalMinutes(): Long {
        return Settings.System.getLong(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_RETRY_INTERVAL,
            RETRY_INTERVAL_MINUTES_DEFAULT
        )
    }

    fun setRetryIntervalMinutes(minutes: Long) {
        Settings.System.putLong(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_RETRY_INTERVAL,
            minutes
        )
    }

    fun getIncomingCallCount(): Int {
        return Settings.System.getInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_INCOMING_CALL_COUNT,
            0
        )
    }

    fun setIncomingCallCount(count: Int) {
        Settings.System.putInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_INCOMING_CALL_COUNT,
            count
        )
    }

    fun getRetryTimes(): Int {
        return Settings.System.getInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_RETRY_TIMES,
            0
        )
    }

    fun setRetryTimes(times: Int) {
        Settings.System.putInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_RETRY_TIMES,
            times
        )
    }

    fun isDomesticTestMode(): Boolean {
        return Settings.System.getInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_DOMESTIC_TEST_MODE,
            0
        ) == 1
    }

    fun setDomesticTestMode(enabled: Boolean) {
        Settings.System.putInt(
            SalesTrackerApplication.getAppContext().contentResolver,
            SALES_TRACKER_DOMESTIC_TEST_MODE,
            if (enabled) 1 else 0
        )
    }

    fun applyDomesticTestPreset() {
        setAutoServerNumber(false)
        setBootTriggerDelayMinutes(DOMESTIC_TEST_BOOT_DELAY_MINUTES)
        setRetryIntervalMinutes(DOMESTIC_TEST_RETRY_INTERVAL_MINUTES)
    }

    fun applyProductionPreset() {
        setAutoServerNumber(true)
        setBootTriggerDelayMinutes(BOOT_TRIGGER_DELAY_MINUTES_DEFAULT)
        setRetryIntervalMinutes(RETRY_INTERVAL_MINUTES_DEFAULT)
    }
}
