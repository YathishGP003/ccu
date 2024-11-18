package a75f.io.messaging

import a75f.io.data.message.MessageDatabaseHelper
import a75f.io.messaging.service.MessageHandlerService
import javax.inject.Inject

class MessageHandlerSubscriber @Inject constructor(){

    @Inject
    lateinit var messageDbHelper : MessageDatabaseHelper

    @Inject
    lateinit var handlerService: MessageHandlerService

    fun subscribeAllHandlers() {

        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.UpdateEntity
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.UpdatePoint
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.UpdateSchedule
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.SiteSync
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.RemoveEntity
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.RemoteCommand
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.PredefinedAlert
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.CustomAlert
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.AlertRemoval
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.AlertDefRemoval
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.AutoCommissioningMode
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.SCHEDULE_MIGRATED
            )
        )

        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.fixAlert
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.sequenceCreated
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.sequenceUpdated
            )
        )
        handlerService.registerMessageHandler(
            MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.sequenceDeleted
            )
        )
    }
}