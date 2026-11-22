package com.ca.screentime.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ca.screentime.data.Permissions

data class PermissionState(
    val usage: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val battery: Boolean,
    val phone: Boolean,
) {
    val core: Boolean get() = usage && overlay

    companion object {
        fun read(context: Context) = PermissionState(
            usage = Permissions.hasUsageAccess(context),
            overlay = Permissions.hasOverlay(context),
            notifications = Permissions.hasNotifications(context),
            battery = Permissions.ignoresBatteryOptimizations(context),
            phone = Permissions.hasPhoneState(context),
        )
    }
}

@Composable
fun PermissionsScreen(perms: PermissionState, onChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onChanged() }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("권한·설정", color = Palette.Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (perms.core) "필수 권한이 모두 허용되어 제한 기능이 작동 중이에요."
            else "제한 기능이 작동하려면 아래 필수 권한을 허용해 주세요.",
            color = Palette.Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )

        PermissionRow(
            title = "사용정보 접근",
            badge = "필수",
            description = "실제 휴대폰 사용시간을 계산하고, 지금 어떤 앱이 열려 있는지 확인해요.",
            granted = perms.usage,
        ) { context.openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS, withPackage = true) }

        PermissionRow(
            title = "다른 앱 위에 표시",
            badge = "필수",
            description = "한도를 넘으면 사용시간 초과 화면을 모든 앱 위에 띄워요.",
            granted = perms.overlay,
        ) { context.openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, withPackage = true) }

        PermissionRow(
            title = "알림",
            badge = "권장",
            description = "제한 기능이 꺼지지 않도록 사용시간 알림을 상단에 표시해요.",
            granted = perms.notifications,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        PermissionRow(
            title = "배터리 최적화 제외",
            badge = "권장",
            description = "절전 기능이 제한 기능을 강제로 끄지 않도록 해요.",
            granted = perms.battery,
        ) { context.requestIgnoreBatteryOptimizations() }

        PermissionRow(
            title = "전화 상태",
            badge = "선택",
            description = "통화 중에는 사용시간 초과 화면을 자동으로 숨겨요.",
            granted = perms.phone,
        ) { requestPermission.launch(Manifest.permission.READ_PHONE_STATE) }

        Card(radius = 18) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("알아두세요", color = Palette.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Bullet("화면이 꺼져 있는 시간은 앱이 실행 중이어도 사용시간에 넣지 않아요.")
                Bullet("전화·통화 화면·긴급전화는 제한하지 않고, 사용시간에도 넣지 않아요.")
                Bullet("잠금화면의 긴급전화는 언제나 쓸 수 있어요.")
                Bullet("한도를 넘은 날에는 그 날 한도를 늘릴 수 없어요. 줄이는 것은 언제든 가능해요.")
                Bullet("삼성·샤오미 등은 설정 › 배터리에서 이 앱을 '제한 없음'으로 두면 더 안정적이에요.")
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("·", color = Palette.Muted, fontSize = 13.sp)
        Text(text, color = Palette.Muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    badge: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Card(radius = 18) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        badge,
                        modifier = Modifier
                            .background(if (badge == "필수") Palette.BadgeBg else Palette.Track, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = if (badge == "필수") Palette.BadgeFg else Palette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Dot(if (granted) Palette.GreenDot else Palette.ClayText)
                    Text(
                        if (granted) "허용됨" else "허용 필요",
                        color = if (granted) Palette.Green else Palette.ClayText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(description, color = Palette.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            if (!granted) {
                Surface(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Palette.Ink,
                    border = BorderStroke(1.dp, Palette.Ink),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("허용하러 가기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun Context.openSettings(action: String, withPackage: Boolean) {
    val primary = Intent(action).apply {
        if (withPackage) data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(primary) }.recoverCatching {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@SuppressLint("BatteryLife")
private fun Context.requestIgnoreBatteryOptimizations() {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }.recoverCatching {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
