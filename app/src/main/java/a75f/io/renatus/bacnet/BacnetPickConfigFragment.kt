package a75f.io.renatus.bacnet

import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IP_CONFIGURATION
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.bacnet.util.CONST_AUTO_DISCOVERY
import a75f.io.renatus.bacnet.util.MSTP_CONFIGURATION
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.SEARCH_DEVICE
import a75f.io.renatus.util.ProgressDialogUtils

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.Toast

import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.runtime.snapshotFlow

@SuppressLint("StaticFieldLeak")
lateinit var context: Context

class BacnetPickConfigDialog : BaseDialogFragment() {

    companion object {
        fun newInstance(
            nodeAddress: Short,
            roomName: String,
            floorName: String
        ): BacnetPickConfigDialog {

            val f = BacnetPickConfigDialog()
            val args = Bundle()

            args.putShort("node", nodeAddress)
            args.putString("room", roomName)
            args.putString("floor", floorName)

            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun getIdString(): String {
        return BacnetPickConfigDialog::class.java.simpleName
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val viewModel = ViewModelProvider(this)[BacnetPickConfigViewModel::class.java]

        val node = requireArguments().getShort("node")
        val room = requireArguments().getString("room") ?: ""
        val floor = requireArguments().getString("floor") ?: ""


        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {

                // Success Flow (Device list found)
                launch {
                    viewModel.searchFinished.collect { finished ->

                        if (finished) {
                            ProgressDialogUtils.hideProgressDialog()
                            viewModel.resetSearchFlag()

                            val devices = viewModel.connectedDevices.value
                            if (devices.isNotEmpty()) {

                                viewModel.openModelSelectionAfterDeviceClick.value = true

                                showDialogFragment(
                                    BacnetDeviceSelectionFragment.newInstance(
                                        viewModel.connectedDevices,
                                        viewModel.onBacnetDeviceSelect,
                                        SEARCH_DEVICE,
                                        viewModel.configurationType.value == MSTP_CONFIGURATION
                                    ),
                                    BacnetDeviceSelectionFragment.ID
                                )
                            }
                        }
                    }
                }

                lifecycleScope.launch {
                    viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                        launch {
                            snapshotFlow { viewModel.selectedDeviceForModel.value }.collect { device ->
                                if (device != null) {
                                    showDialogFragment(
                                        BacnetModelSelectionFragment.newInstance(device.deviceId),
                                        BacnetModelSelectionFragment.ID
                                    )
                                    viewModel.clearSelectedDeviceForModel()
                                }
                            }
                        }
                    }
                }

                // Failure Flow (Timeout / Exception)
                launch {
                    viewModel.searchFailed.collect { failed ->

                        if (failed) {
                            ProgressDialogUtils.hideProgressDialog()
                            viewModel.resetSearchFailedFlag()

                            Toast.makeText(
                                requireContext(),
                                "Connection timeout. Please check the BACnet network.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        //  UI / Compose Screen Rendering
        return ComposeView(requireContext()).apply {
            setContent {
                BacnetPickConfigurationScreen(
                    viewModel = viewModel,

                    onStartAutoDiscovery = {
                        // Show loader
                        ProgressDialogUtils.showProgressDialog(
                            context,
                            CONST_AUTO_DISCOVERY
                        )
                        // Trigger WHO-IS
                        viewModel.sendWhoIsBroadcast(
                            context = requireContext(),
                            configurationType = IP_CONFIGURATION
                        )
                    },

                    onContinueManual = {
                        val dialog = BacNetSelectModelView.newInstance(
                            node.toString(),
                            room,
                            floor,
                            ProfileType.BACNET_DEFAULT,
                            ModbusLevel.ZONE,
                            ""
                        )
                        showDialogFragment(
                            dialog,
                            BacNetSelectModelView.ID
                        )
                    },

                    onBackClick = {
                        dismiss()
                    }
                )
            }
        }
    }
}