package com.ishawahsib.alarm.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishawahsib.alarm.alarm.AlarmRingingService
import com.ishawahsib.alarm.alarm.AlarmScheduler
import com.ishawahsib.alarm.data.AlarmDatabase
import com.ishawahsib.alarm.data.AlarmEntity
import com.ishawahsib.alarm.data.MathDifficulty
import com.ishawahsib.alarm.permissions.PermissionHelper
import com.ishawahsib.alarm.permissions.PermissionStatusReport
import com.ishawahsib.alarm.ui.theme.DawnAmber
import com.ishawahsib.alarm.ui.theme.IshaWahsibTheme
import com.ishawahsib.alarm.ui.theme.MutedText
import com.ishawahsib.alarm.ui.theme.ObsidianBackground
import com.ishawahsib.alarm.ui.theme.SlateSurface
import com.ishawahsib.alarm.ui.theme.SlateSurfaceVariant
import com.ishawahsib.alarm.ui.theme.SoftText
import com.ishawahsib.alarm.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IshaWahsibTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    IshaWahsibHomeScreen()
                }
            }
        }
    }
}

private enum class HomeTab(val title: String) {
    ALARMS("المنبّهات"),
    PERMISSIONS("الصلاحيات")
}

@Composable
private fun IshaWahsibHomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val dao = remember { AlarmDatabase.getInstance(context).alarmDao() }
    val alarms by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var activeTab by remember { mutableStateOf(HomeTab.ALARMS) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var showEditorSheet by remember { mutableStateOf(false) }
    var permissions by remember { mutableStateOf(PermissionHelper.inspect(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissions = PermissionHelper.inspect(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = PermissionHelper.inspect(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = ObsidianBackground,
        floatingActionButton = {
            if (activeTab == HomeTab.ALARMS) {
                FloatingActionButton(
                    onClick = {
                        editingAlarm = null
                        showEditorSheet = true
                    },
                    containerColor = DawnAmber,
                    contentColor = Color(0xFF111827)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة منبّه")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = SlateSurface) {
                NavigationBarItem(
                    selected = activeTab == HomeTab.ALARMS,
                    onClick = { activeTab = HomeTab.ALARMS },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                    label = { Text(HomeTab.ALARMS.title) }
                )
                NavigationBarItem(
                    selected = activeTab == HomeTab.PERMISSIONS,
                    onClick = { activeTab = HomeTab.PERMISSIONS },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text(HomeTab.PERMISSIONS.title) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "اصحى واحسب",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SoftText
            )
            Text(
                text = "منبّه ذكي يعمل في الخلفية ولا يتوقف إلا بحل مسألة رياضيات",
                fontSize = 13.sp,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!permissions.allCriticalGranted && activeTab == HomeTab.ALARMS) {
                PermissionWarningBanner(
                    onOpenPermissions = { activeTab = HomeTab.PERMISSIONS }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (activeTab) {
                HomeTab.ALARMS -> {
                    AlarmsListSection(
                        alarms = alarms,
                        onToggle = { alarm, enabled ->
                            scope.launch(Dispatchers.IO) {
                                val updated = alarm.copy(isEnabled = enabled)
                                dao.update(updated)
                                if (enabled) {
                                    AlarmScheduler.schedule(context, updated)
                                } else {
                                    AlarmScheduler.cancel(context, updated.id)
                                }
                            }
                        },
                        onEdit = { alarm ->
                            editingAlarm = alarm
                            showEditorSheet = true
                        },
                        onDelete = { alarm ->
                            scope.launch(Dispatchers.IO) {
                                AlarmScheduler.cancel(context, alarm.id)
                                dao.delete(alarm)
                            }
                        },
                        onTestRing = { alarm ->
                            val intent = Intent(context, AlarmRingingService::class.java).apply {
                                action = AlarmRingingService.ACTION_TEST_RING
                                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                            }
                            ContextCompat.startForegroundService(context, intent)
                        }
                    )
                }

                HomeTab.PERMISSIONS -> {
                    PermissionsCenterSection(
                        report = permissions,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onRequestExactAlarm = {
                            context.startActivity(PermissionHelper.createExactAlarmSettingsIntent(context))
                        },
                        onRequestFullScreen = {
                            context.startActivity(PermissionHelper.createFullScreenIntentSettings(context))
                        },
                        onRequestBatteryExemption = {
                            context.startActivity(PermissionHelper.createBatteryOptimizationIntent(context))
                        }
                    )
                }
            }
        }
    }

    if (showEditorSheet) {
        AlarmEditorBottomSheet(
            initialAlarm = editingAlarm,
            onDismiss = { showEditorSheet = false },
            onSave = { savedAlarm ->
                showEditorSheet = false
                scope.launch(Dispatchers.IO) {
                    val assignedId = if (savedAlarm.id == 0) {
                        dao.insert(savedAlarm).toInt()
                    } else {
                        dao.update(savedAlarm)
                        savedAlarm.id
                    }
                    val persisted = savedAlarm.copy(id = assignedId)
                    if (persisted.isEnabled) {
                        AlarmScheduler.schedule(context, persisted)
                    } else {
                        AlarmScheduler.cancel(context, persisted.id)
                    }
                }
            }
        )
    }
}

@Composable
private fun PermissionWarningBanner(onOpenPermissions: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF451A03), RoundedCornerShape(16.dp))
            .border(1.dp, DawnAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onOpenPermissions() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = DawnAmber)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "تفعيل صلاحيات الخلفية مطلوب",
                color = SoftText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "اضغط لمنح صلاحية المنبّهات الدقيقة والإشعارات فوق شاشة القفل.",
                color = Color(0xFFFDE68A),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AlarmsListSection(
    alarms: List<AlarmEntity>,
    onToggle: (AlarmEntity, Boolean) -> Unit,
    onEdit: (AlarmEntity) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
    onTestRing: (AlarmEntity) -> Unit
) {
    if (alarms.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد منبّهات مضافة بعد",
                    color = SoftText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اضغط على زر (+) بالأسفل لإنشاء منبّهك الذكي الأول.",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                onToggle = { enabled -> onToggle(alarm, enabled) },
                onEdit = { onEdit(alarm) },
                onDelete = { onDelete(alarm) },
                onTestRing = { onTestRing(alarm) }
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestRing: () -> Unit
) {
    val formattedTime = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
            .clickable { onEdit() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = formattedTime,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) SoftText else MutedText
                )
                Text(
                    text = alarm.label.ifBlank { "منبّه الاستيقاظ" },
                    fontSize = 14.sp,
                    color = if (alarm.isEnabled) DawnAmber else MutedText
                )
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF111827),
                    checkedTrackColor = DawnAmber
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${formatDaysSummary(alarm.repeatDaysMask)} · رياضيات: ${alarm.difficulty.arabicLabel} (${alarm.problemCount} مسائل)",
            fontSize = 12.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onTestRing,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DawnAmber
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("اختبار الرنين الآن", color = SoftText, fontSize = 12.sp)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف المنبّه", tint = MutedText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorBottomSheet(
    initialAlarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val timeState = rememberTimePickerState(
        initialHour = initialAlarm?.hour ?: 6,
        initialMinute = initialAlarm?.minute ?: 30,
        is24Hour = true
    )
    var label by remember { mutableStateOf(initialAlarm?.label ?: "منبّه الاستيقاظ") }
    var repeatMask by remember { mutableIntStateOf(initialAlarm?.repeatDaysMask ?: 0) }
    var difficulty by remember { mutableStateOf(initialAlarm?.difficulty ?: MathDifficulty.MEDIUM) }
    var problemCount by remember { mutableIntStateOf(initialAlarm?.problemCount ?: 2) }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var gradualVolume by remember { mutableStateOf(initialAlarm?.gradualVolume ?: true) }
    var ringtoneUri by remember { mutableStateOf(initialAlarm?.ringtoneUri) }
    var ringtoneDisplayName by remember(ringtoneUri) {
        mutableStateOf(resolveRingtoneLabel(context, ringtoneUri))
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            ringtoneUri = picked?.toString()
            ringtoneDisplayName = resolveRingtoneLabel(context, ringtoneUri)
        }
    }

    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            ringtoneUri = uri.toString()
            ringtoneDisplayName = resolveRingtoneLabel(context, ringtoneUri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SlateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialAlarm == null) "إضافة منبّه جديد" else "تعديل المنبّه",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SoftText
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("تسمية المنبّه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // أيام التكرار
            Text("أيام التكرار", color = MutedText, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf(
                    Calendar.SATURDAY to "س",
                    Calendar.SUNDAY to "ح",
                    Calendar.MONDAY to "ن",
                    Calendar.TUESDAY to "ث",
                    Calendar.WEDNESDAY to "ر",
                    Calendar.THURSDAY to "خ",
                    Calendar.FRIDAY to "ج"
                )
                for ((calDay, shortName) in days) {
                    val bit = 1 shl (calDay - 1)
                    val selected = (repeatMask and bit) != 0
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) DawnAmber else SlateSurfaceVariant)
                            .clickable {
                                repeatMask = if (selected) (repeatMask and bit.inv()) else (repeatMask or bit)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shortName,
                            color = if (selected) Color(0xFF111827) else SoftText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // مستوى صعوبة مسألة الرياضيات
            Text("مستوى تحدي الرياضيات", color = MutedText, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (level in MathDifficulty.entries) {
                    val selected = difficulty == level
                    Button(
                        onClick = { difficulty = level },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) DawnAmber else SlateSurfaceVariant,
                            contentColor = if (selected) Color(0xFF111827) else SoftText
                        )
                    ) {
                        Text(level.arabicLabel, fontSize = 13.sp)
                    }
                }
            }

            // عدد المسائل المطلوبة للإيقاف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("عدد المسائل المطلوبة للإيقاف", color = SoftText, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { count ->
                        val selected = problemCount == count
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) DawnAmber else SlateSurfaceVariant)
                                .clickable { problemCount = count },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                color = if (selected) Color(0xFF111827) else SoftText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // اختيار نغمة الرنين (من النظام أو ملف صوتي خاص من الهاتف)
            Text("نغمة المنبّه: $ringtoneDisplayName", color = MutedText, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة المنبّه")
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                ringtoneUri?.let { Uri.parse(it) }
                            )
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = DawnAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("نغمات النظام", color = SoftText, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        customAudioLauncher.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = DawnAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("ملف من جهازي", color = SoftText, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اهتزاز مستمر", color = SoftText, fontSize = 14.sp)
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("رفع تدريجي للصوت (20 ثانية)", color = SoftText, fontSize = 14.sp)
                Switch(checked = gradualVolume, onCheckedChange = { gradualVolume = it })
            }

            Button(
                onClick = {
                    val alarm = AlarmEntity(
                        id = initialAlarm?.id ?: 0,
                        hour = timeState.hour,
                        minute = timeState.minute,
                        label = label,
                        isEnabled = true,
                        repeatDaysMask = repeatMask,
                        difficulty = difficulty,
                        problemCount = problemCount,
                        ringtoneUri = ringtoneUri,
                        vibrate = vibrate,
                        gradualVolume = gradualVolume
                    )
                    onSave(alarm)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawnAmber,
                    contentColor = Color(0xFF111827)
                )
            ) {
                Text("حفظ وجدولة المنبّه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionsCenterSection(
    report: PermissionStatusReport,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            PermissionStatusCard(
                title = "المنبّهات الدقيقة (Exact Alarms)",
                description = "ضرورية لعمل AlarmManager.setAlarmClock حتى في وضع السكون العميق وبعد إغلاق التطبيق.",
                granted = report.exactAlarmGranted,
                actionLabel = "تفعيل من الإعدادات",
                onAction = onRequestExactAlarm
            )
        }
        item {
            PermissionStatusCard(
                title = "إذن الإشعارات (Android 13+)",
                description = "مطلوب لتشغيل إشعار خدمة الرنين الأمامية وإيقاظ الشاشة.",
                granted = report.notificationsGranted,
                actionLabel = "منح الإذن",
                onAction = onRequestNotifications
            )
        }
        item {
            PermissionStatusCard(
                title = "العرض بملء الشاشة على القفل (Full Screen Intent)",
                description = "يفتح تحدي الرياضيات مباشرة فوق شاشة القفل عند الرنين.",
                granted = report.fullScreenIntentGranted,
                actionLabel = "فتح إعدادات العرض",
                onAction = onRequestFullScreen
            )
        }
        item {
            PermissionStatusCard(
                title = "الاستثناء من تحسين البطارية",
                description = "يمنع واجهات الشركات (مثل Xiaomi وSamsung) من إيقاف الخدمة أو تأخير الرنين.",
                granted = report.batteryOptimizationIgnored,
                actionLabel = "استثناء التطبيق",
                onAction = onRequestBatteryExemption
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = SoftText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) SuccessGreen else DawnAmber
            )
        }
        Text(text = description, color = MutedText, fontSize = 12.sp)
        if (!granted) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawnAmber,
                    contentColor = Color(0xFF111827)
                )
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text("مفعّلة وجاهزة", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatDaysSummary(mask: Int): String {
    if (mask == 0) return "مرة واحدة"
    if (mask == 127) return "كل يوم"
    val ordered = listOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الاثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )
    return ordered
        .filter { (calDay, _) -> (mask and (1 shl (calDay - 1))) != 0 }
        .joinToString("، ") { it.second }
}

private fun resolveRingtoneLabel(context: android.content.Context, uriString: String?): String {
    if (uriString.isNullOrBlank()) return "نغمة النظام الافتراضية"
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return "نغمة مخصصة"

    // أولاً محاولة جلب اسم الملف إذا كان مختاراً من وحدة التخزين
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
    }

    // ثانياً جلب عنوان النغمة من RingtoneManager
    return runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: "نغمة مخصصة"
}
