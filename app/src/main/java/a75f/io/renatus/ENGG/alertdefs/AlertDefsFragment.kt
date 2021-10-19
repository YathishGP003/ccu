package a75f.io.renatus.ENGG.alertdefs

import a75f.io.alerts.AlertManager
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.renatus.R
import a75f.io.renatus.util.extension.showErrorDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.alert_defs_fragment.*

/**
 * Fragment for showing all the Alert Definitions currently in the system, and their status.
 *
 * @author tcase@75f.io
 * Created on 3/2/21.
 */
class AlertDefsFragment: Fragment() {

   private val viewModel: AlertDefsViewModel by viewModels()
   private lateinit var listAdapter: AlertDefsListAdapter
   private val disposables = CompositeDisposable()

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      return inflater.inflate(R.layout.alert_defs_fragment, container, false)
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      setupListView()
      viewModel.injectDependencies(AlertManager.getInstance())
      disposables.add(
         subscribeToViewState()
      )
      refreshButton.setOnClickListener { viewModel.retrieveAlertDefs() }
   }

   override fun onDestroyView() {
      super.onDestroyView()
      disposables.dispose()
   }

   private fun setupListView() {
      listAdapter = AlertDefsListAdapter()
      val listLayoutMgr = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

      alertDefsListView.apply {
         // use this setting to improve performance if you know that changes
         // in content do not change the layout size of the RecyclerView
         setHasFixedSize(true)

         layoutManager = listLayoutMgr
         adapter = listAdapter
      }
   }

   private fun subscribeToViewState(): Disposable {
      return viewModel.viewState
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(
            { viewState -> render(viewState) },
            { error -> unexpectedError(error, "getting alert defs") }
         )
   }

   private fun unexpectedError(t: Throwable? = null, msg: String) {
      val displayMsg = msg + ": " + t?.localizedMessage
      CcuLog.e(L.TAG_CCU, displayMsg)
      showErrorDialog(displayMsg)
   }

   private fun render(viewState: AlertDefsViewState) {
      definitionCount.text = viewState.totalDefs
      activeCount.text =  viewState.activeCount
      resolvedCount.text = viewState.fixedCount
      inProcessCount.text = viewState.inProcessCount

      listAdapter.setAlertDefs(viewState.alertDefRows)
   }
}

