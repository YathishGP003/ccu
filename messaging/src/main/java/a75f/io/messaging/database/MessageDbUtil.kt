package a75f.io.messaging.database

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun insert(message: Message) {
    CcuLog.i(L.TAG_CCU_MESSAGING, " Insert $message")
    appScope.launch {
        MessageDatabaseHelper(MessageDatabaseBuilder
            .getInstance(Globals.getInstance().applicationContext)).insert(message)
    }
}
fun update(message: Message) {
    appScope.launch {
        MessageDatabaseHelper(MessageDatabaseBuilder
            .getInstance(Globals.getInstance().applicationContext)).update(message)
    }
}
fun delete(message: Message) {
    appScope.launch {
        MessageDatabaseHelper(MessageDatabaseBuilder
            .getInstance(Globals.getInstance().applicationContext)).delete(message)
    }
}

fun updateMessage(messageId : String) {
    appScope.launch {
        val dbHelper = MessageDatabaseHelper(MessageDatabaseBuilder
                        .getInstance(Globals.getInstance().applicationContext))
        val message = dbHelper.getMessageById(messageId)
        dbHelper.update(message)
    }
}