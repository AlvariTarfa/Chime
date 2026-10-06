package com.savatech.chimelauncher.core.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.savatech.chimelauncher.data.db.AppDatabase
import com.savatech.chimelauncher.data.db.dao.AppConfigDao
import com.savatech.chimelauncher.data.db.dao.AppGrantDao
import com.savatech.chimelauncher.data.db.dao.CheckInDao
import com.savatech.chimelauncher.data.db.dao.DailyPriorityDao
import com.savatech.chimelauncher.data.db.dao.GoalDao
import com.savatech.chimelauncher.data.db.dao.InterceptEventDao
import com.savatech.chimelauncher.data.db.dao.TaskDao
import com.savatech.chimelauncher.data.db.dao.TaskLogDao
import com.savatech.chimelauncher.data.db.dao.FocusModeDao
import com.savatech.chimelauncher.data.db.dao.FocusSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val builder = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }
        return builder.build()
    }

    @Provides
    fun provideAppConfigDao(database: AppDatabase): AppConfigDao = database.appConfigDao()

    @Provides
    fun provideAppGrantDao(database: AppDatabase): AppGrantDao = database.appGrantDao()

    @Provides
    fun provideInterceptEventDao(database: AppDatabase): InterceptEventDao = database.interceptEventDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskLogDao(database: AppDatabase): TaskLogDao = database.taskLogDao()

    @Provides
    fun provideDailyPriorityDao(database: AppDatabase): DailyPriorityDao = database.dailyPriorityDao()

    @Provides
    fun provideFocusModeDao(database: AppDatabase): FocusModeDao = database.focusModeDao()

    @Provides
    fun provideFocusSessionDao(database: AppDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideCheckInDao(database: AppDatabase): CheckInDao = database.checkInDao()

    private const val DATABASE_NAME = "chime.db"
}
