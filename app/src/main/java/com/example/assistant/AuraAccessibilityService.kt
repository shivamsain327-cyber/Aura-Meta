package com.example.assistant

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional user-enabled AccessibilityService that lets the assistant perform
 * explicit on-screen user commands (Back, Home, Recents, Scroll, Tap button by text, Type text)
 * across any open app on the device.
 */
class AuraAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Commands are executed strictly on-demand when requested by the user.
    }

    override fun onInterrupt() {
        // No continuous background state to tear down.
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun performSystemNavigation(globalAction: Int): Boolean {
        return performGlobalAction(globalAction)
    }

    fun scrollScreen(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        val scrollable = findScrollableNode(root) ?: return false
        return scrollable.performAction(action)
    }

    fun clickNodeByText(targetText: String): Boolean {
        val query = targetText.trim().lowercase()
        if (query.isEmpty()) return false
        val root = rootInActiveWindow ?: return false
        val targetNode = findMatchingTextNode(root, query) ?: return false
        var clickable: AccessibilityNodeInfo? = targetNode
        while (clickable != null && !clickable.isClickable) {
            clickable = clickable.parent
        }
        return (clickable ?: targetNode).performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun inputTextOnFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(root)
            ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findScrollableNode(child)
            if (match != null) return match
        }
        return null
    }

    private fun findMatchingTextNode(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if ((text.isNotEmpty() && text.contains(query)) || (desc.isNotEmpty() && desc.contains(query))) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findMatchingTextNode(child, query)
            if (match != null) return match
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findFirstEditableNode(child)
            if (match != null) return match
        }
        return null
    }

    companion object {
        @Volatile
        var instance: AuraAccessibilityService? = null
            private set

        fun isServiceEnabled(context: Context): Boolean {
            if (instance != null) return true
            val expected = ComponentName(context, AuraAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expected, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}
