package app.accordgarage

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import app.accordgarage.ui.AppNav
import app.accordgarage.ui.LocalContainer
import app.accordgarage.ui.theme.DarkGarage
import app.accordgarage.ui.theme.GarageTheme
import app.accordgarage.ui.theme.LightGarage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GarageApp).container

        setContent {
            var light by remember { mutableStateOf(container.lightTheme) }

            LaunchedEffect(light) {
                val g = if (light) LightGarage else DarkGarage
                @Suppress("DEPRECATION")
                window.statusBarColor = g.bg.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = g.bar.toArgb()
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }

            val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 && !container.prefs.getBoolean("asked_notifications", false)) {
                    container.prefs.edit().putBoolean("asked_notifications", true).apply()
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CompositionLocalProvider(LocalContainer provides container) {
                GarageTheme(light = light) {
                    AppNav(lightTheme = light, onToggleTheme = {
                        light = it
                        container.lightTheme = it
                    })
                }
            }
        }
    }
}
