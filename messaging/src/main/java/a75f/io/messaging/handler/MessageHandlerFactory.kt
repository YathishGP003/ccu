package a75f.io.messaging.handler

class MessageHandlerFactory {
    enum class Command{
        UpdateEntity, UpdatePoint, UpdateSchedule, SiteSync, RemoveEntity, RemoteCommand,
        PredefinedAlert, CustomAlert, AlertRemoval, AlertDefRemoval
    }
    companion object {
        fun createInstance(handlerCmd: Command) : MessageHandler =
            when(handlerCmd) {
                Command.UpdateEntity -> UpdateEntityHandler()
                Command.UpdatePoint -> UpdatePointHandler()
                Command.UpdateSchedule -> UpdateScheduleHandler()
                Command.SiteSync -> SiteSyncHandler()
                Command.RemoveEntity -> RemoveEntityHandler()
                Command.RemoteCommand -> RemoteCommandUpdateHandler()
                Command.PredefinedAlert -> AlertMessageHandler.PredefinedAlertHandler()
                Command.CustomAlert -> AlertMessageHandler.CustomAlertDefHandler()
                Command.AlertRemoval -> AlertMessageHandler.AlertRemoveHandler()
                Command.AlertDefRemoval -> AlertMessageHandler.AlertDefRemoveHandler()
            }
    }
}