package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.service.TextExpanderService
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val serviceEnabled by produceState(initialValue = isTextExpanderEnabled(context), context) {
        while (true) {
            value = isTextExpanderEnabled(context)
            delay(1200)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إعدادات التطبيق", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "هذا الإصدار يركز بالكامل على الاختصارات وتوسيع النصوص، بدون أدوات الشبكة القديمة.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = if (serviceEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = if (serviceEnabled) "الخدمة مفعلة" else "الخدمة غير مفعلة",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            SettingsActionCard(
                title = "تفعيل إمكانية الوصول",
                description = "افتح إعدادات إمكانية الوصول وفعّل خدمة التطبيق حتى تعمل الاختصارات داخل أي تطبيق.",
                primaryAction = "فتح الإعدادات",
                secondaryAction = null,
                onPrimaryClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onSecondaryClick = {}
            )
        }

        item {
            SettingsActionCard(
                title = "حل مشكلة Restricted setting",
                description = "في أندرويد 13 و14 قد تحتاج أولًا إلى السماح بالإعدادات المقيدة من معلومات التطبيق.",
                primaryAction = "معلومات التطبيق",
                secondaryAction = "إعدادات الخدمة",
                onPrimaryClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onSecondaryClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }

        item {
            TipsCard()
        }

        item {
            SupportedTokensCard()
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    description: String,
    primaryAction: String,
    secondaryAction: String?,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPrimaryClick) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(primaryAction)
                }
                if (secondaryAction != null) {
                    FilledTonalButton(onClick = onSecondaryClick) {
                        Text(secondaryAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("نصائح سريعة", style = MaterialTheme.typography.titleLarge)
            Text("اكتب الكلمة المفتاحية ثم مسافة ليتم الاستبدال تلقائيًا.")
            Text("استخدم التثبيت للاختصارات المهمة حتى تظهر دائمًا أولًا.")
            Text("استفد من المعاينة داخل شاشة الإضافة للتأكد من التاريخ والوقت قبل الحفظ.")
        }
    }
}

@Composable
private fun SupportedTokensCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("الأوسمة المدعومة", style = MaterialTheme.typography.titleLarge)
            TokenRow(token = "%date%", description = "التاريخ بصيغة  yyyy-MM-dd")
            TokenRow(token = "%day%", description = "اسم اليوم الحالي")
            TokenRow(token = "%time%", description = "الوقت الحالي")
            TokenRow(token = "%time+1.5h%", description = "الوقت بعد مدة عشرية مثل 1.5 أو 1.3")
        }
    }
}

@Composable
private fun TokenRow(token: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = token,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun isTextExpanderEnabled(context: android.content.Context): Boolean {
    val enabledServices =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
    return enabledServices.contains("${context.packageName}/${TextExpanderService::class.java.name}")
}
