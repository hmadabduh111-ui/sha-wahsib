package com.ishawahsib.alarm.ui

import android.app.KeyguardManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishawahsib.alarm.alarm.AlarmRingingService
import com.ishawahsib.alarm.data.MathDifficulty
import com.ishawahsib.alarm.math.MathChallengeGenerator
import com.ishawahsib.alarm.ui.theme.DangerRed
import com.ishawahsib.alarm.ui.theme.DawnAmber
import com.ishawahsib.alarm.ui.theme.IshaWahsibTheme
import com.ishawahsib.alarm.ui.theme.MutedText
import com.ishawahsib.alarm.ui.theme.ObsidianBackground
import com.ishawahsib.alarm.ui.theme.SlateSurface
import com.ishawahsib.alarm.ui.theme.SlateSurfaceVariant
import com.ishawahsib.alarm.ui.theme.SoftText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AlarmLockScreenActivity : ComponentActivity() {

    private var isSolved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إظهار النشاط فوق شاشة القفل وإيقاظ الشاشة على Android 9 وما فوق
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // منع زر الرجوع من إغلاق شاشة التحدي قبل الحل الصحيح
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // تجاهل مقصود حتى يتم حل المسألة
            }
        })

        val label = intent.getStringExtra(AlarmRingingService.EXTRA_ALARM_LABEL) ?: "منبّه الاستيقاظ"
        val difficultyName = intent.getStringExtra(AlarmRingingService.EXTRA_DIFFICULTY)
        val difficulty = runCatching {
            MathDifficulty.valueOf(difficultyName ?: MathDifficulty.MEDIUM.name)
        }.getOrDefault(MathDifficulty.MEDIUM)
        val problemCount = intent.getIntExtra(AlarmRingingService.EXTRA_PROBLEM_COUNT, 2).coerceIn(1, 5)

        setContent {
            IshaWahsibTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LockScreenMathChallenge(
                        alarmLabel = label,
                        difficulty = difficulty,
                        totalProblems = problemCount,
                        onAllSolved = {
                            isSolved = true
                            dismissAlarmAndFinish()
                        }
                    )
                }
            }
        }
    }

    private fun dismissAlarmAndFinish() {
        startService(AlarmRingingService.createSolvedDismissIntent(this))
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
        finishAndRemoveTask()
    }
}

@Composable
private fun LockScreenMathChallenge(
    alarmLabel: String,
    difficulty: MathDifficulty,
    totalProblems: Int,
    onAllSolved: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(1) }
    var currentProblem by remember { mutableStateOf(MathChallengeGenerator.generate(difficulty)) }
    var userInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentTimeText by remember { mutableStateOf("") }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.US)
        while (true) {
            currentTimeText = formatter.format(Date())
            delay(1000L)
        }
    }

    fun submitAnswer() {
        val parsed = userInput.toIntOrNull()
        if (parsed == null) {
            errorMessage = "أدخل الناتج أولاً"
            return
        }
        if (parsed == currentProblem.answer) {
            errorMessage = null
            userInput = ""
            if (currentIndex >= totalProblems) {
                onAllSolved()
            } else {
                currentIndex += 1
                currentProblem = MathChallengeGenerator.generate(difficulty)
            }
        } else {
            errorMessage = "إجابة غير صحيحة — حاول مجدداً لإيقاف الرنين"
            userInput = ""
            scope.launch {
                val values = listOf(-18f, 18f, -12f, 12f, -6f, 0f)
                for (v in values) {
                    shakeOffset.animateTo(v, animationSpec = tween(45))
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // الجزء العلوي: الوقت الحالي واسم المنبه
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = DawnAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = alarmLabel,
                        color = DawnAmber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTimeText,
                    color = SoftText,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "المسألة $currentIndex من $totalProblems · المستوى: ${difficulty.arabicLabel}",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }

            // بطاقة المسألة الرياضية وشاشة الإدخال
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .background(SlateSurface, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = if (errorMessage != null) DangerRed else Color(0xFF334155),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "حلّ المعادلة لإيقاف المنبّه",
                    color = MutedText,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // اتجاه LTR للمعادلة الرياضية فقط لضمان قراءة الأرقام والعمليات بوضوح
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = currentProblem.expression,
                        color = SoftText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(ObsidianBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInput.ifEmpty { "؟" },
                        color = if (userInput.isEmpty()) MutedText else DawnAmber,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = DangerRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // لوحة أرقام كبيرة مخصصة للاستخدام السريع عند الاستيقاظ
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (digit in row) {
                                KeypadButton(
                                    label = digit,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (userInput.length < 6) {
                                            userInput += digit
                                            errorMessage = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (userInput.isNotEmpty()) {
                                    userInput = userInput.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateSurfaceVariant,
                                contentColor = SoftText
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "مسح"
                            )
                        }

                        KeypadButton(
                            label = "0",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (userInput.length < 6) {
                                    userInput += "0"
                                    errorMessage = null
                                }
                            }
                        )

                        Button(
                            onClick = { submitAnswer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DawnAmber,
                                contentColor = Color(0xFF111827)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تأكيد الإجابة"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SlateSurface,
            contentColor = SoftText
        )
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
