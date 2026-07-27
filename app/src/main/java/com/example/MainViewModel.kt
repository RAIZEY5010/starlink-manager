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

    val shortcuts = db.shortcutDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveShortcut(keyword: String, phrase: String, category: String, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().insert(
                Shortcut(
                    keyword = keyword.trim(),
                    phrase = phrase,
                    category = category.ifBlank { "عام" }.trim(),
                    isPinned = isPinned
                )
            )
        }
    }

    fun updateShortcut(
        shortcut: Shortcut,
        newKeyword: String,
        newPhrase: String,
        newCategory: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortcutDao().update(
                shortcut.copy(
                    keyword = newKeyword.trim(),
                    phrase = newPhrase,
                    category = newCategory.ifBlank { "عام" }.trim(),
                    isPinned = isPinned
                )
            )
        }
    }

    fun togglePin(shortcut: Shortcut) {
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
