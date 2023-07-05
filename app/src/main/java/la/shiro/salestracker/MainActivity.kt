package la.shiro.salestracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import la.shiro.salestracker.ui.theme.SalesTrackerTheme
import la.shiro.salestracker.util.CellInfoUtil
import la.shiro.salestracker.util.ConfigUtil
import la.shiro.salestracker.util.NvRamUtil
import la.shiro.salestracker.config.IS_DEVELOP

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalesTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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
    val activity = (LocalContext.current as? Activity)
    val snackBarHostState = remember { SnackbarHostState() }
    Scaffold(snackbarHost = {
        SnackbarHost(snackBarHostState) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = it.visuals.message)
            }
        }
    }, topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Sales Tracker", maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    val intent = Intent()
                    activity?.setResult(Activity.RESULT_OK, intent)
                    activity?.finish()
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, contentDescription = "Back"
                    )
                }
            },
        )
    }, content = { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Settings(snackBarHostState)
        }
    })
}

@Composable
fun Settings(snackBarHostState: SnackbarHostState) {
    val isTracked = remember {
        mutableStateOf(
            when (NvRamUtil.readNvRamState()) {
                0 -> false
                1 -> true
                else -> false
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .verticalScroll(rememberScrollState())
    ) {
        DeviceTrackState(isTracked)
        ReTrackDevice(isTracked, snackBarHostState)
    }
}

@Composable
fun DeviceTrackState(isTracked: MutableState<Boolean>) {
    val smsInformation = remember { mutableStateOf(CellInfoUtil.getTrackingSMSContent()) }
    Card(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        colors = if (isTracked.value) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isTracked.value) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isTracked.value) "Tracked" else "Not Tracked",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = if (isTracked.value) "Tracked" else "Not Tracked",
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
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "SMS Information",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = smsInformation.value, modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class, ExperimentalComposeUiApi::class
)
@Composable
fun ReTrackDevice(isTracked: MutableState<Boolean>, snackBarHostState: SnackbarHostState) {
    val focusManager = LocalFocusManager.current
    var trackerServerNumber by remember { mutableStateOf(ConfigUtil.getSalesTrackerServerNumber()) }
    var sendTrackSMSTimeDelay by remember { mutableStateOf(ConfigUtil.getSalesTrackerSendSMSDelay()) }
    val modifier = Modifier
        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        .fillMaxWidth(1f)
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = trackerServerNumber,
            onValueChange = { newValue -> trackerServerNumber = newValue },
            label = { Text("Tracker Server Number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
            ),
            modifier = modifier,
            maxLines = 1,
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = sendTrackSMSTimeDelay,
            onValueChange = { newValue -> sendTrackSMSTimeDelay = newValue },
            label = { Text("Auto Send Track SMS Time Delay (Minutes)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
            ),
            modifier = modifier,
            maxLines = 1,
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            shape = RoundedCornerShape(12.dp)
        )
        Card(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                GlobalScope.launch(Dispatchers.Main) {
                    ConfigUtil.setSalesTrackerServerNumber(trackerServerNumber)
                    ConfigUtil.setSalesTrackerSendSMSDelay(sendTrackSMSTimeDelay)
                    snackBarHostState.showSnackbar(
                        message = "Sales Tracker Config Saved",
                        duration = SnackbarDuration.Short,
                        withDismissAction = true,
                    )
                    SalesTrackerApplication.getPowerManager().reboot(null)
                }
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Save Config And Reboot", fontSize = 18.sp
                )
            }
        }
        Card(
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                GlobalScope.launch(Dispatchers.Main) {
                    when (NvRamUtil.readNvRamState()) {
                        1 -> {
                            isTracked.value = false
                            NvRamUtil.writeNvRamState(false)
                            snackBarHostState.showSnackbar(
                                message = "Device Track State Reset To False",
                                duration = SnackbarDuration.Short,
                                withDismissAction = true,
                            )
                        }

                        else -> {
                            snackBarHostState.showSnackbar(
                                message = "Device Not Tracked",
                                duration = SnackbarDuration.Short,
                                withDismissAction = true,
                            )
                        }
                    }
                }
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Device Track State", fontSize = 18.sp
                )
            }
        }
        if (IS_DEVELOP) {
            Card(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    GlobalScope.launch(Dispatchers.Main) {
                        when (NvRamUtil.readNvRamState()) {
                            0 -> {
                                NvRamUtil.writeNvRamState(true)
                                isTracked.value = true
                            }

                            else -> {
                                NvRamUtil.writeNvRamState(false)
                                isTracked.value = false
                            }
                        }
                        snackBarHostState.showSnackbar(
                            message = "Device Track State Changed",
                            duration = SnackbarDuration.Short,
                            withDismissAction = true,
                        )
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Change Device Track State", fontSize = 18.sp
                    )
                }
            }
            Card(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    GlobalScope.launch(Dispatchers.Main) {
                        NvRamUtil.dumpSalesTrackerNvRam()
                        snackBarHostState.showSnackbar(
                            message = "NvRam Dumped, Check Logcat",
                            duration = SnackbarDuration.Short,
                            withDismissAction = true,
                        )
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dump NvRam Data", fontSize = 18.sp
                    )
                }
            }
        }
    }
}