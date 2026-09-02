package com.example.pinmind.core.di

import android.content.Context
import androidx.room.Room
import com.example.pinmind.core.database.PinMindDatabase
import com.example.pinmind.data.local.TaskDao
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
    fun providePinMindDatabase(
        @ApplicationContext context: Context
    ): PinMindDatabase {
        return Room.databaseBuilder(
            context,
            PinMindDatabase::class.java,
            PinMindDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: PinMindDatabase): TaskDao {
        return database.taskDao()
    }
}
