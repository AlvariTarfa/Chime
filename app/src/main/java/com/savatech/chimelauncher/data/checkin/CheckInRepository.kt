package com.savatech.chimelauncher.data.checkin

import com.savatech.chimelauncher.data.db.dao.CheckInDao
import com.savatech.chimelauncher.data.db.entities.CheckIn
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface CheckInRepository {
    suspend fun save(checkIn: CheckIn)
    fun observeHistory(): Flow<List<CheckIn>>
}

@Singleton
class RoomCheckInRepository @Inject constructor(
    private val dao: CheckInDao,
) : CheckInRepository {
    override suspend fun save(checkIn: CheckIn) {
        dao.insert(checkIn)
    }

    override fun observeHistory(): Flow<List<CheckIn>> = dao.observeHistory()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckInRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCheckInRepository(repository: RoomCheckInRepository): CheckInRepository
}
