package a75f.io.renatus.profiles.pcn

import a75f.io.logic.bo.building.pcn.ConnectModule
import a75f.io.logic.connectnode.EquipModel
import a75f.io.renatus.R
import a75f.io.renatus.composables.DeleteDialog
import a75f.io.renatus.compose.BoldStyledGreyTextView
import a75f.io.renatus.compose.FormattedTableWithoutHeader
import a75f.io.renatus.compose.GrayLabelTextColor
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.LabelBoldTextViewForTable
import a75f.io.renatus.compose.SaveTextViewNewExtraBold
import a75f.io.renatus.compose.SearchSpinnerElement
import a75f.io.renatus.compose.TableHeaderRow
import a75f.io.renatus.compose.TextViewWithClickOption
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.util.DISPLAY_UI_CAPITALIZED
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.PARAMETER_CAPITALIZED
import a75f.io.renatus.modbus.util.SCHEDULABLE_CAPITALIZED
import a75f.io.renatus.modbus.util.SEARCH_SLAVE_ID
import a75f.io.renatus.profiles.system.advancedahu.Option
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class PCNUIUtil {

    companion object {
        private const val CONNECT_MODULE = "Connect Module"
        const val EXTERNAL_EQUIP = "External Equip"

        @Composable
        fun ModelConfigurationView(
            isConnect: Boolean,
            serverId: String?,
            connectModule: ConnectModule?,// Optional if no model configured
            viewModel: PCNConfigViewModel,
            fragmentManager: FragmentManager
        ) {
            val hasModel = connectModule?.equipData?.equipModel?.isNotEmpty() == true

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Show header if Connect
                    if (isConnect) {
                        ModuleHeader(
                            moduleTitle = CONNECT_MODULE,
                            serverId = serverId!!,
                            isEnabled = connectModule!!.newConfiguration,
                            viewModel = viewModel,
                            fragmentManager
                        )
                    }

                    if (hasModel) {
                        // Show paired equipment
                        connectModule!!.equipData.equipModel.forEach { equipModel ->
                            ShowPairedEquip(
                                name = equipModel.equipDevice.value.name,
                                serverId = null,
                                equipmentModel = listOf(equipModel),
                                newConfiguration = connectModule.newConfiguration,
                                allowDelete = false,
                                viewModel,
                                fragmentManager
                            )
                        }
                    } else {
                        // No model configured UI
                        Image(
                            modifier = Modifier.padding(top = 44.dp, bottom = 8.dp),
                            painter = painterResource(id = R.drawable.no_content),
                            contentDescription = null
                        )
                        BoldStyledGreyTextView(
                            text = "No Model Configured.",
                            fontSize = 22
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val text = if (isConnect) {
                            "Connect Node."
                        } else {
                            "Smart Node - Custom Code"
                        }
                        GrayLabelTextColor(
                            text = "Go to Site Sequencer to set up a model for this $text.",
                            widthValue = 800,
                            textAlignment = TextAlign.Center
                        )
                    }
                }
            }
        }

        @Composable
        fun ModuleHeader(
            moduleTitle: String,
            serverId: String,
            isEnabled: Boolean = true,
            viewModel: PCNConfigViewModel,
            fragmentManager: FragmentManager
        ) {
            var pendingDeleteConnect by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLeftAlignedTextView(moduleTitle)

                Spacer(modifier = Modifier.width(5.dp))
                SaveTextViewNewExtraBold {
                    pendingDeleteConnect = true

                }

                if (pendingDeleteConnect) {
                    DeleteDialog(
                        onDismissRequest = { pendingDeleteConnect = false },
                        onConfirmation = {
                            if (moduleTitle == CONNECT_MODULE) {
                                viewModel.removeConnectModule(serverId.toInt())
                            } else if (moduleTitle == EXTERNAL_EQUIP) {
                                viewModel.removeExternalEquip(serverId.toInt())
                            }
                            pendingDeleteConnect = false
                        },
                        toDelete = "$moduleTitle $serverId",
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // this one
                showServerList(
                    serverId.toInt(),
                    isEnabled,
                    viewModel,
                    fragmentManager
                )
            }
        }
        @Composable
        fun SpinnerView(
            label: String,
            options: List<Option>,
            defaultIndex: Int,
            isServerId: Boolean = false,
            isEnabled: Boolean = true,
            viewModel: PCNConfigViewModel,
            onSelect: (Option, Option) -> Unit,
        ) {
            var prevSelectedOption by remember {
                mutableStateOf(
                    options.getOrNull(defaultIndex) ?: options.first()
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelBoldTextViewForTable(
                    text = label,
                    modifier = Modifier.width(100.dp),
                    fontSize = 22,
                    fontWeight = if (isServerId) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    fontColor = colorResource(id = R.color.text_color)
                )

                SearchSpinnerElement(
                    default = prevSelectedOption,
                    allItems = options,
                    unit = "",
                    onSelect = { selected ->
                        onSelect(selected, prevSelectedOption)
                        prevSelectedOption = selected
                    },
                    width = 130,
                    isEnabled = isEnabled,
                    disabledIndices = viewModel.getDisabledIndices(isServerId, prevSelectedOption.index + 1)
                )

            }
        }
        fun getSlaveIds(viewModel: PCNConfigViewModel, prevSelectedOption: Int): MutableState<List<String>> {
            val slaveAddress = mutableListOf<String>()

            val disabledIds = viewModel.getDisabledIndices(true, prevSelectedOption)
            slaveAddress.addAll(
                (1..247)
                    .filter { it !in disabledIds }
                    .map { it.toString() }
            )
            return mutableStateOf(slaveAddress)
        }

        private fun showDialogFragment(
            newInstance: DialogFragment,
            id: String,
            fragmentManager: FragmentManager
        ) {
            val ft: FragmentTransaction = fragmentManager.beginTransaction()
            val prev: Fragment? = fragmentManager.findFragmentByTag(id)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)

            // Create and show the dialog.
            newInstance.show(fragmentManager, id)

        }

        @Composable
        fun ShowPairedEquip(
            name: String,
            serverId: Int?,
            equipmentModel: List<EquipModel> = emptyList(),
            newConfiguration: Boolean,
            allowDelete: Boolean,
            viewModel: PCNConfigViewModel,
            fragmentManager: FragmentManager
        ) {
            Column (modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)){
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var pendingDeleteConnect by remember { mutableStateOf(false) }
                    HeaderLeftAlignedTextView(text = name)
                    if (allowDelete) {
                        SaveTextViewNewExtraBold {
                            pendingDeleteConnect = true
                            // viewModel.removeExternalEquip(externalEquip)
                        }
                    }
                    if (pendingDeleteConnect) {
                        DeleteDialog(
                            onDismissRequest = { pendingDeleteConnect = false },
                            onConfirmation = {
                                viewModel.removeExternalEquip(serverId ?: 0)
                                pendingDeleteConnect = false
                            },
                            toDelete = "$name ${serverId ?: ""}",
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    if (serverId != null) {
                        showServerList(
                            serverId,
                            newConfiguration,
                            viewModel,
                            fragmentManager
                        )
                    }
                }
                equipmentModel.sortedBy { it.equipDevice.value.name }.forEach { equipmentModel ->
                    ZoneEquipmentPointsConfigTable(mutableStateOf(equipmentModel), isRootEquip = true, viewModel)
                }
            }
        }

        @Composable
        private fun showServerList(serverId: Int, newConfiguration: Boolean, viewModel: PCNConfigViewModel, fragmentManager: FragmentManager) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                val onItemSelect = object : OnItemSelect {
                    override fun onItemSelected(index: Int, item: String) {
                        viewModel.serverIdChange(item.toInt(), serverId)
                    }
                }
                TextViewWithClickOption(
                    text = mutableStateOf(serverId),
                    onClick = {
                        showDialogFragment(
                            ModelSelectionFragment.newInstance(
                                getSlaveIds(viewModel, serverId),
                                onItemSelect, SEARCH_SLAVE_ID
                            ), ModelSelectionFragment.ID,
                            fragmentManager
                        )
                    },
                    enableClick = newConfiguration,
                )
            }
        }

        @Composable
        fun ZoneEquipmentPointsConfigTable(
            equipmentModel: MutableState<EquipModel>,
            isRootEquip: Boolean,
            viewModel : PCNConfigViewModel
        ) {

            // Stores the width of each column for the table.
            val tableColumnsWidth = remember {
                mutableStateListOf<Float>().apply {
                    repeat(3) {
                        add(0.0f)
                    }
                }
            }

            // List of Column Names to be displayed in the table header.
            val columnList =
                listOf(PARAMETER_CAPITALIZED, DISPLAY_UI_CAPITALIZED, SCHEDULABLE_CAPITALIZED)


            var isAllSelected = true
            equipmentModel.value.parameters.forEach {
                if (!it.displayInUi.value)
                    isAllSelected = false
            }
            equipmentModel.value.selectAllParameters.value = isAllSelected
            // Map of toggle callbacks for the table header.
            val toggleCallbackMap = mapOf(
                DISPLAY_UI_CAPITALIZED to Pair(equipmentModel.value.selectAllParameters.value) { isSelected: Boolean ->
                    equipmentModel.value.selectAllParameters.value = isSelected
                    equipmentModel.value.parameters.forEach {
                        it.displayInUi.value = isSelected
                    }
                }
            )

            // Displays the Table Header and sets the width of each column.
            TableHeaderRow(
                columnList = columnList, toggleCallbackMap
            ) { columnWidth ->
                tableColumnsWidth[columnWidth.first] = columnWidth.second
            }

            // Displays the list of point along with the config options.
            PointListPopulatedConfig(data = equipmentModel, true, tableColumnsWidth)
        }

        @Composable
        fun PointListPopulatedConfig(
            data: MutableState<EquipModel>,
            isRootEquip: Boolean,
            tableColumnsWidth: SnapshotStateList<Float>? = null
        ) {
            data.value.parameters.sortedBy { it.param.value.name }.forEachIndexed { index, registerItem ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {

                    val rowDataList = mutableListOf<Pair<String, Any>>()

                    // For Point Name Column
                    rowDataList.add(
                        Pair(
                            "text", Pair(
                                registerItem.param.value.name,
                                Alignment.CenterStart
                            )
                        )
                    )

                    // For Display in UI Column
                    rowDataList.add(
                        Pair(
                        "toggle",
                        Pair(registerItem.displayInUi.value) { state: Boolean ->
                            registerItem.displayInUi.value = state
                            registerItem.param.value.getParameterId()
                        }
                    ))

                    // For Schedulable Column
                    if (registerItem.param.value.userIntentPointTags.isNullOrEmpty()) {
                        rowDataList.add(Pair("text", Pair("NA", Alignment.Center)))
                    } else {
                        rowDataList.add(
                            Pair(
                                "toggle",
                                Pair(registerItem.schedulable.value) { state: Boolean ->
                                    registerItem.schedulable.value = state
                                    registerItem.param.value.getParameterId()
                                })
                        )
                    }

                    FormattedTableWithoutHeader(
                        rowNo = index,
                        columnWidthList = tableColumnsWidth!!,
                        rowDataList = rowDataList,
                    )
                }
            }
        }
    }
}