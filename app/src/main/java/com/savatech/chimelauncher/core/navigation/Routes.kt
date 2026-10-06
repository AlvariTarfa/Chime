package com.savatech.chimelauncher.core.navigation

import android.net.Uri
import com.savatech.chimelauncher.core.launch.PauseLaunchArgs

object Routes {
    const val Entry = "entry"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Drawer = "drawer"
    const val Goals = "goals"
    const val GoalEdit = "goals/edit/{goalId}"
    const val GoalDetail = "goals/detail/{goalId}"
    const val TaskDetail = "task/{goalId}/{taskId}"
    const val CheckIn = "checkin/{type}"
    const val CheckInHistory = "checkin-history"
    const val Insights = "insights"
    const val GoalPriorities = "goals/priorities"
    const val NewGoalId = "new"
    fun goalEdit(goalId: String = NewGoalId): String = "goals/edit/$goalId"
    fun goalDetail(goalId: String): String = "goals/detail/$goalId"
    fun task(goalId: String, taskId: String): String =
        "task/${Uri.encode(goalId)}/${Uri.encode(taskId)}"
    fun checkIn(type: String): String = "checkin/${Uri.encode(type)}"
    const val Settings = "settings"
    const val HiddenApps = "hidden-apps"
    const val FocusModes = "focus-modes"
    const val FocusSession = "focus-session?goalId={goalId}&taskId={taskId}"
    fun focusSession(goalId: String? = null, taskId: String? = null): String =
        "focus-session?goalId=${Uri.encode(goalId.orEmpty())}&taskId=${Uri.encode(taskId.orEmpty())}"
    const val Intercept = "intercept/{packageName}/{className}/{userSerial}" +
        "?delaySeconds={delaySeconds}&reason={reason}&appLabel={appLabel}" +
        "&usedMillis={usedMillis}&limitMinutes={limitMinutes}"

    fun intercept(args: PauseLaunchArgs): String =
        "intercept/${Uri.encode(args.packageName)}/${Uri.encode(args.className)}/${args.userSerial}" +
            "?delaySeconds=${args.delaySeconds}&reason=${args.reason.name}" +
            "&appLabel=${Uri.encode(args.appLabel)}" +
            "&usedMillis=${args.usedMillis}&limitMinutes=${args.limitMinutes ?: 0}"
}
