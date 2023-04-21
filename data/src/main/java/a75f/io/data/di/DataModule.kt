package a75f.io.data.di

import a75f.io.data.RenatusDatabase
import a75f.io.data.RenatusDatabaseBuilder
import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.MessageDao
import a75f.io.data.message.MessageDatabaseHelper
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
    @Provides
    @Singleton
    fun provideRenatusDatabase(@ApplicationContext appContext: Context): RenatusDatabase {
        return Room.databaseBuilder(
                    appContext,
                    RenatusDatabase::class.java,
                    "renatusDb")
                    .fallbackToDestructiveMigration()
                    .build()
    }

    @Provides
    @Singleton
    fun provideMessageDatabaseHelper(@ApplicationContext appContext: Context): DatabaseHelper = MessageDatabaseHelper(
        RenatusDatabaseBuilder.getInstance(appContext)
    )

    @Provides
    fun provideMessageDao(appDatabase: RenatusDatabase): MessageDao {
        return appDatabase.messageDao()
    }
}