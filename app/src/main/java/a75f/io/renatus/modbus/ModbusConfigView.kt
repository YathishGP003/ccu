package a75f.io.renatus.modbus

import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.compose.CustomDropdownMenu
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[ModbusConfigViewModel::class.java]
        viewModel.configModelDefinition(requireContext())

        rootView.apply {
            setContent { RootView() }
            return rootView
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


    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            ProgressDialogUtils.showProgressDialog(context, "Loading Modbus Models")
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                ) {
                    TitleTextView(text = "MODBUS")
                }
                Row {
                    HeaderTextView("Equipment Type")
                    CustomDropdownMenu(0, viewModel.deviceList) {
                        ProgressDialogUtils.showProgressDialog(context, "Fetching $it details")
                        viewModel.fetchModelDetails(it)
                    }
                    HeaderTextView("Slave Id")
                    CustomDropdownMenu(0, viewModel.slaveIdList) {}
                }

                Row {
                    HeaderTextView(text = "Select All Parameters")
                    ToggleButton(defaultSelection = viewModel.equipModel.value.selectAllParameters.value) {
                        viewModel.equipModel.value.selectAllParameters.value = it
                        viewModel.onSelectAll(it)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.weight(2f)
                    ) {
                        SubTitle("PARAMETER")
                    }
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        SubTitle("DISPLAY UI")
                    }
                    Box(
                        modifier = Modifier.weight(2f)
                    ) {
                        SubTitle("PARAMETER")
                    }
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        SubTitle("DISPLAY UI")
                    }
                }
            }
            item {
                FlowRow {
                    MyGridRecyclerView(data = viewModel.equipModel, gridColumns = 2)
                }
            }

            item {
                FlowRow {
                    SubEquipments(viewModel.equipModel)
                }
            }
            item {
                Button(onClick = {
                    viewModel.saveConfiguration()
                }) { Text(text = "Save") }
            }
        }


    }

    @Composable
    fun MyGridRecyclerView(data: MutableState<EquipModel>, gridColumns: Int) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            columns = GridCells.Fixed(gridColumns), content = {
                items(data.value.parameters) { item ->
                    Row {
                        LabelTextView(text = item.param.value.name)
                        val toggleState = item.displayInUi.value
                        ToggleButton(defaultSelection = toggleState) {
                            item.displayInUi.value = it
                            item.param.value.getParameterId()
                        }
                    }
                }
            })
    }


    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun SubEquipments(data: MutableState<EquipModel>) {
        FlowColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            LazyColumn {
                items(data.value.subEquips) {subEquip ->
                    Row {
                        HeaderTextView("Equipment Type")
                        HeaderTextView(text = subEquip.value.equipDevice.value.name)
                        HeaderTextView("Slave Id")
                        CustomDropdownMenu(0, viewModel.slaveIdList) {}
                    }
                    Row {
                        Row {
                            HeaderTextView(text = "Select All Parameters")
                            ToggleButton(defaultSelection = subEquip.value.selectAllParameters.value) { it
                                subEquip.value.selectAllParameters.value = it
                            }
                        }
                    }
                    MyGridRecyclerView(data = subEquip, gridColumns = 2)
                }
            }

        }
    }
}
