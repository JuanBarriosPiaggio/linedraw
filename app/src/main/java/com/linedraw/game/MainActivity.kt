package com.linedraw.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.linedraw.game.ui.navigation.LineDrawNavGraph
import com.linedraw.game.ui.theme.LineDrawTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LineDrawTheme {
                LineDrawNavGraph(app = application as LineDrawApp)
            }
        }
    }
}
