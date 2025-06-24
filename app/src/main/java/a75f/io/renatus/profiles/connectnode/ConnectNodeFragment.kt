package a75f.io.renatus.profiles.connectnode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectNodeFragment : BaseDialogFragment(), OnPairingCompleteListener {
    val viewModel: ConnectNodeViewModel by viewModels()
    private var isNewPairingDevice: Boolean = true
    override fun getIdString(): String {
        return ConnectNodeFragment::class.java.simpleName
    }
    fun setIsNewPairing(isNewPairing: Boolean): ConnectNodeFragment {
        isNewPairingDevice = isNewPairing
        return this
    }

    companion object {
        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType,
        ): ConnectNodeFragment {
            val fragment = ConnectNodeFragment()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.NODE_TYPE, nodeType.ordinal)
            fragment.arguments = bundle
            this.getIdString()
            return fragment
        }

        fun getIdString(): String {
            return ConnectNodeFragment::class.java.simpleName
        }
    }


    override fun onPairingComplete() {
        this@ConnectNodeFragment.closeAllBaseDialogFragments()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }
        viewModel.isReloadRequired.observe(viewLifecycleOwner) { isDialogOpen ->
            if (isDialogOpen) {
                viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
                    withContext(Dispatchers.Main) {
                        rootView.setContent {
                            RootView()
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@ConnectNodeFragment)
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    RootView()
                }
            }
        }
        return rootView
    }
    @Composable
    fun RootView(){
        MaterialTheme {
            if(viewModel.equipmentDeviceList.isEmpty()){
                ConnectNodeScreen.EmptyConnectNodeScreen(
                    onCancel = {
                        closeAllBaseDialogFragments()
                    },
                    onSave = {
                        viewModel.saveConfiguration()
                        arguments?.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
                            ?.let { showSuccessToast("Connect Node", it.toInt(), requireContext()) }
                    },
                    isNewPairingDevice
                )
            } else {

                ConnectNodeScreen.ConnectNodeConfigScreen(
                    onCancel = {
                        closeAllBaseDialogFragments()
                    },
                    onSave = {
                        viewModel.saveConfiguration()
                        arguments?.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
                            ?.let { showSuccessToast("Connect Node", it.toInt(), requireContext()) }
                    },
                    viewModel
                )
            }
        }
    }
    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = ComposeUtil.primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(1265, 672)
    }
}
