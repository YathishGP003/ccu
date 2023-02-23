package a75f.io.messaging.database

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONException

private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

val messageDbHelper = MessageDatabaseHelper(MessageDatabaseBuilder
    .getInstance(Globals.getInstance().applicationContext));
fun insert(message: Message) {
    CcuLog.i(L.TAG_CCU_MESSAGING, " Insert $message")
    appScope.launch {
        messageDbHelper.insert(message)
    }
}
fun update(message: Message) {
    appScope.launch {
        messageDbHelper.update(message)
    }
}
fun delete(message: Message) {
    appScope.launch {
        messageDbHelper.delete(message)
    }
}

fun updateMessage(messageId : String) {
    appScope.launch {
        val message = messageDbHelper.getMessageById(messageId)
        messageDbHelper.update(message)
    }
}

fun updateMessageHandled(messageId : String) {
    appScope.launch {
        val message = messageDbHelper.getMessageById(messageId)
        message.handlingStatus = true
        messageDbHelper.update(message)
    }
}


