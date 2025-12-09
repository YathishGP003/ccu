package a75f.io.renatus.ui.alerts

import a75f.io.alerts.AlertManager
import a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS
import a75f.io.api.haystack.Alert
import a75f.io.logger.CcuLog
import a75f.io.logic.util.PreferenceUtil
import a75f.io.renatus.R
import a75f.io.renatus.RenatusLandingActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.disposables.Disposable

class AlertsFragment : Fragment(), AlertManager.AlertListListener {

    private var alertDeleteDisposable: Disposable? = null
    private var alertsViewModel: AlertsViewModel = AlertsViewModel()

    @Composable
    fun AlertListScreen(
        onClick: (index: Int, alert: Alert) -> Unit,
        onLongClick: (index: Int, alert: Alert) -> Unit
    ) {
        val alerts = alertsViewModel.alertList
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(alerts.take(60)) { index, alert ->
                AlertRowItem(
                    alert = alert,
                    onClick = { onClick(index, alert) },
                    onLongClick = { onLongClick(index, alert) }
                )
                Divider(
                    color = Color(0xFFDDDDDD),
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 72.dp)
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AlertRowItem(
        alert: Alert,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(10.dp)
        ) {
            val iconRes = if (alert.isFixed)
                R.drawable.ic_green_checkmark
            else
                getAlertIcon(alert.mSeverity)

            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterVertically)
            )

            Text(
                text = alert.mTitle,
                color = Color.Black,
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }

    @Composable
    fun getAlertIcon(severity: Alert.AlertSeverity): Int {
        return when (severity) {
            Alert.AlertSeverity.SEVERE,
            Alert.AlertSeverity.ERROR -> R.drawable.ic_severe

            Alert.AlertSeverity.MODERATE,
            Alert.AlertSeverity.WARN -> R.drawable.ic_moderate

            Alert.AlertSeverity.LOW -> R.drawable.ic_low
            else -> R.drawable.ic_low
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): AlertsFragment {
            return AlertsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent {
                AlertListScreen(
                    onClick = { index, alert ->
                        alertsViewModel.onClicked(index, alert, requireContext())
                    },
                    onLongClick = { index, alert ->
                        alertsViewModel.onLongClicked(index, alert, requireContext())
                    }
                )
            }
        }
        AlertManager.getInstance().setAlertListListener(this)
        return rootView
    }

    override fun onResume() {
        super.onResume()
        alertsViewModel.setAlertList()
    }

    override fun onStop() {
        alertDeleteDisposable?.dispose()
        super.onStop()
    }


    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if (menuVisible) {
            CcuLog.d(TAG_CCU_ALERTS, "menuVisible is visible")
            alertsViewModel.setAlertList()
        }
    }

    override fun onAlertsChanged() {
        CcuLog.d(TAG_CCU_ALERTS, "onAlertsChanged")
        val isAlertFragmentCreated = RenatusLandingActivity.mTabLayout.selectedTabPosition
        if (isAlertFragmentCreated == 4 && !PreferenceUtil.getIsCcuLaunched()) {
            activity?.runOnUiThread { alertsViewModel.setAlertList() }
        }
    }

}