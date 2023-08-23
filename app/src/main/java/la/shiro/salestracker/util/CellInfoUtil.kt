package la.shiro.salestracker.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import la.shiro.salestracker.SalesTrackerApplication
import la.shiro.salestracker.config.TAG
import java.util.Locale

object CellInfoUtil {

    fun getSimState(): Boolean {
        val telephonyManager = SalesTrackerApplication.getTelephonyManager()
        val isTracked = NvRamUtil.readNvRamState() == 1
        val simState0 = telephonyManager.getSimState(0) == TelephonyManager.SIM_STATE_READY
        val simState1 = telephonyManager.getSimState(1) == TelephonyManager.SIM_STATE_READY
        Log.d(
            TAG,
            "getSimState --> isTracked: $isTracked, simState0: $simState0, simState1: $simState1"
        )
        return !isTracked && (simState0 || simState1)
    }

    fun getTrackingSMSContent(): String {
        val telephonyManager = SalesTrackerApplication.getTelephonyManager()

        if (ActivityCompat.checkSelfPermission(
                SalesTrackerApplication.getAppContext(), Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                SalesTrackerApplication.getAppContext(), Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                SalesTrackerApplication.getAppContext(), Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "getTrackingSMSContent --> No permission")
            return "Unknown"
        }
        var phoneImei = "Unknown"
        try {
            phoneImei = telephonyManager.imei?.toString().toString()
            Log.d(TAG, "getTrackingSMSContent --> IMEI: $phoneImei")
        } catch (e: Exception) {
            Log.d(TAG, "getTrackingSMSContent --> No Permission to get IMEI")
        }

        val cellInfo = telephonyManager.allCellInfo

        var phoneLac = "Unknown"
        var phoneTac = "Unknown"
        var phoneCellId = "Unknown"

        if (cellInfo != null && cellInfo.isNotEmpty()) {
            for (info in cellInfo) {
                when (info) {
                    is CellInfoGsm -> {
                        val cellIdentity = info.cellIdentity
                        val lac = cellIdentity.lac
                        val cellId = cellIdentity.cid
                        phoneCellId = cellId.toString()
                        phoneLac = lac.toString()
                        Log.d(TAG, "CellInfoGsm --> LAC: $lac, Cell ID: $cellId")
                    }

                    is CellInfoWcdma -> {
                        val cellIdentity = info.cellIdentity
                        val lac = cellIdentity.lac
                        val cellId = cellIdentity.cid
                        phoneCellId = cellId.toString()
                        phoneLac = lac.toString()
                        Log.d(TAG, "CellInfoWcdma --> LAC: $lac, Cell ID: $cellId")
                    }

                    is CellInfoCdma -> {
                        val cellIdentity = info.cellIdentity
                        val lac = cellIdentity.networkId
                        val cellId = cellIdentity.basestationId
                        phoneCellId = cellId.toString()
                        phoneLac = lac.toString()
                        Log.d(TAG, "CellInfoCdma --> LAC: $lac, Cell ID: $cellId")
                    }

                    is CellInfoLte -> {
                        val cellIdentity = info.cellIdentity
                        val tac = cellIdentity.tac
                        val cellId = cellIdentity.ci
                        phoneCellId = cellId.toString()
                        phoneTac = tac.toString()
                        Log.d(TAG, "CellInfoLte --> TAC: $tac, Cell ID: $cellId")
                    }
                }
                if (phoneCellId != "2147483647") {
                    break
                } else {
                    phoneCellId = "Unknown"
                    phoneLac = "Unknown"
                    phoneTac = "Unknown"
                }
            }
        }
        val phoneTrackingSMSContent: String
        val sb: StringBuilder = StringBuilder()
        sb.append(Build.BRAND)
        sb.append(" ")
        if (Build.MODEL.equals("CORE_X")) {
            sb.append("COREX")
        } else {
            sb.append(Build.MODEL)
        }
        sb.append(" ")
        sb.append(phoneImei)
        sb.append(" ")
        when (phoneTac) {
            "Unknown" -> {
                sb.append(phoneLac)
            }

            else -> {
                sb.append(phoneTac)
            }
        }
        sb.append(" ")
        sb.append(phoneCellId)
        phoneTrackingSMSContent = sb.toString().uppercase(Locale.ROOT)
        Log.d(TAG, "Tracking SMS Content: $phoneTrackingSMSContent")
        return phoneTrackingSMSContent
    }
}