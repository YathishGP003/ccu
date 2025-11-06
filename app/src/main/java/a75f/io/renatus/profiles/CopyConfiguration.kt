package a75f.io.renatus.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.domain.api.Domain
import a75f.io.logic.bo.building.modbus.buildModbusModel
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.bacnet.BacnetProfile
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.logic.bo.building.otn.OtnProfileConfiguration
import a75f.io.logic.bo.building.pcn.PCNUtil
import a75f.io.logic.bo.building.pcn.PcnConfiguration
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.logic.util.bacnet.buildBacnetModel
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.getBacnetPoints
import a75f.io.renatus.modbus.util.getNodeType
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.isAllParamsSelected
import a75f.io.renatus.modbus.util.isAllParamsSelectedBacNet
import a75f.io.renatus.modbus.util.isMyStatEquip
import android.annotation.SuppressLint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import okhttp3.internal.toImmutableList
import kotlin.properties.Delegates

class CopyConfiguration {
    companion object {
        //
        private lateinit var config: ProfileConfiguration
        @SuppressLint("StaticFieldLeak")
        private val ccuHsApiInstance = CCUHsApi.getInstance()

        private var profileType: ProfileType? = null
        private var moduleName: String? = null
        private var nodeType: NodeType? = null
        lateinit var equipModel: SeventyFiveFProfileDirective

        private lateinit var modbusProfile: ModbusProfile
        private var selectedSlaveId by Delegates.notNull<Short>()
        private var modbusEquipModel = mutableStateOf(EquipModel())
        private var modbusModel: String? = null

        private var bacnetModel = mutableStateOf(BacnetModel())
        private lateinit var bacnetProfile: BacnetProfile
        private var selectedBacNetModel: String? = null

        private var equipmentDeviceList: List<EquipmentDevice>? = null
        private lateinit var MyStatDeviceType :String


        fun getCopiedConfiguration(): ProfileConfiguration {
            return config
        }

        fun getCopiedModbusConfiguration(): MutableState<EquipModel> {
            return modbusEquipModel
        }
        fun getCopiedBacnetConfiguration(): MutableState<BacnetModel> {
            return bacnetModel
        }
        fun getCopiedConnectNodeConfiguration(): List<EquipmentDevice>? {
            return equipmentDeviceList
        }

        fun getSelectedModbusModel(): String? {
            return modbusModel
        }
        fun getSelectedBacNetModel(): String? {
            return selectedBacNetModel
        }

        fun getSelectedProfileType(): ProfileType? {
            return profileType
        }

        fun getSelectedNodeType(): NodeType? {
            return nodeType
        }

        fun getModuleName(): String? {
            return moduleName
        }
        fun getMyStatDeviceType(): String {
            return MyStatDeviceType
        }


        fun setSelectedConfiguration(
            address: Int,
            moduleType: String,
            floorPlanFragment: FloorPlanFragment
        ) {
            val equip = ccuHsApiInstance.readEntity("zone and equip and not equipRef and group == \"$address\"")

            if (isMyStatEquip(equip)) {
                val device = Domain.getEquipDevices()[equip["id"].toString()] as MyStatDevice
                val devicesType = device.mystatDeviceVersion.readPointValue()
                moduleName = moduleType.replace("Mystat -",if (devicesType == 1.0)  "MyStat V2 -" else "MyStat V1 -")
                MyStatDeviceType = if (devicesType == 1.0) a75f.io.logic.bo.building.statprofiles.util.MyStatDeviceType.MYSTAT_V2.name else a75f.io.logic.bo.building.statprofiles.util.MyStatDeviceType.MYSTAT_V1.name
            }
            else {
                moduleName = moduleType
            }
            modbusModel = equip["modbus"]?.let { equip["model"].toString() }
            selectedBacNetModel = equip["bacnet"]?.let {
                equip["modelConfig"]
                    ?.toString()
                    ?.substringAfter("modelName:")
                    ?.substringBefore(",")
            }

            profileType = CCUUtils.getProfileType(
                equip["profile"]?.toString(),
                address
            )
            val isConnectNodePaired = ConnectNodeUtil.isZoneContainingConnectNodeWithEquips(address.toString(), ccuHsApiInstance)
            val isPCNPaired = PCNUtil.isZoneContainingPCNWithEquips(address.toString(), ccuHsApiInstance)
            if ((modbusModel == null && selectedBacNetModel == null)) {
                val device =
                    ccuHsApiInstance.readEntity("device and equipRef ==\"${equip["id"]}\"and addr == \"$address\"")
                nodeType = getNodeType(device)

                if (nodeType == null && !isConnectNodePaired && !isPCNPaired) {
                    CcuLog.e(
                        L.TAG_CCU_COPY_CONFIGURATION,
                        " Copy Configuration failed : Address $address ,  Profile Type: $profileType ,  Node Type: null , ModbusModel $modbusModel "
                    )
                    return
                }
            }
            CcuLog.i(
                L.TAG_CCU_COPY_CONFIGURATION,
                " Copy Configuration received : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
            )
            if (profileType == null) {
                CcuLog.e(
                    L.TAG_CCU_COPY_CONFIGURATION,
                    " Copy Configuration failed : Address $address ,  Profile Type: null  ,  Node Type: $nodeType ! , ModbusModel $modbusModel "
                )
                return
            }
            try {
                processProfileType(address, equip)
                CcuLog.i(
                    L.TAG_CCU_COPY_CONFIGURATION,
                    " Copy Configuration completed : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
                )
                floorPlanFragment.enhancedToastMessage(moduleName)

            } catch (e: Exception) {
                CcuLog.e(
                    L.TAG_CCU_COPY_CONFIGURATION,
                    " Copy Configuration failed : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
                )
            }

        }

        private fun processProfileType(
            address: Int,
            equip: HashMap<Any, Any>
        ) {
            when (profileType) {
                ProfileType.VAV_PARALLEL_FAN,
                ProfileType.VAV_SERIES_FAN,
                ProfileType.VAV_REHEAT -> loadActiveVavConfiguration(address, equip)

                ProfileType.VAV_ACB -> loadActiveAcbConfiguration(address, equip)

                ProfileType.DAB -> loadActiveDabConfiguration(address, equip)

                ProfileType.SSE -> loadActiveSseConfiguration(address, equip)

                ProfileType.PLC -> loadActivePlcConfiguration(address, equip)

                ProfileType.OTN -> loadActiveOtnConfiguration(address, equip)

                ProfileType.HYPERSTAT_MONITORING,
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
                ProfileType.HYPERSTAT_HEAT_PUMP_UNIT ,
                ProfileType.HYPERSTAT_TWO_PIPE_FCU -> loadActiveHyperStatProfilesConfiguration(address,equip)

                ProfileType.HYPERSTATSPLIT_CPU -> loadActiveHyperStatSplitConfiguration(address, equip)

                ProfileType.MODBUS_DEFAULT,
                ProfileType.MODBUS_EMR -> loadActiveModbusOrEmrConfiguration(address.toShort())
                ProfileType.BACNET_DEFAULT -> loadActiveBacNetConfiguration(address.toLong())

                ProfileType.MYSTAT_HPU,
                ProfileType.MYSTAT_CPU,
                ProfileType.MYSTAT_PIPE2,
                ProfileType.MYSTAT_PIPE4 -> loadActiveMStatProfilesConfiguration(address, equip)

                ProfileType.CONNECTNODE -> loadConnectNodeConfiguration(address)

                ProfileType.HYPERSTATSPLIT_4PIPE_UV , ProfileType.HYPERSTATSPLIT_2PIPE_UV-> {
                   loadActiveUnitVentilatorConfiguration(equip)
                }

                ProfileType.PCN -> loadActivePCNConfiguration(address)

                else -> throw IllegalArgumentException("Unsupported Profile Type: $profileType")
            }
        }

        private fun loadActivePCNConfiguration(address: Int) {
            val deviceMap = ccuHsApiInstance.readEntity("device and addr == \"$address\"")
            config = PcnConfiguration(
                nodeAddress = deviceMap["addr"].toString().toInt(),
                nodeType = NodeType.PCN.name,
                priority = 0,
                roomRef = deviceMap[Tags.ROOM_REF].toString(),
                floorRef = deviceMap[Tags.FLOOR_REF].toString(),
                profileType = ProfileType.PCN,
            ).getActiveConfiguration()
        }

        private fun loadConnectNodeConfiguration(address: Int) {
            val roomRef = ccuHsApiInstance.readEntity("device and addr == \"$address\"")[Tags.ROOM_REF].toString()
            equipmentDeviceList = buildModbusModel(roomRef)
                .let { ConnectNodeUtil.reorderEquipments(it) }
            CcuLog.i(L.TAG_CONNECT_NODE, "Loaded Connect Node Equipments for address: $address, Equipments: $equipmentDeviceList")
        }

        private fun loadActiveVavConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = getProfileDomainModel()
            config = VavProfileConfiguration(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                profileType!!,
                equipModel
            ).getActiveConfiguration()
        }

