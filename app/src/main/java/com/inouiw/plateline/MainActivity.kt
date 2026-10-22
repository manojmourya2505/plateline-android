package com.inouiw.plateline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inouiw.plateline.ui.PlatelineApp
import com.inouiw.plateline.ui.theme.PlatelineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PlatelineTheme { PlatelineApp() } }
    }
}
