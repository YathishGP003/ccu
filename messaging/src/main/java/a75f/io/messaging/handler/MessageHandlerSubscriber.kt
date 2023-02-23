package a75f.io.messaging.handler

import a75f.io.messaging.service.MessageHandlerService
import android.content.Context

class MessageHandlerSubscriber {
    companion object {
        fun subscribeAllHandlers(context : Context) {
            val handlerService = MessageHandlerService.getInstance(context)
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.UpdateEntity
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.UpdatePoint
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.UpdateSchedule
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.SiteSync
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.RemoveEntity
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.RemoteCommand
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.PredefinedAlert
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.CustomAlert
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.AlertRemoval
            ))
            handlerService.registerMessageHandler(MessageHandlerFactory.createInstance(
                MessageHandlerFactory.Command.AlertDefRemoval
            ))
        }
    }
}