        private fun loadActiveOtnConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = ModelLoader.getOtnTiModel() as SeventyFiveFProfileDirective
            config = OtnProfileConfiguration(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                profileType!!,
                equipModel
            ).getActiveConfiguration()

        }

        private fun loadActiveSseConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = getProfileDomainModel()
            config = SseProfileConfiguration(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                profileType!!,
                equipModel
            ).getActiveConfiguration()
        }

        private fun loadActivePlcConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = getProfileDomainModel()
            config = PlcProfileConfig(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                profileType!!,
                equipModel
            ).getActiveConfiguration()
        }

        private fun loadActiveAcbConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = getProfileDomainModel()
            config = AcbProfileConfiguration(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                profileType!!,
                equipModel
            ).getActiveConfiguration()
        }

        private fun loadActiveDabConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = getProfileDomainModel()
            config = DabProfileConfiguration(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                ProfileType.DAB,
                equipModel
            ).getActiveConfiguration()
        }

        private fun loadActiveHyperStatSplitConfiguration(address: Int, equip: HashMap<Any, Any>) {
            equipModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
            config = HyperStatSplitCpuConfiguration(
                address,
                nodeType.toString(),
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                ProfileType.HYPERSTATSPLIT_CPU,
                equipModel
            ).getActiveConfiguration()
        }

        private fun loadActiveUnitVentilatorConfiguration(equip: HashMap<Any, Any>) {
            config = getSplitConfiguration(equip["id"].toString()) as UnitVentilatorConfiguration
        }

        private fun loadActiveHyperStatProfilesConfiguration(address: Int, equip: HashMap<Any, Any>) {
            config = getHsConfiguration(equip["id"].toString()) as HyperStatConfiguration
        }

        private fun loadActiveMStatProfilesConfiguration(address: Int, equip: HashMap<Any, Any>) {
            config = getMyStatConfiguration(equip["id"].toString()) as MyStatConfiguration
        }

        private fun loadActiveModbusOrEmrConfiguration(address: Short) {
            profileType = null
            nodeType =null
            val profile = L.getProfile(address) as? ModbusProfile ?: return
            modbusProfile = profile
            selectedSlaveId = modbusProfile.slaveId
            val equipmentDevice = buildModbusModel(selectedSlaveId.toInt())

            val model = EquipModel()
            model.equipDevice.value = equipmentDevice
            model.selectAllParameters.value = isAllParamsSelected(equipmentDevice)
            model.parameters = getParameters(equipmentDevice)
            modbusEquipModel.value = model
            val subDeviceList = mutableListOf<MutableState<EquipModel>>()
            equipmentDevice.equips?.forEach {
                val subEquip = EquipModel()
                subEquip.equipDevice.value = it
                subEquip.parameters = getParameters(it)
                subDeviceList.add(mutableStateOf(subEquip))
            }
            modbusEquipModel.value.subEquips = subDeviceList.ifEmpty { mutableListOf() }
        }

        private fun loadActiveBacNetConfiguration(address: Long) {

            if (L.getProfile(address) != null) {
                bacnetProfile = L.getProfile(address) as BacnetProfile
                val equipmentDevice = buildBacnetModel(bacnetProfile.equip.roomRef)
                val model = BacnetModel()
                model.equipDevice.value = equipmentDevice[0]
                model.selectAllParameters.value = isAllParamsSelectedBacNet(equipmentDevice[0])
                model.points = getBacnetPoints(equipmentDevice[0].points.toImmutableList())
                bacnetModel.value = model
            }
        }

        private fun getProfileDomainModel(): SeventyFiveFProfileDirective {
            return when (profileType) {
                ProfileType.DAB -> {
                    if (nodeType == NodeType.SMART_NODE) {
                        ModelLoader.getSmartNodeDabModel()
                    } else {
                        ModelLoader.getHelioNodeDabModel()
                    }
                }

                ProfileType.VAV_ACB -> {
                    if (nodeType == NodeType.SMART_NODE) {
                        ModelLoader.getSmartNodeVavAcbModelDef()
                    } else {
                        ModelLoader.getHelioNodeVavAcbModelDef()
                    }
                }

                ProfileType.SSE -> {
                    if (nodeType == NodeType.SMART_NODE) {
                        ModelLoader.getSmartNodeSSEModel() as SeventyFiveFProfileDirective
                    } else {
                        ModelLoader.getHelioNodeSSEModel() as SeventyFiveFProfileDirective
                    }
                }

                ProfileType.PLC -> {
                    if (nodeType == NodeType.SMART_NODE) {
                        ModelLoader.getSmartNodePidModel() as SeventyFiveFProfileDirective
                    } else {
                        ModelLoader.getHelioNodePidModel() as SeventyFiveFProfileDirective
                    }
                }

                else -> {
                    val modelGetter = when (profileType.toString()) {
                        ProfileType.VAV_SERIES_FAN.toString() -> {
                            if (nodeType == NodeType.SMART_NODE) {
                                ModelLoader::getSmartNodeVavSeriesModelDef
                            } else {
                                ModelLoader::getHelioNodeVavSeriesModelDef
                            }
                        }

                        ProfileType.VAV_PARALLEL_FAN.toString() -> {
                            if (nodeType == NodeType.SMART_NODE) {
                                ModelLoader::getSmartNodeVavParallelFanModelDef
                            } else {
                                ModelLoader::getHelioNodeVavParallelFanModelDef
                            }
                        }

                        else -> {
                            if (nodeType == NodeType.SMART_NODE) {
                                ModelLoader::getSmartNodeVavNoFanModelDef
                            } else {
                                ModelLoader::getHelioNodeVavNoFanModelDef
                            }
                        }
                    }
                    modelGetter.invoke()
                }
            } as SeventyFiveFProfileDirective
        }

    }
}