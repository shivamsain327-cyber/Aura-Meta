package com.example.assistant

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.ai.AiActionPlan
import com.example.ai.AiActionType

enum class ActionCategory(val label: String) {
    ALARM("Alarm"),
    TIMER("Timer"),
    WHATSAPP("WhatsApp"),
    CALL("Phone Call"),
    SMS("SMS"),
    MAPS("Maps"),
    MEDIA("Media"),
    SETTINGS("Settings"),
    OPEN_APP("App Launch"),
    WEB_SEARCH("Web Search"),
    DEVICE("Device Control"),
    EMAIL("Email"),
    SHARE("Share"),
    CALENDAR("Calendar")
}

data class CommandOutcome(
    val reply: String,
    val category: ActionCategory,
    val detail: String,
    val executed: Boolean = true,
    val needsContactsPermission: Boolean = false,
    val needsAccessibilityPermission: Boolean = false,
    val pendingIntents: List<Intent> = emptyList(),
    val failureReply: String? = null
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val componentClassName: String
)

/**
 * Turns explicit Hinglish/English commands AND Gemini-planned [AiActionPlan]s
 * into real Android device action Intents across all installed apps.
 */
class CommandHandler(private val ctx: Context) {

    private val knownPackages = mapOf(
        "whatsapp business" to "com.whatsapp.w4b",
        "wa business" to "com.whatsapp.w4b",
        "business whatsapp" to "com.whatsapp.w4b",
        "whatsapp" to "com.whatsapp",
        "youtube music" to "com.google.android.apps.youtube.music",
        "yt music" to "com.google.android.apps.youtube.music",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "instagram" to "com.instagram.android",
        "insta" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "facebook" to "com.facebook.katana",
        "fb" to "com.facebook.katana",
        "snapchat" to "com.snapchat.android",
        "snap" to "com.snapchat.android",
        "spotify" to "com.spotify.music",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "photos" to "com.google.android.apps.photos",
        "calendar" to "com.google.android.calendar",
        "play store" to "com.android.vending",
        "settings" to "com.android.settings",
        "camera" to "com.android.camera2",
        "clock" to "com.android.deskclock",
        "calculator" to "com.android.calculator",
        "phone" to "com.google.android.dialer",
        "dialer" to "com.google.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "google pay" to "com.google.android.apps.nbu.paisa.user",
        "amazon" to "in.amazon.mShop.android.shopping",
        "flipkart" to "com.flipkart.android",
        "netflix" to "com.netflix.mediaclient",
        "truecaller" to "com.truecaller",
        "twitter" to "com.twitter.android",
        "jiosaavn" to "com.jio.media.jiobeats",
        "mx player" to "com.mxtech.videoplayer.ad"
    )

    private val typoAliases = mapOf(
        "whatsap" to "whatsapp",
        "watsapp" to "whatsapp",
        "bussiness" to "business",
        "buisness" to "business",
        "busines" to "business",
        "instgram" to "instagram",
        "instagrm" to "instagram",
        "youtbe" to "youtube",
        "utube" to "youtube",
        "teligram" to "telegram"
    )

    private fun queryLauncherActivities(): List<ResolveInfo> {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcher,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
        }
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = ctx.packageManager
        val selfPkg = ctx.packageName
        val seenPackages = HashSet<String>()
        return queryLauncherActivities()
            .asSequence()
            .filter { info ->
                val pkg = info.activityInfo.packageName
                pkg != selfPkg && seenPackages.add(pkg)
            }
            .map { info ->
                InstalledAppInfo(
                    label = info.loadLabel(pm).toString().trim().ifEmpty { info.activityInfo.packageName },
                    packageName = info.activityInfo.packageName,
                    componentClassName = info.activityInfo.name
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun buildLaunchIntentsForInstalledApp(app: InstalledAppInfo): List<Intent> {
        val componentIntent = Intent.makeMainActivity(
            ComponentName(app.packageName, app.componentClassName)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val pkgIntent = ctx.packageManager.getLaunchIntentForPackage(app.packageName)
        return listOfNotNull(componentIntent, pkgIntent)
    }

    fun buildPlayStoreIntents(appOrQuery: String): List<Intent> {
        val cleaned = normalizeQueryText(appOrQuery).trim()
        val matchedPkg = knownPackages[cleaned]
            ?: knownPackages.entries.sortedByDescending { it.key.length }
                .firstOrNull { cleaned.contains(it.key) }?.value
        return if (matchedPkg != null) {
            listOf(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$matchedPkg")),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$matchedPkg"))
            )
        } else {
            val encoded = Uri.encode(appOrQuery.trim())
            listOf(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded&c=apps")),
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps"))
            )
        }
    }

    private val fillerTokens = setOf(
        "my", "the", "a", "an", "app", "application",
        "mera", "meri", "mere", "please", "pls", "plz",
        "zara", "jara", "bhai", "yaar", "toh", "ko", "kar", "do"
    )

    fun handle(raw: String): CommandOutcome? {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()
        return alarm(lower)
            ?: timer(lower)
            ?: directDeviceControl(lower, trimmed)
            ?: phoneCall(trimmed)
            ?: smsMessage(trimmed)
            ?: whatsapp(trimmed)
            ?: mapsNavigation(trimmed)
            ?: mediaPlay(trimmed, lower)
            ?: systemSettings(lower)
            ?: youtubeInChrome(trimmed, lower)
            ?: youtubeSearch(trimmed)
            ?: openApp(lower)
            ?: webSearch(trimmed)
    }

    /**
     * Converts a Gemini-planned [AiActionPlan] into an executable [CommandOutcome],
     * or null when [AiActionPlan.action] is [AiActionType.NONE].
     */
    fun resolveAiPlan(plan: AiActionPlan): CommandOutcome? {
        return when (plan.action) {
            AiActionType.NONE -> null

            AiActionType.PLAY_MEDIA -> {
                val q = plan.query?.takeIf { it.isNotBlank() } ?: "Trending Hindi Songs"
                val appHint = plan.appName?.lowercase().orEmpty()
                buildPlayMediaOutcome(
                    query = q,
                    appHint = appHint,
                    replyOverride = plan.reply
                )
            }

            AiActionType.SET_ALARM -> {
                val h = plan.hour?.coerceIn(0, 23)
                val min = (plan.minute ?: 0).coerceIn(0, 59)
                if (h == null) {
                    CommandOutcome(
                        reply = plan.reply,
                        category = ActionCategory.ALARM,
                        detail = "Time missing",
                        executed = false
                    )
                } else {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, h)
                        .putExtra(AlarmClock.EXTRA_MINUTES, min)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, plan.query ?: "AuraMate")
                    val formatted = "%02d:%02d".format(h, min)
                    CommandOutcome(
                        reply = plan.reply,
                        category = ActionCategory.ALARM,
                        detail = formatted,
                        executed = true,
                        pendingIntents = listOf(intent),
                        failureReply = "Is device par Alarm app nahi khul paaya."
                    )
                }
            }

            AiActionType.SET_TIMER -> {
                val secs = (plan.seconds ?: 0).coerceAtLeast(1)
                val label = when {
                    secs >= 3600 && secs % 3600 == 0 -> "${secs / 3600} hr"
                    secs >= 60 && secs % 60 == 0 -> "${secs / 60} min"
                    else -> "$secs sec"
                }
                val intent = Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, secs)
                CommandOutcome(
                    reply = plan.reply,
                    category = ActionCategory.TIMER,
                    detail = label,
                    executed = true,
                    pendingIntents = listOf(intent),
                    failureReply = "Is device par Timer app nahi khul paaya."
                )
            }

