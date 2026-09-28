package com.hifth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hifth.ui.HifthApp
import com.hifth.ui.HifthTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HifthViewModel by viewModels {
        HifthViewModel.Factory((application as HifthApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HifthTheme {
                HifthApp(viewModel)
            }
        }
    }
}
