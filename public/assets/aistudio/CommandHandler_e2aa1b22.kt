package com.auramate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Turns Hinglish text/voice commands into real Android actions.
 * handle() returns the reply to show/speak, or null if the text is not a command
 * (then MainActivity sends it to the AI chat instead).
 */
class CommandHandler(private val ctx: Context) {

    fun handle(raw: String): String? {
        val t = raw.trim().lowercase()
        return alarm(t) ?: timer(t) ?: whatsapp(raw.trim()) ?: openApp(t) ?: webSearch(raw.trim())
    }

    private fun launch(i: Intent): Boolean = try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) { false }

    // "Morning me 5 baje ka alarm laga do", "alarm 6:30 baje shaam"
    private fun alarm(t: String): String? {
        if (!t.contains("alarm")) return null
        val m = Regex("""(\d{1,2})(?::(\d{2}))?""").find(t) ?: return "Kitne baje ka alarm lagana hai?"
        var h = m.groupValues[1].toInt()
        val min = m.groupValues[2].ifEmpty { "0" }.toInt()
        val pm = listOf("shaam", "raat", "evening", "night", "dopahar", "afternoon", "pm").any { t.contains(it) }
        if (pm && h < 12) h += 12
        if (!pm && h == 12 && listOf("subah", "morning", "am").any { t.contains(it) }) h = 0
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, h)
            .putExtra(AlarmClock.EXTRA_MINUTES, min)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "AuraMate")
        return if (launch(i)) "Theek hai, %02d:%02d ka alarm laga raha hoon.".format(h, min)
        else "Alarm app nahi khul paaya."
    }

    // "10 minute ka timer lagao"
    private fun timer(t: String): String? {
        if (!t.contains("timer")) return null
        val n = Regex("""(\d+)""").find(t)?.value?.toInt() ?: return "Kitne minute ka timer?"
        val secs = if (t.contains("second")) n else if (t.contains("ghante") || t.contains("hour")) n * 3600 else n * 60
        val i = Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, secs)
        return if (launch(i)) "Timer shuru kar raha hoon." else "Timer app nahi khul paaya."
    }

    // "WhatsApp pe Veera ko hello message kar do"
    private fun whatsapp(raw: String): String? {
        if (!raw.contains("whatsapp", ignoreCase = true)) return null
        val m = Regex("""whatsapp\s+(?:pe|par|on)\s+(.+?)\s+ko\s+(.+?)(?:\s+(?:message|msg|bhej|kar|likh).*)?$""",
            RegexOption.IGNORE_CASE).find(raw)
            ?: return launch(ctx.packageManager.getLaunchIntentForPackage("com.whatsapp") ?: return "WhatsApp installed nahi hai.")
                .let { "WhatsApp khol raha hoon." }
        val name = m.groupValues[1].trim()
        val text = m.groupValues[2].trim()
        val number = findNumber(name)
        val uri = if (number != null) "https://wa.me/${number}?text=${Uri.encode(text)}"
        else "https://wa.me/?text=${Uri.encode(text)}"   // opens WhatsApp share sheet, pre-filled
        launch(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.whatsapp"))
        return if (number != null) "$name ke liye WhatsApp message taiyaar hai. Bas send dabaiye."
        else "$name ka number nahi mila, isliye chat chunne ke liye WhatsApp khol diya."
    }

    private fun findNumber(name: String): String? {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$name%"), null
        )?.use { c ->
            if (c.moveToFirst()) {
                var n = c.getString(0).filter { it.isDigit() }
                if (n.length == 10) n = "91$n"        // assume India if no country code
                return n
            }
        }
        return null
    }

    // "Instagram kholo", "open YouTube"
    private fun openApp(t: String): String? {
        val m = Regex("""^(?:open\s+(.+)|(.+?)\s+(?:kholo|khol do|open kar do|chalao))$""").find(t) ?: return null
        val want = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val hit = pm.queryIntentActivities(launcher, 0)
            .firstOrNull { it.loadLabel(pm).toString().lowercase().contains(want) } ?: return null
        val i = pm.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return null
        launch(i)
        return "${hit.loadLabel(pm)} khol raha hoon."
    }

    // "search karo best biryani near me", "google pe ... search"
    private fun webSearch(raw: String): String? {
        val m = Regex("""(?:search\s+(?:karo|kar do)?\s*(.+)|(.+?)\s+search\s+(?:karo|kar do))""", RegexOption.IGNORE_CASE).find(raw) ?: return null
        val q = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim()
        launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", q))
        return "\"$q\" search kar raha hoon."
    }
}
