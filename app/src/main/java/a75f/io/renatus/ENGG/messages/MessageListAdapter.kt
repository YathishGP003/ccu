package a75f.io.renatus.ENGG.messages

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.data.message.Message
import a75f.io.renatus.R
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

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

    @SuppressLint("NotifyDataSetChanged")
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


    @SuppressLint("ResourceAsColor", "SimpleDateFormat", "SetTextI18n")
    fun bind(message: Message) {
        val format = SimpleDateFormat("MM/dd - HH:mm:ss")
        messageId.text = message.messageId+": (${format.format(Date(message.timeToken))})"
        cmd.text = message.command
        point.text = when {
            message.id != null -> if (message.value != null) message.id+", val: "+message.value else message.id
            message.ids != null -> if (message.ids!!.size > 1) message.ids!![0]+" ...+${message.ids!!.size-1}" else message.ids!![0]
            else -> ""
        }
        status.text = if (message.handlingStatus.toString().toBoolean()) "HANDLED" else "NOT HANDLED"

        message.handlingStatus.let {
            when(it) {
                true -> status.setTextColor(Color.GREEN)
                false -> status.setTextColor(Color.RED)
            }
        }
    }
}