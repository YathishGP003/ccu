package a75f.io.data.message
import a75f.io.data.RenatusDatabaseBuilder
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

private var messageDbHelper : MessageDatabaseHelper? = null
fun insert(message: Message, context: Context) {
    appScope.launch {
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        Log.i("CCU_MESSAGING", " DbUtil:Insert $message")
        try {
            messageDbHelper?.insert(message)
        } catch (e : Exception) {
            e.printStackTrace()
            Log.i("CCU_MESSAGING", " Insert Failed $e")
        }
    }
}
fun update(message: Message, context : Context) {
    appScope.launch {
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        Log.i("CCU_MESSAGING", " DbUtil:Update $message")
        messageDbHelper?.update(message)
    }
}
fun deleteMessage(message: Message, context : Context) {
    appScope.launch {
        Log.i("CCU_MESSAGING", " DbUtil:Delete $message")
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        messageDbHelper?.delete(message)
    }
}

fun updateMessage(messageId : String, context: Context) {
    appScope.launch {
        val message = messageDbHelper?.getMessageById(messageId)
        if (message != null) {
            messageDbHelper?.update(message)
        }
    }
}

fun updateMessageHandled(messageId : String, context: Context) {
    appScope.launch {
        val message = messageDbHelper?.getMessageById(messageId)
        message?.let {
            it.handlingStatus = true
            Log.i("CCU_MESSAGING","updateMessageHandled $message")
            messageDbHelper?.update(it)
        }
    }
}


