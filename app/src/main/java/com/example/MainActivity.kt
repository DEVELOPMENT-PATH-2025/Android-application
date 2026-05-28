package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.annotation.Keep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.ClassSchedule
import com.example.ui.ClassScheduleViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityText
import com.example.ui.theme.HighDensityPrimary
import com.example.ui.theme.HighDensityPrimaryContainer
import com.example.ui.theme.HighDensityOnPrimaryContainer
import com.example.ui.theme.HighDensitySurfaceVariant
import com.example.ui.theme.HighDensitySecondaryText
import com.example.ui.theme.HighDensityAlertRed
import kotlinx.coroutines.delay
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val viewModel: ClassScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ClassScheduleViewModel) {
    val context = LocalContext.current
    val schedules by viewModel.allSchedules.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedScheduleForDetails by remember { mutableStateOf<ClassSchedule?>(null) }

    // State variables for form input (Creation Dialog)
    var classNameInput by remember { mutableStateOf("") }
    var classNumberInput by remember { mutableStateOf("") }
    var startHourInput by remember { mutableIntStateOf(12) }
    var startMinuteInput by remember { mutableIntStateOf(10) }
    var mondayRecur by remember { mutableStateOf(true) }
    var tuesdayRecur by remember { mutableStateOf(true) }
    var wednesdayRecur by remember { mutableStateOf(true) }
    var thursdayRecur by remember { mutableStateOf(true) }
    var fridayRecur by remember { mutableStateOf(true) }
    var saturdayRecur by remember { mutableStateOf(false) }
    var sundayRecur by remember { mutableStateOf(false) }
    var alertSoundInput by remember { mutableStateOf("Default Deep Pulse") }
    var c20MinInput by remember { mutableStateOf(true) }
    var c10MinInput by remember { mutableStateOf(true) }

    // Sound Options
    val soundOptions = listOf(
        "Default Deep Pulse",
        "Beryl Radar Pulse",
        "Chime Echo",
        "Digital Warning Buzz"
    )

    // Request permissions launcher
    val registerNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled for upcoming classes", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please enable notifications to receive warning alerts", Toast.LENGTH_LONG).show()
        }
    }

    // Trigger permission requests on start
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                registerNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Identify current/main upcoming class
    val activeTrackingClass = remember(schedules) {
        schedules.firstOrNull { it.isEnabled } ?: schedules.firstOrNull()
    }

    // Navigation View Switcher (Main vs Details View)
    AnimatedContent(
        targetState = selectedScheduleForDetails,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "nav_transition"
    ) { targetSchedule ->
        if (targetSchedule != null) {
            // Display 'Class Details' Screen
            ClassDetailsScreen(
                schedule = targetSchedule,
                viewModel = viewModel,
                soundOptions = soundOptions,
                onBack = { selectedScheduleForDetails = null }
            )
        } else {
            // Display main schedules dashboard
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Schedule Launcher Logo",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Faculty Timer",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 21.sp,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Faculty Timer: 20 min and 10 min pre-class warning alerts. Choose sounds, snooze, and view countdowns in the Class Details screen.", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu Options",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            // Populate initial default states for creation dialog
                            classNameInput = ""
                            classNumberInput = ""
                            startHourInput = 12
                            startMinuteInput = 10
                            mondayRecur = true
                            tuesdayRecur = true
                            wednesdayRecur = true
                            thursdayRecur = true
                            fridayRecur = true
                            saturdayRecur = false
                            sundayRecur = false
                            alertSoundInput = "Default Deep Pulse"
                            c20MinInput = true
                            c10MinInput = true
                            showAddDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .testTag("add_class_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Schedule",
                            modifier = Modifier.size(24.dp)
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
                    // MAIN DASHBOARD AREA
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ACTIVE CLASS TRACKING CARD
                        item {
                            if (activeTrackingClass != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            RoundedCornerShape(28.dp)
                                        )
                                        .clickable { selectedScheduleForDetails = activeTrackingClass }
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "CURRENT TRACKING",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Text(
                                                text = activeTrackingClass.className,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 22.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Class ID: ${activeTrackingClass.classNumber} • Repeats: ${
                                                    activeTrackingClass.getRecurrentDaysList().joinToString(", ").ifEmpty { "Today Only" }
                                                }",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.5f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = String.format("%02d:%02d %s", 
                                                    if (activeTrackingClass.startHour % 12 == 0) 12 else activeTrackingClass.startHour % 12,
                                                    activeTrackingClass.startMinute,
                                                    if (activeTrackingClass.startHour >= 12) "PM" else "AM"
                                                ),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Alert Schedule breakdown subcard
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "Active Alert indicator",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Sound Warning Schedule",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (activeTrackingClass.isEnabled) "ACTIVE" else "MUTED",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 20-min offset warning card
                                            if (activeTrackingClass.custom20MinEnabled) {
                                                val time20 = Calendar.getInstance().apply {
                                                    set(Calendar.HOUR_OF_DAY, activeTrackingClass.startHour)
                                                    set(Calendar.MINUTE, activeTrackingClass.startMinute)
                                                    add(Calendar.MINUTE, -20)
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.White)
                                                        .border(
                                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(10.dp)
                                                ) {
                                                    Text(
                                                        text = "-20 MINS PRE-ALERT",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                    Text(
                                                        text = String.format("%02d:%02d %s",
                                                            if (time20.get(Calendar.HOUR_OF_DAY) % 12 == 0) 12 else time20.get(Calendar.HOUR_OF_DAY) % 12,
                                                            time20.get(Calendar.MINUTE),
                                                            if (time20.get(Calendar.HOUR_OF_DAY) >= 12) "PM" else "AM"
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = "Speaker Icon",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "15s Sound Playing",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.White.copy(alpha = 0.5f))
                                                        .padding(10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("-20m Muted", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                            }

                                            // 10-min offset warning card
                                            if (activeTrackingClass.custom10MinEnabled) {
                                                val time10 = Calendar.getInstance().apply {
                                                    set(Calendar.HOUR_OF_DAY, activeTrackingClass.startHour)
                                                    set(Calendar.MINUTE, activeTrackingClass.startMinute)
                                                    add(Calendar.MINUTE, -10)
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.White)
                                                        .border(
                                                            BorderStroke(1.dp, HighDensityAlertRed.copy(alpha = 0.3f)),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(10.dp)
                                                ) {
                                                    Text(
                                                        text = "-10 MINS URGENT",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = HighDensityAlertRed
                                                    )
                                                    Text(
                                                        text = String.format("%02d:%02d %s",
                                                            if (time10.get(Calendar.HOUR_OF_DAY) % 12 == 0) 12 else time10.get(Calendar.HOUR_OF_DAY) % 12,
                                                            time10.get(Calendar.MINUTE),
                                                            if (time10.get(Calendar.HOUR_OF_DAY) >= 12) "PM" else "AM"
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = "Speaker Icon",
                                                            tint = HighDensityAlertRed,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "30s Sound Playing",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = HighDensityAlertRed,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.White.copy(alpha = 0.5f))
                                                        .padding(10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("-10m Muted", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Selected Alert Sound: ${activeTrackingClass.alertSound}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Empty current tracking placeholder
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationAdd,
                                            contentDescription = "No active tracks",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "No Class Schedules Added Yet",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Tap the '+' button down below to add your classes and automatically configure deep warning tones prior to lectures.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // INTENT-COMPLIANT INSTANT SIMULATOR / TESTING DECK
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "INSTANT DEEP WARNING SIMULATOR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "Audio-audition any of the synthesized alarm sounds immediately with pre-configured settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                var simulatorSoundSelection by remember { mutableStateOf("Default Deep Pulse") }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Sound:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Box(modifier = Modifier.weight(1f)) {
                                        var showSimDropdown by remember { mutableStateOf(false) }
                                        Text(
                                            text = simulatorSoundSelection,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clickable { showSimDropdown = true }
                                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                        DropdownMenu(expanded = showSimDropdown, onDismissRequest = { showSimDropdown = false }) {
                                            soundOptions.forEach { sOpt ->
                                                DropdownMenuItem(
                                                    text = { Text(sOpt) },
                                                    onClick = {
                                                        simulatorSoundSelection = sOpt
                                                        showSimDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.triggerInstantPreview(minutesBefore = 20, durationSeconds = 15, alertSound = simulatorSoundSelection)
                                            Toast.makeText(context, "Playing 15s pre-alert: $simulatorSoundSelection", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("preview_20_button"),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play"
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "20-Min Warning",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Play 15 Sec",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.triggerInstantPreview(minutesBefore = 10, durationSeconds = 30, alertSound = simulatorSoundSelection)
                                            Toast.makeText(context, "Playing 30s pre-alert: $simulatorSoundSelection", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HighDensityAlertRed
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("preview_10_button"),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play"
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "10-Min Warning",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Play 30 Sec",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // UPCOMING CHRONO RECURRENCE LIST
                        item {
                            Text(
                                text = "UPCOMING CLASS TIMETABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (schedules.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Your timetable is currently empty.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(schedules, key = { it.id }) { itemSchedule ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { selectedScheduleForDetails = itemSchedule }
                                        .testTag("schedule_card_${itemSchedule.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Time Badge Block
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp, 54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = String.format("%02d:%02d", 
                                                            if (itemSchedule.startHour % 12 == 0) 12 else itemSchedule.startHour % 12,
                                                            itemSchedule.startMinute
                                                        ),
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = if (itemSchedule.startHour >= 12) "PM" else "AM",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Class Information description
                                            Column {
                                                Text(
                                                    text = itemSchedule.className,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "ID: ${itemSchedule.classNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                
                                                // Day Recurrence Dots list
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    DayDot(label = "M", active = itemSchedule.recurMonday)
                                                    DayDot(label = "T", active = itemSchedule.recurTuesday)
                                                    DayDot(label = "W", active = itemSchedule.recurWednesday)
                                                    DayDot(label = "T", active = itemSchedule.recurThursday)
                                                    DayDot(label = "F", active = itemSchedule.recurFriday)
                                                    DayDot(label = "S", active = itemSchedule.recurSaturday)
                                                    DayDot(label = "S", active = itemSchedule.recurSunday)
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Switch(
                                                checked = itemSchedule.isEnabled,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.toggleSchedule(itemSchedule, isChecked)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                ),
                                                modifier = Modifier.scale(0.8f).testTag("schedule_switch_${itemSchedule.id}")
                                            )

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteSchedule(itemSchedule)
                                                    Toast.makeText(context, "Class schedule deleted.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("delete_btn_${itemSchedule.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete class",
                                                    tint = HighDensityAlertRed.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // FORM DIALOG FOR CREATING A NEW CLASS SCHEDULE
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Schedule Class Session",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    OutlinedTextField(
                        value = classNameInput,
                        onValueChange = { classNameInput = it },
                        label = { Text("Class Name (e.g. Advanced Algorithms)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_class_name")
                    )

                    OutlinedTextField(
                        value = classNumberInput,
                        onValueChange = { classNumberInput = it },
                        label = { Text("Class Number/Room (e.g. CS-402, Room 305B)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_class_number")
                    )

                    // CUSTOM TIME SELECTOR CHIPS & DIAL
                    Text(
                        text = "Time Setup (Hour & Minute)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hour setup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("Hour: %02d (%s)", 
                                    if (startHourInput == 0 || startHourInput == 12) 12 else startHourInput % 12,
                                    if (startHourInput >= 12) "PM" else "AM"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = { if (startHourInput > 0) startHourInput-- else startHourInput = 23 },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Remove, "Decrement Hour", modifier = Modifier.size(14.dp))
                                }
                                FilledIconButton(
                                    onClick = { if (startHourInput < 23) startHourInput++ else startHourInput = 0 },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, "Increment Hour", modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Minute setup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("Minute: %02d", startMinuteInput),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = { if (startMinuteInput > 0) startMinuteInput-- else startMinuteInput = 59 },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Remove, "Decrement Minute", modifier = Modifier.size(14.dp))
                                }
                                FilledIconButton(
                                    onClick = { if (startMinuteInput < 59) startMinuteInput++ else startMinuteInput = 0 },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, "Increment Minute", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // WEEKDAY RECURRENCE SELECTOR
                    Text(
                        text = "Recurrence Days",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RecurToggleBtn(label = "M", checked = mondayRecur, onToggle = { mondayRecur = it })
                        RecurToggleBtn(label = "T", checked = tuesdayRecur, onToggle = { tuesdayRecur = it })
                        RecurToggleBtn(label = "W", checked = wednesdayRecur, onToggle = { wednesdayRecur = it })
                        RecurToggleBtn(label = "T", checked = thursdayRecur, onToggle = { thursdayRecur = it })
                        RecurToggleBtn(label = "F", checked = fridayRecur, onToggle = { fridayRecur = it })
                        RecurToggleBtn(label = "S", checked = saturdayRecur, onToggle = { saturdayRecur = it })
                        RecurToggleBtn(label = "S", checked = sundayRecur, onToggle = { sundayRecur = it })
                    }

                    // NEW DETAILED PREFERENCE CONFIGURATORS
                    Text(
                        text = "Alert Preferences",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sound Group:", style = MaterialTheme.typography.bodySmall)
                        Box {
                            Text(
                                text = alertSoundInput,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { dropdownExpanded = true }
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                soundOptions.forEach { soundName ->
                                    DropdownMenuItem(
                                        text = { Text(soundName) },
                                        onClick = {
                                            alertSoundInput = soundName
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Include -20 Min Pre-Alert (15s Deep Sound)", style = MaterialTheme.typography.bodySmall)
                        Checkbox(
                            checked = c20MinInput,
                            onCheckedChange = { c20MinInput = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Include -10 Min Urgent Alert (30s Deep Sound)", style = MaterialTheme.typography.bodySmall)
                        Checkbox(
                            checked = c10MinInput,
                            onCheckedChange = { c10MinInput = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (classNameInput.isBlank()) {
                                    Toast.makeText(context, "Please write a class name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val actualClassNum = classNumberInput.ifBlank { "N/A" }
                                
                                viewModel.addSchedule(
                                    className = classNameInput,
                                    classNumber = actualClassNum,
                                    startHour = startHourInput,
                                    startMinute = startMinuteInput,
                                    recurMonday = mondayRecur,
                                    recurTuesday = tuesdayRecur,
                                    recurWednesday = wednesdayRecur,
                                    recurThursday = thursdayRecur,
                                    recurFriday = fridayRecur,
                                    recurSaturday = saturdayRecur,
                                    recurSunday = sundayRecur,
                                    alertSound = alertSoundInput,
                                    custom20MinEnabled = c20MinInput,
                                    custom10MinEnabled = c10MinInput
                                )
                                Toast.makeText(context, "Class Scheduled successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("save_class_btn")
                        ) {
                            Text("Schedule")
                        }
                    }
                }
            }
        }
    }
}

// CLASS DETAILS SCREEN WITH HIGH CONTRAST CIRCULAR COUNTDOWN TIMER & PREFERRED SOUND SELECTION
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsScreen(
    schedule: ClassSchedule,
    viewModel: ClassScheduleViewModel,
    soundOptions: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Local states to handle edits
    var editedName by remember { mutableStateOf(schedule.className) }
    var editedNumber by remember { mutableStateOf(schedule.classNumber) }
    var editedHour by remember { mutableIntStateOf(schedule.startHour) }
    var editedMinute by remember { mutableIntStateOf(schedule.startMinute) }
    
    var mRecur by remember { mutableStateOf(schedule.recurMonday) }
    var tRecur by remember { mutableStateOf(schedule.recurTuesday) }
    var wRecur by remember { mutableStateOf(schedule.recurWednesday) }
    var thRecur by remember { mutableStateOf(schedule.recurThursday) }
    var fRecur by remember { mutableStateOf(schedule.recurFriday) }
    var sRecur by remember { mutableStateOf(schedule.recurSaturday) }
    var suRecur by remember { mutableStateOf(schedule.recurSunday) }

    var chosenSound by remember { mutableStateOf(schedule.alertSound) }
    var c20Enabled by remember { mutableStateOf(schedule.custom20MinEnabled) }
    var c10Enabled by remember { mutableStateOf(schedule.custom10MinEnabled) }

    // REAL-TIME CIRCULAR COUNTDOWN TIMER CALC ENGINE
    var remainingTimeMs by remember { mutableStateOf(0L) }

    LaunchedEffect(schedule, editedHour, editedMinute, mRecur, tRecur, wRecur, thRecur, fRecur, sRecur, suRecur) {
        val dummySchedule = schedule.copy(
            startHour = editedHour,
            startMinute = editedMinute,
            recurMonday = mRecur,
            recurTuesday = tRecur,
            recurWednesday = wRecur,
            recurThursday = thRecur,
            recurFriday = fRecur,
            recurSaturday = sRecur,
            recurSunday = suRecur
        )
        while (true) {
            val nextTime = calculateNextOccurrenceMs(editedHour, editedMinute, dummySchedule)
            val diff = nextTime - System.currentTimeMillis()
            remainingTimeMs = maxOf(0L, diff)
            delay(1000)
        }
    }

    val totalSecs = remainingTimeMs / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    val tickingFraction = (seconds % 60) / 60f

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                title = {
                    Text("Class Alert Specifications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteSchedule(schedule)
                        Toast.makeText(context, "Class schedule deleted.", Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Schedule",
                            tint = HighDensityAlertRed
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DYNAMIC CIRCULAR COUNTDOWN TIMER DISPLAY (HIGH DENSITY RADIAL DESIGN)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("circular_timer_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TIME TO CLASS START TIME",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(160.dp)
                        ) {
                            // Circular Ring Canvas
                            val ringColor = MaterialTheme.colorScheme.primary
                            Canvas(modifier = Modifier.size(140.dp)) {
                                // Draw background ring
                                drawCircle(
                                    color = ringColor.copy(alpha = 0.15f),
                                    style = Stroke(width = 8.dp.toPx())
                                )
                                // Draw live ticking sweep ring based on active timer seconds countdown
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * (1f - tickingFraction),
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            // Time remaining central digit counter
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format("%02dh %02dm", hours, minutes),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = String.format("%02ds", seconds),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = HighDensityAlertRed
                                )
                                Text(
                                    text = "ticking...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // CLASS BASICS CONFIGURATION CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Class Particulars", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Class Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_class_name")
                        )

                        OutlinedTextField(
                            value = editedNumber,
                            onValueChange = { editedNumber = it },
                            label = { Text("Class Number / Room ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_class_number")
                        )
                    }
                }
            }

            // CLASS TIME CLOCK SETUP CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Clock Time Configuration", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("Hour: %02d (%s)", 
                                    if (editedHour == 0 || editedHour == 12) 12 else editedHour % 12,
                                    if (editedHour >= 12) "PM" else "AM"
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = { if (editedHour > 0) editedHour-- else editedHour = 23 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, "Dec Hr")
                                }
                                FilledIconButton(
                                    onClick = { if (editedHour < 23) editedHour++ else editedHour = 0 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Inc Hr")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("Minute: %02d", editedMinute),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = { if (editedMinute > 0) editedMinute-- else editedMinute = 59 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, "Dec Min")
                                }
                                FilledIconButton(
                                    onClick = { if (editedMinute < 59) editedMinute++ else editedMinute = 0 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Inc Min")
                                }
                            }
                        }
                    }
                }
            }

            // DAY RECURRENCE SETTINGS SPECIFICATION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Day Recurrence Cycles", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RecurToggleBtn(label = "M", checked = mRecur, onToggle = { mRecur = it })
                            RecurToggleBtn(label = "T", checked = tRecur, onToggle = { tRecur = it })
                            RecurToggleBtn(label = "W", checked = wRecur, onToggle = { wRecur = it })
                            RecurToggleBtn(label = "T", checked = thRecur, onToggle = { thRecur = it })
                            RecurToggleBtn(label = "F", checked = fRecur, onToggle = { fRecur = it })
                            RecurToggleBtn(label = "S", checked = sRecur, onToggle = { sRecur = it })
                            RecurToggleBtn(label = "S", checked = suRecur, onToggle = { suRecur = it })
                        }
                    }
                }
            }

            // PREFERRED ALARM SOUND PREFERENCES COLUMN & PREVIEW ACTIONS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Tone Sound Profile Preference", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Choose your synthesized deep pulse audio tone. Use the preview triggers below to pre-hear any of the generated sounds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        soundOptions.forEach { soundOpt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (chosenSound == soundOpt) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { chosenSound = soundOpt }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RadioButton(
                                        selected = chosenSound == soundOpt,
                                        onClick = { chosenSound = soundOpt }
                                    )
                                    Text(text = soundOpt, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                }

                                // Play Preview Auditing Button
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.triggerInstantPreview(minutesBefore = 20, durationSeconds = 5, alertSound = soundOpt)
                                        Toast.makeText(context, "Playing preview of '$soundOpt'...", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Listen", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                }
                            }
                        }
                    }
                }
            }

            // CUSTOM WARNING THRESHOLD ACTIVATOR CHECKS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Interactive Warnings Setup", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("-20 Mins Warning Alert", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("Synth beep pulses for 15 seconds.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Switch(
                                checked = c20Enabled,
                                onCheckedChange = { c20Enabled = it }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("-10 Mins Warning Alert", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("Urgent beep pulses for 30 seconds.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Switch(
                                checked = c10Enabled,
                                onCheckedChange = { c10Enabled = it }
                            )
                        }
                    }
                }
            }

            // SAVE UPDATE & CANCEL CTA FOOTER
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Discard")
                    }

                    Button(
                        onClick = {
                            if (editedName.isBlank()) {
                                Toast.makeText(context, "Class name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val updatedClass = schedule.copy(
                                className = editedName,
                                classNumber = editedNumber.ifBlank { "N/A" },
                                startHour = editedHour,
                                startMinute = editedMinute,
                                recurMonday = mRecur,
                                recurTuesday = tRecur,
                                recurWednesday = wRecur,
                                recurThursday = thRecur,
                                recurFriday = fRecur,
                                recurSaturday = sRecur,
                                recurSunday = suRecur,
                                alertSound = chosenSound,
                                custom20MinEnabled = c20Enabled,
                                custom10MinEnabled = c10Enabled
                            )
                            viewModel.updateSchedule(updatedClass)
                            Toast.makeText(context, "Class schedules updated successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.weight(1f).testTag("save_details_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

// Math logic to calculate next occurrence of the class alert start time
fun calculateNextOccurrenceMs(startHour: Int, startMinute: Int, schedule: ClassSchedule): Long {
    val now = Calendar.getInstance()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, startHour)
        set(Calendar.MINUTE, startMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val recurDays = mutableListOf<Int>()
    if (schedule.recurSunday) recurDays.add(Calendar.SUNDAY)
    if (schedule.recurMonday) recurDays.add(Calendar.MONDAY)
    if (schedule.recurTuesday) recurDays.add(Calendar.TUESDAY)
    if (schedule.recurWednesday) recurDays.add(Calendar.WEDNESDAY)
    if (schedule.recurThursday) recurDays.add(Calendar.THURSDAY)
    if (schedule.recurFriday) recurDays.add(Calendar.FRIDAY)
    if (schedule.recurSaturday) recurDays.add(Calendar.SATURDAY)

    if (recurDays.isEmpty()) {
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    var nextTriggerMs = Long.MAX_VALUE
    for (day in recurDays) {
        val testCal = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.DAY_OF_WEEK, day)
        }
        if (testCal.before(now)) {
            testCal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        if (testCal.timeInMillis < nextTriggerMs) {
            nextTriggerMs = testCal.timeInMillis
        }
    }

    return nextTriggerMs
}

@Composable
fun DayDot(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .size(15.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (active) Color.White else Color.Gray
        )
    }
}

@Composable
fun RecurToggleBtn(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (checked) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                ),
                shape = CircleShape
            )
            .clickable { onToggle(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
