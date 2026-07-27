package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppPrefs
import com.example.util.OverlayHelper

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    var overlayEnabled by remember { mutableStateOf(AppPrefs.isOverlayEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(OverlayHelper.canShowOverlay(context)) }
    var quickTimesText by remember { mutableStateOf(AppPrefs.getQuickTimesRaw(context)) }
    var scanIntervalText by remember { mutableStateOf(AppPrefs.getScanIntervalSeconds(context).toString()) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("الإعدادات", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("الإشعار العائم (Overlay)", fontWeight = FontWeight.Bold)
                Text(
                    "يظهر فوق أي تطبيق فاتحه لما يتصل جهاز جديد، مع أزرار سريعة لإضافة الوقت.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(
                        checked = overlayEnabled,
                        onCheckedChange = {
                            overlayEnabled = it
                            AppPrefs.setOverlayEnabled(context, it)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (overlayEnabled) "مفعّل" else "متوقف")
                }

                if (!hasOverlayPermission) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "لازم تسمح للتطبيق بالظهور فوق التطبيقات الأخرى عشان يشتغل الإشعار العائم.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) {
                        Text("منح إذن الظهور فوق التطبيقات")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("الأوقات السريعة", fontWeight = FontWeight.Bold)
                Text(
                    "الأرقام اللي بتظهر كأزرار سريعة لإضافة الوقت (افصل بينها بفاصلة، وتقدر تستخدم كسور زي 1.5).",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quickTimesText,
                    onValueChange = { quickTimesText = it },
                    label = { Text("مثال: 1,1.5,2,3,5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("فترة الفحص التلقائي للشبكة", fontWeight = FontWeight.Bold)
                Text(
                    "كل قد إيه (بالثواني) يفحص التطبيق الشبكة تلقائيًا في الخلفية.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = scanIntervalText,
                    onValueChange = { if (it.all(Char::isDigit)) scanIntervalText = it },
                    label = { Text("بالثواني (الحد الأدنى 10)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                AppPrefs.setQuickTimesRaw(context, quickTimesText)
                val seconds = scanIntervalText.toIntOrNull() ?: 60
                AppPrefs.setScanIntervalSeconds(context, seconds)
                hasOverlayPermission = OverlayHelper.canShowOverlay(context)
                savedMessage = "تم الحفظ"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ الإعدادات")
        }

        savedMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}
