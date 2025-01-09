package a75f.io.renatus.ENGG.alertdefs

import a75f.io.alerts.AlertManager
import a75f.io.alerts.log.SequenceMethodLog
import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.ALERT_DEF_ID
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_EXPIRE_AT
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_MESSAGE
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_OPERATION
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_TIMESTAMP
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_TYPE
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.TAG
import a75f.io.renatus.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

object BundleConstants {
    const val ALERT_DEF_ID = "alertDefId"
    const val SEQUENCE_RESULT_JSON = "sequenceResultJson"
    const val SORT_BY_TYPE = "type"
    const val SORT_BY_EXPIRE_AT = "expireAt"
    const val SORT_BY_MESSAGE = "message"
    const val SORT_BY_OPERATION = "operation"
    const val SORT_BY_TIMESTAMP = "timestamp"
    const val LOG_TYPE_ERROR = "ERROR"
    const val LOG_TYPE_TRACE = "TRACE"
    const val LOG_TYPE_WARN = "WARN"
    const val LOG_TYPE_INFO = "INFO"
    const val TAG = "CCU_ALERTS_DEFS"
}

class AlertLogViewFragment : Fragment(), AdapterLogSequenceCallback {

    private val viewModel: AlertDefsViewModel by viewModels()
    private var alertDefId: String? = ""
    private lateinit var alertLogSequenceAdapter: AlertLogSequenceAdapter
    private var sequenceLogsList = mutableListOf<SequenceMethodLog>()

    private var isTypeSortByAscending = false
    private var isExpireAtSortByAscending = false
    private var isMessageSortByAscending = false
    private var isOperationSortByAscending = false
    private var isTimeStampSortByAscending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alertDefId = arguments?.getString(ALERT_DEF_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.alert_log_view_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.injectDependencies(AlertManager.getInstance())
        alertLogSequenceAdapter = AlertLogSequenceAdapter(sequenceLogsList, this)


        view.findViewById<View>(R.id.img_close).setOnClickListener(View.OnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        })

        CcuLog.d(TAG, "---alertDefId---$alertDefId")
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewLogSequences)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = alertLogSequenceAdapter

        if (!alertDefId.isNullOrEmpty()) {
            val sequenceLogs = viewModel.getLogsByDefId(alertDefId)
            if (sequenceLogs != null) {
                sequenceLogsList.addAll(sequenceLogs.logs)
                alertLogSequenceAdapter.notifyDataSetChanged()
            }
        }

        view.findViewById<View>(R.id.iv_sort_type).setOnClickListener(View.OnClickListener {
            isTypeSortByAscending = !isTypeSortByAscending
            alertLogSequenceAdapter.sort(SORT_BY_TYPE, isTypeSortByAscending)
        })

        view.findViewById<View>(R.id.iv_sort_expire_at).setOnClickListener(View.OnClickListener {
            isExpireAtSortByAscending = !isExpireAtSortByAscending
            alertLogSequenceAdapter.sort(SORT_BY_EXPIRE_AT, isExpireAtSortByAscending)
        })

        view.findViewById<View>(R.id.iv_sort_message).setOnClickListener(View.OnClickListener {
            isMessageSortByAscending = !isMessageSortByAscending
            alertLogSequenceAdapter.sort(SORT_BY_MESSAGE, isMessageSortByAscending)
        })

        view.findViewById<View>(R.id.iv_sort_operation).setOnClickListener(View.OnClickListener {
            isOperationSortByAscending = !isOperationSortByAscending
            alertLogSequenceAdapter.sort(SORT_BY_OPERATION, isOperationSortByAscending)
        })

        view.findViewById<View>(R.id.iv_sort_timestamp).setOnClickListener(View.OnClickListener {
            isTimeStampSortByAscending = !isTimeStampSortByAscending
            alertLogSequenceAdapter.sort(SORT_BY_TIMESTAMP, isTimeStampSortByAscending)
        })

        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                CcuLog.d(TAG, "onQueryTextSubmit-->$query")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                CcuLog.d(TAG, "onQueryTextChange-->$newText")
                if (newText != null) {
                    alertLogSequenceAdapter.filter(newText)
                }
                return true
            }
        })
    }

    override fun onItemClicked(resultJson: String?) {
        if(resultJson == null){
            Toast.makeText(requireContext(), "No data found", Toast.LENGTH_SHORT).show()
        }else{
            launchResultDetails(resultJson)
        }
    }

    private fun launchResultDetails(resultJson: String?) {
        var fragment: Fragment = JsonLikeFragment()

        val args = Bundle()
        args.putString(BundleConstants.SEQUENCE_RESULT_JSON, resultJson)
        fragment.arguments = args

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.container_property_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}

interface AdapterLogSequenceCallback {
    fun onItemClicked(alertDefId: String?)
}

