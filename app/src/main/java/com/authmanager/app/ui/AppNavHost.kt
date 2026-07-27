package com.authmanager.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.authmanager.app.data.AppViewModel
import com.authmanager.app.ui.screens.AboutScreen
import com.authmanager.app.ui.screens.DeviceManagementScreen
import com.authmanager.app.ui.screens.HomeScreen
import com.authmanager.app.ui.screens.KeyManagementScreen
import com.authmanager.app.ui.screens.LoginScreen

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val KEY_MANAGEMENT = "key_management"
    const val DEVICE_MANAGEMENT = "device_management"
    const val ABOUT = "about"
}

@Composable
fun AppNavHost() {
    val navController: NavHostController = rememberNavController()
    val viewModel: AppViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    viewModel.login()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenKeyManagement = { navController.navigate(Routes.KEY_MANAGEMENT) },
                onOpenDeviceManagement = { navController.navigate(Routes.DEVICE_MANAGEMENT) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.KEY_MANAGEMENT) {
            KeyManagementScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.DEVICE_MANAGEMENT) {
            DeviceManagementScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
