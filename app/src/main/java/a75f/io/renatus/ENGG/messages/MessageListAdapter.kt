package a75f.io.renatus.ENGG.messages

import a75f.io.messaging.database.Message
import a75f.io.renatus.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageListAdapter(messages : List<Message>) : RecyclerView.Adapter<MessageViewHolder>() {
    private var messageList = messages

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item_layout,parent, false )
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder : MessageViewHolder, position : Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount() = messageList.size

    fun addItems(messages : List<Message>) {
        messageList = messages
        notifyDataSetChanged()
    }
}


class MessageViewHolder(view : View) : RecyclerView.ViewHolder (view) {

    private val messageId: TextView = view.findViewById(R.id.messageId)
    private val cmd: TextView = view.findViewById(R.id.messageCmd)
    private val point: TextView = view.findViewById(R.id.pointId)
    private val status: TextView = view.findViewById(R.id.handlingStatus)


    fun bind(message: Message) {
        messageId.text = message.messageId
        cmd.text = message.command
        point.text = message.id+"    "+message.value
        status.text = message.handlingStatus.toString()
    }
}