            AiActionType.OPEN_APP -> {
                val target = (plan.appName ?: plan.query).orEmpty().trim()
                if (target.isEmpty()) return null
                buildOpenAppOutcome(target, replyOverride = plan.reply)
                    ?: CommandOutcome(
                        reply = "\"$target\" app is device par nahi mila.",
                        category = ActionCategory.OPEN_APP,
                        detail = "Not found",
                        executed = false
                    )
            }

            AiActionType.WEB_SEARCH -> {
                val q = plan.query?.trim().orEmpty()
                if (q.isEmpty()) return null
                buildWebSearchOutcome(q, replyOverride = plan.reply)
            }

            AiActionType.WHATSAPP -> {
                val contact = plan.contactName?.trim().orEmpty()
                val text = plan.messageText?.trim().orEmpty()
                val isBusiness = plan.appName?.lowercase()?.contains("business") == true ||
                    plan.appName?.lowercase()?.contains("bussiness") == true
                val targetPkg = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
                val appTitle = if (isBusiness) "WhatsApp Business" else "WhatsApp"
                if (contact.isEmpty() && text.isEmpty()) {
                    val launchIntent = ctx.packageManager.getLaunchIntentForPackage(targetPkg)
                    return CommandOutcome(
                        reply = if (launchIntent != null) plan.reply else "Is device par $appTitle installed nahi hai.",
                        category = ActionCategory.WHATSAPP,
                        detail = if (launchIntent != null) "$appTitle opened" else "Not installed",
                        executed = launchIntent != null,
                        pendingIntents = listOfNotNull(launchIntent),
                        failureReply = "$appTitle nahi khul paaya."
                    )
                }
                buildWhatsAppMessageOutcome(
                    name = contact.ifEmpty { "Contact" },
                    text = text,
                    targetPackage = targetPkg,
                    appTitle = appTitle,
                    replyOverride = plan.reply
                )
            }

            AiActionType.PHONE_CALL -> {
                val target = (plan.contactName ?: plan.query).orEmpty().trim()
                if (target.isEmpty()) return null
                buildPhoneCallOutcome(target, replyOverride = plan.reply)
            }

            AiActionType.SEND_SMS -> {
                val contact = plan.contactName?.trim().orEmpty()
                val body = (plan.messageText ?: plan.query).orEmpty().trim()
                buildSmsOutcome(contact.ifEmpty { "Contact" }, body, replyOverride = plan.reply)
            }

            AiActionType.MAPS_SEARCH -> {
                val place = plan.query?.trim().orEmpty()
                if (place.isEmpty()) return null
                buildMapsOutcome(place, replyOverride = plan.reply)
            }

