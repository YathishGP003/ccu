package a75f.io.renatus.ENGG

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.cloud.RenatusServicesEnvironment
import a75f.io.logic.cloud.RenatusServicesEnvironment.Companion.createWithSharedPrefs
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.R
import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.preference.PreferenceManager
import android.support.constraint.ConstraintLayout
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import android.util.AttributeSet
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_local_connection.view.*

/**
 * A simple view that indicates this build points to local environment, or, if not, is invisible (gone).
 * Also shows current IP address, isReachable status for that address, and a button to change address.
 * The button puts up an AlertDialog allowing entry of a new address.
 *
 * @author Tony Case
 * Created on 1/29/21.
 */
class LocalConnectionView @JvmOverloads constructor(
   context: Context,
   attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

   // To dispose RxJava disposables, i.e. cancel subscriptions
   private val disposables: CompositeDisposable = CompositeDisposable()
   // Autofill values for the text entry are previously successful IP addresses
   private var savedPrepopulateValues: List<String>? = null

   // Obtain view model.  The usual pattern is to get Viewmodels in Activities or Fragments,
   // but I'd rather not refactor DevSettings at this time, so I'll use this workaround of getting the
   // Viewmodel from this View.
   private val activity: FragmentActivity by lazy {
      if (isInEditMode) FragmentActivity()   // this is here so Layout Preview does not crash
      else try {
         context as FragmentActivity
      } catch (exception: ClassCastException) {
         throw ClassCastException("Please ensure that the provided Context is a valid FragmentActivity")
      }
   }
   private var viewModel = ViewModelProviders.of(activity).get(LocalConnectionViewModel::class.java)

   init {
      val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
      inflater.inflate(R.layout.view_local_connection, this)
   }

   override fun onAttachedToWindow() {
      super.onAttachedToWindow()

      changeIpButton.setOnClickListener { openIpDialog() }
      disposables.add(
         viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
               { viewState -> renderViewState(viewState) },
               { error -> failFast(error, "local connection view state") }  // no reason for error
            )
      )
      viewModel.injectDependencies(RenatusServicesEnvironment.instance)
   }

   override fun onDetachedFromWindow() {
      super.onDetachedFromWindow()
      disposables.dispose()
   }

   @SuppressLint("SetTextI18n")
   private fun renderViewState(viewState: LocalConnectionViewState) {
      when (viewState) {
         is NonLocalViewState -> this.hide()
         is LocalViewState -> {
            this.show()
            ipAddressValue.text = "http://" + viewState.baseIpAddress
            networkStatus.text = viewState.connectionStatus.displayMessage
            networkStatus.setTextColor(context.getColor(viewState.connectionStatus.displayColor))
            // HERE -- get these from the viewmodel
            savedPrepopulateValues = viewState.prepopulateStrings
         }
      }
   }

   private fun View.hide() {
      visibility = View.GONE
   }

   private fun View.show() {
      visibility = View.VISIBLE
   }

   private fun failFast(t: Throwable, msg: String) {
      throw RuntimeException("Fail fast, $msg", t)
   }

   private fun openIpDialog() {

      val message = "Enter new IP address for your local Renatus sandbox.  E.g.: \"129.144.50.56\""

      ipEntryPopup(context, message, savedPrepopulateValues) { text ->
         disposables.add(
            viewModel.setIpAddress(text)
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(
                  { _: Boolean -> { } },  // no further action on success
                  { error: Throwable ->
                     val msg = "Error trying to reach new IP: ${error.message}"
                     CcuLog.e(L.TAG_CCU, msg, error)
                     showErrorDialog(msg, context)
                  }
               )
         )
      }
   }
}

fun ipEntryPopup(context: Context,
                 message: String,
                 autoCompleteValues: List<String>? = null,
                 callback: (String) -> Unit) {

   val editText = AutoCompleteTextView(context).apply {
      setRawInputType(TYPE_CLASS_NUMBER.or(TYPE_NUMBER_FLAG_DECIMAL))
      autoCompleteValues?.let {
         setAdapter(ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, it))
      }
   }

   fun processText(text: String) {
      if (text.isNotEmpty()) {
         if (Patterns.IP_ADDRESS.matcher(text).matches()) {
            callback(text)
         } else {
            showErrorDialog("Improper IP address format: $text. ", context)
         }
      }
   }

   val dialog = AlertDialog.Builder(context)
      .setTitle("Local Env IP Address")
      .setMessage(message)
      .setView(editText)
      .setPositiveButton("Done") { _, _ -> processText(editText.text.toString().trim()) }
      .create()
   dialog.show()
}

// Shows a simple error dialog for the given message.  (We should have a general tool for this.)
fun showErrorDialog(msg: String, context: Context) {
   AlertDialog.Builder(context)
      .setTitle("Error")
      .setIcon(R.drawable.ic_alert)
      .setMessage(msg)
      .show()
}

@JvmOverloads
fun checkForIpEntryOnLocalBuild(
   context: Context,
   disposables: CompositeDisposable? = null
) {
   if (BuildConfig.BUILD_TYPE == "local") {
      val servicesEnvironment = createWithSharedPrefs(
         PreferenceManager.getDefaultSharedPreferences(context))
      val message = "Override existing IP address " + servicesEnvironment.getLocalBaseIp() + " with new IP address, or dismiss to leave as is."
      ipEntryPopup(context, message) { text ->
         val disposable =
            servicesEnvironment.setLocalBaseIpAddress(text)
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(
                  { isReachable -> if (!isReachable) showErrorDialog("New Ip address $text is not reachable.  Back out and try again", context) },  // no further action on success
                  { error: Throwable ->
                     val msg = "Error trying to reach new IP: ${error.message}"
                     CcuLog.e(L.TAG_CCU, msg, error)
                     showErrorDialog(msg, context)
                  }
               )
         disposables?.add(disposable)
      }
   }
}

