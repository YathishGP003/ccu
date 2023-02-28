package a75f.io.data.message
import a75f.io.data.RenatusDatabaseBuilder
import android.content.Context
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

private var messageDbHelper : MessageDatabaseHelper? = null
fun insert(message: Message, context: Context) {
    Log.i("CCU_MESSAGING", " Insert $message")
    appScope.launch {
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        messageDbHelper?.insert(message)
    }
}
fun update(message: Message, context : Context) {
    appScope.launch {
        if (messageDbHelper == null) {
            messageDbHelper = MessageDatabaseHelper(RenatusDatabaseBuilder.getInstance(context))
        }
        messageDbHelper?.update(message)
    }
}
fun delete(message: Message) {
    appScope.launch {
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
            messageDbHelper?.update(it)
        }
    }
}


