package a75f.io.messaging.di

import a75f.io.data.RenatusDatabaseBuilder
import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.MessageDatabaseHelper
import a75f.io.logic.Globals
import a75f.io.messaging.service.MessageHandlerService
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MessagingModule {
    @Provides
    @Singleton
    fun provideMessageHandlerService(@ApplicationContext appContext: Context, dbHelper:
                MessageDatabaseHelper): MessageHandlerService =
                                    MessageHandlerService(appContext, dbHelper)
}