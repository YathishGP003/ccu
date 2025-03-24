package a75f.io.messaging

import a75f.io.messaging.handler.*

class MessageHandlerFactory {
    enum class Command{
        UpdateEntity, UpdatePoint, UpdateSchedule, SiteSync, RemoveEntity, RemoteCommand,
        PredefinedAlert, CustomAlert, AlertRemoval, AlertDefRemoval, AutoCommissioningMode,
        SCHEDULE_MIGRATED, sequenceCreated, sequenceUpdated, sequenceDeleted,fixAlert,updateDashboard,
        AddEntity
    }
    companion object {
        fun createInstance(handlerCmd: Command) : MessageHandler =
            when(handlerCmd) {
                Command.AddEntity -> AddEntityHandler()
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
                Command.AutoCommissioningMode -> AutoCommissioningStateHandler()
                Command.SCHEDULE_MIGRATED -> SchedulerRevampMigrationHandler()
                Command.sequenceCreated -> SiteSequencerMessageHandler.SequenceCreatedHandler()
                Command.sequenceUpdated -> SiteSequencerMessageHandler.SequenceUpdatedHandler()
                Command.sequenceDeleted -> SiteSequencerMessageHandler.SequenceDeletedHandler()
                Command.fixAlert -> FixAlertHandler()
                Command.updateDashboard -> DashboardHandler()
            }
    }
}