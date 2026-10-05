package com.savatech.chimelauncher.data.goals

import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import com.savatech.chimelauncher.domain.model.DailyPriorityModel
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import java.time.LocalDate

fun Goal.toModel(): GoalModel = GoalModel(
    id = id,
    title = title,
    why = why,
    category = category,
    targetDate = targetDate?.let(LocalDate::parse),
    unit = unit,
    targetValue = targetValue,
    status = GoalStatus.fromStorage(status),
    createdAt = createdAt,
)

fun GoalModel.toEntity(): Goal = Goal(
    id = id,
    title = title,
    why = why,
    category = category,
    targetDate = targetDate?.toString(),
    unit = unit,
    targetValue = targetValue,
    status = status.name,
    createdAt = createdAt,
)

fun Task.toModel(): TaskModel = TaskModel(
    id = id,
    goalId = goalId,
    title = title,
    recurrence = Recurrence.fromStorage(recurrence),
    daysMask = daysMask,
    reminderTime = reminderTime,
    createdAt = createdAt,
)

fun TaskModel.toEntity(): Task = Task(
    id = id,
    goalId = goalId,
    title = title,
    recurrence = recurrence.name,
    daysMask = daysMask,
    reminderTime = reminderTime,
    createdAt = createdAt,
)

fun TaskLog.toModel(): TaskLogModel = TaskLogModel(
    id = id,
    taskId = taskId,
    date = LocalDate.parse(date),
    value = value,
    completed = completed,
)

fun TaskLogModel.toEntity(): TaskLog = TaskLog(
    id = id,
    taskId = taskId,
    date = date.toString(),
    value = value,
    completed = completed,
)

fun DailyPriority.toModel(): DailyPriorityModel = DailyPriorityModel(
    date = LocalDate.parse(date),
    goalId = goalId,
    position = position,
)

fun DailyPriorityModel.toEntity(): DailyPriority = DailyPriority(
    date = date.toString(),
    goalId = goalId,
    position = position,
)
