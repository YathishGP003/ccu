package a75f.io.renatus.ENGG.messages

import a75f.io.messaging.database.DatabaseHelper
import a75f.io.messaging.database.Message
import a75f.io.messaging.database.MessageDatabaseBuilder
import a75f.io.messaging.database.MessageDatabaseHelper
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
@HiltViewModel
class MessageListViewModel @Inject constructor(messageDbHelper : DatabaseHelper) : ViewModel() {
    private val messageList = messageDbHelper.getAllMessages()
    fun getMessageList() : LiveData<List<Message>> {
        return messageList
    }
}