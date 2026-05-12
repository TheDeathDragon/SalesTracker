package la.shiro.salestracker.config

const val TAG: String = "Rin"
const val IS_DEVELOP: Boolean = false

const val SMS_KEYWORD: String = "GDLS"
const val MODEL_NAME: String = "NOTEAIR"

const val SERVER_NUMBER_GRAMEENPHONE: String = "16272"
const val SERVER_NUMBER_OTHERS: String = "16303"
const val DEFAULT_SERVER_NUMBER: String = SERVER_NUMBER_OTHERS

const val MCC_BANGLADESH: String = "470"
const val MNC_GRAMEENPHONE: String = "01"
const val MNC_ROBI: String = "02"
const val MNC_AIRTEL: String = "07"
const val MNC_BANGLALINK: String = "03"
const val MNC_TELETALK: String = "04"

const val SALES_TRACKER_NV_FILENAME: String = "/mnt/vendor/nvdata/APCFG/APRDEB/SALES_TRACKER"
const val WIFI_NV_FILENAME: String = "/mnt/vendor/nvdata/APCFG/APRDEB/WIFI"
const val SALES_TRACKER_NV_FILE_SIZE: Int = 1024
const val WIFI_NV_FILE_SIZE: Int = 2050
const val TRACKED_STATE: Int = 1
const val UNTRACKED_STATE: Int = 0
const val UNKNOWN_STATE: Int = -1

const val INCOMING_CALL_TRIGGER_COUNT: Int = 2
const val BOOT_TRIGGER_DELAY_MINUTES_DEFAULT: Long = 240L
const val RETRY_INTERVAL_MINUTES_DEFAULT: Long = 360L
const val MAX_RETRY_TIMES: Int = 22

const val SALES_TRACKER_SERVER_NUMBER: String = "sales_tracker_server_number"
const val SALES_TRACKER_BOOT_TRIGGER_DELAY: String = "sales_tracker_boot_trigger_delay"
const val SALES_TRACKER_RETRY_INTERVAL: String = "sales_tracker_retry_interval"
const val SALES_TRACKER_INCOMING_CALL_COUNT: String = "sales_tracker_incoming_call_count"
const val SALES_TRACKER_RETRY_TIMES: String = "sales_tracker_retry_times"
const val SALES_TRACKER_AUTO_SERVER_NUMBER: String = "sales_tracker_auto_server_number"
const val SALES_TRACKER_DOMESTIC_TEST_MODE: String = "sales_tracker_domestic_test_mode"

const val DOMESTIC_TEST_BOOT_DELAY_MINUTES: Long = 1L
const val DOMESTIC_TEST_RETRY_INTERVAL_MINUTES: Long = 5L

const val STS_STATUS_SECRET_CODE: String = "1234"
const val SECRET_CODE_ACTION: String = "android.provider.Telephony.SECRET_CODE"
