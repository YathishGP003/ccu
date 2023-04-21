package a75f.io.renatus.messaging

import a75f.io.data.message.DatabaseHelper
import a75f.io.messaging.di.MessagingModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn (components = [SingletonComponent::class],
    replaces = [MessagingModule::class])
class TestMessagingModule {
    @Provides
    fun provideMessageDatabaseHelper() : DatabaseHelper = TestMessageDatabaseHelper()
}
