package com.example.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.TextExpanderService
import com.example.util.TextExpanderPreferences

@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var serviceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var blacklistText by remember { mutableStateOf(TextExpanderPreferences.getBlacklistedPackagesText(context)) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isAccessibilityServiceEnabled(context)
                blacklistText = TextExpanderPreferences.getBlacklistedPackagesText(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("إعدادات التوسيع", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "اضبط التطبيقات المستثناة، راقب حالة الخدمة، وراجع المتغيرات الذكية المدعومة.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            SettingsActionCard(
                icon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                title = "حالة خدمة إمكانية الوصول",
                description = if (serviceEnabled) {
                    "الخدمة مفعلة وجاهزة لتوسيع الاختصارات داخل التطبيقات."
                } else {
                    "الخدمة غير مفعلة بعد. فعّلها من إعدادات إمكانية الوصول."
                },
                primaryAction = "فتح الإعدادات",
                secondaryAction = "معلومات التطبيق",
                onPrimaryClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onSecondaryClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
        item {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null)
                        Text("استثناء التطبيقات", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "أضف أسماء الحزم سطرًا بسطر حتى لا يعمل الموسّع داخلها. مثال: com.whatsapp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = blacklistText,
                        onValueChange = { blacklistText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text("الحزم المستثناة") }
                    )
                    Button(
                        onClick = {
                            TextExpanderPreferences.setBlacklistedPackages(context, blacklistText)
                            savedMessage = "تم حفظ التطبيقات المستثناة"
                        }
                    ) {
                        Text("حفظ القائمة")
                    }
                }
            }
        }
        item {
            SettingsInfoCard(
                icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                title = "المتغيرات الذكية",
                lines = listOf(
                    "%date% يعرض التاريخ الحالي",
                    "%day% يعرض اسم اليوم",
                    "%time% يعرض الوقت الحالي",
                    "%time+1.5h% يضيف ساعات عشرية مثل 1.3 و1.5",
                    "%clipboard% يدرج آخر نص منسوخ",
                    "%cursor% يحدد موضع المؤشر بعد التوسيع"
                )
            )
        }
        item {
            SettingsInfoCard(
                icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                title = "نصائح الاستخدام",
                lines = listOf(
                    "قسّم الاختصارات داخل التطبيق حسب التصنيفات لتسريع الوصول.",
                    "ثبّت القوالب المتكررة حتى تظهر أولاً في القائمة.",
                    "إذا ظهر إعداد مقيد في أندرويد 13/14 فامنحه من معلومات التطبيق."
                )
            )
        }
        item {
            savedMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    primaryAction: String,
    secondaryAction: String? = null,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null
) {
    ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icon()
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPrimaryClick) {
                    Text(primaryAction)
                }
                if (secondaryAction != null && onSecondaryClick != null) {
                    TextButton(onClick = onSecondaryClick) {
                        Text(secondaryAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(
    icon: @Composable () -> Unit,
    title: String,
    lines: List<String>
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icon()
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            lines.forEach { line ->
                Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    val componentName = ComponentName(context, TextExpanderService::class.java).flattenToString()
    return enabledServices.split(':').any { it.equals(componentName, ignoreCase = true) }
}
