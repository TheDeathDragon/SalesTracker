package la.shiro.salestracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import la.shiro.salestracker.config.IS_DEVELOP
import la.shiro.salestracker.config.MAX_RETRY_TIMES
import la.shiro.salestracker.config.TRACKED_STATE
import la.shiro.salestracker.service.SalesTrackerService
import la.shiro.salestracker.ui.theme.SalesTrackerTheme
import la.shiro.salestracker.util.CellInfoUtil
import la.shiro.salestracker.util.ConfigUtil
import la.shiro.salestracker.util.NvRamUtil

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalesTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsAppBar()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppBar() {
    val activity: Activity? = LocalActivity.current
    val snackBarHostState: SnackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackBarHostState) {
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(text = it.visuals.message)
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Sales Tracker",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent: Intent = Intent()
                        activity?.setResult(Activity.RESULT_OK, intent)
                        activity?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Settings(snackBarHostState)
            }
        }
    )
}

@Composable
fun Settings(snackBarHostState: SnackbarHostState) {
    val tracked: MutableState<Boolean> = remember {
        mutableStateOf(NvRamUtil.readNvRamState() == TRACKED_STATE)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .verticalScroll(rememberScrollState())
    ) {
        StatusCards(tracked)
        ConfigCards(tracked, snackBarHostState)
    }
}

@Composable
fun StatusCards(tracked: MutableState<Boolean>) {
    val smsPreview: MutableState<String> = remember {
        mutableStateOf(CellInfoUtil.buildTrackingSmsContent())
    }
    val retryTimes: Int = ConfigUtil.getRetryTimes()
    Card(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        colors = if (tracked.value) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (tracked.value) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (tracked.value) "Tracked" else "Pending",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = if (tracked.value) {
                    "STS Sent OK"
                } else {
                    "STS Pending (retry $retryTimes/$MAX_RETRY_TIMES)"
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "SMS Preview",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(text = smsPreview.value, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    DelicateCoroutinesApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ConfigCards(tracked: MutableState<Boolean>, snackBarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var testMode: Boolean by remember { mutableStateOf(ConfigUtil.isDomesticTestMode()) }
    var autoServer: Boolean by remember { mutableStateOf(ConfigUtil.isAutoServerNumber()) }
    var serverOverride: String by remember { mutableStateOf(ConfigUtil.getServerNumberOverride()) }
    var bootDelay: String by remember {
        mutableStateOf(ConfigUtil.getBootTriggerDelayMinutes().toString())
    }
    var retryInterval: String by remember {
        mutableStateOf(ConfigUtil.getRetryIntervalMinutes().toString())
    }

    val rowModifier: Modifier = Modifier
        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        .fillMaxWidth(1f)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = rowModifier,
            colors = CardDefaults.cardColors(
                containerColor = if (testMode) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Domestic SIM Test Mode")
                    Text(
                        text = "Auto OFF, boot 1m, retry 5m",
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = testMode,
                    onCheckedChange = { enabled ->
                        testMode = enabled
                        if (enabled) {
                            ConfigUtil.applyDomesticTestPreset()
                        } else {
                            ConfigUtil.applyProductionPreset()
                        }
                        ConfigUtil.setDomesticTestMode(enabled)
                        autoServer = ConfigUtil.isAutoServerNumber()
                        bootDelay = ConfigUtil.getBootTriggerDelayMinutes().toString()
                        retryInterval = ConfigUtil.getRetryIntervalMinutes().toString()
                    }
                )
            }
        }
        Card(
            modifier = rowModifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Auto Server Number (by MCC/MNC)")
                Switch(
                    checked = autoServer,
                    onCheckedChange = { autoServer = it }
                )
            }
        }
        OutlinedTextField(
            value = serverOverride,
            onValueChange = { serverOverride = it },
            label = { Text("Server Number Override (when Auto is off)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done
            ),
            modifier = rowModifier,
            maxLines = 1,
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = bootDelay,
            onValueChange = { bootDelay = it },
            label = { Text("Boot Trigger Delay (Minutes, default 240)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
            ),
            modifier = rowModifier,
            maxLines = 1,
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = retryInterval,
            onValueChange = { retryInterval = it },
            label = { Text("Retry Interval (Minutes, default 360)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
            ),
            modifier = rowModifier,
            maxLines = 1,
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            shape = RoundedCornerShape(12.dp)
        )
        Card(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                GlobalScope.launch(Dispatchers.Main) {
                    ConfigUtil.setAutoServerNumber(autoServer)
                    ConfigUtil.setServerNumberOverride(serverOverride)
                    ConfigUtil.setBootTriggerDelayMinutes(
                        bootDelay.trim().toLongOrNull() ?: 240L
                    )
                    ConfigUtil.setRetryIntervalMinutes(
                        retryInterval.trim().toLongOrNull() ?: 360L
                    )
                    snackBarHostState.showSnackbar(
                        message = "Config Saved",
                        duration = SnackbarDuration.Short,
                        withDismissAction = true,
                    )
                    if (NvRamUtil.readNvRamState() == TRACKED_STATE) {
                        NvRamUtil.writeNvRamState(false)
                        tracked.value = false
                    }
                    SalesTrackerApplication.getPowerManager().reboot(null)
                }
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = rowModifier,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Save Config And Reboot", fontSize = 18.sp)
            }
        }

        Card(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                ConfigUtil.setAutoServerNumber(autoServer)
                ConfigUtil.setServerNumberOverride(serverOverride)
                val sendIntent: Intent =
                    Intent(context, SalesTrackerService::class.java).apply {
                        action = SalesTrackerService.ACTION_FORCE_SEND
                    }
                context.startService(sendIntent)
                GlobalScope.launch(Dispatchers.Main) {
                    snackBarHostState.showSnackbar(
                        message = "Triggered Test Send (watch Toast/Logcat)",
                        duration = SnackbarDuration.Short,
                        withDismissAction = true,
                    )
                }
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = rowModifier,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Send Test SMS Now", fontSize = 18.sp)
            }
        }

        if (IS_DEVELOP) {
            Card(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    GlobalScope.launch(Dispatchers.Main) {
                        if (NvRamUtil.readNvRamState() == TRACKED_STATE) {
                            NvRamUtil.writeNvRamState(false)
                            ConfigUtil.setRetryTimes(0)
                            ConfigUtil.setIncomingCallCount(0)
                            tracked.value = false
                            snackBarHostState.showSnackbar(
                                message = "Track State Reset",
                                duration = SnackbarDuration.Short,
                                withDismissAction = true,
                            )
                        } else {
                            snackBarHostState.showSnackbar(
                                message = "Already Untracked",
                                duration = SnackbarDuration.Short,
                                withDismissAction = true,
                            )
                        }
                    }
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = rowModifier,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Reset Track State", fontSize = 18.sp)
                }
            }
            Card(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    GlobalScope.launch(Dispatchers.Main) {
                        NvRamUtil.dumpSalesTrackerNvRam()
                        snackBarHostState.showSnackbar(
                            message = "NvRam Dumped to Logcat",
                            duration = SnackbarDuration.Short,
                            withDismissAction = true,
                        )
                    }
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = rowModifier,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Dump NvRam Data", fontSize = 18.sp)
                }
            }
        }
    }
}
