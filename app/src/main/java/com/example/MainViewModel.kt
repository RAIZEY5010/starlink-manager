package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.Shortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val shortcuts = db.shortcutDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveShortcut(keyword: String, phrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().insert(
                Shortcut(
                    keyword = keyword.trim(),
                    phrase = phrase.trim()
                )
            )
        }
    }

    fun updateShortcut(shortcut: Shortcut, newKeyword: String, newPhrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().update(
                shortcut.copy(
                    keyword = newKeyword.trim(),
                    phrase = newPhrase.trim()
                )
            )
        }
    }

    fun toggleShortcutPin(shortcut: Shortcut) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().update(shortcut.copy(isPinned = !shortcut.isPinned))
        }
    }

    fun deleteShortcut(shortcut: Shortcut) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().delete(shortcut)
        }
    }
}
