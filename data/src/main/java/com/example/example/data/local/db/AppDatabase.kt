package com.example.example.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.example.data.local.db.dao.OfflineAreaDao
import com.example.example.data.local.db.dao.PlaceOfInterestDao
import com.example.example.data.local.db.entity.OfflineAreaEntity
import com.example.example.data.local.db.entity.PlaceOfInterestEntity

@Database(
    entities = [PlaceOfInterestEntity::class, OfflineAreaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineAreaDao(): OfflineAreaDao
    abstract fun placeOfInterestDao(): PlaceOfInterestDao
}
