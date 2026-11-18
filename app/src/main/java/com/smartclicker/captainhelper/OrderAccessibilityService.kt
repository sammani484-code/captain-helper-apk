package com.smartclicker.captainhelper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OrderAccessibilityService : AccessibilityService() {

    private var lastAcceptTimeMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in TARGET_PACKAGES) return

        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(MainActivity.KEY_AUTO_ACCEPT, true)
        if (!enabled) return

        val minFare = prefs.getInt(MainActivity.KEY_MIN_FARE, MainActivity.DEFAULT_MIN_FARE)
        val root = rootInActiveWindow ?: return

        val now = System.currentTimeMillis()
        if (now - lastAcceptTimeMs < COOLDOWN_MS) return

        val allTexts = mutableListOf<String>()
        collectTexts(root, allTexts)

        val maxFare = extractMaxFare(allTexts)
        if (maxFare != null && maxFare >= minFare) {
            val acceptNode = findAcceptNode(root)
            if (acceptNode != null && performClick(acceptNode)) {
                lastAcceptTimeMs = now
            }
        }
    }

    override fun onInterrupt() {
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            collectTexts(node.getChild(i), out)
        }
    }

    private fun extractMaxFare(texts: List<String>): Double? {
        var maxFare: Double? = null
        val currencyRegex = Regex("""(?:₹|Rs\.?|INR)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        for (text in texts) {
            for (match in currencyRegex.findAll(text)) {
                val value = match.groupValues[1].toDoubleOrNull() ?: continue
                if (maxFare == null || value > maxFare) {
                    maxFare = value
                }
            }
        }
        return maxFare
    }

    private fun findAcceptNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val label = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").trim().lowercase()
        if (label.isNotEmpty() && ACCEPT_KEYWORDS.any { label == it || label.contains(it) }) {
            if (REJECT_KEYWORDS.none { label.contains(it) }) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val found = findAcceptNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    companion object {
        private const val COOLDOWN_MS = 2000L
        private val TARGET_PACKAGES = setOf(
            "com.rapido.rider",
            "com.ind.redtaxi.driverapp"
        )
        private val ACCEPT_KEYWORDS = listOf(
            "accept",
            "accept order",
            "accept ride",
            "confirm"
        )
        private val REJECT_KEYWORDS = listOf(
            "reject",
            "decline",
            "cancel",
            "skip"
        )
    }
}
