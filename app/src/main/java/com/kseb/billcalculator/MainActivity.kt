package com.kseb.billcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kseb.billcalculator.navigation.KSEBBillCalculatorApp
import com.kseb.billcalculator.ui.theme.KSEBBillCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KSEBBillCalculatorTheme {
                KSEBBillCalculatorApp()
            }
        }
    }
}
