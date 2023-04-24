package a75f.io.messaging.di

import a75f.io.data.message.MessageDatabaseHelper
import a75f.io.messaging.service.MessageHandlerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MessagingEntryPoint {
    var dbHelper : MessageDatabaseHelper
    var messagingHandlerService : MessageHandlerService
}