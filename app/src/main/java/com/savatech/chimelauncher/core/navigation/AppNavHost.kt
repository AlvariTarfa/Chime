package com.savatech.chimelauncher.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.savatech.chimelauncher.feature.drawer.DrawerScreen
import com.savatech.chimelauncher.feature.drawer.HiddenAppsScreen
import com.savatech.chimelauncher.feature.goals.GoalsScreen
import com.savatech.chimelauncher.feature.goals.GoalDetailScreen
import com.savatech.chimelauncher.feature.goals.GoalEditScreen
import com.savatech.chimelauncher.feature.goals.PriorityScreen
import com.savatech.chimelauncher.feature.home.HomeScreen
import com.savatech.chimelauncher.feature.intercept.InterceptScreen
import com.savatech.chimelauncher.feature.focus.FocusModesScreen
import com.savatech.chimelauncher.feature.focus.FocusSessionScreen
import com.savatech.chimelauncher.feature.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onOpenDrawer = { navController.navigate(Routes.Drawer) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenGoals = { navController.navigate(Routes.Goals) },
                onCreateGoal = { navController.navigate(Routes.goalEdit()) },
                onOpenPriorities = { navController.navigate(Routes.GoalPriorities) },
                onOpenGoal = { goalId -> navController.navigate(Routes.goalDetail(goalId)) },
                onNeedsPause = { args -> navController.navigate(Routes.intercept(args)) },
                onManageFocusModes = { navController.navigate(Routes.FocusModes) },
            )
        }
        composable(Routes.Drawer) {
            DrawerScreen(
                onOpenHiddenApps = { navController.navigate(Routes.HiddenApps) },
                onOpenGoals = { navController.navigate(Routes.Goals) },
                onNeedsPause = { args -> navController.navigate(Routes.intercept(args)) },
            )
        }
        composable(
            route = Routes.Intercept,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType },
                navArgument("userSerial") { type = NavType.LongType },
                navArgument("delaySeconds") { type = NavType.IntType },
                navArgument("reason") { type = NavType.StringType },
                navArgument("appLabel") { type = NavType.StringType },
                navArgument("usedMillis") { type = NavType.LongType; defaultValue = 0L },
                navArgument("limitMinutes") {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) {
            InterceptScreen(
                onFinish = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.Goals) {
            GoalsScreen(
                onCreateGoal = { navController.navigate(Routes.goalEdit()) },
                onOpenGoal = { goalId -> navController.navigate(Routes.goalDetail(goalId)) },
                onOpenPriorities = { navController.navigate(Routes.GoalPriorities) },
            )
        }
        composable(Routes.GoalEdit) {
            GoalEditScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GoalDetail) {
            GoalDetailScreen(
                onEditGoal = { goalId -> navController.navigate(Routes.goalEdit(goalId)) },
                onStartFocusSession = { goalId, taskId ->
                    navController.navigate(Routes.focusSession(goalId, taskId))
                },
            )
        }
        composable(Routes.GoalPriorities) {
            PriorityScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(onOpenHiddenApps = { navController.navigate(Routes.HiddenApps) })
        }
        composable(Routes.HiddenApps) { HiddenAppsScreen() }
        composable(Routes.FocusModes) {
            FocusModesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.FocusSession,
            arguments = listOf(
                navArgument("goalId") { type = NavType.StringType; defaultValue = "" },
                navArgument("taskId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            FocusSessionScreen(
                goalId = entry.arguments?.getString("goalId")?.takeIf(String::isNotBlank),
                taskId = entry.arguments?.getString("taskId")?.takeIf(String::isNotBlank),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
