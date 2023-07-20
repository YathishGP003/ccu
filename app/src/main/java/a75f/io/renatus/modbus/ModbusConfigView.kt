package a75f.io.renatus.modbus

import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.compose.SpinnerView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.ParameterLabel
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.EQUIP_TYPE
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.SELECT_ALL
import a75f.io.renatus.modbus.util.SLAVE_ID
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
            meshAddress: Short, roomName: String, floorName: String, profileType: ProfileType
        ): ModbusConfigView {
            val fragment = ModbusConfigView()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            ProgressDialogUtils.showProgressDialog(context, LOADING)
            viewModel.configModelDefinition(requireContext())
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { TitleTextView(MODBUS) }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column {
                        HeaderTextView(EQUIP_TYPE)
                        SpinnerView(selected = 0,
                            items = viewModel.deviceList,
                            itemSelected = { _, selected ->
                                ProgressDialogUtils.showProgressDialog(
                                    context, "Fetching $selected details"
                                )
                                viewModel.fetchModelDetails(selected)
                            })
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        HeaderTextView(if (viewModel.equipModel.value.equipDevice.value.name.isNullOrEmpty()) "" else viewModel.equipModel.value.equipDevice.value.name )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            HeaderTextView(SLAVE_ID)
                            SpinnerView(selected = 0,
                                items = viewModel.slaveIdList,
                                itemSelected = { index, _ ->
                                    viewModel.equipModel.value.slaveId.value =
                                        viewModel.slaveIdList.value[index].toInt()
                                })
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    HeaderTextView(SELECT_ALL)
                    ToggleButton(defaultSelection = viewModel.equipModel.value.selectAllParameters.value) {
                        viewModel.equipModel.value.selectAllParameters.value = it
                        viewModel.onSelectAll(it)
                    }
                }
                ParameterLabel()
            }
            item { FlowRow { ParametersListView(data = viewModel.equipModel, gridColumns = 2) } }
            item { FlowRow { SubEquipments(viewModel.equipModel) } }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                    contentAlignment = Alignment.CenterEnd) {
                    SaveTextView(SAVE)  { viewModel.saveConfiguration() }
                }
            }
        }
    }

    @Composable
    fun ParametersListView(data: MutableState<EquipModel>, gridColumns: Int) {
        LazyVerticalGrid(modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
            columns = GridCells.Fixed(gridColumns),
            content = {
                items(data.value.parameters) { item ->
                    Row {
                        LabelTextView(item.param.value.name)
                        ToggleButton(item.displayInUi.value) {
                            item.displayInUi.value = it
                            item.param.value.getParameterId()
                            viewModel.updateSelectAll()
                        }
                    }
                }
            })
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun SubEquipments(data: MutableState<EquipModel>) {
        FlowColumn(modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)) {
            LazyColumn {
                items(data.value.subEquips) { subEquip ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            HeaderTextView(text = subEquip.value.equipDevice.value.name)
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                HeaderTextView(SLAVE_ID)
                                SpinnerView(0, viewModel.childSlaveIdList) { index, _ ->
                                    if (index == 0) {
                                        subEquip.value.slaveId.value =
                                            viewModel.equipModel.value.slaveId.value
                                    } else {
                                        subEquip.value.slaveId.value =
                                            viewModel.childSlaveIdList.value[index].toInt()
                                    }
                                }
                            }
                        }
                    }
                    Row { ParameterLabel() }
                    ParametersListView(data = subEquip, gridColumns = 2)
                }
            }
        }
    }

    override fun getIdString(): String {
        return ModbusConfigView::class.java.simpleName
    }
    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1200
            val height = 700
            dialog.window!!.setLayout(width, height)
        }
    }
}
