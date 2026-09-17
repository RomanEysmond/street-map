package com.example.example.data.local.db.di

import android.content.Context
import androidx.room.Room
import com.example.example.data.local.db.AppDatabase
import com.example.example.data.local.db.dao.OfflineAreaDao
import com.example.example.data.local.db.dao.PlaceOfInterestDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "street-map.db").build()

    @Provides
    fun provideOfflineAreaDao(database: AppDatabase): OfflineAreaDao = database.offlineAreaDao()

    @Provides
    fun providePlaceOfInterestDao(database: AppDatabase): PlaceOfInterestDao = database.placeOfInterestDao()
}
