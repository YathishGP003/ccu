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

fun updateMessage(message: Message) {
    appScope.launch {
        if (message != null) {
            messageDbHelper?.update(message)
        }
    }
}

fun updateMessageHandled(message: Message, context: Context) {
    appScope.launch {
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        message.handlingStatus = true
        message.handledTime = System.currentTimeMillis()
        Log.i("CCU_MESSAGING","updateMessageHandled $message")
        messageDbHelper?.update(message)
    }
}

/**
 * When app version is upgraded,all the upgrade requests pending in the message db
 * are marked as handled. This avoids ending up in a situation we message db has queued up too many
 * upgrade/downgrade request and continuously doing it.
 */
fun updateAllRemoteCommandsHandled(context: Context, cmdType : String) {
    appScope.launch {
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        messageDbHelper?.getAllUnhandledMessage()?.collect {
            for (message in it) {
                if (message.remoteCmdType != null && message.remoteCmdType == cmdType) {
                    message.handlingStatus = true
                    message.handledTime = System.currentTimeMillis()
                    Log.i("CCU_MESSAGING","updateMessageHandled $message")
                    messageDbHelper?.update(message)
                }
            }
        }

    }
}


