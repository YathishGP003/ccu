package a75f.io.renatus.profiles

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.device.modbus.buildModbusModel
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.bacnet.BacnetProfile
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.monitoring.HyperStatV2MonitoringProfile
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.logic.bo.building.otn.OtnProfileConfiguration
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.cpu.HyperStatCpuProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.hpu.HyperStatHpuProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatCpuProfile
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatHpuProfile
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration

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


        fun setSelectedConfiguration(
            address: Int,
            moduleType: String,
            floorPlanFragment: FloorPlanFragment
        ) {
            val equip =
                ccuHsApiInstance.readEntity("zone and equip and not equipRef and group == \"$address\"")
            moduleName = moduleType

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
            if ((modbusModel == null && selectedBacNetModel == null)) {
                val device =
                    ccuHsApiInstance.readEntity("device and equipRef ==\"${equip["id"]}\"and addr == \"$address\"")
                nodeType = getNodeType(device)

                if (nodeType == null && !isConnectNodePaired) {
                    CcuLog.e(
                        L.TAG_CCU_COPY_CONFIGURATION,
                        " Copy Configuration failed : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
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
                    " Copy Configuration failed : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
                )
                return
            }
            try {
                processProfileType(address, equip, ccuHsApiInstance)
                CcuLog.i(
                    L.TAG_CCU_COPY_CONFIGURATION,
                    " Copy Configuration completed : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
                )
                floorPlanFragment.enhancedToastMessage(moduleType)

            } catch (e: Exception) {
                CcuLog.e(
                    L.TAG_CCU_COPY_CONFIGURATION,
                    " Copy Configuration failed : Address $address ,  Profile Type: $profileType ,  Node Type: $nodeType , ModbusModel $modbusModel "
                )
            }

        }

        private fun processProfileType(
            address: Int,
            equip: HashMap<Any, Any>,
            ccuHsApiInstance: CCUHsApi
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
                ProfileType.MYSTAT_PIPE2 -> loadActiveMStatProfilesConfiguration(address, equip)

                ProfileType.CONNECTNODE -> loadConnectNodeConfiguration(address, ccuHsApiInstance)

                else -> throw IllegalArgumentException("Unsupported Profile Type: $profileType")
            }
        }

        private fun loadConnectNodeConfiguration(address: Int, ccuHsApiInstance: CCUHsApi) {
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

        private fun loadActiveHyperStatProfilesConfiguration(address: Int, equip: HashMap<Any, Any>) {
            when (L.getProfile(address.toShort())) {
                is HyperStatCpuProfile -> {
                    config = getHsConfiguration(equip["id"].toString()) as CpuConfiguration
                }
                is HyperStatHpuProfile -> {
                    config = getHsConfiguration(equip["id"].toString()) as HpuConfiguration
                }
                is HyperStatPipe2Profile -> {
                    config = getHsConfiguration(equip["id"].toString()) as a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
                }
                is HyperStatV2MonitoringProfile -> {
                    config = getHsConfiguration(equip["id"].toString()) as MonitoringConfiguration
                }
            }
        }

        private fun loadActiveMStatProfilesConfiguration(address: Int, equip: HashMap<Any, Any>) {
            when (L.getProfile(address.toShort())) {
                is MyStatCpuProfile -> {
                    config = getMyStatConfiguration(equip["id"].toString()) as a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
                }
                is MyStatHpuProfile -> {
                    config = getMyStatConfiguration(equip["id"].toString()) as MyStatHpuConfiguration
                }
                is MyStatPipe2Profile -> {
                    config = getMyStatConfiguration(equip["id"].toString()) as MyStatPipe2Configuration
                }
            }
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