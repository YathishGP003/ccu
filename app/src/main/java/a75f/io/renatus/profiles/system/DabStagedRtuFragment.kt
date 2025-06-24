package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment
import a75f.io.renatus.util.AddProgressGif
import a75f.io.renatus.util.TestSignalManager
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DabStagedRtuFragment : DStagedRtuFragment() {
    private val viewModel: DabStagedRtuViewModel by viewModels()

    fun hasUnsavedChanged(): Boolean{
        return viewModel.hasUnsavedChanges()
    }

    companion object {
        val ID: String = DabStagedRtuFragment::class.java.simpleName
        fun newInstance(): DabStagedRtuFragment {
            return DabStagedRtuFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext()).apply {
            setContent {
                AddProgressGif()
                CcuLog.i(Domain.LOG_TAG, "Show Progress")
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher){
            viewModel.init(requireContext(), CCUHsApi.getInstance())
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    CcuLog.i(Domain.LOG_TAG, "Hide Progress")
                    RootView()
                }
            }
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
            }

            override fun onViewDetachedFromWindow(v: View) {
                if (Globals.getInstance().isTestMode()) {
                    Globals.getInstance().setTestMode(false)
                    TestSignalManager.restoreAllPoints()
                }
            }
        })
    }

    @Preview
    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            item{
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
                {
                    Image(
                        painter = painterResource(id = R.drawable.input_vav_rtu_2),
                        contentDescription = "Relays",
                        modifier = Modifier
                            .padding(top = 85.dp, bottom = 5.dp, start = 20.dp)
                            .height(475.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        )
                        {
                            Text(
                                text = "ENABLE",
                                fontSize = 20.sp,
                                color = ComposeUtil.greyColor
                            )
                            Spacer(modifier = Modifier.width(270.dp))
                            Text(
                                text = "MAPPING",
                                fontSize = 20.sp,
                                color = ComposeUtil.greyColor
                            )
                            Spacer(modifier = Modifier.width(155.dp))
                            Text(
                                text = "TEST SIGNAL",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 30.dp),
                                color = ComposeUtil.greyColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        StagedRtuRelayMappingView(viewModel = viewModel)
                    }
                }
            }
            if (viewModel.viewState.value.unusedPortState.isNotEmpty()) {
                item {
                    UnusedPortsFragment.DividerRow()
                }
                item {
                    UnusedPortsFragment.LabelUnusedPorts()
                }
                item {
                    UnusedPortsFragment.UnUsedPortsListView(viewModel)
                }
            }
            item {
                SaveConfig()
            }
        }
    }

    @Composable
    fun SaveConfig() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 20.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 5.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(CANCEL, viewModel.viewState.value.isStateChanged) {
                    viewModel.reset()
                    viewModel.viewState.value.isSaveRequired = false
                    viewModel.viewState.value.isStateChanged = false
                }
            }
            Divider(
                modifier = Modifier
                    .height(25.dp)
                    .width(2.dp)
                    .padding(bottom = 6.dp),
                color = Color.LightGray
            )
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(SAVE, viewModel.viewState.value.isSaveRequired) {
                    viewModel.saveConfiguration()
                    viewModel.viewState.value.isSaveRequired = false
                    viewModel.viewState.value.isStateChanged = false
                }
            }
        }
    }

}