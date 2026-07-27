package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.MainViewModel
import com.example.db.Shortcut
import com.example.service.TextExpanderService
import com.example.util.ShortcutFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpanderScreen(viewModel: MainViewModel) {
    val shortcuts by viewModel.shortcuts.collectAsState()
    val context = LocalContext.current
    val clipboardText = rememberClipboardText(context)
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var serviceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val categories = remember(shortcuts) {
        listOf("الكل") + shortcuts.map { it.category }.distinct().sorted()
    }
    val filteredShortcuts = remember(shortcuts, searchQuery, selectedCategory) {
        shortcuts.filter { shortcut ->
            val categoryMatches = selectedCategory == "الكل" || shortcut.category == selectedCategory
            val textMatches = searchQuery.isBlank() ||
                shortcut.keyword.contains(searchQuery, ignoreCase = true) ||
                shortcut.phrase.contains(searchQuery, ignoreCase = true) ||
                shortcut.category.contains(searchQuery, ignoreCase = true)
            categoryMatches && textMatches
        }
    }
    val pinnedCount = shortcuts.count { it.isPinned }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة اختصار")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeroSection(
                    shortcutsCount = shortcuts.size,
                    pinnedCount = pinnedCount,
                    serviceEnabled = serviceEnabled,
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
            item {
                SearchAndFilterSection(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it }
                )
            }
            item {
                SectionTitle(
                    title = "اختصاراتك الجاهزة",
                    subtitle = if (filteredShortcuts.isEmpty()) {
                        "لا توجد نتائج مطابقة حالياً."
                    } else {
                        "${filteredShortcuts.size} اختصار معروض"
                    }
                )
            }
            items(filteredShortcuts, key = { it.id }) { shortcut ->
                ShortcutCard(
                    shortcut = shortcut,
                    clipboardText = clipboardText,
                    onTogglePin = { viewModel.togglePin(shortcut) },
                    onDelete = { viewModel.deleteShortcut(shortcut) },
                    onUpdate = { keyword, phrase, category, isPinned ->
                        viewModel.updateShortcut(shortcut, keyword, phrase, category, isPinned)
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showDialog) {
        AddShortcutDialog(
            clipboardText = clipboardText,
            onDismiss = { showDialog = false },
            onSave = { keyword, phrase, category, isPinned ->
                viewModel.saveShortcut(keyword, phrase, category, isPinned)
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
    onOpenSettings: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Text Expander بواجهة أسرع",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "أنشئ قوالب ذكية، ابحث عنها بسرعة، واستخدم المتغيرات الديناميكية أثناء الكتابة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(title = "الإجمالي", value = shortcutsCount.toString())
                StatPill(title = "المثبت", value = pinnedCount.toString())
                StatPill(title = "الخدمة", value = if (serviceEnabled) "مفعلة" else "متوقفة")
            }
            Button(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تفعيل الخدمة")
            }
        }
    }
}

@Composable
private fun RowScope.StatPill(title: String, value: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("ابحث عن اختصار أو تصنيف") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShortcutCard(
    shortcut: Shortcut,
    clipboardText: String,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (String, String, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    val previewText = remember(shortcut, clipboardText) {
        ShortcutFormatter.applyTokens(shortcut.phrase, clipboardText).text
    }

    if (showEditDialog) {
        AddShortcutDialog(
            initialKeyword = shortcut.keyword,
            initialPhrase = shortcut.phrase,
            initialCategory = shortcut.category,
            initialPinned = shortcut.isPinned,
            clipboardText = clipboardText,
            onDismiss = { showEditDialog = false },
            onSave = { keyword, phrase, category, isPinned ->
                onUpdate(keyword, phrase, category, isPinned)
                showEditDialog = false
            }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(shortcut.category) }
                        )
                        if (shortcut.isPinned) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                leadingIcon = {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("مثبت") }
                            )
                        }
                    }
                    Text(
                        text = shortcut.keyword,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = shortcut.phrase,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (shortcut.isPinned) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "تثبيت"
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "المعاينة المباشرة",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { showEditDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل")
                }
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("shortcut_preview", previewText))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ المعاينة")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddShortcutDialog(
    initialKeyword: String = "",
    initialPhrase: String = "",
    initialCategory: String = "عام",
    initialPinned: Boolean = false,
    clipboardText: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var keyword by remember { mutableStateOf(initialKeyword) }
    var phrase by remember { mutableStateOf(initialPhrase) }
    var category by remember { mutableStateOf(initialCategory) }
    var isPinned by remember { mutableStateOf(initialPinned) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val preview = remember(phrase, clipboardText) {
        ShortcutFormatter.applyTokens(phrase, clipboardText).text
    }

    if (showTimeDialog) {
        TimeAdditionDialog(
            onDismiss = { showTimeDialog = false },
            onConfirm = { hoursText ->
                phrase += ShortcutFormatter.buildTimeToken(hoursText)
                showTimeDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialKeyword.isBlank()) "اختصار جديد" else "تعديل الاختصار")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("الكلمة المفتاحية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("النص الممتد") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickTokenButton("التاريخ", "%date%") { phrase += it }
                    QuickTokenButton("اليوم", "%day%") { phrase += it }
                    QuickTokenButton("الوقت", "%time%") { phrase += it }
                    QuickTokenButton("الحافظة", "%clipboard%") { phrase += it }
                    QuickTokenButton("المؤشر", "%cursor%") { phrase += it }
                    QuickTokenButton("إضافة ساعات", null) { showTimeDialog = true }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تثبيت الاختصار")
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "المعاينة الحية",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = preview.ifBlank { "اكتب النص لترى النتيجة هنا." },
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyword.isNotBlank() && phrase.isNotBlank()) {
                        onSave(keyword.trim(), phrase, category.trim(), isPinned)
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun QuickTokenButton(label: String, token: String?, onClick: (String) -> Unit) {
    TextButton(onClick = { onClick(token.orEmpty()) }) {
        when (token) {
            "%date%" -> Icon(Icons.Default.CalendarToday, contentDescription = null)
            "%time%" -> Icon(Icons.Default.AccessTime, contentDescription = null)
            else -> Unit
        }
        if (token == "%date%" || token == "%time%") {
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(label)
    }
}

@Composable
private fun TimeAdditionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var hours by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة وقت ديناميكي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أدخل عدد الساعات ويمكنك استخدام الكسور مثل 1.5 أو 1.3")
                OutlinedTextField(
                    value = hours,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                            hours = it
                        }
                    },
                    label = { Text("عدد الساعات") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(hours) }) { Text("إدراج") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun rememberClipboardText(context: Context): String {
    var clipboardText by remember { mutableStateOf("") }
    var refreshKey by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refreshKey) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    }
    DisposableEffect(context) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            refreshKey = System.currentTimeMillis()
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.addPrimaryClipChangedListener(listener)
        onDispose {
            clipboard?.removePrimaryClipChangedListener(listener)
        }
    }
    return clipboardText
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    val componentName = ComponentName(context, TextExpanderService::class.java).flattenToString()
    return enabledServices.split(':').any { it.equals(componentName, ignoreCase = true) }
}
