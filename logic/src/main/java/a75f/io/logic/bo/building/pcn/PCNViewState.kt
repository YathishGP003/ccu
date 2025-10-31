package a75f.io.logic.bo.building.pcn

import a75f.io.logic.connectnode.EquipModel
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf

class PCNViewState {
    sealed interface Equip {
        val serverId: Int
    }
    var pcnEquip = mutableListOf<HashMap<Any, Any>>()
    var connectModuleList = mutableStateListOf<ConnectModule>()
    var externalEquipList = mutableStateListOf<ExternalEquip>()
    var pcnEquips = listOf<PCN>()
    val addedConnectList = mutableStateListOf<ConnectModule>()

    var deletedCNList = mutableListOf<Int>()
    var deletedExternalEquipList = mutableListOf<Int>()

    var baudRate = mutableDoubleStateOf(0.0)
    var parity = mutableDoubleStateOf(0.0)
    var dataBits = mutableDoubleStateOf(0.0)
    var stopBits = mutableDoubleStateOf(0.0)

    /* If newConfiguration means it created before configuration
    *  If modelUpdated means it is updated after configuration
    * */
}

data class PCN(
    // Default serverId is 100
    var serverId: Int = 100,
    val name: String,
    val newConfiguration : Boolean = false,
    val modelUpdated : Boolean,
    val equipModelList: EquipModel,

    val equipData : EquipData = EquipData(
        serverId = 100,
        equipModel = listOf(equipModelList),
        newConfiguration = newConfiguration,
        modelUpdated = modelUpdated
    )
)

data class ConnectModule(
    override var serverId: Int,
    val newConfiguration : Boolean,
    val modelUpdated : Boolean,
    val equipModelList: List<EquipModel> = emptyList(),

    val equipData : EquipData = EquipData(
        serverId = serverId,
        equipModel = equipModelList,
        newConfiguration = newConfiguration,
        modelUpdated = modelUpdated
    )
) : PCNViewState.Equip

data class ExternalEquip(
    override val serverId: Int,
    val name: String,
    val equipModel : EquipModel,
    val newConfiguration : Boolean,
    val modelUpdated : Boolean,

    val equipData : EquipData = EquipData(
        serverId = serverId,
        equipModel = listOf(equipModel),
        newConfiguration = newConfiguration,
        modelUpdated = modelUpdated
    )
): PCNViewState.Equip

data class Point(
    val pointName: String,
    val register: Int,
    var displayInUI: Boolean = false,
    var schedulable: Boolean = false
)

data class EquipData(
    val serverId: Int,
    val equipModel : List<EquipModel>,
    val newConfiguration : Boolean,
    val modelUpdated : Boolean,
    val registerCount : Int = PCNValidation.getSumOfRegisters(equipModel)
)