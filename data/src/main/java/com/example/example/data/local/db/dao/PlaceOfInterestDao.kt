package com.example.example.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.example.data.local.db.entity.PlaceOfInterestEntity

@Dao
interface PlaceOfInterestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<PlaceOfInterestEntity>)

    @Query("SELECT * FROM places_of_interest WHERE offlineAreaId = :offlineAreaId")
    suspend fun getByOfflineAreaId(offlineAreaId: Long): List<PlaceOfInterestEntity>

    @Query("DELETE FROM places_of_interest WHERE offlineAreaId = :offlineAreaId")
    suspend fun deleteByOfflineAreaId(offlineAreaId: Long)

    @Query(
        """
        SELECT * FROM places_of_interest
        WHERE latitude BETWEEN :south AND :north
        AND longitude BETWEEN :west AND :east
        """
    )
    suspend fun getWithinBoundingBox(north: Double, south: Double, east: Double, west: Double): List<PlaceOfInterestEntity>
}
