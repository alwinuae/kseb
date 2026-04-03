package com.kseb.billcalculator.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kseb.billcalculator.model.BillingCycle
import com.kseb.billcalculator.model.PhaseType
import com.kseb.billcalculator.model.ReverseResult
import com.kseb.billcalculator.pdf.PdfGenerator
import com.kseb.billcalculator.pdf.PdfSharer
import com.kseb.billcalculator.ui.appliance.ApplianceScreen
import com.kseb.billcalculator.ui.billtounits.BillToUnitsScreen
import com.kseb.billcalculator.ui.phasecompare.PhaseCompareScreen
import com.kseb.billcalculator.ui.settings.SettingsScreen
import com.kseb.billcalculator.ui.unitstobill.UnitsToBillScreen

sealed class Screen(val route: String) {
    data object BillCalc : Screen("bill_calc")
    data object Appliance : Screen("appliance")
    data object Reverse : Screen("reverse")
    data object Compare : Screen("compare")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.BillCalc, "Bill Calc", Icons.Filled.Receipt),
    BottomNavItem(Screen.Appliance, "Appliance", Icons.Filled.Power),
    BottomNavItem(Screen.Reverse, "Reverse", Icons.Filled.SwapVert),
    BottomNavItem(Screen.Compare, "Compare", Icons.Filled.CompareArrows)
)

@Composable
fun KSEBBillCalculatorApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isSettingsScreen = currentRoute == Screen.Settings.route
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (!isSettingsScreen) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.BillCalc.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.BillCalc.route) {
                UnitsToBillScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onExportPdf = { breakdown ->
                        val file = PdfGenerator.generateBillPdf(
                            context, breakdown, !breakdown.isTelescopicBilling
                        )
                        PdfSharer.sharePdf(context, file)
                    }
                )
            }

            composable(Screen.Appliance.route) {
                ApplianceScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCompare = { units, cycle ->
                        navController.navigate(Screen.Compare.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExportPdf = { applianceResult, breakdown ->
                        val file = PdfGenerator.generateAppliancePdf(
                            context, applianceResult, breakdown, false
                        )
                        PdfSharer.sharePdf(context, file)
                    }
                )
            }

            composable(Screen.Reverse.route) {
                BillToUnitsScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCalculator = { units, phase, cycle ->
                        navController.navigate(Screen.BillCalc.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExportPdf = { result ->
                        if (result is ReverseResult.Match) {
                            val file = PdfGenerator.generateReversePdf(context, result, false)
                            PdfSharer.sharePdf(context, file)
                        }
                    }
                )
            }

            composable(Screen.Compare.route) {
                PhaseCompareScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onExportPdf = { singlePhase, threePhase ->
                        val file = PdfGenerator.generateComparisonPdf(
                            context, singlePhase, threePhase, false
                        )
                        PdfSharer.sharePdf(context, file)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
