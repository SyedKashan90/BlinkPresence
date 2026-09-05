package com.smartattendance.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartattendance.app.ServiceLocator
import com.smartattendance.app.ui.components.AppScaffold
import com.smartattendance.app.ui.screens.enroll.FaceEnrollScreen
import com.smartattendance.app.ui.screens.history.HistoryScreen
import com.smartattendance.app.ui.screens.home.HomeScreen
import com.smartattendance.app.ui.screens.login.LoginScreen
import com.smartattendance.app.ui.screens.profile.ProfileScreen
import com.smartattendance.app.ui.screens.result.AttendanceResultScreen
import com.smartattendance.app.ui.screens.scan.QrScanScreen
import com.smartattendance.app.ui.screens.verify.FaceVerifyScreen
import java.net.URLDecoder

@Composable
fun SmartAttendanceNavGraph(services: ServiceLocator) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (services.authRepository.isLoggedIn) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                services = services,
                onLoggedIn = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }

        composable("home") {
            AppScaffold(
                currentRoute = "home",
                onTabSelected = { route -> navController.navigate(route) { launchSingleTop = true; popUpTo("home") } },
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier) {
                    HomeScreen(
                        services = services,
                        onScanQr = {
                            if (services.authRepository.isFaceEnrolled) {
                                navController.navigate(Routes.QR_SCAN)
                            } else {
                                navController.navigate(Routes.FACE_ENROLL)
                            }
                        },
                        onEnrollFace = { navController.navigate(Routes.FACE_ENROLL) },
                    )
                }
            }
        }

        composable("history") {
            AppScaffold(
                currentRoute = "history",
                onTabSelected = { route -> navController.navigate(route) { launchSingleTop = true; popUpTo("home") } },
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier) { HistoryScreen(services) }
            }
        }

        composable("profile") {
            AppScaffold(
                currentRoute = "profile",
                onTabSelected = { route -> navController.navigate(route) { launchSingleTop = true; popUpTo("home") } },
            ) { modifier ->
                androidx.compose.foundation.layout.Box(modifier) {
                    ProfileScreen(
                        services = services,
                        onLoggedOut = {
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
            }
        }

        composable(Routes.FACE_ENROLL) {
            FaceEnrollScreen(
                services = services,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(Routes.QR_SCAN) {
            QrScanScreen(services = services) { token, courseCode ->
                navController.navigate(Routes.faceVerify(token, courseCode)) {
                    popUpTo(Routes.QR_SCAN) { inclusive = true }
                }
            }
        }

        composable(
            route = Routes.FACE_VERIFY,
            arguments = listOf(navArgument("sessionToken") { type = NavType.StringType }, navArgument("courseCode") { type = NavType.StringType }),
        ) { backStackEntry ->
            val token = URLDecoder.decode(backStackEntry.arguments?.getString("sessionToken") ?: "", "UTF-8")
            val courseCode = URLDecoder.decode(backStackEntry.arguments?.getString("courseCode") ?: "", "UTF-8")
            FaceVerifyScreen(services = services, qrToken = token, courseCode = courseCode) { outcome, message ->
                navController.navigate(Routes.result(outcome, message)) {
                    popUpTo("home")
                }
            }
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument("outcome") { type = NavType.StringType }, navArgument("message") { type = NavType.StringType }),
        ) { backStackEntry ->
            val outcome = backStackEntry.arguments?.getString("outcome") ?: "error"
            val message = URLDecoder.decode(backStackEntry.arguments?.getString("message") ?: "", "UTF-8")
            AttendanceResultScreen(outcome = outcome, message = message) {
                navController.navigate("home") { popUpTo("home") { inclusive = true } }
            }
        }
    }
}
