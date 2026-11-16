package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.assistant.InstalledAppInfo
import com.example.settings.AssistantSettings
import com.example.settings.AuraThemeOption
import com.example.settings.VoiceOption
import com.example.ui.theme.ActionAmber
import com.example.ui.theme.ActionEmerald
import com.example.ui.theme.ActionSky
import com.example.ui.theme.ActionViolet
import com.example.ui.theme.themePreviewSwatches
import kotlin.math.roundToInt

private data class GuideCommandExample(
    val category: String,
    val command: String,
    val description: String,
    val icon: ImageVector,
    val tint: androidx.compose.ui.graphics.Color
)

private val GuideExamples = listOf(
    GuideCommandExample(
        category = "Alarm",
        command = "Morning me 6:30 baje ka alarm laga do",
        description = "Subah, shaam ya exact time ke saath alarm set karein",
        icon = Icons.Rounded.Alarm,
        tint = ActionAmber
    ),
    GuideCommandExample(
        category = "Timer",
        command = "10 minute ka timer lagao",
        description = "Minutes, seconds ya ghante ka countdown timer shuru karein",
        icon = Icons.Rounded.Timer,
        tint = ActionAmber
    ),
    GuideCommandExample(
        category = "WhatsApp / Business",
        command = "WhatsApp Business pe Veera ko hello message kar do",
        description = "WhatsApp ya WhatsApp Business par pre-filled message bhejein",
        icon = Icons.Rounded.ChatBubbleOutline,
        tint = ActionEmerald
    ),
    GuideCommandExample(
        category = "Phone Call",
        command = "Rahul ko call karo",
        description = "Contacts mein number dhoondhkar turant dial karein",
        icon = Icons.Rounded.Call,
        tint = ActionEmerald
    ),
    GuideCommandExample(
        category = "SMS",
        command = "Veera ko SMS bhejo main aa raha hoon",
        description = "Messages app mein contact aur message ready karein",
        icon = Icons.Rounded.Sms,
        tint = ActionSky
    ),
    GuideCommandExample(
        category = "Maps",
        command = "Maps pe India Gate dikhao",
        description = "Google Maps par location ya navigation kholein",
        icon = Icons.Rounded.Map,
        tint = ActionEmerald
    ),
    GuideCommandExample(
        category = "Music / Media",
        command = "Kishore Kumar ka koi accha old song play karo",
        description = "Spotify, YT Music ya Chrome YouTube par gaana chalayein",
        icon = Icons.Rounded.MusicNote,
        tint = ActionViolet
    ),
    GuideCommandExample(
        category = "Open Any App",
        command = "Instagram kholo",
        description = "Phone mein installed kisi bhi app ko turant launch karein",
        icon = Icons.Rounded.Apps,
        tint = ActionViolet
    ),
    GuideCommandExample(
        category = "Device Control",
        command = "Flashlight on karo",
        description = "Flashlight, Volume, Back, Home, Scroll, Tap & Type control karein",
        icon = Icons.Rounded.Settings,
        tint = ActionAmber
    ),
    GuideCommandExample(
        category = "Settings",
        command = "WiFi settings kholo",
        description = "Wi-Fi, Bluetooth ya Camera turant kholein",
        icon = Icons.Rounded.Settings,
        tint = ActionAmber
    ),
    GuideCommandExample(
        category = "Web Search",
        command = "Search karo best biryani near me",
        description = "Google ya browser par seedha web search karein",
        icon = Icons.Rounded.Search,
        tint = ActionSky
    ),
    GuideCommandExample(
        category = "Gemini AI",
        command = "Mujhe 3 healthy breakfast ideas batao",
        description = "Koi bhi Hinglish ya English sawaal ChatGPT ki tarah poochiye",
        icon = Icons.Rounded.AutoAwesome,
        tint = ActionAmber
    )
)

private val NamePresets = listOf("AuraMate", "Aura", "Jarvis", "Priya", "Dost")

