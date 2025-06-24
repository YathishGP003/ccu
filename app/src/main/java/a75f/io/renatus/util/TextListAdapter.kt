package a75f.io.renatus.util

import a75f.io.renatus.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TextListAdapter(private val textList: List<CharSequence>)
    : RecyclerView.Adapter<TextListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView : TextView = itemView.findViewById(R.id.basic_text_view)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.basic_text_item, p0, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return textList.size
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        p0.textView.text = textList[p1]
    }

}