package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.ActionCategory
import com.example.assistant.CommandHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AuraMate", appName)
    }

    @Test
    fun `alarm command parses evening time correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("alarm 6:30 baje shaam")
        assertNotNull(outcome)
        assertEquals(ActionCategory.ALARM, outcome?.category)
        assertEquals("18:30", outcome?.detail)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `timer command parses minutes correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("10 minute ka timer lagao")
        assertNotNull(outcome)
        assertEquals(ActionCategory.TIMER, outcome?.category)
        assertEquals("10 min", outcome?.detail)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `open my YouTube is handled as app launch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("open my YouTube")
        assertNotNull(outcome)
        assertEquals(ActionCategory.OPEN_APP, outcome?.category)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `web search command parses query correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("search karo best biryani near me")
        assertNotNull(outcome)
        assertEquals(ActionCategory.WEB_SEARCH, outcome?.category)
        assertEquals("best biryani near me", outcome?.detail)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `conversational prompt routes to AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("Mujhe ek accha motivational quote sunao")
        assertNull(outcome)
    }

    @Test
    fun `natural hinglish song chalao prompt routes to AI planner`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("Mere Sapno Ki Rani song chalao")
        assertNull(outcome)
    }

    @Test
    fun `open YouTube in Chrome targets Chrome YouTube URL`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("open YouTube in Chrome")
        assertNotNull(outcome)
        assertEquals("YouTube (Chrome)", outcome?.detail)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `youtube song search parses query cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("YouTube pe kya baat hai song chalao")
        assertNotNull(outcome)
        assertEquals(ActionCategory.WEB_SEARCH, outcome?.category)
        assertEquals("kya baat hai song", outcome?.detail)
    }

    @Test
    fun `phone call command creates dial intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("call 9876543210")
        assertNotNull(outcome)
        assertEquals(ActionCategory.CALL, outcome?.category)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `sms command creates smsto intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("Veera ko SMS bhejo main aa raha hoon")
        assertNotNull(outcome)
        assertEquals(ActionCategory.SMS, outcome?.category)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `maps command parses destination`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("Maps pe India Gate dikhao")
        assertNotNull(outcome)
        assertEquals(ActionCategory.MAPS, outcome?.category)
        assertEquals("India Gate", outcome?.detail)
    }

    @Test
    fun `wifi settings command parses correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("WiFi settings kholo")
        assertNotNull(outcome)
        assertEquals(ActionCategory.SETTINGS, outcome?.category)
        assertEquals("Wi-Fi Settings", outcome?.detail)
    }

    @Test
    fun `AI action plan PLAY_MEDIA builds playable media intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val plan = com.example.ai.AiActionPlan(
            reply = "Kishore Kumar ka gaana play kar rahi hoon.",
            action = com.example.ai.AiActionType.PLAY_MEDIA,
            query = "Mere Sapno Ki Rani Kishore Kumar"
        )
        val outcome = handler.resolveAiPlan(plan)
        assertNotNull(outcome)
        assertEquals(ActionCategory.MEDIA, outcome?.category)
        assertEquals("Mere Sapno Ki Rani Kishore Kumar", outcome?.detail)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `AI action plan SET_ALARM builds alarm intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val plan = com.example.ai.AiActionPlan(
            reply = "Subah 6:15 ka alarm laga rahi hoon.",
            action = com.example.ai.AiActionType.SET_ALARM,
            hour = 6,
            minute = 15
        )
        val outcome = handler.resolveAiPlan(plan)
        assertNotNull(outcome)
        assertEquals(ActionCategory.ALARM, outcome?.category)
        assertEquals("06:15", outcome?.detail)
        assertTrue(outcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `whatsapp business typo command resolves to WhatsApp Business package`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("whatsap bussiness pe Veera ko hello message")
        assertNotNull(outcome)
        assertEquals(ActionCategory.WHATSAPP, outcome?.category)
        assertEquals("com.whatsapp.w4b", outcome?.pendingIntents?.firstOrNull()?.`package`)
    }

    @Test
    fun `getInstalledApps runs safely without throwing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val apps = handler.getInstalledApps()
        assertNotNull(apps)
    }

    @Test
    fun `sending open my youtube in MainActivity does not crash and shows reply`() {
        composeRule.onNodeWithTag("command_input_field").performTextInput("open my youtube")
        composeRule.onNodeWithTag("send_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("open my youtube").assertIsDisplayed()
    }

    @Test
    fun `AI action plan PLAY_STORE_SEARCH resolves known app to market details URI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val plan = com.example.ai.AiActionPlan(
            reply = "Play Store par WhatsApp Business khol rahi hoon.",
            action = com.example.ai.AiActionType.PLAY_STORE_SEARCH,
            query = "WhatsApp Business"
        )
        val outcome = handler.resolveAiPlan(plan)
        assertNotNull(outcome)
        assertEquals(ActionCategory.OPEN_APP, outcome?.category)
        assertEquals("market://details?id=com.whatsapp.w4b", outcome?.pendingIntents?.firstOrNull()?.dataString)
    }

    @Test
    fun `flashlight and volume commands handle hardware controls safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)

        val torchOutcome = handler.handle("flashlight on karo")
        assertNotNull(torchOutcome)
        assertEquals(ActionCategory.DEVICE, torchOutcome?.category)

        val volumeOutcome = handler.handle("volume up")
        assertNotNull(volumeOutcome)
        assertEquals(ActionCategory.DEVICE, volumeOutcome?.category)
        assertTrue(volumeOutcome?.executed == true)
    }

    @Test
    fun `back navigation requests accessibility permission when service disabled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)
        val outcome = handler.handle("back jao")
        assertNotNull(outcome)
        assertEquals(ActionCategory.DEVICE, outcome?.category)
        assertEquals(false, outcome?.executed)
        assertEquals(true, outcome?.needsAccessibilityPermission)
    }

    @Test
    fun `AI action plans for SEND_EMAIL SHARE_TEXT and ADD_CALENDAR_EVENT build valid intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = CommandHandler(context)

        val emailOutcome = handler.resolveAiPlan(
            com.example.ai.AiActionPlan(
                reply = "Email compose kar rahi hoon.",
                action = com.example.ai.AiActionType.SEND_EMAIL,
                query = "team@example.com",
                appName = "Meeting Update",
                messageText = "Hello team"
            )
        )
        assertNotNull(emailOutcome)
        assertEquals(ActionCategory.EMAIL, emailOutcome?.category)
        assertTrue(emailOutcome?.pendingIntents?.isNotEmpty() == true)

        val shareOutcome = handler.resolveAiPlan(
            com.example.ai.AiActionPlan(
                reply = "Share sheet khol rahi hoon.",
                action = com.example.ai.AiActionType.SHARE_TEXT,
                messageText = "AuraMate se share kiya"
            )
        )
        assertNotNull(shareOutcome)
        assertEquals(ActionCategory.SHARE, shareOutcome?.category)
        assertTrue(shareOutcome?.pendingIntents?.isNotEmpty() == true)

        val calOutcome = handler.resolveAiPlan(
            com.example.ai.AiActionPlan(
                reply = "Calendar event add kar rahi hoon.",
                action = com.example.ai.AiActionType.ADD_CALENDAR_EVENT,
                query = "Project Review"
            )
        )
        assertNotNull(calOutcome)
        assertEquals(ActionCategory.CALENDAR, calOutcome?.category)
        assertTrue(calOutcome?.pendingIntents?.isNotEmpty() == true)
    }

    @Test
    fun `customize sheet updates assistant name in top bar`() {
        composeRule.onNodeWithTag("customize_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("assistant_name_input").performTextInput("X")
        composeRule.onNodeWithTag("save_assistant_name_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("topbar_assistant_name").assertIsDisplayed()
    }
}
