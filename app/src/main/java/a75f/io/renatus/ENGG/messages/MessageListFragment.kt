package a75f.io.renatus.ENGG.messages

import a75f.io.logic.L
import a75f.io.renatus.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.message_list_layout.*
@AndroidEntryPoint
class MessageListFragment : Fragment(){

    private val viewModel : MessageListViewModel by viewModels()
    private val messageListAdapter = MessageListAdapter(listOf())

    override fun onCreateView(inflator : LayoutInflater, container : ViewGroup?, saveInstanceState : Bundle?) : View?{
        return inflator.inflate(R.layout.message_list_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageListView.adapter = messageListAdapter
        messageListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        viewModel.getMessageList().observe(viewLifecycleOwner, Observer {
            messageListAdapter.addItems(it)
            Log.i(L.TAG_CCU_MESSAGING, " Display messages size "+it.size)
            if (it.isNotEmpty()) {
                messageListView.smoothScrollToPosition(messageListAdapter.itemCount - 1)
            }
        })
    }
}