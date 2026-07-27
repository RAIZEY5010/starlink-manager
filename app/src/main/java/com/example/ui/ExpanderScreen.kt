package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.MainViewModel
import com.example.db.Shortcut
import com.example.service.TextExpanderService
import com.example.util.ShortcutFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpanderScreen(viewModel: MainViewModel) {
    val shortcuts by viewModel.shortcuts.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val serviceEnabled by produceState(initialValue = isTextExpanderEnabled(context), context) {
        while (true) {
            value = isTextExpanderEnabled(context)
            delay(1200)
        }
    }

    val filteredShortcuts = remember(shortcuts, searchQuery) {
        val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
        if (normalizedQuery.isEmpty()) {
            shortcuts
        } else {
            shortcuts.filter {
                it.keyword.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    it.phrase.lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }
    }
    val pinnedCount = shortcuts.count { it.isPinned }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "إضافة اختصار") },
                text = { Text("اختصار جديد") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroSection(
                    shortcutsCount = shortcuts.size,
                    pinnedCount = pinnedCount,
                    serviceEnabled = serviceEnabled,
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }

            item {
                SearchSection(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            item {
                QuickGuideCard(
                    serviceEnabled = serviceEnabled,
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenAppInfo = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            if (filteredShortcuts.isEmpty()) {
                item {
                    EmptyShortcutsCard(
                        isSearching = searchQuery.isNotBlank(),
                        onAddClick = { showDialog = true }
                    )
                }
            } else {
                items(filteredShortcuts, key = { it.id }) { shortcut ->
                    ShortcutCard(shortcut = shortcut, viewModel = viewModel)
                }
            }
        }
    }

    if (showDialog) {
        AddShortcutDialog(
            onDismiss = { showDialog = false },
            onSave = { keyword, phrase ->
                viewModel.saveShortcut(keyword, phrase)
                showDialog = false
            }
        )
    }
}

@Composable
private fun HeroSection(
    shortcutsCount: Int,
    pinnedCount: Int,
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = "اختصارات أسرع بواجهة أنظف",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "احفظ نصوصك المتكررة، ثبّت أهم الاختصارات، واستخدم الوقت الديناميكي بصيغ مرنة مثل %time+1.5h%.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(title = "الإجمالي", value = shortcutsCount.toString())
                StatPill(title = "المثبتة", value = pinnedCount.toString())
                StatPill(title = "الخدمة", value = if (serviceEnabled) "مفعلة" else "متوقفة")
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onOpenAccessibility) {
                Text(if (serviceEnabled) "فتح إعدادات الخدمة" else "تفعيل الخدمة الآن")
            }
        }
    }
}

@Composable
private fun StatPill(title: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun SearchSection(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        label = { Text("ابحث باسم الاختصار أو النص") },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun QuickGuideCard(
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("الحالة الحالية", style = MaterialTheme.typography.titleMedium)
            Text(
                if (serviceEnabled) {
                    "خدمة الاختصارات مفعلة، وسيتم استبدال الكلمة المفتاحية عندما تكتب مسافة بعدها."
                } else {
                    "الخدمة غير مفعلة حاليًا. فعّلها من إعدادات إمكانية الوصول حتى تعمل الاختصارات داخل التطبيقات."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(
                "إذا ظهر خيار Restricted setting في أندرويد 13/14، افتح معلومات التطبيق وفعّل السماح بالإعدادات المقيدة.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onOpenAccessibility) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعدادات الخدمة")
                }
                TextButton(onClick = onOpenAppInfo) {
                    Text("معلومات التطبيق")
                }
            }
        }
    }
}

@Composable
private fun EmptyShortcutsCard(isSearching: Boolean, onAddClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSearching) "لا توجد نتائج مطابقة" else "ابدأ أول اختصار الآن",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isSearching) {
                    "جرّب كلمة بحث مختلفة أو امسح البحث لعرض كل الاختصارات."
                } else {
                    "أضف اختصارًا لنصوصك المتكررة مثل التحيات، القوالب، أو التاريخ والوقت الديناميكي."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddClick) {
                Text("إضافة اختصار")
            }
        }
    }
}

@Composable
fun ShortcutCard(shortcut: Shortcut, viewModel: MainViewModel) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        AddShortcutDialog(
            initialKeyword = shortcut.keyword,
            initialPhrase = shortcut.phrase,
            onDismiss = { showEditDialog = false },
            onSave = { keyword, phrase ->
                viewModel.updateShortcut(shortcut, keyword, phrase)
                showEditDialog = false
            }
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = shortcut.keyword,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = shortcut.phrase,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.toggleShortcutPin(shortcut) }) {
                    Icon(
                        imageVector = if (shortcut.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (shortcut.isPinned) "إزالة من المثبتة" else "تثبيت",
                        tint = if (shortcut.isPinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "معاينة",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(renderShortcutPreview(shortcut.phrase))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تعديل")
                }
                FilledTonalButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("shortcut", shortcut.phrase))
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ")
                }
                TextButton(onClick = { viewModel.deleteShortcut(shortcut) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddShortcutDialog(
    initialKeyword: String = "",
    initialPhrase: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var keyword by remember { mutableStateOf(initialKeyword) }
    var phrase by remember { mutableStateOf(initialPhrase) }
    var showTimeDialog by remember { mutableStateOf(false) }

    if (showTimeDialog) {
        TimeAdditionDialog(
            onDismiss = { showTimeDialog = false },
            onConfirm = { hoursText ->
                phrase = ShortcutFormatter.insertPlaceholder(phrase, "%time+${hoursText}h%")
                showTimeDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialKeyword.isBlank()) "إضافة اختصار جديد" else "تعديل الاختصار")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("الكلمة المفتاحية مثل: mn") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("النص الكامل") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(18.dp)
                )
                Text(
                    "إضافات جاهزة",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = { phrase = ShortcutFormatter.insertPlaceholder(phrase, "%date%") }) {
                        Text("التاريخ")
                    }
                    FilledTonalButton(onClick = { phrase = ShortcutFormatter.insertPlaceholder(phrase, "%day%") }) {
                        Text("اليوم")
                    }
                    FilledTonalButton(onClick = { phrase = ShortcutFormatter.insertPlaceholder(phrase, "%time%") }) {
                        Text("الوقت الحالي")
                    }
                    FilledTonalButton(onClick = { showTimeDialog = true }) {
                        Text("وقت مخصص")
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "معاينة مباشرة",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(renderShortcutPreview(phrase))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyword.isNotBlank() && phrase.isNotBlank()) {
                        onSave(keyword.trim(), phrase.trim())
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun TimeAdditionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var hours by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("كم عدد الساعات المراد إضافتها؟") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { input ->
                        val normalized = input.replace(",", ".")
                        if (normalized.all { it.isDigit() || it == '.' } && normalized.count { it == '.' } <= 1) {
                            hours = input
                        }
                    },
                    label = { Text("مثال: 1.5 أو 1.3") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Text(
                    "المدخل يقبل الأرقام العشرية، وسيتم تحويله إلى وسم مثل %time+1.5h%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalized = ShortcutFormatter.normalizeDecimalHoursInput(hours)
                    if (normalized != null) onConfirm(normalized)
                }
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

private fun renderShortcutPreview(phrase: String): String {
    if (phrase.isBlank()) return "ستظهر المعاينة هنا بعد كتابة النص."
    return ShortcutFormatter.applyTokens(phrase)
}

private fun isTextExpanderEnabled(context: Context): Boolean {
    val enabledServices =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
    return enabledServices.contains("${context.packageName}/${TextExpanderService::class.java.name}")
}
