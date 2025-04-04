@file:OptIn(ExperimentalMaterial3Api::class)

package com.tutpro.baresip

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_CALL
import android.content.Intent.ACTION_DIAL
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Chronometer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tutpro.baresip.BaresipService.Companion.contactNames
import com.tutpro.baresip.BaresipService.Companion.uas
import com.tutpro.baresip.BaresipService.Companion.uasStatus
import com.tutpro.baresip.CustomElements.Checkbox
import com.tutpro.baresip.CustomElements.DropdownMenu
import com.tutpro.baresip.CustomElements.PullToRefreshBox
import com.tutpro.baresip.CustomElements.Text
import com.tutpro.baresip.CustomElements.verticalScrollbar
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private lateinit var imm: InputMethodManager
    private lateinit var nm: NotificationManager
    private lateinit var am: AudioManager
    private lateinit var kgm: KeyguardManager
    private lateinit var screenEventReceiver: BroadcastReceiver
    private lateinit var serviceEventObserver: Observer<Event<Long>>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var accountsRequest: ActivityResultLauncher<Intent>
    private lateinit var chatRequests: ActivityResultLauncher<Intent>
    private lateinit var configRequest: ActivityResultLauncher<Intent>
    private lateinit var backupRequest: ActivityResultLauncher<Intent>
    private lateinit var restoreRequest: ActivityResultLauncher<Intent>
    private lateinit var logcatRequest: ActivityResultLauncher<Intent>
    private lateinit var contactsRequest: ActivityResultLauncher<Intent>
    private lateinit var callsRequest: ActivityResultLauncher<Intent>
    private lateinit var accountRequest: ActivityResultLauncher<Intent>
    private lateinit var comDevChangedListener: AudioManager.OnCommunicationDeviceChangedListener
    private lateinit var permissions: Array<String>

    private lateinit var baresipService: Intent

    private var callHandler: Handler = Handler(Looper.getMainLooper())
    private var callRunnable: Runnable? = null
    private var downloadsInputUri: Uri? = null
    private var downloadsOutputUri: Uri? = null
    private var audioModeChangedListener: AudioManager.OnModeChangedListener? = null
    private var keyboardController: SoftwareKeyboardController? = null

    private var restart = false
    private var atStartup = false
    private var initialized = false

    private var resumeUri = ""
    private var resumeUap = 0L
    private var resumeCall: Call? = null
    private var resumeAction = ""

    private val viewModel: ViewModel by viewModels()

    private var callUri = mutableStateOf("")
    private var callUriEnabled = mutableStateOf(true)
    private var callUriLabel = mutableStateOf("")
    private var securityIcon = mutableIntStateOf(-1)
    private var callTimer: Chronometer? = null
    private var showCallTimer = mutableStateOf(false)
    private var showCallButton = mutableStateOf(true)
    private var showAnswerRejectButtons = mutableStateOf(false)
    private var showHangupButton = mutableStateOf(false)
    private var showOnHoldNotice = mutableStateOf(false)
    private var showPasswordDialog = mutableStateOf(false)
    private var showPasswordsDialog = mutableStateOf(false)
    private var holdIcon = mutableIntStateOf(R.drawable.call_hold)
    private var transferButtonEnabled = mutableStateOf(false)
    private var transferIcon = mutableIntStateOf(R.drawable.call_transfer)
    private var dtmfText = mutableStateOf("")
    private var dtmfEnabled = mutableStateOf(false)
    private var focusDtmf = mutableStateOf(false)
    private var showVmIcon by mutableStateOf(false)
    private var vmIcon = mutableIntStateOf(R.drawable.voicemail)
    private var messagesIcon = mutableIntStateOf(R.drawable.messages)
    private var callsIcon = mutableIntStateOf(R.drawable.calls)
    private var micIcon by mutableIntStateOf(R.drawable.mic_on)
    private var speakerIcon by mutableIntStateOf(R.drawable.speaker_off)
    private var dialpad by mutableStateOf(false)
    private var dialpadButtonEnabled by mutableStateOf(true)
    private var pullToRefreshEnabled by mutableStateOf(true)
    private var passwordAccounts = mutableListOf<String>()
    private var passwordTitle by mutableStateOf("")

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            moveTaskToBack(true)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val extraAction = intent.getStringExtra("action")
        Log.d(TAG, "Main onCreate ${intent.action}/${intent.data}/$extraAction")

        window.addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES)

        BaresipService.darkTheme.value = Utils.isThemeDark(this)

        // Must be done after view has been created
        this.setShowWhenLocked(true)
        this.setTurnScreenOn( true)
        Utils.requestDismissKeyguard(this)

        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        am = getSystemService(AUDIO_SERVICE) as AudioManager
        kgm = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        serviceEventObserver = Observer {
            val event = it.getContentIfNotHandled()
            Log.d(TAG, "Observed event $event")
            if (event != null && BaresipService.serviceEvents.isNotEmpty()) {
                val first = BaresipService.serviceEvents.removeAt(0)
                handleServiceEvent(first.event, first.params)
            }
        }

        BaresipService.serviceEvent.observeForever(serviceEventObserver)

        screenEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context, intent: Intent) {
                if (kgm.isKeyguardLocked) {
                    Log.d(TAG, "Screen on when locked")
                    this@MainActivity.setShowWhenLocked(Call.inCall())
                }
            }
        }

        this.registerReceiver(screenEventReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
        })

        if (Build.VERSION.SDK_INT >= 31) {
            comDevChangedListener = AudioManager.OnCommunicationDeviceChangedListener { device ->
                if (device != null) {
                    Log.d(TAG, "Com device changed to type ${device.type} in mode ${am.mode}")
                    speakerIcon = if (Utils.isSpeakerPhoneOn(am))
                        R.drawable.speaker_on
                    else
                        R.drawable.speaker_off
                }
            }
            am.addOnCommunicationDeviceChangedListener(mainExecutor, comDevChangedListener)
        }

        initialized = true

        accountsRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Account.ofAor(activityAor) != null)
                spinToAor(activityAor)
            else {
                if (Account.ofAor(viewModel.selectedAor.value) == null) {
                    if (uas.value.isNotEmpty())
                        viewModel.updateSelectedAor(uas.value.first().account.aor)
                    else
                        viewModel.updateSelectedAor("")
                }
                updateIcons(Account.ofAor(viewModel.selectedAor.value))
            }
            if (BaresipService.isServiceRunning) {
                baresipService.action = "Update Notification"
                startService(baresipService)
            }
        }

        accountRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            spinToAor(activityAor)
            val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
            updateIcons(ua.account)
            if (it.resultCode == RESULT_OK)
                if (BaresipService.aorPasswords[activityAor] == NO_AUTH_PASS) {
                    passwordAccounts = String(
                        Utils.getFileContents(filesDir.absolutePath + "/accounts")!!,
                        Charsets.UTF_8
                    ).lines().toMutableList()
                    showPasswordsDialog.value = true
                }
        }

        contactsRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

        chatRequests = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            spinToAor(activityAor)
            updateIcons(Account.ofAor(activityAor)!!)
        }

        callsRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            spinToAor(activityAor)
            updateIcons(Account.ofAor(viewModel.selectedAor.value))
        }

        configRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if ((it.data != null) && it.data!!.hasExtra("restart")) {
                with(MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)) {
                    setTitle(R.string.restart_request)
                    setMessage(getString(R.string.config_restart))
                    setPositiveButton(getText(R.string.restart)) { dialog, _ ->
                        dialog.dismiss()
                        quitRestart(true)
                    }
                    setNeutralButton(getText(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            }
            val displayTheme = Preferences(applicationContext).displayTheme
            if (displayTheme != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(displayTheme)
            }
        }

        backupRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK)
                it.data?.data?.also { uri ->
                    downloadsOutputUri = uri
                    passwordTitle = getString(R.string.encrypt_password)
                    showPasswordDialog.value = true
                }
        }

        restoreRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK)
                it.data?.data?.also { uri ->
                    downloadsInputUri = uri
                    passwordTitle = getString(R.string.decrypt_password)
                    showPasswordDialog.value = true
                }
        }

        logcatRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK)
                it.data?.data?.also { uri ->
                    try {
                        val out = contentResolver.openOutputStream(uri)
                        val process = Runtime.getRuntime().exec("logcat -d --pid=${Process.myPid()}")
                        val bufferedReader = process.inputStream.bufferedReader()
                        bufferedReader.forEachLine { line ->
                            out!!.write(line.toByteArray())
                            out.write('\n'.code.toByte().toInt())
                        }
                        out!!.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write logcat to file: $e")
                    }
                }
        }

        micIcon = if (BaresipService.isMicMuted)
            R.drawable.mic_off
        else
            R.drawable.mic_on

        setContent {
            AppTheme {
                keyboardController = LocalSoftwareKeyboardController.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalCustomColors.current.background
                ) {
                    MainScreen(this)
                }
            }
        }

        baresipService = Intent(this@MainActivity, BaresipService::class.java)

        atStartup = intent.hasExtra("onStartup")

        when (intent?.action) {
            ACTION_DIAL, ACTION_CALL, ACTION_VIEW ->
                if (BaresipService.isServiceRunning)
                    callAction(intent.data, if (intent?.action == ACTION_CALL) "call" else "dial")
                else
                    BaresipService.callActionUri = intent.data.toString()
                            .replace("tel:%2B", "tel:+")
        }

        permissions = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(POST_NOTIFICATIONS, RECORD_AUDIO, BLUETOOTH_CONNECT)
        else if (Build.VERSION.SDK_INT >= 31)
            arrayOf(RECORD_AUDIO, BLUETOOTH_CONNECT)
        else
            arrayOf(RECORD_AUDIO)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

        requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                val denied = mutableListOf<String>()
                val shouldShow = mutableListOf<String>()
                it.forEach { permission ->
                    if (!permission.value) {
                        denied.add(permission.key)
                        if (shouldShowRequestPermissionRationale(permission.key))
                            shouldShow.add(permission.key)
                    }
                }
                if (denied.contains(POST_NOTIFICATIONS) &&
                        !shouldShow.contains(POST_NOTIFICATIONS)) {
                    with(MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)) {
                        setTitle(getString(R.string.notice))
                        setMessage(getString(R.string.no_notifications))
                        setPositiveButton(getString(R.string.ok)) { _, _ ->
                            quitRestart(false)
                        }
                        show()
                    }
                } else {
                    if (shouldShow.isNotEmpty())
                        Utils.alertView(this, getString(R.string.permissions_rationale),
                            getString(R.string.audio_permissions)
                        ) { requestPermissionsLauncher.launch(permissions) }
                    else
                        startBaresip()
                }
            }

        if (!BaresipService.isServiceRunning) {
            if (File(filesDir.absolutePath + "/accounts").exists()) {
                passwordAccounts = String(
                    Utils.getFileContents(filesDir.absolutePath + "/accounts")!!,
                    Charsets.UTF_8
                ).lines().toMutableList()
                showPasswordsDialog.value = true
            } else {
                // Baresip is started for the first time
                requestPermissionsLauncher.launch(permissions)
            }
        }

    } // OnCreate

    @Composable
    private fun MainScreen(ctx: Context) {
        Scaffold(
            modifier = Modifier.safeDrawingPadding(),
            containerColor = LocalCustomColors.current.background,
            topBar = { TopAppBar(ctx, String.format(getString(R.string.baresip))) },
            bottomBar = { BottomBar(ctx) },
            content = { contentPadding ->
                MainContent(ctx, contentPadding)
            }
        )
    }

    @Composable
    fun MainContent(ctx: Context, contentPadding: PaddingValues) {
        var isRefreshing by remember { mutableStateOf(false) }
        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                delay(1000)
                isRefreshing = false
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            modifier = Modifier.fillMaxSize(),
            enabled = pullToRefreshEnabled,
            onRefresh = {
                isRefreshing = true
                if (uas.value.isNotEmpty()) {
                    if (viewModel.selectedAor.value == "")
                        spinToAor(uas.value.first().account.aor)
                    val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
                    if (ua.account.regint > 0)
                        Api.ua_register(ua.uap)
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .consumeWindowInsets(contentPadding)
                    .padding(
                        PaddingValues(
                            top = 76.dp, bottom = 6.dp,
                            start = 16.dp, end = 16.dp
                        )
                    )
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AccountSpinner(ctx)
                CallUriRow(ctx)
                CallRow(ctx)
                OnHoldNotice()
                AskPasswords(ctx)
                AskPassword(ctx)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopAppBar(ctx: Context, title: String) {

        val recOffImage = ImageVector.vectorResource(R.drawable.rec_off)
        val recOnImage = ImageVector.vectorResource(R.drawable.rec_on)

        var recImage by remember { mutableStateOf(recOffImage) }
        var menuExpanded by remember { mutableStateOf(false) }

        val about = String.format(getString(R.string.about))
        val settings = String.format(getString(R.string.configuration))
        val accounts = String.format(getString(R.string.accounts))
        val backup = String.format(getString(R.string.backup))
        val restore = String.format(getString(R.string.restore))
        val logcat = String.format(getString(R.string.logcat))
        val restart = String.format(getString(R.string.restart))
        val quit = String.format(getString(R.string.quit))

        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = LocalCustomColors.current.light,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = LocalCustomColors.current.primary
            ),
            actions = {
                IconButton(
                    modifier = Modifier.padding(end=12.dp),
                    onClick = {
                        if (Call.call("connected") == null) {
                            BaresipService.isRecOn = !BaresipService.isRecOn
                            recImage = if (BaresipService.isRecOn) {
                                Api.module_load("sndfile")
                                recOnImage
                            }
                            else {
                                Api.module_unload("sndfile")
                                recOffImage
                            }
                        }
                        else
                            Toast.makeText(ctx, R.string.rec_in_call, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = recImage,
                        tint = Color.Unspecified,
                        contentDescription = null)
                }
                IconButton(
                    modifier = Modifier.padding(end=12.dp),
                    onClick = {
                        if (Call.call("connected") != null) {
                            BaresipService.isMicMuted = !BaresipService.isMicMuted
                            if (BaresipService.isMicMuted) {
                                micIcon = R.drawable.mic_off
                                Api.calls_mute(true)
                            } else {
                                micIcon = R.drawable.mic_on
                                Api.calls_mute(false)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(micIcon),
                        tint = Color.Unspecified,
                        contentDescription = null)
                }
                IconButton(
                    modifier = Modifier.padding(end=6.dp),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 31)
                            Log.d(TAG, "Toggling speakerphone when dev/mode is " +
                                    "${am.communicationDevice!!.type}/${am.mode}"
                            )
                        Utils.toggleSpeakerPhone(ContextCompat.getMainExecutor(ctx), am)
                        speakerIcon = if (Utils.isSpeakerPhoneOn(am))
                            R.drawable.speaker_on
                        else
                            R.drawable.speaker_off
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(speakerIcon),
                        tint = Color.Unspecified,
                        contentDescription = null)
                }
                IconButton(
                    onClick = { menuExpanded = !menuExpanded }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = LocalCustomColors.current.light
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = if (Build.VERSION.SDK_INT >= 29)
                        listOf(about, settings, accounts, backup, restore, logcat, restart, quit)
                    else
                        listOf(about, settings, accounts, backup, restore, restart, quit),
                    onItemClick = { selectedItem ->
                        menuExpanded = false
                        when (selectedItem) {
                            about -> {
                                startActivity(Intent(ctx, AboutActivity::class.java))
                            }
                            settings -> {
                                startActivity(Intent(ctx, ConfigActivity::class.java))
                            }
                            accounts -> {
                                val i = Intent(ctx, AccountsActivity::class.java)
                                val b = Bundle()
                                b.putString("aor", viewModel.selectedAor.value)
                                i.putExtras(b)
                                accountsRequest.launch(i)
                            }
                            backup -> {
                                when {
                                    Build.VERSION.SDK_INT >= 29 ->
                                        pickupFileFromDownloads("backup")
                                    ContextCompat.checkSelfPermission(ctx, WRITE_EXTERNAL_STORAGE) ==
                                            PackageManager.PERMISSION_GRANTED -> {
                                        Log.d(TAG, "Write External Storage permission granted")
                                        val path = Utils.downloadsPath("baresip.bs")
                                        downloadsOutputUri = File(path).toUri()
                                        passwordTitle = getString(R.string.encrypt_password)
                                        showPasswordDialog.value = true
                                    }
                                    shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE) ->
                                        Utils.alertView(ctx, getString(R.string.notice), getString(R.string.no_backup)) {
                                            requestPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                                        }
                                    else ->
                                        requestPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                                }
                            }
                            restore -> {
                                when {
                                    Build.VERSION.SDK_INT >= 29 ->
                                        pickupFileFromDownloads("restore")
                                    ContextCompat.checkSelfPermission(ctx, READ_EXTERNAL_STORAGE) ==
                                            PackageManager.PERMISSION_GRANTED -> {
                                        Log.d(TAG, "Read External Storage permission granted")
                                        val path = Utils.downloadsPath("baresip.bs")
                                        downloadsInputUri = File(path).toUri()
                                        passwordTitle = getString(R.string.decrypt_password)
                                        showPasswordDialog.value = true
                                    }
                                    shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE) ->
                                        Utils.alertView(ctx, getString(R.string.notice), getString(R.string.no_restore)) {
                                            requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
                                        }
                                    else ->
                                        requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
                                }
                            }
                            logcat -> {
                                if (Build.VERSION.SDK_INT >= 29)
                                    pickupFileFromDownloads("logcat")
                            }
                            restart -> {
                                quitRestart(true)
                            }
                            quit -> {
                                quitRestart(false)
                            }
                        }
                    }
                )
            }
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AccountSpinner(ctx: Context) {

        var expanded by rememberSaveable { mutableStateOf(false) }
        val selected: String by viewModel.selectedAor.collectAsState()

        if (uas.value.isEmpty())
            viewModel.updateSelectedAor("")
        else
            if (selected == "" || UserAgent.ofAor(selected) == null) {
                viewModel.updateSelectedAor(uas.value.first().account.aor)
            }

        showCall(UserAgent.ofAor(selected))
        updateIcons(Account.ofAor(selected))

        if (selected == "") {
            OutlinedButton(
                onClick = {
                    val i = Intent(this@MainActivity, AccountsActivity::class.java)
                    val b = Bundle()
                    b.putString("aor", selected)
                    i.putExtras(b)
                    accountsRequest.launch(i)
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(50.dp)
                    .fillMaxWidth(),
                colors = ButtonColors(
                    containerColor = LocalCustomColors.current.grayLight,
                    contentColor = LocalCustomColors.current.dark,
                    disabledContainerColor = LocalCustomColors.current.grayLight,
                    disabledContentColor = LocalCustomColors.current.dark
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text(text = "")
            }
        }
        else
            OutlinedButton(
                onClick = {
                    expanded = !expanded
                },
                enabled = true,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(50.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                expanded = true
                            },
                            onLongPress = {
                                val ua = UserAgent.ofAor(selected)
                                if (ua != null) {
                                    val acc = ua.account
                                    if (Api.account_regint(acc.accp) > 0) {
                                        Api.account_set_regint(acc.accp, 0)
                                        Api.ua_unregister(ua.uap)
                                    } else {
                                        Api.account_set_regint(
                                            acc.accp,
                                            acc.configuredRegInt
                                        )
                                        Api.ua_register(ua.uap)
                                    }
                                    acc.regint = Api.account_regint(acc.accp)
                                    AccountsActivity.saveAccounts()
                                }
                            }
                        )
                    },
                colors = ButtonColors(
                    containerColor = LocalCustomColors.current.grayLight,
                    contentColor = LocalCustomColors.current.dark,
                    disabledContainerColor = LocalCustomColors.current.grayLight,
                    disabledContentColor = LocalCustomColors.current.dark
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        uasStatus.value[selected] ?:
                        R.drawable.locked_yellow),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .clickable(onClick = {
                            val i = Intent(ctx, AccountActivity::class.java)
                            val b = Bundle()
                            b.putString("aor", selected)
                            i.putExtras(b)
                            accountRequest.launch(i)
                        })
                )
                Text(
                    text = Account.ofAor(selected)?.text() ?: "",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { expanded = true },
                            onLongClick = {
                                val ua = UserAgent.ofAor(selected)
                                if (ua != null) {
                                    val acc = ua.account
                                    if (Api.account_regint(acc.accp) > 0) {
                                        Api.account_set_regint(acc.accp, 0)
                                        Api.ua_unregister(ua.uap)
                                    } else {
                                        Api.account_set_regint(
                                            acc.accp,
                                            acc.configuredRegInt
                                        )
                                        Api.ua_register(ua.uap)
                                    }
                                    acc.regint = Api.account_regint(acc.accp)
                                    AccountsActivity.saveAccounts()
                                }
                            }
                        )
                )
                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    uas.value.forEachIndexed { _, ua ->
                        val acc = ua.account
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                run {
                                    viewModel.updateSelectedAor(acc.aor)
                                }
                                showCall(ua)
                                updateIcons(acc)
                            },
                            text = { Text(
                                text = acc.text(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            ) },
                            leadingIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(uasStatus.value[acc.aor]!!),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                )
                            }
                        )
                    }
                }
            }
    }

    @Composable
    fun CallUriRow(ctx: Context) {
        val suggestions by remember { contactNames }
        var filteredSuggestions by remember { mutableStateOf(suggestions) }
        var showSuggestions by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val lazyListState = rememberLazyListState()

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = callUri.value,
                    enabled = callUriEnabled.value,
                    singleLine = true,
                    onValueChange = {
                        val newValue = it
                        if (it != callUri.value) {
                            callUri.value = newValue
                            showSuggestions = newValue.length > 2
                            filteredSuggestions = suggestions.filter { suggestion ->
                                newValue.length > 2 && suggestion.startsWith(newValue, ignoreCase = true)
                            }
                        }
                    },
                    trailingIcon = {
                        if (callUriEnabled.value && callUri.value.isNotEmpty())
                            Icon(Icons.Outlined.Clear,
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable {
                                        callUri.value = ""
                                        showSuggestions = false
                                    }
                            )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 2.dp)
                        .focusRequester(focusRequester),
                    label = {
                        Text(
                            text = callUriLabel.value,
                            fontSize = 18.sp,
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        color = LocalCustomColors.current.itemText
                    ),
                    keyboardOptions = if (dialpad)
                        KeyboardOptions(keyboardType = KeyboardType.Phone)
                    else
                        KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .background(
                            LocalCustomColors.current.grayLight,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .animateContentSize()
                ) {
                    if (showSuggestions && filteredSuggestions.isNotEmpty()) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScrollbar(
                                        state = lazyListState,
                                        color = LocalCustomColors.current.gray
                                    ),
                                horizontalAlignment = Alignment.Start,
                                state = lazyListState
                            ) {
                                items(
                                    items = filteredSuggestions,
                                    key = { suggestion -> suggestion }
                                ) { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                callUri.value = suggestion
                                                showSuggestions = false
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = LocalCustomColors.current.grayDark,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showCallTimer.value) {
                val textColor = LocalCustomColors.current.itemText.toArgb()
                AndroidView(
                    factory = { context ->
                        Chronometer(context).also { callTimer = it
                            callTimer?.textSize = 16F
                            callTimer?.setTextColor(textColor)
                        }
                    },
                    modifier = Modifier.padding(start = 6.dp,
                        top = 4.dp,
                        end = if (securityIcon.intValue != -1) 6.dp else 0.dp),
                )
            }
            if (securityIcon.intValue != -1) {
                Icon(
                    imageVector = ImageVector.vectorResource(securityIcon.intValue),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(top = 4.dp)
                        .clickable {
                            when (securityIcon.intValue) {
                                R.drawable.unlocked -> {
                                    Utils.alertView(
                                        ctx, getString(R.string.alert),
                                        getString(R.string.call_not_secure)
                                    )
                                }

                                R.drawable.locked_yellow -> {
                                    Utils.alertView(
                                        ctx, getString(R.string.alert),
                                        getString(R.string.peer_not_verified)
                                    )
                                }

                                R.drawable.locked_green -> {
                                    with(
                                        MaterialAlertDialogBuilder(
                                            ctx,
                                            R.style.AlertDialogTheme
                                        )
                                    ) {
                                        setTitle(R.string.info)
                                        setMessage(getString(R.string.call_is_secure))
                                        setPositiveButton(getString(R.string.unverify)) { dialog, _ ->
                                            val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
                                            val call = ua.currentCall()
                                            if (call != null) {
                                                if (Api.cmd_exec("zrtp_unverify " + call.zid) != 0)
                                                    Log.e(
                                                        TAG,
                                                        "Command 'zrtp_unverify ${call.zid}' failed"
                                                    )
                                                else
                                                    securityIcon.intValue = R.drawable.locked_yellow
                                            }
                                            dialog.dismiss()
                                        }
                                        setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        show()
                                    }
                                }
                            }
                        },
                    tint = Color.Unspecified,
                )
            }
        }
    }

    @Composable
    fun CallRow(ctx: Context) {

        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Absolute.SpaceBetween
        ) {
            if (showCallButton.value)
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        callClick(ctx)
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.call),
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }

            if (showHangupButton.value) {

                var ua: UserAgent = userAgentofSelectedAor()

                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        if (Build.VERSION.SDK_INT < 31) {
                            if (callRunnable != null) {
                                callHandler.removeCallbacks(callRunnable!!)
                                callRunnable = null
                                BaresipService.abandonAudioFocus(applicationContext)
                                showCall(ua)
                            }
                        } else {
                            if (audioModeChangedListener != null) {
                                am.removeOnModeChangedListener(audioModeChangedListener!!)
                                audioModeChangedListener = null
                                BaresipService.abandonAudioFocus(applicationContext)
                                showCall(ua)
                            }
                        }
                        val aor = ua.account.aor
                        val uaCalls = ua.calls()
                        if (uaCalls.isNotEmpty()) {
                            val call = uaCalls[uaCalls.size - 1]
                            val callp = call.callp
                            Log.d(TAG, "AoR $aor hanging up call $callp with ${callUri.value}")
                            showHangupButton.value = false
                            Api.ua_hangup(ua.uap, callp, 0, "")
                        }
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.hangup),
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }

                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        val aor = ua.account.aor
                        val call = ua.currentCall()!!
                        if (call.onhold) {
                            Log.d(TAG, "AoR $aor resuming call ${call.callp} with ${callUri.value}")
                            call.resume()
                            call.onhold = false
                            holdIcon.intValue = R.drawable.call_hold
                        } else {
                            Log.d(TAG, "AoR $aor holding call ${call.callp} with ${callUri.value}")
                            call.hold()
                            call.onhold = true
                            holdIcon.intValue = R.drawable.resume
                        }
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = holdIcon.intValue),
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }

                var showTransferDialog by remember { mutableStateOf(false) }
                IconButton(
                    modifier = Modifier.size(48.dp),
                    enabled = transferButtonEnabled.value,
                    onClick = {
                        val call = ua.currentCall()
                        if (call != null) {
                            if (call.onHoldCall != null) {
                                if (!call.executeTransfer())
                                    Utils.alertView(
                                        ctx, getString(R.string.notice),
                                        String.format(getString(R.string.transfer_failed))
                                    )
                            }
                            else
                                showTransferDialog = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(transferIcon.intValue),
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }

                if (showTransferDialog) {
                    val call = ua.currentCall()
                    val showDialog = remember { mutableStateOf(call != null) }
                    val blindChecked = remember { mutableStateOf(true) }
                    if (showDialog.value)
                        BasicAlertDialog(
                            onDismissRequest = {
                                keyboardController?.hide()
                                showDialog.value = false
                                showTransferDialog = false
                            }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .wrapContentHeight(),
                                color = LocalCustomColors.current.grayLight,
                                shape = MaterialTheme.shapes.large,
                                tonalElevation = AlertDialogDefaults.TonalElevation
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = ContextCompat.getString(ctx, R.string.call_transfer),
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(16.dp),
                                        color = LocalCustomColors.current.primary,
                                    )
                                    var uri by remember { mutableStateOf("") }
                                    val suggestions by remember { contactNames }
                                    var filteredSuggestions by remember { mutableStateOf(suggestions) }
                                    var showSuggestions by remember { mutableStateOf(false) }
                                    val focusRequester = remember { FocusRequester() }
                                    val lazyListState = rememberLazyListState()
                                    OutlinedTextField(
                                        value = uri,
                                        singleLine = true,
                                        onValueChange = {
                                            uri = it
                                            showSuggestions = uri.isNotEmpty()
                                            filteredSuggestions = if (uri.isEmpty())
                                                suggestions
                                            else
                                                suggestions.filter { suggestion ->
                                                    uri.length > 2 && suggestion.startsWith(uri,
                                                        ignoreCase = true)
                                                }
                                        },
                                        trailingIcon = {
                                            if (uri.isNotEmpty())
                                                Icon(
                                                    Icons.Outlined.Clear,
                                                    contentDescription = null,
                                                    modifier = Modifier.clickable {
                                                        uri = ""
                                                        showSuggestions = false
                                                    }
                                                )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 4.dp,
                                                end = 4.dp,
                                                top = 12.dp,
                                                bottom = 2.dp
                                            )
                                            .focusRequester(focusRequester),
                                        label = {
                                            Text(
                                                text = stringResource(R.string.transfer_destination),
                                                fontSize = 18.sp,
                                                color = LocalCustomColors.current.dark
                                            )
                                        },
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            color = LocalCustomColors.current.dark
                                        ),
                                        keyboardOptions = if (dialpad)
                                            KeyboardOptions(keyboardType = KeyboardType.Phone)
                                        else
                                            KeyboardOptions(keyboardType = KeyboardType.Text)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(8.dp, RoundedCornerShape(8.dp))
                                            .background(
                                                LocalCustomColors.current.grayLight,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .animateContentSize()
                                    ) {
                                        if (showSuggestions && filteredSuggestions.isNotEmpty()) {
                                            Box(modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(150.dp)) {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .verticalScrollbar(
                                                            state = lazyListState,
                                                            color = LocalCustomColors.current.gray
                                                        ),
                                                    horizontalAlignment = Alignment.Start,
                                                    state = lazyListState,
                                                ) {
                                                    items(
                                                        items = filteredSuggestions,
                                                        key = { suggestion -> suggestion }
                                                    ) { suggestion ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    callUri.value = suggestion
                                                                    showSuggestions = false
                                                                }
                                                                .padding(12.dp)
                                                        ) {
                                                            Text(
                                                                text = suggestion,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color = LocalCustomColors.current.grayDark,
                                                                fontSize = 18.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (call!!.replaces())
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = ContextCompat.getString(ctx, R.string.blind),
                                                color = LocalCustomColors.current.dark,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                            Checkbox(
                                                checked = blindChecked.value
                                            ) {
                                                blindChecked.value = true
                                            }
                                            Text(
                                                text = ContextCompat.getString(ctx, R.string.attended),
                                                color = LocalCustomColors.current.dark,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                            Checkbox(
                                                checked = !blindChecked.value
                                            ) {
                                                blindChecked.value = false
                                            }
                                        }
                                    Row(
                                        horizontalArrangement = Arrangement.Absolute.SpaceEvenly
                                    ) {
                                        TextButton(
                                            onClick = {
                                                keyboardController?.hide()
                                                showDialog.value = false
                                                showTransferDialog = false
                                            },
                                            modifier = Modifier.padding(8.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.cancel),
                                                color = LocalCustomColors.current.gray
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                var uriText = uri.trim()
                                                if (uriText.isNotEmpty()) {
                                                    val uris = Contact.contactUris(uriText)
                                                    if (uris.size > 1) {
                                                        val destinationBuilder =
                                                            MaterialAlertDialogBuilder(
                                                                ctx,
                                                                R.style.AlertDialogTheme
                                                            )
                                                        with(destinationBuilder) {
                                                            setTitle(R.string.choose_destination_uri)
                                                            setItems(uris.toTypedArray()) { _, which ->
                                                                uriText = uris[which]
                                                                transfer(
                                                                    ua,
                                                                    if (Utils.isTelNumber(uriText))
                                                                        "tel:$uriText"
                                                                    else
                                                                        uriText,
                                                                    !blindChecked.value
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        if (uris.size == 1)
                                                            uriText = uris[0]
                                                    }
                                                    transfer(
                                                        ua,
                                                        if (Utils.isTelNumber(uriText)) "tel:$uriText" else uriText,
                                                        !blindChecked.value
                                                    )
                                                    keyboardController?.hide()
                                                    showDialog.value = false
                                                    showTransferDialog = false
                                                }
                                            },
                                            modifier = Modifier.padding(8.dp),
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    if (blindChecked.value)
                                                        R.string.transfer
                                                    else
                                                        R.string.call
                                                ).uppercase(),
                                                color = LocalCustomColors.current.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }

                val focusRequester = remember { FocusRequester() }
                val shouldRequestFocus by focusDtmf
                TextField(
                    value = dtmfText.value,
                    onValueChange = { newText ->
                        if (newText.length > dtmfText.value.length) {
                            val char = newText.last()
                            Log.d(TAG, "Got DTMF digit '$char'")
                            if (char.isDigit() || char == '*' || char == '#') {
                                dtmfText.value = newText
                                ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
                                ua.currentCall()!!.sendDigit(char)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .width(80.dp)
                        .focusRequester(focusRequester),
                    enabled = dtmfEnabled.value,
                    textStyle = TextStyle(fontSize = 16.sp),
                    label = { Text(getString(R.string.dtmf), fontSize = 16.sp) },
                    singleLine = true
                )
                LaunchedEffect(shouldRequestFocus) {
                    if (shouldRequestFocus) {
                        focusRequester.requestFocus()
                        focusDtmf.value = false
                    }
                }

                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
                        val call = ua.currentCall()!!
                        val stats = call.stats("audio")
                        if (stats != "") {
                            val parts = stats.split(",") as java.util.ArrayList
                            if (parts[2] == "0/0") {
                                parts[2] = "?/?"
                                parts[3] = "?/?"
                                parts[4] = "?/?"
                            }
                            val codecs = call.audioCodecs()
                            val duration = call.duration()
                            val txCodec = codecs.split(',')[0].split("/")
                            val rxCodec = codecs.split(',')[1].split("/")
                            Utils.alertView(
                                ctx, getString(R.string.call_info),
                                "${String.format(getString(R.string.duration), duration)}\n" +
                                        "${getString(R.string.codecs)}: ${txCodec[0]} ch ${txCodec[2]}/" +
                                        "${rxCodec[0]} ch ${rxCodec[2]}\n" +
                                        "${String.format(getString(R.string.rate), parts[0])}\n" +
                                        "${
                                            String.format(
                                                getString(R.string.average_rate),
                                                parts[1]
                                            )
                                        }\n" +
                                        "${getString(R.string.packets)}: ${parts[2]}\n" +
                                        "${getString(R.string.lost)}: ${parts[3]}\n" +
                                        String.format(getString(R.string.jitter), parts[4])
                            )
                        } else {
                            Utils.alertView(
                                ctx, getString(R.string.call_info),
                                getString(R.string.call_info_not_available)
                            )
                        }
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.info),
                        modifier = Modifier.size(36.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }
            }

            if (showAnswerRejectButtons.value) {

                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        answer(ctx)
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.call),
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        reject()
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.hangup),
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                }
            }
        }

    }

    private fun callClick(ctx: Context) {
        if (viewModel.selectedAor.value != "") {
            if (Utils.checkPermissions(ctx, arrayOf(RECORD_AUDIO)))
                makeCall()
            else
                Toast.makeText(applicationContext, R.string.no_calls, Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCall(lookForContact: Boolean = true) {
        val ua = UserAgent.ofAor(viewModel.selectedAor.value)
        val aor = ua!!.account.aor
        if (!Call.inCall()) {
            var uriText = callUri.value.trim()
            if (uriText.isNotEmpty()) {
                if (lookForContact) {
                    val uris = Contact.contactUris(uriText)
                    if (uris.size == 1)
                        uriText = uris[0]
                    else if (uris.size > 1) {
                        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                        with(builder) {
                            setTitle(R.string.choose_destination_uri)
                            setItems(uris.toTypedArray()) { _, which ->
                                callUri.value = uris[which]
                                makeCall(false)
                            }
                            setNeutralButton(getString(R.string.cancel)) { _: DialogInterface, _: Int -> }
                            show()
                        }
                        return
                    }
                }
                if (Utils.isTelNumber(uriText))
                    uriText = "tel:$uriText"
                val uri = if (Utils.isTelUri(uriText)) {
                    if (ua.account.telProvider == "") {
                        Utils.alertView(this, getString(R.string.notice),
                            String.format(getString(R.string.no_telephony_provider), aor))
                        return
                    }
                    Utils.telToSip(uriText, ua.account)
                } else {
                    Utils.uriComplete(uriText, aor)
                }
                if (!Utils.checkUri(uri)) {
                    Utils.alertView(this, getString(R.string.notice),
                        String.format(getString(R.string.invalid_sip_or_tel_uri), uri))
                } else if (!BaresipService.requestAudioFocus(applicationContext)) {
                    Toast.makeText(applicationContext, R.string.audio_focus_denied,
                        Toast.LENGTH_SHORT).show()
                } else {
                    callUriEnabled.value = false
                    showCallButton.value = false
                    showHangupButton.value = true
                    if (Build.VERSION.SDK_INT < 31) {
                        Log.d(TAG, "Setting audio mode to MODE_IN_COMMUNICATION")
                        am.mode = AudioManager.MODE_IN_COMMUNICATION
                        runCall(ua, uri)
                    } else {
                        if (am.mode == AudioManager.MODE_IN_COMMUNICATION) {
                            runCall(ua, uri)
                        } else {
                            audioModeChangedListener = AudioManager.OnModeChangedListener { mode ->
                                if (mode == AudioManager.MODE_IN_COMMUNICATION) {
                                    Log.d(TAG, "Audio mode changed to MODE_IN_COMMUNICATION using " +
                                            "device ${am.communicationDevice!!.type}")
                                    if (audioModeChangedListener != null) {
                                        am.removeOnModeChangedListener(audioModeChangedListener!!)
                                        audioModeChangedListener = null
                                    }
                                    runCall(ua, uri)
                                } else {
                                    Log.d(TAG, "Audio mode changed to mode ${am.mode} using " +
                                            "device ${am.communicationDevice!!.type}")
                                }
                            }
                            am.addOnModeChangedListener(mainExecutor, audioModeChangedListener!!)
                            Log.d(TAG, "Setting audio mode to MODE_IN_COMMUNICATION")
                            am.mode = AudioManager.MODE_IN_COMMUNICATION
                        }
                    }
                }
            } else {
                val latestPeerUri = CallHistoryNew.aorLatestPeerUri(aor)
                if (latestPeerUri != null)
                    callUri.value = Utils.friendlyUri(this, latestPeerUri, ua.account)
            }
        }
    }

    private fun answer(ctx: Context) {
        val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
        val call = ua.currentCall()
        if (call != null) {
            Log.d(TAG, "AoR ${ua.account.aor} answering call from ${callUri.value}")
            val intent = Intent(ctx, BaresipService::class.java)
            intent.action = "Call Answer"
            intent.putExtra("uap", ua.uap)
            intent.putExtra("callp", call.callp)
            intent.putExtra("video", Api.VIDMODE_OFF)
            startService(intent)
        }
    }

    private fun reject() {
        val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
        val aor = ua.account.aor
        val call = ua.currentCall()!!
        val callp = call.callp
        Log.d(TAG, "AoR $aor rejecting call $callp from ${callUri.value}")
        call.rejected = true
        Api.ua_hangup(ua.uap, callp, 486, "Busy Here")
    }

    @Composable
    fun BottomBar(ctx: Context) {
        val buttonSize = 48.dp
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showVmIcon)
                IconButton(
                    onClick = {
                        if (viewModel.selectedAor.value != "") {
                            val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
                            val acc = ua.account
                            if (acc.vmUri != "") {
                                val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> {
                                            val i = Intent(ctx, MainActivity::class.java)
                                            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            i.putExtra("action", "call")
                                            i.putExtra("uap", ua.uap)
                                            i.putExtra("peer", acc.vmUri)
                                            startActivity(i)
                                        }
                                        DialogInterface.BUTTON_NEGATIVE -> {
                                        }
                                    }
                                }
                                with(MaterialAlertDialogBuilder(ctx, R.style.AlertDialogTheme)) {
                                    setTitle(R.string.voicemail_messages)
                                    setMessage(acc.vmMessages(ctx))
                                    setPositiveButton(getString(R.string.listen), dialogClickListener)
                                    setNeutralButton(getString(R.string.cancel), dialogClickListener)
                                    show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .size(buttonSize)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(vmIcon.intValue),
                        contentDescription = null,
                        Modifier.size(buttonSize),
                        tint = Color.Unspecified
                    )
                }

            IconButton(
                onClick = {
                    if (viewModel.selectedAor.value != "") {
                        val i = Intent(ctx, ContactsActivity::class.java)
                        val b = Bundle()
                        b.putString("aor", viewModel.selectedAor.value)
                        i.putExtras(b)
                        contactsRequest.launch(i)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .size(buttonSize)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.contacts),
                    contentDescription = null,
                    Modifier.size(buttonSize),
                    tint = LocalCustomColors.current.secondary
                )
            }

            IconButton(
                onClick = {
                    if (viewModel.selectedAor.value != "") {
                        val i = Intent(this@MainActivity, ChatsActivity::class.java)
                        val b = Bundle()
                        b.putString("aor", viewModel.selectedAor.value)
                        b.putString("peer", resumeUri)
                        i.putExtras(b)
                        chatRequests.launch(i)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .size(buttonSize)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(messagesIcon.intValue),
                    contentDescription = null,
                    Modifier.size(buttonSize),
                    tint = Color.Unspecified
                )
            }

            IconButton(
                onClick = {
                    calls(ctx)
                },
                modifier = Modifier
                    .weight(1f)
                    .size(buttonSize)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.calls),
                    contentDescription = null,
                    Modifier.size(buttonSize),
                    tint = Color.Unspecified
                )
            }

            IconButton(
                onClick = { dialpad = !dialpad },
                modifier = Modifier
                    .weight(1f)
                    .size(buttonSize),
                enabled = dialpadButtonEnabled
            ) {
                Icon(
                    imageVector = if (dialpad)
                        ImageVector.vectorResource(R.drawable.dialpad_on)
                    else
                        ImageVector.vectorResource(R.drawable.dialpad_off),
                    contentDescription = null,
                    modifier = Modifier.size(buttonSize),
                    tint = Color.Unspecified
                )
            }
        }

    }

    @Composable
    fun OnHoldNotice() {
        if (showOnHoldNotice.value)
            OutlinedButton(
                onClick = {},
                border = BorderStroke(1.dp, LocalCustomColors.current.accent),
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(20)
           ) {
                Text(
                    text = getString(R.string.call_is_on_hold),
                    fontSize = 18.sp,
                    color = LocalCustomColors.current.itemText,
                )
            }
    }

    @Composable
    fun AskPasswords(ctx: Context) {
        if (showPasswordsDialog.value) {
            if (passwordAccounts.isNotEmpty()) {
                val account = passwordAccounts.removeAt(0)
                val params = account.substringAfter(">")
                if (Utils.paramValue(params, "auth_user") != "" && Utils.paramValue(params, "auth_pass") == "") {
                    val aor = account.substringAfter("<").substringBefore(">")
                    val showPassword = remember { mutableStateOf(false) }
                    BasicAlertDialog(
                        onDismissRequest = {
                            keyboardController?.hide()
                            showPasswordsDialog.value = false
                        }
                    ) {
                        Surface(
                            modifier = Modifier
                                .wrapContentWidth()
                                .wrapContentHeight(),
                            color = LocalCustomColors.current.background,
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = AlertDialogDefaults.TonalElevation
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = getString(R.string.authentication_password),
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                                    color = LocalCustomColors.current.alert,
                                )
                                val message =
                                    getString(R.string.account) + " " + Utils.plainAor(aor)
                                Text(
                                    text = message,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(16.dp),
                                    color = LocalCustomColors.current.itemText,
                                )
                                var password by remember { mutableStateOf("") }
                                val focusRequester = remember { FocusRequester() }
                                OutlinedTextField(
                                    value = password,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = LocalCustomColors.current.textFieldBackground,
                                        unfocusedContainerColor = LocalCustomColors.current.textFieldBackground,
                                        cursorColor = LocalCustomColors.current.primary,
                                    ),
                                    onValueChange = {
                                        password = it
                                    },
                                    visualTransformation = if (showPassword.value)
                                        VisualTransformation.None
                                    else
                                        PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            showPassword.value = !showPassword.value
                                        }) {
                                            Icon(
                                                if (showPassword.value)
                                                    ImageVector.vectorResource(R.drawable.visibility)
                                                else
                                                    ImageVector.vectorResource(R.drawable.visibility_off),
                                                contentDescription = "Visibility",
                                                tint = LocalCustomColors.current.grayDark

                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 4.dp,
                                            end = 4.dp,
                                            top = 12.dp,
                                            bottom = 2.dp
                                        )
                                        .focusRequester(focusRequester),
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        color = LocalCustomColors.current.dark
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TextButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            showPasswordsDialog.value = false
                                            showPasswordsDialog.value = true
                                        },
                                        modifier = Modifier.padding(8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cancel),
                                            color = LocalCustomColors.current.gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            showPasswordsDialog.value = false
                                            password = password.trim()
                                            if (!Account.checkAuthPass(password)) {
                                                Toast.makeText(
                                                    ctx,
                                                    String.format(
                                                        getString(R.string.invalid_authentication_password),
                                                        password
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                password = ""
                                                passwordAccounts.add(0, account)
                                            } else
                                                BaresipService.aorPasswords[aor] = password
                                            showPasswordsDialog.value = true
                                        },
                                        modifier = Modifier.padding(8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ok),
                                            color = LocalCustomColors.current.alert
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    showPasswordsDialog.value = false
                    showPasswordsDialog.value = true
                }
            }
            else
                requestPermissionsLauncher.launch(permissions)
        }
    }

    @Composable
    fun AskPassword(ctx: Context) {
        if (showPasswordDialog.value) {
            val showPassword = remember { mutableStateOf(false) }
            BasicAlertDialog(
                onDismissRequest = {
                    keyboardController?.hide()
                    showPasswordDialog.value = false
                }
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    color = LocalCustomColors.current.background,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = passwordTitle,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                            color = LocalCustomColors.current.alert,
                        )
                        var password by remember { mutableStateOf("") }
                        val focusRequester = remember { FocusRequester() }
                        OutlinedTextField(
                            value = password,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = LocalCustomColors.current.textFieldBackground,
                                unfocusedContainerColor = LocalCustomColors.current.textFieldBackground,
                                cursorColor = LocalCustomColors.current.primary,
                            ),
                            onValueChange = {
                                password = it
                            },
                            visualTransformation = if (showPassword.value)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    showPassword.value = !showPassword.value
                                }) {
                                    Icon(
                                        if (showPassword.value)
                                            ImageVector.vectorResource(R.drawable.visibility)
                                        else
                                            ImageVector.vectorResource(R.drawable.visibility_off),
                                        contentDescription = "Visibility",
                                        tint = LocalCustomColors.current.grayDark

                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 4.dp,
                                    end = 4.dp,
                                    top = 12.dp,
                                    bottom = 2.dp
                                )
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 18.sp,
                                color = LocalCustomColors.current.dark
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        Row(
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = {
                                    keyboardController?.hide()
                                    showPasswordDialog.value = false
                                    if (downloadsOutputUri != null) {
                                        Utils.deleteFile(ctx, downloadsOutputUri!!)
                                    }
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    color = LocalCustomColors.current.gray
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    keyboardController?.hide()
                                    showPasswordDialog.value = false
                                    password = password.trim()
                                    if (!Account.checkAuthPass(password)) {
                                        Toast.makeText(ctx,
                                            String.format(getString(R.string.invalid_authentication_password), password),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        password = ""
                                    }
                                    if (password != "") {
                                        if (passwordTitle == getString(R.string.encrypt_password))
                                            backup(password)
                                        else
                                            restore(password)
                                    }
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.ok),
                                    color = LocalCustomColors.current.alert
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun calls(ctx: Context) {
        if (viewModel.selectedAor.value != "") {
            val i = Intent(ctx, CallsActivity::class.java)
            val b = Bundle()
            b.putString("aor", viewModel.selectedAor.value)
            i.putExtras(b)
            callsRequest.launch(i)
        }
    }

    private fun updateIcons(acc: Account?) {
        if (acc == null) {
            showVmIcon = false
            messagesIcon.intValue = R.drawable.messages
            callsIcon.intValue = R.drawable.calls
        }
        else {
            if (acc.vmUri != "") {
                showVmIcon = true
                vmIcon.intValue = if (acc.vmNew > 0)
                    R.drawable.voicemail_new
                else
                    R.drawable.voicemail
            } else
                showVmIcon = false
            messagesIcon.intValue= if (acc.unreadMessages)
                R.drawable.messages_unread
            else
                R.drawable.messages
            callsIcon.intValue = if (acc.missedCalls)
                R.drawable.calls_missed
            else
                R.drawable.calls
        }
    }

    override fun onStart() {
        super.onStart()
        Log.e(TAG, "Main onStart")
        val action = intent.getStringExtra("action")
        if (action != null) {
            // MainActivity was not visible when call, message, or transfer request came in
            intent.removeExtra("action")
            handleIntent(intent, action)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Main onResume with action '$resumeAction'")
        nm.cancelAll()
        BaresipService.isMainVisible = true
        when (resumeAction) {
            "call show" -> {
                handleServiceEvent ("call incoming",
                    arrayListOf(resumeCall!!.ua.uap, resumeCall!!.callp))
            }
            "call answer" -> {
                answer(this@MainActivity)
                showCall(resumeCall!!.ua)
            }
            "call missed" -> {
                calls(this@MainActivity)
            }
            "call reject" ->
                reject()
            "call" -> {
                callUri.value = Account.ofAor(viewModel.selectedAor.value)!!.resumeUri
                callClick(this@MainActivity)
            }
            "dial" -> {
                callUri.value = Account.ofAor(viewModel.selectedAor.value)!!.resumeUri
            }
            "call transfer", "transfer show", "transfer accept" ->
                handleServiceEvent("$resumeAction,$resumeUri",
                    arrayListOf(resumeCall!!.ua.uap, resumeCall!!.callp))
            "message", "message show", "message reply" ->
                handleServiceEvent(resumeAction, arrayListOf(resumeUap, resumeUri))
            else -> {
                val incomingCall = Call.call("incoming")
                if (incomingCall != null) {
                    spinToAor(incomingCall.ua.account.aor)
                } else {
                    restoreActivities()
                    if (uas.value.isNotEmpty()) {
                        if (viewModel.selectedAor.value == "") {
                            if (Call.inCall())
                                spinToAor(Call.calls()[0].ua.account.aor)
                            else
                                spinToAor(uas.value.first().account.aor)
                        }
                    }
                }
                val ua = UserAgent.ofAor(viewModel.selectedAor.value)
                if (ua != null) {
                    showCall(ua)
                    updateIcons(ua.account)
                }
            }
        }
        resumeAction = ""
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Main onPause")
        Utils.addActivity("main")
        BaresipService.isMainVisible = false
        callTimer?.stop()
        saveCallUri()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Main onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Main onDestroy")
        this.unregisterReceiver(screenEventReceiver)
        if (Build.VERSION.SDK_INT >= 31)
            am.removeOnCommunicationDeviceChangedListener(comDevChangedListener)
        BaresipService.serviceEvent.removeObserver(serviceEventObserver)
        BaresipService.serviceEvents.clear()
        BaresipService.activities.clear()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (dtmfEnabled.value)
            focusDtmf.value = true
    }

    override fun onNewIntent(intent: Intent) {
        // Called when MainActivity already exists at the top of current task
        super.onNewIntent(intent)

        this.setShowWhenLocked(true)
        this.setTurnScreenOn(true)

        resumeAction = ""
        resumeUri = ""

        Log.d(TAG, "onNewIntent with action/data '${intent.action}/${intent.data}'")

        when (intent.action) {
            ACTION_DIAL, ACTION_CALL, ACTION_VIEW ->
                callAction(intent.data, if (intent.action == ACTION_CALL) "call" else "dial")
            else -> {
                val action = intent.getStringExtra("action")
                if (action != null) {
                    intent.removeExtra("action")
                    handleIntent(intent, action)
                }
            }
        }
    }

   private fun callAction(uri: Uri?, action: String) {
        if (Call.inCall() || uas.value.isEmpty())
            return
        Log.d(TAG, "Action $action to $uri")
        if (uri != null) {
            when (uri.scheme) {
                "sip" -> {
                    val uriStr = Utils.uriUnescape(uri.toString())
                    var ua = UserAgent.ofDomain(Utils.uriHostPart(uriStr))
                    if (ua == null && uas.value.isNotEmpty())
                        ua = uas.value[0]
                    if (ua == null) {
                        Log.w(TAG, "No accounts for '$uriStr'")
                        return
                    }
                    spinToAor(ua.account.aor)
                    resumeAction = action
                    ua.account.resumeUri = uriStr
                }
                "tel" -> {
                    val uriStr = uri.toString().replace("%2B", "+")
                        .replace("%20", "")
                        .filterNot{setOf('-', ' ', '(', ')').contains(it)}
                    var account: Account? = null
                    for (a in Account.accounts())
                        if (a.telProvider != "") {
                            account = a
                            break
                        }
                    if (account == null) {
                        Log.w(TAG, "No telephony providers for '$uriStr'")
                        return
                    }
                    spinToAor(account.aor)
                    resumeAction = action
                    account.resumeUri = uriStr
                }
                else -> {
                    Log.w(TAG, "Unsupported URI scheme ${uri.scheme}")
                    return
                }
            }
        }
    }

    private fun handleIntent(intent: Intent, action: String) {
        Log.d(TAG, "Handling intent '$action'")
        val ev = action.split(",")
        when (ev[0]) {
            "no network" -> {
                Utils.alertView(this, getString(R.string.notice),
                    getString(R.string.no_network))
                return
            }
            "call", "dial" -> {
                if (Call.inCall()) {
                    Toast.makeText(applicationContext, getString(R.string.call_already_active),
                            Toast.LENGTH_SHORT).show()
                    return
                }
                val uap = intent.getLongExtra("uap", 0L)
                val ua = UserAgent.ofUap(uap)
                if (ua == null) {
                    Log.w(TAG, "handleIntent 'call' did not find ua $uap")
                    return
                }
                spinToAor(ua.account.aor)
                resumeAction = action
                ua.account.resumeUri = intent.getStringExtra("peer")!!
            }
            "call show", "call answer" -> {
                val callp = intent.getLongExtra("callp", 0L)
                val call = Call.ofCallp(callp)
                if (call == null) {
                    Log.w(TAG, "handleIntent '$action' did not find call $callp")
                    return
                }
                val ua = call.ua
                spinToAor(ua.account.aor)
                resumeAction = action
                resumeCall = call
            }
            "call missed" -> {
                val uap = intent.getLongExtra("uap", 0L)
                val ua = UserAgent.ofUap(uap)
                if (ua == null) {
                    Log.w(TAG, "handleIntent did not find ua $uap")
                    return
                }
                spinToAor(ua.account.aor)
                resumeAction = action
            }
            "call transfer", "transfer show", "transfer accept" -> {
                val callp = intent.getLongExtra("callp", 0L)
                val call = Call.ofCallp(callp)
                if (call == null) {
                    Log.w(TAG, "handleIntent '$action' did not find call $callp")
                    moveTaskToBack(true)
                    return
                }
                resumeAction = ev[0]
                resumeCall = call
                resumeUri = if (ev[0] == "call transfer")
                    ev[1]
                else
                    intent.getStringExtra("uri")!!
            }
            "message", "message show", "message reply" -> {
                val uap = intent.getLongExtra("uap", 0L)
                val ua = UserAgent.ofUap(uap)
                if (ua == null) {
                    Log.w(TAG, "handleIntent did not find ua $uap")
                    return
                }
                spinToAor(ua.account.aor)
                resumeAction = action
                resumeUap = uap
                resumeUri = intent.getStringExtra("peer")!!
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val stream = if (am.mode == AudioManager.MODE_RINGTONE)
            AudioManager.STREAM_RING
        else
            AudioManager.STREAM_MUSIC
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                am.adjustStreamVolume(stream,
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                            AudioManager.ADJUST_LOWER else
                            AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI)
                Log.d(TAG, "Adjusted volume $keyCode of stream $stream to ${am.getStreamVolume(stream)}")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleServiceEvent(event: String, params: ArrayList<Any>) {

        fun handleNextEvent(logMessage: String? = null) {
            if (logMessage != null)
                Log.w(TAG, logMessage)
            if (BaresipService.serviceEvents.isNotEmpty()) {
                val first = BaresipService.serviceEvents.removeAt(0)
                handleServiceEvent(first.event, first.params)
            }
        }

        if (taskId == -1) {
            handleNextEvent("Omit service event '$event' for task -1")
            return
        }

        if (event == "started") {
            val uriString = params[0] as String
            Log.d(TAG, "Handling service event 'started' with URI '$uriString'")
            if (!initialized) {
                // Android has restarted baresip when permission has been denied in app settings
                recreate()
                return
            }
            if (uriString != "")
                callAction(uriString.toUri(), "dial")
            else {
                if (viewModel.selectedAor.value == "" && uas.value.isNotEmpty())
                    viewModel.updateSelectedAor(uas.value.first().account.aor)
            }
            if (Preferences(applicationContext).displayTheme != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(Preferences(applicationContext).displayTheme)
            }
            handleNextEvent()
            return
        }

        if (event == "stopped") {
            Log.d(TAG, "Handling service event 'stopped' with start error '${params[0]}'")
            if (params[0] != "") {
                Utils.alertView(this, getString(R.string.notice), getString(R.string.start_failed))
            } else {
                finishAndRemoveTask()
                if (restart)
                    reStart()
                else
                    exitProcess(0)
            }
            return
        }

        val uap = params[0] as Long
        val ua = UserAgent.ofUap(uap)
        if (ua == null) {
            handleNextEvent("handleServiceEvent '$event' did not find ua $uap")
            return
        }

        val ev = event.split(",")
        Log.d(TAG, "Handling service event '${ev[0]}' for $uap")
        val acc = ua.account
        val aor = ua.account.aor

        when (ev[0]) {
            "call rejected" -> {
                if (aor == viewModel.selectedAor.value)
                    callsIcon.intValue = R.drawable.calls_missed
            }
            "call incoming", "call outgoing" -> {
                val callp = params[1] as Long
                if (BaresipService.isMainVisible) {
                    spinToAor(aor)
                    showCall(ua, Call.ofCallp(callp))
                } else {
                    Log.d(TAG, "Reordering to front")
                    BaresipService.activities.clear()
                    BaresipService.serviceEvents.clear()
                    val i = Intent(applicationContext, MainActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    i.putExtra("action", "call show")
                    i.putExtra("callp", callp)
                    startActivity(i)
                    return
                }
            }
            "call answered" -> {
                showCall(ua)
            }
            "call redirect" -> {
                val redirectUri = ev[1]
                val target = Utils.friendlyUri(this, redirectUri, acc)
                if (acc.autoRedirect) {
                    redirect(ua, target)
                    Toast.makeText(applicationContext,
                        String.format(getString(R.string.redirect_notice), target),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    with(MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)) {
                        setTitle(R.string.redirect_request)
                        setMessage(String.format(getString(R.string.redirect_request_query), target))
                        setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                            redirect(ua, target)
                            dialog.dismiss()
                        }
                        setNeutralButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        show()
                    }
                }
                showCall(ua)
            }
            "call established" -> {
                if (aor == viewModel.selectedAor.value) {
                    dtmfText.value = ""
                    showCall(ua)
                }
            }
            "call update" -> {
                showCall(ua)
            }
            "call verify" -> {
                val callp = params[1] as Long
                val call = Call.ofCallp(callp)
                if (call == null) {
                    handleNextEvent("Call $callp to be verified is not found")
                    return
                }
                with(MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)) {
                    setTitle(R.string.verify)
                    setMessage(String.format(getString(R.string.verify_sas), ev[1]))
                    setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                        call.security = if (Api.cmd_exec("zrtp_verify ${ev[2]}") != 0) {
                            Log.e(TAG, "Command 'zrtp_verify ${ev[2]}' failed")
                            R.drawable.locked_yellow
                        } else {
                            R.drawable.locked_green
                        }
                        call.zid = ev[2]
                        if (aor == viewModel.selectedAor.value)
                            securityIcon.intValue = call.security
                        dialog.dismiss()
                    }
                    setNeutralButton(getString(R.string.no)) { dialog, _ ->
                        call.security = R.drawable.locked_yellow
                        call.zid = ev[2]
                        if (aor == viewModel.selectedAor.value)
                            securityIcon.intValue = R.drawable.locked_yellow
                        dialog.dismiss()
                    }
                    show()
                }
            }
            "call verified", "call secure" -> {
                val callp = params[1] as Long
                val call = Call.ofCallp(callp)
                if (call == null) {
                    handleNextEvent("Call $callp that is verified is not found")
                    return
                }
                if (aor == viewModel.selectedAor.value)
                    securityIcon.intValue = call.security
            }
            "call transfer", "transfer show" -> {
                val callp = params[1] as Long
                if (!BaresipService.isMainVisible) {
                    Log.d(TAG, "Reordering to front")
                    BaresipService.activities.clear()
                    BaresipService.serviceEvents.clear()
                    val i = Intent(applicationContext, MainActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    i.putExtra("action", event)
                    i.putExtra("callp", callp)
                    startActivity(i)
                    return
                }
                val call = Call.ofCallp(callp)
                val target = Utils.friendlyUri(this, ev[1], acc)
                with(MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)) {
                    if (call != null) {
                        setTitle(R.string.transfer_request)
                        setMessage(
                            String.format(
                                getString(R.string.transfer_request_query),
                                target
                            )
                        )
                    } else {
                        setTitle(R.string.call_request)
                        setMessage(
                            String.format(
                                getString(R.string.call_request_query),
                                target
                            )
                        )
                    }
                    setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                        if (call in Call.calls())
                            acceptTransfer(ua, call!!, ev[1])
                        else {
                            callUri.value = ev[1]
                            makeCall()
                        }
                        dialog.dismiss()
                    }
                    setNeutralButton(getString(R.string.no)) { dialog, _ ->
                        if (call in Call.calls())
                            call!!.notifySipfrag(603, "Decline")
                        dialog.dismiss()
                    }
                    show()
                }
            }
            "transfer accept" -> {
                val callp = params[1] as Long
                val call = Call.ofCallp(callp)
                if (call in Call.calls())
                    Api.ua_hangup(uap, callp, 0, "")
                call(ua, ev[1])
                showCall(ua)
            }
            "transfer failed" -> {
                showCall(ua)
            }
            "call closed" -> {
                val call = ua.currentCall()
                if (call != null) {
                    call.resume()
                    startCallTimer(call)
                }
                else
                    callTimer?.stop()
                if (aor == viewModel.selectedAor.value) {
                    ua.account.resumeUri = ""
                    showCall(ua)
                    if (acc.missedCalls)
                        callsIcon.intValue = R.drawable.calls_missed
                }
                if (kgm.isDeviceLocked)
                    this.setShowWhenLocked(false)
            }
            "message", "message show", "message reply" -> {
                val i = Intent(applicationContext, ChatActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                val b = Bundle()
                b.putString("aor", aor)
                b.putString("peer", params[1] as String)
                b.putBoolean("focus", ev[0] == "message reply")
                i.putExtras(b)
                chatRequests.launch(i)
            }
            "mwi notify" -> {
                val lines = ev[1].split("\n")
                for (line in lines) {
                    if (line.startsWith("Voice-Message:")) {
                        val counts = (line.split(" ")[1]).split("/")
                        acc.vmNew = counts[0].toInt()
                        acc.vmOld = counts[1].toInt()
                        break
                    }
                }
                if (aor == viewModel.selectedAor.value) {
                    vmIcon.intValue = if (acc.vmNew > 0)
                        R.drawable.voicemail_new
                    else
                        R.drawable.voicemail
                }
            }
            else -> Log.e(TAG, "Unknown event '${ev[0]}'")
        }

        handleNextEvent()
    }

    private fun redirect(ua: UserAgent, redirectUri: String) {
        if (ua.account.aor != viewModel.selectedAor.value)
            spinToAor(ua.account.aor)
        callUri.value = redirectUri
        callClick(this@MainActivity)
    }

    private fun reStart() {
        Log.d(TAG, "Trigger restart")
        val pm = applicationContext.packageManager
        val intent = pm.getLaunchIntentForPackage(this.packageName)
        this.startActivity(intent)
        exitProcess(0)
    }

    @RequiresApi(29)
    private fun pickupFileFromDownloads(action: String) {
        when (action) {
            "backup" -> {
                backupRequest.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_TITLE, "baresip_" +
                            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date()))
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                })
            }
            "restore" -> {
                restoreRequest.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                })
            }
            "logcat" -> {
                logcatRequest.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, "baresip_logcat_" +
                            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(Date()))
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                })
            }
        }
    }

    private fun quitRestart(reStart: Boolean) {
        Log.i(TAG, "quitRestart Restart = $reStart")
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        if (BaresipService.isServiceRunning) {
            restart = reStart
            baresipService.action = "Stop"
            startService(baresipService)
        } else {
            finishAndRemoveTask()
            if (reStart)
                reStart()
            else
                exitProcess(0)
        }
    }

    private fun transfer(ua: UserAgent, uriText: String, attended: Boolean) {
        val uri = if (Utils.isTelUri(uriText))
            Utils.telToSip(uriText, ua.account)
        else
            Utils.uriComplete(uriText, ua.account.aor)
        if (!Utils.checkUri(uri)) {
            Utils.alertView(this@MainActivity, getString(R.string.notice),
                String.format(getString(R.string.invalid_sip_or_tel_uri), uri))
        } else {
            val call = ua.currentCall()
            if (call != null) {
                if (attended) {
                    if (call.hold()) {
                        call.referTo = uri
                        call(ua, uri, call)
                    }
                } else {
                    if (!call.transfer(uri)) {
                        Utils.alertView(this@MainActivity, getString(R.string.notice),
                            String.format(getString(R.string.transfer_failed)))
                    }
                }
                showCall(ua)
            }
        }
    }

    private fun startBaresip() {
       if (!BaresipService.isStartReceived) {
            baresipService.action = "Start"
            startService(baresipService)
            if (atStartup)
                moveTaskToBack(true)
        }
    }

    private fun backup(password: String) {
        val files = arrayListOf("accounts", "config", "contacts", "call_history",
            "messages", "gzrtp.zid", "cert.pem", "ca_cert", "ca_certs.crt")
        File(BaresipService.filesPath).walk().forEach {
            if (it.name.endsWith(".png"))
                files.add(it.name)
        }
        val zipFile = getString(R.string.app_name) + ".zip"
        val zipFilePath = BaresipService.filesPath + "/$zipFile"
        if (!Utils.zip(files, zipFile)) {
            Log.w(TAG, "Failed to write zip file '$zipFile'")
            Utils.alertView(this, getString(R.string.error),
                    String.format(getString(R.string.backup_failed),
                            Utils.fileNameOfUri(applicationContext, downloadsOutputUri!!)))
            downloadsOutputUri = null
            return
        }
        val content = Utils.getFileContents(zipFilePath)
        if (content == null) {
            Log.w(TAG, "Failed to read zip file '$zipFile'")
            Utils.alertView(this, getString(R.string.error),
                    String.format(getString(R.string.backup_failed),
                            Utils.fileNameOfUri(applicationContext, downloadsOutputUri!!)))
            downloadsOutputUri = null
            return
        }
        if (!Utils.encryptToUri(applicationContext, downloadsOutputUri!!, content, password)) {
            Utils.alertView(this, getString(R.string.error),
                    String.format(getString(R.string.backup_failed),
                            Utils.fileNameOfUri(applicationContext, downloadsOutputUri!!)))
            downloadsOutputUri = null
            return
        }
        Utils.alertView(this, getString(R.string.info),
                String.format(getString(R.string.backed_up),
                        Utils.fileNameOfUri(applicationContext, downloadsOutputUri!!)))
        Utils.deleteFile(File(zipFilePath))
        downloadsOutputUri = null
    }

    private fun restore(password: String) {
        val zipFile = getString(R.string.app_name) + ".zip"
        val zipFilePath = BaresipService.filesPath + "/$zipFile"
        val zipData = Utils.decryptFromUri(applicationContext, downloadsInputUri!!, password)
        if (zipData == null) {
            Utils.alertView(
                this, getString(R.string.error),
                String.format(
                    getString(R.string.restore_failed),
                    Utils.fileNameOfUri(applicationContext, downloadsInputUri!!)
                )
            )
            downloadsOutputUri = null
            return
        }
        if (!Utils.putFileContents(zipFilePath, zipData)) {
            Log.w(TAG, "Failed to write zip file '$zipFile'")
            Utils.alertView(
                this, getString(R.string.error),
                String.format(
                    getString(R.string.restore_failed),
                    Utils.fileNameOfUri(applicationContext, downloadsInputUri!!)
                )
            )
            downloadsOutputUri = null
            return
        }
        if (!Utils.unZip(zipFilePath)) {
            Log.w(TAG, "Failed to unzip file '$zipFile'")
            Utils.alertView(
                this, getString(R.string.error),
                String.format(getString(R.string.restore_unzip_failed), "baresip", "60.0.0")
            )
            downloadsOutputUri = null
            return
        }
        Utils.deleteFile(File(zipFilePath))

        File("${BaresipService.filesPath}/recordings").walk().forEach {
            if (it.name.startsWith("dump"))
                Utils.deleteFile(it)
        }

        CallHistoryNew.restore()
        for (h in BaresipService.callHistory)
            h.recording = arrayOf("", "")
        CallHistoryNew.save()

        with(MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)) {
            setTitle(getString(R.string.info))
            setMessage(getString(R.string.restored))
            setPositiveButton(getText(R.string.restart)) { dialog, _ ->
                quitRestart(true)
                dialog.dismiss()
            }
            setNeutralButton(getText(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
        downloadsOutputUri = null
    }

    private fun spinToAor(aor: String) {
        if (aor != viewModel.selectedAor.value)
            viewModel.updateSelectedAor(aor)
        updateIcons(Account.ofAor(aor))
    }

    private fun userAgentofSelectedAor(): UserAgent {
        return UserAgent.ofAor(viewModel.selectedAor.value)!!
    }

    private fun call(ua: UserAgent, uri: String, onHoldCall: Call? = null): Boolean {
        spinToAor(ua.account.aor)
        val callp = ua.callAlloc(0L, Api.VIDMODE_OFF)
        return if (callp != 0L) {
            Log.d(TAG, "Adding outgoing call ${ua.uap}/$callp/$uri")
            val call = Call(callp, ua, uri, "out", "outgoing", null)
            call.onHoldCall = onHoldCall
            call.add()
            if (onHoldCall != null)
                onHoldCall.newCall = call
            if (call.connect(uri)) {
                showCall(ua)
                true
            } else {
                Log.w(TAG, "call_connect $callp failed")
                if (onHoldCall != null)
                    onHoldCall.newCall = null
                call.remove()
                call.destroy()
                showCall(ua)
                false
            }
        } else {
            Log.w(TAG, "callAlloc for ${ua.uap} to $uri failed")
            false
        }
    }

    private fun acceptTransfer(ua: UserAgent, call: Call, uri: String) {
        val newCallp = ua.callAlloc(call.callp, Api.VIDMODE_OFF)
        if (newCallp != 0L) {
            Log.d(TAG, "Adding outgoing call ${ua.uap}/$newCallp/$uri")
            val newCall = Call(newCallp, ua, uri, "out", "transferring", null)
            newCall.add()
            if (newCall.connect(uri)) {
                if (ua.account.aor != viewModel.selectedAor.value)
                    spinToAor(ua.account.aor)
                showCall(ua)
            } else {
                Log.w(TAG, "call_connect $newCallp failed")
                call.notifySipfrag(500, "Call Error")
            }
        } else {
            Log.w(TAG, "callAlloc for ua ${ua.uap} call ${call.callp} transfer failed")
            call.notifySipfrag(500, "Call Error")
        }
    }

    private fun runCall(ua: UserAgent, uri: String) {
        callRunnable = Runnable {
            callRunnable = null
            if (!call(ua, uri)) {
                BaresipService.abandonAudioFocus(applicationContext)
                showCallButton.value = true
                showHangupButton.value = false
            }
        }
        callHandler.postDelayed(callRunnable!!, BaresipService.audioDelay)
    }

    private fun showCall(ua: UserAgent?, showCall: Call? = null) {
        if (ua == null)
            return
        val call = showCall ?: ua.currentCall()
        if (call == null) {
            pullToRefreshEnabled = true
            if (ua.account.resumeUri != "")
                callUri.value = ua.account.resumeUri
            else
                callUri.value = ""
            callUriLabel.value = getString(R.string.outgoing_call_to_dots)
            callUriEnabled.value = true
            keyboardController?.hide()
            showCallTimer.value = false
            securityIcon.intValue = -1
            showHangupButton.value = false
            transferIcon.intValue = R.drawable.call_transfer
            dtmfEnabled.value = false
            focusDtmf.value = false
            showCallButton.value = true
            showAnswerRejectButtons.value = false
            showOnHoldNotice.value = false
            dialpadButtonEnabled = true
            if (BaresipService.isMicMuted) {
                BaresipService.isMicMuted = false
                micIcon = R.drawable.mic_on
            }
        } else {
            pullToRefreshEnabled = false
            callUriEnabled.value = false
            when (call.status) {
                "outgoing", "transferring", "answered" -> {
                    callUriLabel.value = if (call.status == "answered")
                        getString(R.string.incoming_call_from_dots)
                    else
                        getString(R.string.outgoing_call_to_dots)
                    callUri.value = Utils.friendlyUri(this, call.peerUri, ua.account)
                    showCallTimer.value = false
                    securityIcon.intValue = -1
                    showCallButton.value = false
                    showHangupButton.value = true
                    showAnswerRejectButtons.value = false
                    showOnHoldNotice.value = false
                    dialpadButtonEnabled = false
                }
                "incoming" -> {
                    showCallTimer.value = false
                    securityIcon.intValue = -1
                    val uri = call.diverterUri()
                    if (uri != "") {
                        callUriLabel.value = getString(R.string.diverted_by_dots)
                        callUri.value = Utils.friendlyUri(this, uri, ua.account)
                    }
                    else {
                        callUriLabel.value = getString(R.string.incoming_call_from_dots)
                        callUri.value = Utils.friendlyUri(this, call.peerUri, ua.account)
                    }
                    showCallButton.value = false
                    showHangupButton.value = false
                    showAnswerRejectButtons.value = true
                    showOnHoldNotice.value = false
                    dialpadButtonEnabled = false
                }
                "connected" -> {
                    if (call.referTo != "") {
                        callUriLabel.value = getString(R.string.outgoing_call_to_dots)
                        callUri.value = Utils.friendlyUri(this, call.referTo, ua.account)
                        transferButtonEnabled.value = false
                    } else {
                        if (call.dir == "out") {
                            callUriLabel.value = getString(R.string.outgoing_call_to_dots)
                            callUri.value = Utils.friendlyUri(this, call.peerUri, ua.account)
                        } else {
                            callUriLabel.value = getString(R.string.incoming_call_from_dots)
                            callUri.value = Utils.friendlyUri(this, call.peerUri, ua.account)
                        }
                        transferButtonEnabled.value = true
                    }
                    transferIcon.intValue = if (call.onHoldCall == null)
                        R.drawable.call_transfer
                    else
                        R.drawable.call_transfer_execute
                    showCallTimer.value = true
                    startCallTimer(call)
                    if (ua.account.mediaEnc == "")
                        securityIcon.intValue = -1
                    else
                       securityIcon.intValue = call.security
                    showCallButton.value = false
                    showHangupButton.value = true
                    showAnswerRejectButtons.value = false
                    if (call.onhold)
                        holdIcon.intValue = R.drawable.resume
                    else
                        holdIcon.intValue = R.drawable.call_hold
                    Handler(Looper.getMainLooper()).postDelayed({
                        showOnHoldNotice.value = call.held
                    }, 100)
                    if (call.held) {
                        keyboardController?.hide()
                        dtmfEnabled.value = false
                        focusDtmf.value = false
                    } else {
                        dtmfEnabled.value = true
                        focusDtmf.value = true
                        if (resources.configuration.orientation == ORIENTATION_PORTRAIT)
                            keyboardController?.show()
                    }
                }
            }
        }
    }

    private fun restoreActivities() {
        if (BaresipService.activities.isEmpty()) return
        Log.d(TAG, "Activity stack ${BaresipService.activities}")
        val activity = BaresipService.activities[0].split(",")
        BaresipService.activities.removeAt(0)
        when (activity[0]) {
            "main" -> {
                if (!Call.inCall() && (BaresipService.activities.size > 1))
                    restoreActivities()
            }
            "config" -> {
                configRequest.launch(Intent(this, ConfigActivity::class.java))
            }
            "audio" -> {
                startActivity(Intent(this, AudioActivity::class.java))
            }
            "accounts" -> {
                val i = Intent(this, AccountsActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                i.putExtras(b)
                accountsRequest.launch(i)
            }
            "account" -> {
                val i = Intent(this, AccountActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                i.putExtras(b)
                accountsRequest.launch(i)
            }
            "codecs" -> {
                val i = Intent(this, CodecsActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                b.putString("media", activity[2])
                i.putExtras(b)
                startActivity(i)
            }
            "about" -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
            "contacts" -> {
                val i = Intent(this, ContactsActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                i.putExtras(b)
                contactsRequest.launch(i)
            }
            "contact" -> {
                val i = Intent(this, BaresipContactActivity::class.java)
                val b = Bundle()
                if (activity[1] == "true") {
                    b.putBoolean("new", true)
                    b.putString("uri", activity[2])
                } else {
                    b.putBoolean("new", false)
                    b.putInt("index", activity[2].toInt())
                }
                i.putExtras(b)
                startActivity(i)
            }
            "chats" -> {
                val i = Intent(this, ChatsActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                i.putExtras(b)
                chatRequests.launch(i)
            }
            "chat" -> {
                val i = Intent(this, ChatActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                b.putString("peer", activity[2])
                b.putBoolean("focus", activity[3] == "true")
                i.putExtras(b)
                chatRequests.launch(i)
            }
            "calls" -> {
                val i = Intent(this, CallsActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                i.putExtras(b)
                callsRequest.launch(i)
            }
            "call_details" -> {
                val i = Intent(this, CallDetailsActivity::class.java)
                val b = Bundle()
                b.putString("aor", activity[1])
                b.putString("peer", activity[2])
                b.putInt("position", activity[3].toInt())
                i.putExtras(b)
                callsRequest.launch(i)
            }
        }
        return
    }

    private fun saveCallUri() {
        if (uas.value.isNotEmpty() && viewModel.selectedAor.value != "") {
            val ua = UserAgent.ofAor(viewModel.selectedAor.value)!!
            if (ua.calls().isEmpty())
                ua.account.resumeUri = callUri.value
            else
                ua.account.resumeUri = ""
        }
    }

    private fun startCallTimer(call: Call) {
        Handler(Looper.getMainLooper()).postDelayed({
            callTimer?.stop()
            callTimer?.base = SystemClock.elapsedRealtime() - (call.duration() * 1000L)
            callTimer?.start()
        }, 100)
    }

    companion object {
        var activityAor = ""
    }

    init {
        if (!BaresipService.libraryLoaded) {
            Log.d(TAG, "Loading baresip library")
            System.loadLibrary("baresip")
            BaresipService.libraryLoaded = true
        }
    }

}
