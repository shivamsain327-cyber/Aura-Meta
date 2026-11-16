package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.assistant.ActionCategory
import com.example.assistant.InstalledAppInfo
import com.example.settings.AuraThemeOption
import com.example.ui.theme.ActionAmber
import com.example.ui.theme.ActionEmerald
import com.example.ui.theme.ActionSky
import com.example.ui.theme.ActionViolet

private val QuickChips = listOf(
    "Subah 6 baje ka alarm laga do",
    "5 minute ka timer lagao",
    "YouTube kholo",
    "Koi accha hindi song play karo",
    "Search karo weather today"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraMateScreen(
    uiState: AuraMateUiState,
    snackbarHostState: SnackbarHostState,
    onInputChanged: (String) -> Unit,
    onSend: (String) -> Unit,
    onStartVoiceInput: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onToggleVoiceLocale: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onClearChat: () -> Unit,
    onRetryMessage: (String) -> Unit,
    onOpenCommandsSheet: (Int) -> Unit,
    onSelectCommandsTab: (Int) -> Unit,
    onCloseCommandsSheet: () -> Unit,
    onOpenCustomizeSheet: () -> Unit,
    onCloseCustomizeSheet: () -> Unit,
    onSaveAssistantName: (String) -> Unit,
    onSelectTheme: (AuraThemeOption) -> Unit,
    onSelectVoice: (String?) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onPreviewVoice: () -> Unit,
    onLaunchInstalledApp: (InstalledAppInfo) -> Unit,
    onSearchInPlayStore: (String) -> Unit,
    onGrantContactsClick: () -> Unit,
    onDismissContactsBanner: () -> Unit,
    onGrantAllRuntimePermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onDismissAccessibilityBanner: () -> Unit
) {
    val listState = rememberLazyListState()
    val primaryGlow = MaterialTheme.colorScheme.primary
    val tertiaryGlow = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.14f),
                        radius = size.width * 0.72f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryGlow.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.78f),
                        radius = size.width * 0.65f
                    )
                )
            }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                AuraTopBar(
                    assistantName = uiState.assistantName,
                    isThinking = uiState.isThinking,
                    isSpeaking = uiState.isSpeaking,
                    isAutoSpeakEnabled = uiState.isAutoSpeakEnabled,
                    voiceLocaleTag = uiState.voiceLocaleTag,
                    onToggleAutoSpeak = onToggleAutoSpeak,
                    onOpenAppsSheet = { onOpenCommandsSheet(0) },
                    onOpenCustomizeSheet = onOpenCustomizeSheet,
                    onClearChat = onClearChat
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AuraComposerBar(
                        inputText = uiState.inputText,
                        isSpeaking = uiState.isSpeaking,
                        onInputChanged = onInputChanged,
                        onSend = { onSend(uiState.inputText) },
                        onMicClick = onStartVoiceInput,
                        onStopSpeaking = onStopSpeaking,
                        modifier = Modifier.widthIn(max = 720.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                ) {
                    AnimatedVisibility(
                        visible = uiState.showContactsBanner,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ContactsPermissionBanner(
                            onGrantClick = onGrantContactsClick,
                            onDismissClick = onDismissContactsBanner,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.showAccessibilityBanner,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        AccessibilityPermissionBanner(
                            onEnableClick = onOpenAccessibilitySettings,
                            onDismissClick = onDismissAccessibilityBanner,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.messages.size <= 1) {
                            item(key = "hero_welcome") {
                                AuraHeroHeader(
                                    assistantName = uiState.assistantName,
                                    installedAppsCount = uiState.installedApps.size,
                                    isSpeaking = uiState.isSpeaking,
                                    isThinking = uiState.isThinking,
                                    onOpenAllApps = { onOpenCommandsSheet(0) },
                                    onOpenCustomize = onOpenCustomizeSheet,
                                    onQuickChipClick = { chip -> onSend(chip) }
                                )
                            }
                        }

                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            ChatMessageBubble(
                                message = message,
                                onSpeakClick = { onSpeakMessage(message.text) },
                                onRetryClick = { onRetryMessage(message.id) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showCommandsSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onCloseCommandsSheet,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                CommandsAndAppsSheetContent(
                    selectedTab = uiState.commandsSheetTab,
                    onSelectTab = onSelectCommandsTab,
                    installedApps = uiState.installedApps,
                    hasContactsPermission = uiState.hasContactsPermission,
                    hasMicPermission = uiState.hasMicPermission,
                    hasNotificationsPermission = uiState.hasNotificationsPermission,
                    hasAccessibilityPermission = uiState.hasAccessibilityPermission,
                    onGrantAllRuntimePermissions = onGrantAllRuntimePermissions,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onSearchInPlayStore = onSearchInPlayStore,
                    onLaunchInstalledApp = onLaunchInstalledApp,
                    onTryCommand = { cmd ->
                        onCloseCommandsSheet()
                        onSend(cmd)
                    }
                )
            }
        }

        if (uiState.showCustomizeSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onCloseCustomizeSheet,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                CustomizeSheetContent(
                    settings = uiState.settings,
                    availableVoices = uiState.availableVoices,
                    onSaveAssistantName = onSaveAssistantName,
                    onSelectTheme = onSelectTheme,
                    onSelectVoice = onSelectVoice,
                    onPitchChanged = onPitchChanged,
                    onSpeedChanged = onSpeedChanged,
                    onToggleAutoSpeak = onToggleAutoSpeak,
                    onToggleVoiceLocale = onToggleVoiceLocale,
                    onPreviewVoice = onPreviewVoice
                )
            }
        }
    }
}

@Composable
private fun AuraTopBar(
    assistantName: String,
    isThinking: Boolean,
    isSpeaking: Boolean,
    isAutoSpeakEnabled: Boolean,
    voiceLocaleTag: String,
    onToggleAutoSpeak: () -> Unit,
    onOpenAppsSheet: () -> Unit,
    onOpenCustomizeSheet: () -> Unit,
    onClearChat: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.aura_orb_avatar_1794652297969),
                    contentDescription = stringResource(R.string.cd_aura_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assistantName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("topbar_assistant_name")
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dotColor = when {
                            isThinking -> MaterialTheme.colorScheme.primary
                            isSpeaking -> ActionEmerald
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        val langBadge = if (voiceLocaleTag == "hi-IN") "HI" else "EN"
                        val statusText = when {
                            isThinking -> stringResource(R.string.subtitle_thinking)
                            isSpeaking -> stringResource(R.string.subtitle_speaking)
                            else -> "Ready • $langBadge"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onOpenAppsSheet,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("commands_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = stringResource(R.string.cd_commands_guide),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onOpenCustomizeSheet,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("customize_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.cd_customize_settings),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onToggleAutoSpeak,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("voice_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isAutoSpeakEnabled) {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        } else {
                            Icons.AutoMirrored.Outlined.VolumeOff
                        },
                        contentDescription = stringResource(R.string.cd_voice_toggle),
                        tint = if (isAutoSpeakEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_clear_chat),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AuraHeroHeader(
    assistantName: String,
    installedAppsCount: Int,
    isSpeaking: Boolean,
    isThinking: Boolean,
    onOpenAllApps: () -> Unit,
    onOpenCustomize: () -> Unit,
    onQuickChipClick: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isThinking || isSpeaking) 900 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size((118 * pulseScale).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Image(
                painter = painterResource(id = R.drawable.aura_orb_avatar_1794652297969),
                contentDescription = stringResource(R.string.cd_aura_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Namaste! Main $assistantName hoon",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Direct discovery actions for All Apps & Customization
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            Surface(
                onClick = onOpenAllApps,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("hero_all_apps_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (installedAppsCount > 0) "Sabhi Apps ($installedAppsCount)" else "Sabhi Apps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Surface(
                onClick = onOpenCustomize,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("hero_customize_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Name • Voice • Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickChips.forEachIndexed { index, prompt ->
                Surface(
                    onClick = { onQuickChipClick(prompt) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("quick_chip_$index")
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onSpeakClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 6.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else {
            val borderColor = if (message.isError) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            val containerColor = if (message.isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 6.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                tonalElevation = 2.dp,
                modifier = Modifier.widthIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (message.actionCategory != null) {
                        ActionOutcomeBadge(
                            category = message.actionCategory,
                            detail = message.actionDetail.orEmpty(),
                            executed = message.actionExecuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (message.isError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else if (message.isStreaming && message.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.subtitle_thinking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = if (message.isStreaming) "${message.text} ▍" else message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!message.isStreaming && message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (message.isError && message.retryPrompt != null) {
                                TextButton(
                                    onClick = onRetryClick,
                                    modifier = Modifier.testTag("retry_message_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.retry_label),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onSpeakClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = stringResource(R.string.cd_replay_voice),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
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

@Composable
private fun ActionOutcomeBadge(
    category: ActionCategory,
    detail: String,
    executed: Boolean
) {
    val (icon, tint) = when (category) {
        ActionCategory.ALARM -> Icons.Rounded.Alarm to ActionAmber
        ActionCategory.TIMER -> Icons.Rounded.Timer to MaterialTheme.colorScheme.secondary
        ActionCategory.WHATSAPP -> Icons.Rounded.ChatBubbleOutline to ActionEmerald
        ActionCategory.CALL -> Icons.Rounded.Call to ActionEmerald
        ActionCategory.SMS -> Icons.Rounded.Sms to ActionSky
        ActionCategory.MAPS -> Icons.Rounded.Map to ActionEmerald
        ActionCategory.MEDIA -> Icons.Rounded.MusicNote to ActionViolet
        ActionCategory.SETTINGS -> Icons.Rounded.Settings to ActionAmber
        ActionCategory.OPEN_APP -> Icons.Rounded.Apps to ActionViolet
        ActionCategory.WEB_SEARCH -> Icons.Rounded.Search to ActionSky
        ActionCategory.DEVICE -> Icons.Rounded.Tune to ActionAmber
        ActionCategory.EMAIL -> Icons.Rounded.Sms to ActionSky
        ActionCategory.SHARE -> Icons.Rounded.ChatBubbleOutline to ActionViolet
        ActionCategory.CALENDAR -> Icons.Rounded.Alarm to ActionEmerald
    }
    val badgeTint = if (executed) tint else MaterialTheme.colorScheme.error

    Surface(
        shape = RoundedCornerShape(50),
        color = badgeTint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeTint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (detail.isNotBlank()) "${category.label} • $detail" else category.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = badgeTint
            )
        }
    }
}

@Composable
private fun ContactsPermissionBanner(
    onGrantClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, ActionEmerald.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Contacts,
                contentDescription = null,
                tint = ActionEmerald,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.contacts_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.contacts_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionEmerald,
                    contentColor = Color(0xFF042217)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("grant_contacts_button")
            ) {
                Text(
                    text = stringResource(R.string.contacts_grant_action),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccessibilityPermissionBanner(
    onEnableClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Screen Control Permission",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Back, Home, Scroll, Tap & Type commands ke liye Accessibility enable karein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onEnableClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("enable_accessibility_banner_button")
            ) {
                Text(
                    text = "Enable",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuraComposerBar(
    inputText: String,
    isSpeaking: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.composer_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (inputText.isNotBlank()) onSend() }
                )
            )

            if (isSpeaking && inputText.isBlank()) {
                FilledIconButton(
                    onClick = onStopSpeaking,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("stop_speaking_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = stringResource(R.string.cd_stop_speaking)
                    )
                }
            }

            if (inputText.isNotBlank()) {
                FilledIconButton(
                    onClick = onSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cd_send_button)
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onMicClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = stringResource(R.string.cd_mic_button)
                    )
                }
            }
        }
    }
}
