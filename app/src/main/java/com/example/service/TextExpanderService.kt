package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.db.AppDatabase
import com.example.util.ShortcutFormatter
import com.example.util.TextExpanderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
        if (TextExpanderPreferences.isPackageBlocked(this, event.packageName)) return

        val activeNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: event.source

        if (activeNode == null) return
        if (!activeNode.isEditable) return

        val text = activeNode.text?.toString() ?: return
        if (shortcuts.isEmpty()) return
        if (!text.lastOrNull()?.isWhitespace().orFalse()) return
        if (text == lastExpandedText && System.currentTimeMillis() - lastExpansionAt < 1000) return

        val trailingWhitespace = text.takeLastWhile(Char::isWhitespace)
        val rawText = text.dropLast(trailingWhitespace.length)
        if (rawText.isEmpty()) return
        val words = rawText.split(Regex("\\s+"))
        if (words.isEmpty()) return

        val lastWord = words.last()
        val matched = shortcuts.firstOrNull { it.keyword == lastWord } ?: return
        val prefix = rawText.substring(0, rawText.length - lastWord.length)
        val clipboardText = getClipboardText()
        val formatted = ShortcutFormatter.applyTokens(matched.phrase, clipboardText)
        val newText = prefix + formatted.text + trailingWhitespace

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
            val cursorOffset = formatted.cursorOffset ?: formatted.text.length
            val newCursorPosition = (prefix.length + cursorOffset).coerceIn(0, newText.length)
            val selectionArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPosition)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPosition)
            }
            activeNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun getClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        return clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
    }

    private fun Boolean?.orFalse(): Boolean = this == true
}
