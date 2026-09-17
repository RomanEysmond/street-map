package com.example.example.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.example.data.local.db.entity.OfflineAreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineAreaDao {
    @Query("SELECT * FROM offline_areas ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<OfflineAreaEntity>>

    @Query("SELECT COUNT(*) FROM offline_areas")
    suspend fun count(): Int

    @Insert
    suspend fun insert(area: OfflineAreaEntity): Long

    @Query("DELETE FROM offline_areas WHERE id = :id")
    suspend fun deleteById(id: Long)
}
