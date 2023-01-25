package a75f.io.renatus.ENGG.messages

import a75f.io.messaging.database.Message
import a75f.io.messaging.database.MessageDatabaseBuilder
import a75f.io.messaging.database.MessageDatabaseHelper
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class MessageListViewModel : ViewModel() {
    private lateinit var messageList : LiveData<List<Message>>

    fun init(context : Context) {
        messageList = MessageDatabaseHelper(MessageDatabaseBuilder.getInstance(context)).getAllMessages()
        /*viewModelScope.launch(Dispatchers.IO) {
            messageList = MessageDatabaseHelper(MessageDatabaseBuilder.getInstance(context)).getAllMessages()
        }*/
    }
    fun getMessageList() : LiveData<List<Message>> {
        return messageList
    }
}