@Composable
fun CommandsAndAppsSheetContent(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    installedApps: List<InstalledAppInfo>,
    hasContactsPermission: Boolean,
    hasMicPermission: Boolean,
    hasNotificationsPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onGrantAllRuntimePermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSearchInPlayStore: (String) -> Unit,
    onLaunchInstalledApp: (InstalledAppInfo) -> Unit,
    onTryCommand: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.commands_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.commands_sheet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                modifier = Modifier.testTag("tab_all_apps"),
                text = {
                    Text(
                        text = "Sabhi Apps (${installedApps.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                modifier = Modifier.testTag("tab_commands_guide"),
                text = {
                    Text(
                        text = "Commands & Perms",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            AllInstalledAppsTab(
                installedApps = installedApps,
                onSearchInPlayStore = onSearchInPlayStore,
                onLaunchInstalledApp = onLaunchInstalledApp
            )
        } else {
            CommandsGuideTab(
                hasContactsPermission = hasContactsPermission,
                hasMicPermission = hasMicPermission,
                hasNotificationsPermission = hasNotificationsPermission,
                hasAccessibilityPermission = hasAccessibilityPermission,
                onGrantAllRuntimePermissions = onGrantAllRuntimePermissions,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onTryCommand = onTryCommand
            )
        }
    }
}

@Composable
private fun AllInstalledAppsTab(
    installedApps: List<InstalledAppInfo>,
    onSearchInPlayStore: (String) -> Unit,
    onLaunchInstalledApp: (InstalledAppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null
                )
            },
            placeholder = {
                Text(
                    text = "App search karein (WhatsApp Business, Instagram…)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field")
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Is device par ${installedApps.size} installed apps detected hain. WhatsApp Business / Instagram jaise apps phone mein install hote hi yahan apne aap dikhenge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        if (filteredApps.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Is device par koi launcher app nahi mila."
                        } else {
                            "\"$searchQuery\" abhi is device par install nahi hai."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Phone mein jab yeh app install hoga, turant yahan list mein dikhega aur voice command se khulega.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotBlank()) {
                        Button(
                            onClick = { onSearchInPlayStore(searchQuery) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Play Store par dekhein")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    InstalledAppRow(
                        app = app,
                        onClick = { onLaunchInstalledApp(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledAppRow(
    app: InstalledAppInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val iconBitmap: ImageBitmap? = remember(app.packageName) {
        runCatching {
            context.packageManager
                .getApplicationIcon(app.packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("installed_app_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = "${app.label} icon",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "\"${app.label} kholo\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Open ${app.label}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CommandsGuideTab(
    hasContactsPermission: Boolean,
    hasMicPermission: Boolean,
    hasNotificationsPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onGrantAllRuntimePermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onTryCommand: (String) -> Unit
) {
    val allRuntimeGranted = hasContactsPermission && hasMicPermission && hasNotificationsPermission

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp,
                if (allRuntimeGranted && hasAccessibilityPermission) {
                    ActionEmerald.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = ActionEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Permissions & Full Device Access",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                PermissionStatusRow(label = "Contacts (Call / WhatsApp / SMS)", granted = hasContactsPermission)
                PermissionStatusRow(label = "Microphone (Voice Commands)", granted = hasMicPermission)
                PermissionStatusRow(label = "Notifications (Alerts)", granted = hasNotificationsPermission)
                PermissionStatusRow(label = "Screen Control (Back / Scroll / Tap)", granted = hasAccessibilityPermission)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!allRuntimeGranted) {
                        Button(
                            onClick = onGrantAllRuntimePermissions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ActionEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("sheet_grant_contacts_button")
                        ) {
                            Text(
                                text = "Allow Permissions",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenAccessibilitySettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sheet_enable_accessibility_button")
                    ) {
                        Text(
                            text = if (hasAccessibilityPermission) "Screen Control Active" else "Enable Screen Control",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        GuideExamples.forEachIndexed { index, example ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTryCommand(example.command) }
                    .testTag("guide_example_$index")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(example.tint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = example.icon,
                            contentDescription = null,
                            tint = example.tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = example.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = example.tint,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"${example.command}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = example.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizeSheetContent(
    settings: AssistantSettings,
    availableVoices: List<VoiceOption>,
    onSaveAssistantName: (String) -> Unit,
    onSelectTheme: (AuraThemeOption) -> Unit,
    onSelectVoice: (String?) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onToggleVoiceLocale: () -> Unit,
    onPreviewVoice: () -> Unit
) {
    var nameDraft by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.customize_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.customize_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Custom Assistant Name
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Assistant Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { if (it.length <= 24) nameDraft = it },
                        singleLine = true,
                        placeholder = { Text("Jaise: Jarvis, Priya, Aura") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_name_input")
                    )
                    Button(
                        onClick = { onSaveAssistantName(nameDraft) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_assistant_name_button")
                    ) {
                        Text("Save")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NamePresets.forEach { preset ->
                        val selected = settings.assistantName.equals(preset, ignoreCase = true)
                        Surface(
                            onClick = {
                                nameDraft = preset
                                onSaveAssistantName(preset)
                            },
                            shape = RoundedCornerShape(50),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: App Theme Switcher
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AuraThemeOption.entries.forEach { option ->
                    val isSelected = settings.themeOption == option
                    val swatches = themePreviewSwatches(option)
                    Surface(
                        onClick = { onSelectTheme(option) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_option_${option.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                swatches.forEach { swatch ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(swatch)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Voice & Speech Settings
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Voice & Awaaz",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = onPreviewVoice,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("preview_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Voice", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Auto-speak switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Speak Jawab",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Har reply ko apne aap bolkar sunaye",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isAutoSpeakEnabled,
                        onCheckedChange = { onToggleAutoSpeak() }
                    )
                }

                // Recognition locale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mic Speech Language",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        onClick = onToggleVoiceLocale,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = if (settings.voiceLocaleTag == "hi-IN") "Hindi (HI)" else "English (EN)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Device TTS Voice Picker
                Text(
                    text = "Voice Style (${availableVoices.size} device voices)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val defaultSelected = settings.selectedVoiceName == null
                    VoiceChoiceChip(
                        label = "Default Hindi/IN",
                        selected = defaultSelected,
                        onClick = { onSelectVoice(null) }
                    )
                    availableVoices.forEach { voice ->
                        VoiceChoiceChip(
                            label = voice.displayLabel,
                            selected = settings.selectedVoiceName == voice.name,
                            onClick = { onSelectVoice(voice.name) }
                        )
                    }
                }

                // Pitch slider + quick presets
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Voice Pitch (Awaaz Tone)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(settings.voicePitch * 100).roundToInt() / 100f}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = onPitchChanged,
                        valueRange = 0.65f..1.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PitchPresetChip("Deep", 0.78f, settings.voicePitch, onPitchChanged)
                        PitchPresetChip("Natural", 1.0f, settings.voicePitch, onPitchChanged)
                        PitchPresetChip("Bright", 1.22f, settings.voicePitch, onPitchChanged)
                    }
                }

                // Speed slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bolne ki Speed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(settings.voiceSpeed * 100).roundToInt() / 100f}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = onSpeedChanged,
                        valueRange = 0.75f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PitchPresetChip(
    label: String,
    value: Float,
    currentPitch: Float,
    onSelect: (Float) -> Unit
) {
    val selected = kotlin.math.abs(currentPitch - value) < 0.06f
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (granted) "Granted" else "Needed",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) ActionEmerald else MaterialTheme.colorScheme.primary
        )
    }
}

