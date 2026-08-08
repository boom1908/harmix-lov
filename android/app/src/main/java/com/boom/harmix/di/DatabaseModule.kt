package com.boom.harmix.di

import android.content.Context
import androidx.room.Room
import com.boom.harmix.data.local.HarmixDatabase
import com.boom.harmix.data.local.dao.PlaylistDao
import com.boom.harmix.data.local.dao.SavedSongDao
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
    fun provideHarmixDatabase(@ApplicationContext context: Context): HarmixDatabase =
        Room.databaseBuilder(context, HarmixDatabase::class.java, "harmix.db").build()

    @Provides
    fun provideSavedSongDao(database: HarmixDatabase): SavedSongDao = database.savedSongDao()

    @Provides
    fun providePlaylistDao(database: HarmixDatabase): PlaylistDao = database.playlistDao()
}
