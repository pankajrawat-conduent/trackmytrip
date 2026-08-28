package com.example.trackmytrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.trackmytrip.ui.TrackMyTripApp
import com.example.trackmytrip.ui.theme.TrackMyTripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrackMyTripTheme {
                TrackMyTripApp()
            }
        }
    }
}
