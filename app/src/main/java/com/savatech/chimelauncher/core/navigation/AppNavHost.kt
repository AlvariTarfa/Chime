package com.savatech.chimelauncher.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.savatech.chimelauncher.feature.settings.AccessibilityDisclosureScreen
import com.savatech.chimelauncher.feature.settings.DataManagementScreen
import com.savatech.chimelauncher.feature.checkin.CheckInScreen
import com.savatech.chimelauncher.feature.checkin.CheckInHistoryScreen
import com.savatech.chimelauncher.feature.insights.InsightsScreen
import com.savatech.chimelauncher.feature.digest.DigestAllowListScreen
import com.savatech.chimelauncher.feature.digest.DigestConsentScreen
import com.savatech.chimelauncher.feature.digest.DigestItemsScreen
import com.savatech.chimelauncher.feature.digest.DigestSettingsScreen
import com.savatech.chimelauncher.feature.onboarding.OnboardingScreen
import com.savatech.chimelauncher.feature.onboarding.OnboardingViewModel

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Entry,
    ) {
        composable(Routes.Entry) { entry ->
            val viewModel: OnboardingViewModel = hiltViewModel(entry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.onboardingCompleted) {
                state.onboardingCompleted?.let { completed ->
                    navController.navigate(if (completed) Routes.Home else Routes.Onboarding) {
                        popUpTo(Routes.Entry) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onGoHome = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
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
            SettingsScreen(
                onOpenHiddenApps = { navController.navigate(Routes.HiddenApps) },
                onOpenCheckInHistory = { navController.navigate(Routes.CheckInHistory) },
                onOpenInsights = { navController.navigate(Routes.Insights) },
                onOpenDigest = { navController.navigate(Routes.DigestSettings) },
                onOpenAccessibilityDisclosure = {
                    navController.navigate(Routes.AccessibilityDisclosure)
                },
                onOpenDataManagement = { navController.navigate(Routes.DataManagement) },
                onRunSetupAgain = { navController.navigate(Routes.Onboarding) },
            )
        }
        composable(Routes.DataManagement) {
            DataManagementScreen(
                onBack = { navController.popBackStack() },
                onDeleted = {
                    navController.navigate(Routes.Onboarding) {
                        popUpTo(Routes.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.AccessibilityDisclosure) {
            AccessibilityDisclosureScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DigestSettings) {
            DigestSettingsScreen(
                onOpenConsent = { navController.navigate(Routes.DigestConsent) },
                onOpenAllowList = { navController.navigate(Routes.DigestAllowList) },
                onOpenItems = { navController.navigate(Routes.DigestItems) },
            )
        }
        composable(Routes.DigestConsent) {
            DigestConsentScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DigestAllowList) { DigestAllowListScreen() }
        composable(Routes.DigestItems) { DigestItemsScreen() }
        composable(Routes.Insights) {
            InsightsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CheckIn) { entry ->
            CheckInScreen(
                type = checkNotNull(entry.arguments?.getString("type")),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CheckInHistory) {
            CheckInHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.TaskDetail,
            arguments = listOf(
                navArgument("goalId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType },
            ),
        ) { entry ->
            GoalDetailScreen(
                onEditGoal = { goalId -> navController.navigate(Routes.goalEdit(goalId)) },
                onStartFocusSession = { goalId, taskId ->
                    navController.navigate(Routes.focusSession(goalId, taskId))
                },
                focusTaskId = entry.arguments?.getString("taskId"),
            )
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
