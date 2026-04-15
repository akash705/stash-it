package com.stashed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stashed.app.billing.BillingManager
import com.stashed.app.ui.navigation.StashedNavHost
import com.stashed.app.ui.theme.StashedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager.connect()
        enableEdgeToEdge()
        setContent {
            StashedTheme {
                StashedNavHost()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}
