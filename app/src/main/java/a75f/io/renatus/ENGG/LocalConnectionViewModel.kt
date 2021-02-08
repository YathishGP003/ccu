@file:Suppress("PackageName")
package a75f.io.renatus.ENGG

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.cloud.RenatusServicesEnvironment
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.ENGG.ConnectionStatus.*
import a75f.io.renatus.R
import android.arch.lifecycle.ViewModel
import android.support.annotation.ColorRes
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * ViewModel to direct the LocalConnectionView.  The view subscribes "viewState", which is an
 * RxJava BehaviorSubject.
 * This ViewModel subscribes to RenatusUrls to get its data, principally the current base IP address and
 * w
 *
 * @author Tony Case (case.tony@gmail.com)
 * Created on 1/29/21.
 */
class LocalConnectionViewModel : ViewModel() {

   val viewState: BehaviorSubject<LocalConnectionViewState> = BehaviorSubject.create()

   private lateinit var renatusUrls: RenatusServicesEnvironment
   private val disposables: CompositeDisposable = CompositeDisposable()

   init {
      // default values
      viewState.onNext(LocalViewState("", CHECKING, listOf()))
   }

   /**
    * This needs to be called at initialization.  When we implement Hilt, these dependencies will be
    * injected for us.
    */
   fun injectDependencies(
      renatusUrls: RenatusServicesEnvironment
   ) {
      this.renatusUrls = renatusUrls
      connect()
   }

   /**
    * Set a new ip address to use as the local base ip address for renatus services.
    */
   fun setIpAddress(ipAddress: String): Single<Boolean> {
      viewState.onNext(
         currentLocalViewState().copy(
            baseIpAddress = ipAddress,
            connectionStatus = CHECKING
         ))

      return renatusUrls.setLocalBaseIpAddress(ipAddress)
         .doOnSuccess { isReachable ->
            viewState.onNext(
               currentLocalViewState().copy(
                  connectionStatus = ConnectionStatus.from(isReachable),
                  prepopulateStrings = renatusUrls.getIpListForPrepopulate()
               ))
         } // handle error on subscription where we'll put up a dialog.
   }

   private fun connect() {

      if (BuildConfig.BUILD_TYPE != "local") {
         viewState.onNext(NonLocalViewState)
         return
      }

      viewState.onNext(
         currentLocalViewState().copy(
            baseIpAddress = renatusUrls.urls.base,
            connectionStatus = CHECKING
      ))

      disposables.add(
         renatusUrls.pingServices()
            .subscribe(
               { isReachable ->
                  viewState.onNext(currentLocalViewState().copy(
                     connectionStatus = ConnectionStatus.from(isReachable),
                     prepopulateStrings = renatusUrls.getIpListForPrepopulate()
                  ))
               },
               { error ->
                  val msg = "Error determining local Ip status: " + error.localizedMessage
                  CcuLog.e(L.TAG_CCU, msg, error)
                  viewState.onNext(
                     currentLocalViewState().copy(connectionStatus = ERROR)
                  )
               }
            )
      )
   }


   // call only when in local build env.
   private fun currentLocalViewState(): LocalViewState = viewState.value!! as LocalViewState

   override fun onCleared() {
      disposables.dispose()
   }
}

sealed class LocalConnectionViewState { }

data class LocalViewState(
   val baseIpAddress: String,
   val connectionStatus: ConnectionStatus,
   val prepopulateStrings: List<String>
) : LocalConnectionViewState()

object NonLocalViewState : LocalConnectionViewState()

enum class ConnectionStatus(val displayMessage: String, @ColorRes val displayColor: Int) {

   REACHABLE("Reachable", R.color.safe_status),
   UNREACHABLE("Unreachable", R.color.danger_status),
   CHECKING("Pinging...", R.color.grey_select),
   ERROR("ERROR", R.color.danger_status);

   companion object {
      fun from(isReachable: Boolean) = if (isReachable) REACHABLE else UNREACHABLE
   }
}
