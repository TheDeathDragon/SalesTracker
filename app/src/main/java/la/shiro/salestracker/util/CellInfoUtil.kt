package la.shiro.salestracker.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.MCC_BANGLADESH
import la.shiro.salestracker.config.MNC_GRAMEENPHONE
import la.shiro.salestracker.config.MODEL_NAME
import la.shiro.salestracker.config.SERVER_NUMBER_GRAMEENPHONE
import la.shiro.salestracker.config.SERVER_NUMBER_OTHERS
import la.shiro.salestracker.config.SMS_KEYWORD
import la.shiro.salestracker.config.TAG

object CellInfoUtil {

    fun hasAnySim(): Boolean {
        val telephonyManager: TelephonyManager = SalesTrackerApplication.getTelephonyManager()
        val sim0Ready: Boolean = telephonyManager.getSimState(0) == TelephonyManager.SIM_STATE_READY
        val sim1Ready: Boolean = telephonyManager.getSimState(1) == TelephonyManager.SIM_STATE_READY
        Log.d(TAG, "hasAnySim --> sim0Ready: $sim0Ready, sim1Ready: $sim1Ready")
        return sim0Ready || sim1Ready
    }

    @SuppressLint("MissingPermission")
    fun getImei(slotIndex: Int): String {
        val telephonyManager: TelephonyManager = SalesTrackerApplication.getTelephonyManager()
        if (!hasReadPhoneStatePermission()) {
            return ""
        }
        return try {
            telephonyManager.getImei(slotIndex) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "getImei($slotIndex) --> Exception: $e")
            ""
        }
    }

    @SuppressLint("MissingPermission")
    fun getActiveSubscriptions(): List<SubscriptionInfo> {
        if (!hasReadPhoneStatePermission()) {
            return emptyList()
        }
        val subscriptionManager: SubscriptionManager =
            SalesTrackerApplication.getSubscriptionManager()
        return try {
            subscriptionManager.activeSubscriptionInfoList ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveSubscriptions --> Exception: $e")
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun resolveServerNumberForSlot(slotIndex: Int): String {
        if (!ConfigUtil.isAutoServerNumber()) {
            return ConfigUtil.getServerNumberOverride()
        }
        val info: SubscriptionInfo? = getActiveSubscriptions().firstOrNull {
            it.simSlotIndex == slotIndex
        }
        if (info == null) {
            return SERVER_NUMBER_OTHERS
        }
        val mcc: String = info.mccString ?: ""
        val mnc: String = info.mncString ?: ""
        Log.d(TAG, "resolveServerNumberForSlot($slotIndex) --> MCC=$mcc, MNC=$mnc")
        if (mcc != MCC_BANGLADESH) {
            return SERVER_NUMBER_OTHERS
        }
        return if (mnc == MNC_GRAMEENPHONE) SERVER_NUMBER_GRAMEENPHONE else SERVER_NUMBER_OTHERS
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        val locationManager: LocationManager = SalesTrackerApplication.getAppContext()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers: List<String> = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            try {
                val location: Location? = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    if (best == null || location.time > best.time) {
                        best = location
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getLastKnownLocation($provider) --> Exception: $e")
            }
        }
        return best
    }

    fun buildTrackingSmsContent(): String {
        val imei1: String = getImei(0)
        val imei2: String = getImei(1)
        val location: Location? = getLastKnownLocation()
        val latitude: String = location?.latitude?.let { "%.6f".format(it) } ?: "0.000000"
        val longitude: String = location?.longitude?.let { "%.6f".format(it) } ?: "0.000000"
        return "$SMS_KEYWORD $MODEL_NAME $imei1;$imei2:0:LOC$latitude:$longitude"
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            SalesTrackerApplication.getAppContext(),
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        val context: Context = SalesTrackerApplication.getAppContext()
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
