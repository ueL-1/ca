package com.ca.screentime.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ca.screentime.data.Engine
import com.ca.screentime.service.MonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MonitorService.startIfReady(this)
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val engine = remember { Engine(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    var status by remember { mutableStateOf<Engine.Status?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var wasReady = perms.core
            while (true) {
                val p = PermissionState.read(context)
                perms = p
                if (p.core && !wasReady) MonitorService.startIfReady(context)
                wasReady = p.core
                status = if (p.usage) withContext(Dispatchers.Default) { engine.compute() } else null
                delay(1_000L)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        when (tab) {
            0 -> MainScreen(
                status = status,
                perms = perms,
                engine = engine,
                onOpenPermissions = { tab = 1 },
                modifier = Modifier.weight(1f),
            )
            else -> PermissionsScreen(
                perms = perms,
                onChanged = { perms = PermissionState.read(context) },
                modifier = Modifier.weight(1f),
            )
        }
        BottomBar(selected = tab, onSelect = { tab = it })
    }
}
