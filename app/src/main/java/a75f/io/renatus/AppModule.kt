package a75f.io.renatus

import a75f.io.logic.Globals
import a75f.io.messaging.database.DatabaseHelper
import a75f.io.messaging.database.MessageDatabaseBuilder
import a75f.io.messaging.database.MessageDatabaseHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideMessageDatabaseHelper() : DatabaseHelper =
        MessageDatabaseHelper(MessageDatabaseBuilder.getInstance(Globals.getInstance().applicationContext))
}