            AiActionType.OPEN_SETTINGS -> {
                val key = plan.query?.lowercase().orEmpty()
                val (action, label, cat) = when {
                    key.contains("wifi") -> Triple(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings", ActionCategory.SETTINGS)
                    key.contains("bluetooth") -> Triple(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings", ActionCategory.SETTINGS)
                    key.contains("camera") -> Triple(MediaStore.ACTION_IMAGE_CAPTURE, "Camera", ActionCategory.OPEN_APP)
                    key.contains("accessibility") -> Triple(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility Settings", ActionCategory.SETTINGS)
                    else -> Triple(Settings.ACTION_SETTINGS, "Settings", ActionCategory.SETTINGS)
                }
                CommandOutcome(
                    reply = plan.reply,
                    category = cat,
                    detail = label,
                    executed = true,
                    pendingIntents = listOf(Intent(action)),
                    failureReply = "$label nahi khul paaya."
                )
            }

            AiActionType.DEVICE_CONTROL -> {
                val cmd = plan.query?.trim().orEmpty()
                if (cmd.isEmpty()) return null
                executeDeviceControlCommand(cmd, replyOverride = plan.reply)
            }

            AiActionType.SEND_EMAIL -> {
                val to = plan.query?.trim().orEmpty()
                val subject = plan.appName?.trim().orEmpty()
                val body = plan.messageText?.trim().orEmpty()
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${Uri.encode(to)}")
                    if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
                    if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
                }
                CommandOutcome(
                    reply = plan.reply,
                    category = ActionCategory.EMAIL,
                    detail = to.ifEmpty { subject.ifEmpty { "Compose Email" } },
                    executed = true,
                    pendingIntents = listOf(emailIntent),
                    failureReply = "Email app nahi khul paaya."
                )
            }

            AiActionType.SHARE_TEXT -> {
                val text = (plan.messageText ?: plan.query).orEmpty().trim()
                if (text.isEmpty()) return null
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                CommandOutcome(
                    reply = plan.reply,
                    category = ActionCategory.SHARE,
                    detail = text.take(28),
                    executed = true,
                    pendingIntents = listOf(Intent.createChooser(sendIntent, "Share via")),
                    failureReply = "Share sheet nahi khul paayi."
                )
            }

            AiActionType.ADD_CALENDAR_EVENT -> {
                val title = (plan.query ?: plan.messageText).orEmpty().trim().ifEmpty { "AuraMate Reminder" }
                val calIntent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    if (!plan.messageText.isNullOrBlank()) {
                        putExtra(CalendarContract.Events.DESCRIPTION, plan.messageText)
                    }
                }
                CommandOutcome(
                    reply = plan.reply,
                    category = ActionCategory.CALENDAR,
                    detail = title,
                    executed = true,
                    pendingIntents = listOf(calIntent),
                    failureReply = "Calendar app nahi khul paaya."
                )
            }

            AiActionType.PLAY_STORE_SEARCH -> {
                val q = plan.query?.trim().orEmpty()
                if (q.isEmpty()) return null
                CommandOutcome(
                    reply = plan.reply,
                    category = ActionCategory.OPEN_APP,
                    detail = "Play Store • $q",
                    executed = true,
                    pendingIntents = buildPlayStoreIntents(q),
                    failureReply = "Play Store nahi khul paaya."
                )
            }
        }
    }

    private fun directDeviceControl(lower: String, raw: String): CommandOutcome? {
        return when {
            (lower.contains("flashlight") || lower.contains("torch")) &&
                (lower.contains("on") || lower.contains("chalu") || lower.contains("jala") || lower.contains("khol")) ->
                executeDeviceControlCommand("flashlight_on")
            (lower.contains("flashlight") || lower.contains("torch")) &&
                (lower.contains("off") || lower.contains("band")) ->
                executeDeviceControlCommand("flashlight_off")
            lower == "volume up" || lower.contains("volume badhao") || lower.contains("awaz badhao") || lower.contains("awaaz badhao") ->
                executeDeviceControlCommand("volume_up")
            lower == "volume down" || lower.contains("volume kam") || lower.contains("awaz kam") || lower.contains("awaaz kam") ->
                executeDeviceControlCommand("volume_down")
            lower == "mute" || lower.contains("volume mute") || lower.contains("awaz band") || lower.contains("awaaz band") ->
                executeDeviceControlCommand("volume_mute")
            lower == "back" || lower == "back jao" || lower == "go back" || lower == "peeche jao" ->
                executeDeviceControlCommand("back")
            lower == "home" || lower == "home jao" || lower == "go home" || lower == "home screen" ->
                executeDeviceControlCommand("home")
            lower == "recents" || lower == "recent apps" || lower == "recent apps dikhao" ->
                executeDeviceControlCommand("recents")
            lower == "notifications kholo" || lower == "open notifications" ->
                executeDeviceControlCommand("notifications")
            lower == "scroll down" || lower == "niche scroll karo" || lower == "neeche scroll karo" ->
                executeDeviceControlCommand("scroll_down")
            lower == "scroll up" || lower == "upar scroll karo" ->
                executeDeviceControlCommand("scroll_up")
            lower.startsWith("click ") || lower.startsWith("tap ") ->
                executeDeviceControlCommand("click:${raw.substringAfter(' ').trim()}")
            else -> null
        }
    }

    private fun executeDeviceControlCommand(command: String, replyOverride: String? = null): CommandOutcome {
        val cmd = command.trim()
        val lower = cmd.lowercase()
        return when {
            lower == "flashlight_on" || lower == "flashlight_off" -> {
                val enable = lower == "flashlight_on"
                val toggled = toggleFlashlight(enable)
                CommandOutcome(
                    reply = if (toggled) {
                        replyOverride ?: if (enable) "Flashlight on kar di hai." else "Flashlight band kar di hai."
                    } else {
                        "Is device par flashlight available nahi hai."
                    },
                    category = ActionCategory.DEVICE,
                    detail = if (enable) "Flashlight ON" else "Flashlight OFF",
                    executed = toggled
                )
            }

            lower == "volume_up" || lower == "volume_down" || lower == "volume_mute" || lower == "volume_max" -> {
                val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audio != null) {
                    when (lower) {
                        "volume_up" -> audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                        "volume_down" -> audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                        "volume_mute" -> audio.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_MUTE,
                            AudioManager.FLAG_SHOW_UI
                        )
                        "volume_max" -> {
                            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                }
                val label = when (lower) {
                    "volume_up" -> "Volume Up"
                    "volume_down" -> "Volume Down"
                    "volume_mute" -> "Muted"
                    else -> "Max Volume"
                }
                CommandOutcome(
                    reply = replyOverride ?: "Media volume update kar diya hai ($label).",
                    category = ActionCategory.DEVICE,
                    detail = label,
                    executed = audio != null
                )
            }

            else -> executeAccessibilityCommand(cmd, replyOverride)
        }
    }

    private fun toggleFlashlight(enable: Boolean): Boolean {
        return try {
            val cam = ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
            val torchCameraId = cam.cameraIdList.firstOrNull { id ->
                cam.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cam.setTorchMode(torchCameraId, enable)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeAccessibilityCommand(cmd: String, replyOverride: String?): CommandOutcome {
        val service = AuraAccessibilityService.instance
        if (service == null) {
            return CommandOutcome(
                reply = "Screen control commands (Back, Home, Scroll, Tap) ke liye pehle Screen Control (Accessibility) permission allow karein.",
                category = ActionCategory.DEVICE,
                detail = "Permission needed",
                executed = false,
                needsAccessibilityPermission = true
            )
        }

        val lower = cmd.lowercase()
        val (ok, detail, defaultReply) = when {
            lower == "back" -> Triple(
                service.performSystemNavigation(AccessibilityService.GLOBAL_ACTION_BACK),
                "Back",
                "Back jaa rahi hoon."
            )
            lower == "home" -> Triple(
                service.performSystemNavigation(AccessibilityService.GLOBAL_ACTION_HOME),
                "Home",
                "Home screen par jaa rahi hoon."
            )
            lower == "recents" -> Triple(
                service.performSystemNavigation(AccessibilityService.GLOBAL_ACTION_RECENTS),
                "Recents",
                "Recent apps khol rahi hoon."
            )
            lower == "notifications" -> Triple(
                service.performSystemNavigation(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
                "Notifications",
                "Notifications panel khol rahi hoon."
            )
            lower == "quick_settings" -> Triple(
                service.performSystemNavigation(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS),
                "Quick Settings",
                "Quick settings khol rahi hoon."
            )
            lower == "scroll_down" -> Triple(
                service.scrollScreen(forward = true),
                "Scroll Down",
                "Neeche scroll kar rahi hoon."
            )
            lower == "scroll_up" -> Triple(
                service.scrollScreen(forward = false),
                "Scroll Up",
                "Upar scroll kar rahi hoon."
            )
            lower.startsWith("click:") -> {
                val target = cmd.substringAfter(':').trim()
                Triple(
                    service.clickNodeByText(target),
                    "Tap • $target",
                    if (target.isNotEmpty()) "\"$target\" par tap kar rahi hoon." else "Tap complete."
                )
            }
            lower.startsWith("type:") -> {
                val text = cmd.substringAfter(':').trim()
                Triple(
                    service.inputTextOnFocusedField(text),
                    "Type",
                    "\"$text\" type kar rahi hoon."
                )
            }
            else -> Triple(false, cmd, "Yeh screen action run nahi ho paaya.")
        }

        return CommandOutcome(
            reply = if (ok) (replyOverride ?: defaultReply) else "Screen par yeh action abhi nahi ho paaya.",
            category = ActionCategory.DEVICE,
            detail = detail,
            executed = ok
        )
    }

    // "Morning me 5 baje ka alarm laga do", "alarm 6:30 baje shaam", "set alarm 7 am"
    private fun alarm(t: String): CommandOutcome? {
        if (!t.contains("alarm")) return null
        val m = Regex("""(\d{1,2})(?::(\d{2}))?""").find(t)
            ?: return CommandOutcome(
                reply = "Kitne baje ka alarm lagana hai? Jaise: 'Subah 6:30 baje ka alarm laga do'.",
                category = ActionCategory.ALARM,
                detail = "Time missing",
                executed = false
            )
        var h = m.groupValues[1].toIntOrNull() ?: return null
        val min = (m.groupValues[2].ifEmpty { "0" }).toIntOrNull() ?: 0
        if (h !in 0..23 || min !in 0..59) {
            return CommandOutcome(
                reply = "Ye samay sahi nahi lag raha. Kripya 1 se 12 ya 00:00 se 23:59 ke beech ka time batayein.",
                category = ActionCategory.ALARM,
                detail = "Invalid time",
                executed = false
            )
        }
        val pm = listOf("shaam", "raat", "evening", "night", "dopahar", "afternoon", "pm")
            .any { t.contains(it) }
        val am = listOf("subah", "morning", "am").any { t.contains(it) }
        if (pm && h < 12) h += 12
        if (!pm && h == 12 && am) h = 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, h)
            .putExtra(AlarmClock.EXTRA_MINUTES, min)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "AuraMate")

        val formatted = "%02d:%02d".format(h, min)
        return CommandOutcome(
            reply = "Theek hai, $formatted ka alarm laga rahi hoon.",
            category = ActionCategory.ALARM,
            detail = formatted,
            executed = true,
            pendingIntents = listOf(intent),
            failureReply = "Is device par Alarm app nahi khul paaya."
        )
    }

    // "10 minute ka timer lagao", "30 second timer", "set 5 min timer"
    private fun timer(t: String): CommandOutcome? {
        if (!t.contains("timer")) return null
        val n = Regex("""(\d+)""").find(t)?.value?.toIntOrNull()
            ?: return CommandOutcome(
                reply = "Kitne minute ya second ka timer lagana hai?",
                category = ActionCategory.TIMER,
                detail = "Duration missing",
                executed = false
            )
        val unitLabel: String
        val secs = when {
            t.contains("second") || t.contains("sec") -> {
                unitLabel = "$n sec"
                n
            }
            t.contains("ghante") || t.contains("ghanta") || t.contains("hour") -> {
                unitLabel = "$n hr"
                n * 3600
            }
            else -> {
                unitLabel = "$n min"
                n * 60
            }
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, secs)
        return CommandOutcome(
            reply = "$unitLabel ka timer shuru kar rahi hoon.",
            category = ActionCategory.TIMER,
            detail = unitLabel,
            executed = true,
            pendingIntents = listOf(intent),
            failureReply = "Is device par Timer app nahi khul paaya."
        )
    }

    // "Rahul ko call karo", "call 9876543210", "Papa ko phone lagao"
    private fun phoneCall(raw: String): CommandOutcome? {
        val m = Regex(
            """^(?:call\s+(.+)|(.+?)\s+ko\s+(?:call|phone)\s*(?:karo|kar do|lagao|laga do)?)$""",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: return null

        val target = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        if (target.isEmpty()) return null
        return buildPhoneCallOutcome(target)
    }

    private fun buildPhoneCallOutcome(target: String, replyOverride: String? = null): CommandOutcome {
        val digitsOnly = target.filter { it.isDigit() || it == '+' }
        if (digitsOnly.length >= 6 && target.none { it.isLetter() }) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digitsOnly"))
            return CommandOutcome(
                reply = replyOverride ?: "$digitsOnly par call dial kar rahi hoon.",
                category = ActionCategory.CALL,
                detail = digitsOnly,
                executed = true,
                pendingIntents = listOf(dialIntent),
                failureReply = "Phone dialer nahi khul paaya."
            )
        }

        val hasContactsPerm = hasContactsPermission()
        val number = if (hasContactsPerm) findNumber(target, addIndiaPrefix = false) else null
        return if (number != null) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            CommandOutcome(
                reply = replyOverride ?: "$target ($number) ko call dial kar rahi hoon.",
                category = ActionCategory.CALL,
                detail = target,
                executed = true,
                pendingIntents = listOf(dialIntent),
                failureReply = "Phone dialer nahi khul paaya."
            )
        } else if (!hasContactsPerm) {
            val dialerIntent = Intent(Intent.ACTION_DIAL)
            CommandOutcome(
                reply = "Contacts permission nahi hai, isliye $target ka number nahi dekh paayi. Dialer khol rahi hoon.",
                category = ActionCategory.CALL,
                detail = "Dialer",
                executed = true,
                needsContactsPermission = true,
                pendingIntents = listOf(dialerIntent),
                failureReply = "Phone dialer nahi khul paaya."
            )
        } else {
            val dialerIntent = Intent(Intent.ACTION_DIAL)
            CommandOutcome(
                reply = "$target ka number contacts mein nahi mila. Dialer khol rahi hoon.",
                category = ActionCategory.CALL,
                detail = "Not found",
                executed = true,
                pendingIntents = listOf(dialerIntent),
                failureReply = "Phone dialer nahi khul paaya."
            )
        }
    }

    // "Rahul ko SMS bhejo hello", "SMS Rahul I am on the way"
    private fun smsMessage(raw: String): CommandOutcome? {
        if (raw.contains("whatsapp", ignoreCase = true)) return null
        val m = Regex(
            """^(?:(.+?)\s+ko\s+(?:sms|text)\s+(?:bhejo|karo|kar do|send\s+karo)\s+(.+)|(?:sms|text)\s+(.+?)\s+(?:that|ko)?\s*(.+))$""",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: return null

        val name = (m.groupValues[1].ifEmpty { m.groupValues[3] }).trim()
        val body = (m.groupValues[2].ifEmpty { m.groupValues[4] }).trim()
        if (name.isEmpty() || body.isEmpty()) return null
        return buildSmsOutcome(name, body)
    }

    private fun buildSmsOutcome(name: String, body: String, replyOverride: String? = null): CommandOutcome {
        val hasContactsPerm = hasContactsPermission()
        val number = if (hasContactsPerm) findNumber(name, addIndiaPrefix = false) else null
        val uri = if (number != null) Uri.parse("smsto:$number") else Uri.parse("smsto:")
        val smsIntent = Intent(Intent.ACTION_SENDTO, uri).putExtra("sms_body", body)

        val reply = replyOverride ?: when {
            number != null -> "$name ke liye SMS taiyaar hai."
            !hasContactsPerm -> "Contacts permission nahi hai, isliye Messages app mein text pre-fill kar diya hai."
            else -> "$name ka number nahi mila, Messages app khol rahi hoon."
        }
        return CommandOutcome(
            reply = reply,
            category = ActionCategory.SMS,
            detail = name,
            executed = true,
            needsContactsPermission = !hasContactsPerm && number == null,
            pendingIntents = listOf(smsIntent),
            failureReply = "Messages app nahi khul paaya."
        )
    }

    // "WhatsApp pe Veera ko hello message kar do", "WhatsApp Business pe ...", "WhatsApp kholo"
    private fun whatsapp(raw: String): CommandOutcome? {
        val normalized = normalizeQueryText(raw)
        if (!normalized.contains("whatsapp")) return null
        val isBusiness = normalized.contains("business")
        val targetPkg = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
        val appTitle = if (isBusiness) "WhatsApp Business" else "WhatsApp"

        val m = Regex(
            """whats?app?(?:\s+(?:business|bussiness|buisness|busines))?\s+(?:pe|par|on)\s+(.+?)\s+ko\s+(.+?)(?:\s+(?:message|msg|bhej|bhejo|kar|likh).*)?$""",
            RegexOption.IGNORE_CASE
        ).find(raw)

        if (m == null) {
            val wantsOpenOnly = normalized.contains("khol") || normalized.contains("open") || normalized.contains("chala")
            if (!wantsOpenOnly && raw.trim().split(Regex("\\s+")).size > 4) {
                return null
            }
            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(targetPkg)
                ?: return CommandOutcome(
                    reply = "Is device par $appTitle installed nahi hai.",
                    category = ActionCategory.WHATSAPP,
                    detail = "Not installed",
                    executed = false
                )
            return CommandOutcome(
                reply = "$appTitle khol rahi hoon.",
                category = ActionCategory.WHATSAPP,
                detail = "$appTitle opened",
                executed = true,
                pendingIntents = listOf(launchIntent),
                failureReply = "$appTitle kholne mein dikkat aayi."
            )
        }

        val name = m.groupValues[1].trim()
        val text = m.groupValues[2].trim()
        return buildWhatsAppMessageOutcome(name, text, targetPackage = targetPkg, appTitle = appTitle)
    }

    private fun buildWhatsAppMessageOutcome(
        name: String,
        text: String,
        targetPackage: String = "com.whatsapp",
        appTitle: String = "WhatsApp",
        replyOverride: String? = null
    ): CommandOutcome {
        val hasContactsPerm = hasContactsPermission()
        val number = if (hasContactsPerm) findNumber(name, addIndiaPrefix = true) else null
        val uri = if (number != null) {
            "https://wa.me/${number}?text=${Uri.encode(text)}"
        } else {
            "https://wa.me/?text=${Uri.encode(text)}"
        }

        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(targetPackage)
        val replyText = replyOverride ?: when {
            number != null -> "$name ke liye $appTitle message taiyaar hai. Bas send dabaiye."
            !hasContactsPerm -> "Contacts permission nahi hai, isliye $name ka number nahi dekh paayi. Chat chunne ke liye $appTitle khol diya hai."
            else -> "$name ka number contacts mein nahi mila, isliye chat chunne ke liye $appTitle khol diya."
        }
        val detail = when {
            number != null -> name
            !hasContactsPerm -> "Share sheet"
            else -> "Pick contact"
        }

        return CommandOutcome(
            reply = replyText,
            category = ActionCategory.WHATSAPP,
            detail = detail,
            executed = true,
            needsContactsPermission = !hasContactsPerm,
            pendingIntents = listOf(waIntent),
            failureReply = "$appTitle is device par installed nahi hai ya khul nahi paaya."
        )
    }

    // "Maps pe Taj Mahal dikhao", "navigate to India Gate"
    private fun mapsNavigation(raw: String): CommandOutcome? {
        val m = Regex(
            """^(?:maps\s+(?:pe|par|on)\s+(.+?)\s+(?:dikhao|search(?:\s+karo)?|kholo)|navigate\s+to\s+(.+)|(.+?)\s+ka\s+rasta\s+dikhao)$""",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: return null
        val place = (m.groupValues[1].ifEmpty { m.groupValues[2] }.ifEmpty { m.groupValues[3] }).trim()
        if (place.isEmpty()) return null
        return buildMapsOutcome(place)
    }

    private fun buildMapsOutcome(place: String, replyOverride: String? = null): CommandOutcome {
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(place)}"))
            .setPackage("com.google.android.apps.maps")
        val geoFallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(place)}"))
        val webFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://maps.google.com/?q=${Uri.encode(place)}")
        )
        return CommandOutcome(
            reply = replyOverride ?: "Maps par \"$place\" dikha rahi hoon.",
            category = ActionCategory.MAPS,
            detail = place,
            executed = true,
            pendingIntents = listOf(mapsIntent, geoFallback, webFallback),
            failureReply = "Maps app nahi khul paaya."
        )
    }

    // "Spotify pe Kesariya chalao", "play Arijit Singh on Spotify"
    private fun mediaPlay(raw: String, lower: String): CommandOutcome? {
        if (lower.contains("youtube")) return null
        val m = Regex(
            """^(?:spotify\s+(?:pe|par|on)\s+(.+?)\s+(?:chalao|play|bajao)|play\s+(.+?)\s+on\s+spotify)$""",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: return null
        val query = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        if (query.isEmpty()) return null
        return buildPlayMediaOutcome(query = query, appHint = "spotify")
    }

    private fun buildPlayMediaOutcome(
        query: String,
        appHint: String = "",
        replyOverride: String? = null
    ): CommandOutcome {
        val spotifyIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            setPackage("com.spotify.music")
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra("query", query)
        }
        val ytMusicIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            setPackage("com.google.android.apps.youtube.music")
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra("query", query)
        }
        val chromeYtSearch = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://m.youtube.com/results?search_query=${Uri.encode(query)}")
        ).setPackage("com.android.chrome")
        val webYtSearch = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://m.youtube.com/results?search_query=${Uri.encode(query)}")
        )

        val candidates = when {
            appHint.contains("youtube") && !appHint.contains("music") ->
                listOf(chromeYtSearch, webYtSearch, ytMusicIntent, spotifyIntent)
            appHint.contains("yt music") || appHint.contains("youtube music") ->
                listOf(ytMusicIntent, spotifyIntent, chromeYtSearch, webYtSearch)
            else ->
                listOf(spotifyIntent, ytMusicIntent, chromeYtSearch, webYtSearch)
        }
        return CommandOutcome(
            reply = replyOverride ?: "Music mein \"$query\" chala rahi hoon.",
            category = ActionCategory.MEDIA,
            detail = query,
            executed = true,
            pendingIntents = candidates,
            failureReply = "\"$query\" play karne ke liye music player ya browser nahi mila."
        )
    }

    // "WiFi settings kholo", "Bluetooth settings kholo", "Camera kholo"
    private fun systemSettings(t: String): CommandOutcome? {
        val action: String
        val label: String
        when {
            t.contains("wifi") && (t.contains("setting") || t.contains("khol") || t.contains("open")) -> {
                action = Settings.ACTION_WIFI_SETTINGS
                label = "Wi-Fi Settings"
            }
            t.contains("bluetooth") && (t.contains("setting") || t.contains("khol") || t.contains("open")) -> {
                action = Settings.ACTION_BLUETOOTH_SETTINGS
                label = "Bluetooth Settings"
            }
            t == "camera kholo" || t == "open camera" || t == "camera open karo" -> {
                action = MediaStore.ACTION_IMAGE_CAPTURE
                label = "Camera"
            }
            else -> return null
        }

        val intent = Intent(action)
        val category = if (label == "Camera") ActionCategory.OPEN_APP else ActionCategory.SETTINGS
        return CommandOutcome(
            reply = "$label khol rahi hoon.",
            category = category,
            detail = label,
            executed = true,
            pendingIntents = listOf(intent),
            failureReply = "$label nahi khul paaya."
        )
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun findNumber(name: String, addIndiaPrefix: Boolean): String? {
        if (!hasContactsPermission()) return null

        return try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    var n = cursor.getString(0).filter { it.isDigit() }
                    if (addIndiaPrefix && n.length == 10) n = "91$n"
                    n.ifEmpty { null }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // "Chrome pe YouTube kholo", "open YouTube in Chrome"
    private fun youtubeInChrome(raw: String, t: String): CommandOutcome? {
        if (!t.contains("youtube") || !t.contains("chrome")) return null
        val queryMatch = Regex(
            """(?:youtube\s+(?:pe|par|on)\s+(.+?)\s+(?:chalao|search|kholo|play)|play\s+(.+?)\s+on\s+youtube)""",
            RegexOption.IGNORE_CASE
        ).find(raw)
        val q = queryMatch?.let { (it.groupValues[1].ifEmpty { it.groupValues[2] }).trim() }
        val url = if (!q.isNullOrBlank()) {
            "https://m.youtube.com/results?search_query=${Uri.encode(q)}"
        } else {
            "https://m.youtube.com"
        }
        val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.android.chrome")
        val fallbackBrowser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return CommandOutcome(
            reply = if (q.isNullOrBlank()) "Chrome mein YouTube khol rahi hoon." else "Chrome mein YouTube par \"$q\" search kar rahi hoon.",
            category = ActionCategory.OPEN_APP,
            detail = "YouTube (Chrome)",
            executed = true,
            pendingIntents = listOf(chromeIntent, fallbackBrowser),
            failureReply = "Chrome mein YouTube nahi khul paaya."
        )
    }

    // "YouTube pe kya baat hai song chalao", "play kya baat hai song on youtube"
    private fun youtubeSearch(raw: String): CommandOutcome? {
        val m = Regex(
            """^(?:youtube\s+(?:pe|par|on)\s+(.+?)\s+(?:chalao|play|search(?:\s+karo|\s+kar\s+do)?|dikhao)|(?:play|search)\s+(.+?)\s+(?:on|in)\s+youtube)$""",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: return null
        val q = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        if (q.isEmpty()) return null

        val ytIntent = Intent(Intent.ACTION_SEARCH)
            .setPackage("com.google.android.youtube")
            .putExtra("query", q)
        val chromeFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://m.youtube.com/results?search_query=${Uri.encode(q)}")
        ).setPackage("com.android.chrome")
        val webFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://m.youtube.com/results?search_query=${Uri.encode(q)}")
        )
        return CommandOutcome(
            reply = "YouTube par \"$q\" search kar rahi hoon.",
            category = ActionCategory.WEB_SEARCH,
            detail = q,
            executed = true,
            pendingIntents = listOf(ytIntent, chromeFallback, webFallback),
            failureReply = "YouTube search nahi khul paaya."
        )
    }

    // "Instagram kholo", "open YouTube", "open my YouTube", "kholo Chrome"
    private fun openApp(t: String): CommandOutcome? {
        val m = Regex(
            """^(?:(?:please\s+|pls\s+|zara\s+|jara\s+)?(?:open|launch|start|kholo|chalao)\s+(.+)|(.+?)\s+(?:kholo|khol do|open kar do|open karo|chalao))$"""
        ).find(t) ?: return null

        val rawTarget = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        val outcome = buildOpenAppOutcome(rawTarget)
        if (outcome != null) return outcome

        val hasExplicitAppVerb = t.endsWith("kholo") || t.endsWith("khol do") ||
            t.endsWith("open kar do") || t.endsWith("open karo")
        val looksLikeMediaOrPhrase = t.contains("chalao") ||
            rawTarget.split(Regex("""\s+""")).size > 2 ||
            listOf("song", "gaana", "gana", "music", "video", "bhajan", "movie", "play").any { t.contains(it) }
        if (hasExplicitAppVerb && !looksLikeMediaOrPhrase) {
            return CommandOutcome(
                reply = "\"$rawTarget\" app is device par nahi mila.",
                category = ActionCategory.OPEN_APP,
                detail = "Not found",
                executed = false
            )
        }

        return null
    }

    private fun normalizeQueryText(input: String): String {
        return input.lowercase()
            .split(Regex("""\s+"""))
            .joinToString(" ") { token -> typoAliases[token] ?: token }
    }

    private fun buildOpenAppOutcome(rawTarget: String, replyOverride: String? = null): CommandOutcome? {
        val cleanedTokens = rawTarget.lowercase()
            .split(Regex("""\s+"""))
            .map { typoAliases[it] ?: it }
            .filter { it.isNotBlank() && it !in fillerTokens }
        val want = cleanedTokens.joinToString(" ").ifEmpty { normalizeQueryText(rawTarget).trim() }
        if (want.isEmpty()) return null

        val pm = ctx.packageManager
        val activities = queryLauncherActivities()

        val hit = activities.firstOrNull { it.loadLabel(pm).toString().lowercase() == want }
            ?: activities.firstOrNull { it.loadLabel(pm).toString().lowercase().startsWith(want) }
            ?: activities.firstOrNull { it.loadLabel(pm).toString().lowercase().contains(want) }
            ?: activities.firstOrNull { info ->
                val label = info.loadLabel(pm).toString().lowercase()
                cleanedTokens.isNotEmpty() && cleanedTokens.all { token -> label.contains(token) }
            }

        if (hit != null) {
            val componentIntent = Intent.makeMainActivity(
                ComponentName(hit.activityInfo.packageName, hit.activityInfo.name)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val fallbackPkgIntent = pm.getLaunchIntentForPackage(hit.activityInfo.packageName)
            val candidates = listOfNotNull(componentIntent, fallbackPkgIntent)
            val label = hit.loadLabel(pm).toString()
            return CommandOutcome(
                reply = replyOverride ?: "$label khol rahi hoon.",
                category = ActionCategory.OPEN_APP,
                detail = label,
                executed = true,
                pendingIntents = candidates,
                failureReply = "$label kholne mein dikkat aayi."
            )
        }

        val knownPkg = knownPackages[want]
            ?: knownPackages.entries
                .sortedByDescending { it.key.length }
                .firstOrNull { want.contains(it.key) }
                ?.value
        if (knownPkg != null) {
            val pkgIntent = pm.getLaunchIntentForPackage(knownPkg)
            val displayName = want.replaceFirstChar { it.uppercase() }
            val candidates = buildList {
                if (pkgIntent != null) add(pkgIntent)
                if (knownPkg == "com.google.android.youtube") {
                    add(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com")))
                }
            }
            if (candidates.isNotEmpty()) {
                return CommandOutcome(
                    reply = replyOverride ?: "$displayName khol rahi hoon.",
                    category = ActionCategory.OPEN_APP,
                    detail = displayName,
                    executed = true,
                    pendingIntents = candidates,
                    failureReply = "$displayName app is device par nahi khul paaya."
                )
            }
            return CommandOutcome(
                reply = "$displayName app is device par installed nahi hai.",
                category = ActionCategory.OPEN_APP,
                detail = "Not installed",
                executed = false
            )
        }

        return null
    }

    // "search karo best biryani near me", "google pe ... search"
    private fun webSearch(raw: String): CommandOutcome? {
        val m = Regex(
            """(?:search\s+(?:karo|kar do|for)?\s*(.+)|(.+?)\s+search\s+(?:karo|kar do))""",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: return null
        val query = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        if (query.isEmpty()) return null
        return buildWebSearchOutcome(query)
    }

    private fun buildWebSearchOutcome(query: String, replyOverride: String? = null): CommandOutcome {
        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query)
        val browserFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        )
        return CommandOutcome(
            reply = replyOverride ?: "\"$query\" search kar rahi hoon.",
            category = ActionCategory.WEB_SEARCH,
            detail = query,
            executed = true,
            pendingIntents = listOf(webSearchIntent, browserFallback),
            failureReply = "Web search kholne ke liye browser ya search app nahi mila."
        )
    }
}
