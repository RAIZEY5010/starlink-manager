package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.db.AppDatabase
import com.example.util.ShortcutFormatter
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class TextExpanderService : AccessibilityService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val isApplyingExpansion = AtomicBoolean(false)
    @Volatile
    private var shortcuts = listOf<com.example.db.Shortcut>()
    private var lastExpandedText: String? = null
    private var lastExpansionAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val db = AppDatabase.getDatabase(this)
        scope.launch {
            db.shortcutDao().getAll().collect {
                shortcuts = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (isApplyingExpansion.get()) return

        // Find focused input node across the active window or fall back to the event source.
        val activeNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: event.source

        if (activeNode == null) return
        if (!activeNode.isEditable) return

        val text = activeNode.text?.toString() ?: return

        if (shortcuts.isEmpty()) return

        // Expanding on every text-change event causes premature replacements and loops.
        if (!text.lastOrNull()?.isWhitespace().orFalse()) return
        if (text == lastExpandedText && System.currentTimeMillis() - lastExpansionAt < 1000) return

        val trailingWhitespace = text.takeLastWhile(Char::isWhitespace)
        val rawText = text.dropLast(trailingWhitespace.length)
        if (rawText.isEmpty()) return
        run {
            val words = rawText.split(Regex("\\s+"))
            if (words.isNotEmpty()) {
                val lastWord = words.last()
                val matched = shortcuts.firstOrNull { it.keyword == lastWord }

                if (matched != null) {
                    val prefix = rawText.substring(0, rawText.length - lastWord.length)
                    val expandedPhrase = processPhrase(matched.phrase)
                    val newText = prefix + expandedPhrase + trailingWhitespace

                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    }
                    isApplyingExpansion.set(true)
                    val setSuccess = try {
                        activeNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    } finally {
                        isApplyingExpansion.set(false)
                    }

                    if (setSuccess) {
                        lastExpandedText = newText
                        lastExpansionAt = System.currentTimeMillis()
                        val selectionArgs = Bundle().apply {
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newText.length)
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newText.length)
                        }
                        activeNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun processPhrase(phrase: String): String {
        return ShortcutFormatter.applyTokens(phrase)
    }

    private fun Boolean?.orFalse(): Boolean = this == true
}
