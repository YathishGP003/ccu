package a75f.io.renatus.modbus

import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.ParameterLabel
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TextViewCompose
import a75f.io.renatus.compose.TextViewWithClick
import a75f.io.renatus.compose.TextViewWithClickOption
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.EQUIP_TYPE
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.modbus.util.SEARCH_SLAVE_ID
import a75f.io.renatus.modbus.util.SELECT_ALL
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.modbus.util.SLAVE_ID
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider

/**
 * Created by Manjunath K on 13-07-2023.
 */

class ModbusConfigView : BaseDialogFragment() {
    private lateinit var viewModel: ModbusConfigViewModel

    companion object {
        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, profileType: ProfileType,
            level: ModbusLevel, filer: String
        ): ModbusConfigView {
            val fragment = ModbusConfigView()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.MODBUS_LEVEL, level.ordinal)
            bundle.putString(FragmentCommonBundleArgs.MODBUS_FILTER, filer)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[ModbusConfigViewModel::class.java]
        viewModel.holdBundleValues(requireArguments())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
            if (!isDialogOpen) {
                this@ModbusConfigView.closeAllBaseDialogFragments()
            }
        }
    }

    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            viewModel.configModelDefinition(requireContext())
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { TitleTextView(MODBUS) }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { HeaderTextView(EQUIP_TYPE) }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (viewModel.equipModel.value.isDevicePaired) {
                        viewModel.modelName.value =
                            getName(viewModel.equipModel.value.equipDevice.value.name)
                        TextViewWithClick(
                            text = viewModel.modelName,
                            onClick = { },
                            enableClick = false,
                            isCompress = false
                        )
                    } else {
                        TextViewWithClick(
                            text = viewModel.modelName,
                            onClick = {
                                ProgressDialogUtils.showProgressDialog(context, LOADING)
                                showDialogFragment(
                                    ModelSelectionFragment.newInstance(
                                        viewModel.deviceList,
                                        viewModel.onItemSelect,SEARCH_MODEL
                                    ), ModelSelectionFragment.ID
                                )
                            },
                            enableClick = true, isCompress = false
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {

                    Box(modifier = Modifier.weight(7f)) {
                        HeaderLeftAlignedTextView(
                            if (viewModel.equipModel.value.equipDevice.value.name.isNullOrEmpty()) "" else getName(
                                viewModel.equipModel.value.equipDevice.value.name
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) { HeaderTextView(SLAVE_ID) }
                    Box(modifier = Modifier.weight(1f)) {
                        val onItemSelect = object : OnItemSelect {
                            override fun onItemSelected(index: Int, item: String) {
                                viewModel.equipModel.value.slaveId.value = item.toInt()
                            }
                        }
                        TextViewWithClickOption(
                            text = viewModel.equipModel.value.slaveId,
                            onClick = {
                                ProgressDialogUtils.showProgressDialog(context, LOADING)
                                showDialogFragment(
                                    ModelSelectionFragment.newInstance(
                                        viewModel.slaveIdList,
                                        onItemSelect, SEARCH_SLAVE_ID
                                    ), ModelSelectionFragment.ID
                                )
                            },
                            enableClick = !viewModel.equipModel.value.isDevicePaired,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PaddingValues(bottom = 5.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderTextView(SELECT_ALL)
                    ToggleButton(defaultSelection = viewModel.equipModel.value.selectAllParameters.value) {
                        viewModel.equipModel.value.selectAllParameters.value = it
                        viewModel.onSelectAll(it)
                    }
                }
                Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabel() }
            }
            item { ParametersListView(data = viewModel.equipModel) }
            item { SubEquipments(viewModel.equipModel) }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    SaveTextView(SET) { viewModel.saveConfiguration() }
                }
            }
        }
    }

    @Composable
    fun ParametersListView(data: MutableState<EquipModel>) {
        if (data.value.parameters.isNotEmpty()) {
            var index = 0
            while (index < data.value.parameters.size) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row {
                        for (rowIndex in 0 until 2) {
                            if (index < data.value.parameters.size) {
                                val item = data.value.parameters[index]
                                LabelTextView(
                                    if (item.param.value.name.length > 30)
                                        item.param.value.name.substring(0, 30)
                                    else
                                        item.param.value.name
                                )
                                ToggleButton(item.displayInUi.value) {
                                    item.displayInUi.value = it
                                    item.param.value.getParameterId()
                                    viewModel.updateSelectAll()
                                }
                                Box(modifier = Modifier.width(50.dp)) { }
                                index++
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SubEquipments(data: MutableState<EquipModel>) {

        Column {
            data.value.subEquips.forEach { subEquip ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.weight(7f)) {
                        HeaderLeftAlignedTextView(
                            text = getName(
                                subEquip.value.equipDevice.value.name
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) { HeaderTextView(SLAVE_ID) }
                    Box(modifier = Modifier.weight(2f)) {
                        if (viewModel.equipModel.value.isDevicePaired) {
                            TextViewCompose(subEquip.value.slaveId.value.toString())
                        } else {
                            val onItemSelect = object : OnItemSelect {
                                override fun onItemSelected(index: Int, item: String) {
                                    if (index == 0) {
                                        subEquip.value.slaveId.value = 0
                                        subEquip.value.childSlaveId.value = SAME_AS_PARENT
                                    } else {
                                        subEquip.value.slaveId.value = item.toInt()
                                        subEquip.value.childSlaveId.value = item
                                    }
                                }
                            }
                            TextViewWithClick(
                                text = subEquip.value.childSlaveId,
                                onClick = {
                                    ProgressDialogUtils.showProgressDialog(context, LOADING)
                                    showDialogFragment(
                                        ModelSelectionFragment.newInstance(
                                            viewModel.childSlaveIdList,
                                            onItemSelect, SEARCH_SLAVE_ID
                                        ), ModelSelectionFragment.ID
                                    )
                                },
                                enableClick = !viewModel.equipModel.value.isDevicePaired,
                                isCompress = false
                            )
                        }
                    }
                }
                Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabel() }
                ParametersListView(data = subEquip)
            }
        }
    }

    fun getName(name: String): String {
        if (name.contains("-")) {
            val titles = name.split("-")
            val splitName = "${titles[1]} ${titles[2]}"
            return if (splitName.length > 30) splitName.substring(0, 30) else splitName
        }
        return if (name.length > 30) name.substring(0, 30) else name

    }

    override fun getIdString(): String {
        return ModbusConfigView::class.java.simpleName
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }
}
