package a75f.io.renatus.profiles.profileUtils

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.system.vav.config.DabModulatingRtuProfileConfig
import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.renatus.profiles.acb.AcbProfileViewModel
import a75f.io.renatus.profiles.dab.DabProfileViewModel
import a75f.io.renatus.profiles.system.DabModulatingRtuViewModel
import a75f.io.renatus.profiles.system.DabStagedRtuViewModel
import a75f.io.renatus.profiles.system.DabStagedVfdRtuViewModel
import a75f.io.renatus.profiles.sse.SseProfileViewModel
import a75f.io.renatus.profiles.oao.OAOViewModel
import a75f.io.renatus.profiles.system.StagedRtuProfileViewModel
import a75f.io.renatus.profiles.system.VavModulatingRtuViewModel
import a75f.io.renatus.profiles.vav.VavProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.system.measureTimeMillis

open class UnusedPortsModel {
    companion object {
        fun saveConfiguration(viewModel: Any, firstUnusedPort: String, it: Boolean) {
            when (viewModel) {
                is StagedRtuProfileViewModel -> {
                    viewModel.viewState.value.unusedPortState[firstUnusedPort] = it
                    viewModel.setStateChanged()
                }
                is VavModulatingRtuViewModel -> {
                    viewModel.viewState.value.unusedPortState[firstUnusedPort] = it
                    viewModel.setStateChanged()
                }
                is VavProfileViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                }
                is AcbProfileViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                }
                is DabProfileViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                }
                is DabStagedRtuViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                    viewModel.setStateChanged()
                }
                is DabStagedVfdRtuViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                    viewModel.setStateChanged()
                }
                is DabModulatingRtuViewModel -> {
                    viewModel.viewState.value.unusedPortState[firstUnusedPort] = it
                    viewModel.setStateChanged()
                }
                is SseProfileViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                }
            }
        }

        fun saveUnUsedPortStatusOfSystemProfile(profileConfiguration: Any, hayStack: CCUHsApi) {
            CcuLog.i(L.TAG_CCU_DOMAIN, "Saving unused ports")
            val unusedPorts = when (profileConfiguration) {
                is DabModulatingRtuProfileConfig -> profileConfiguration.unusedPorts
                is ModulatingRtuProfileConfig -> profileConfiguration.unusedPorts
                is StagedVfdRtuProfileConfig -> profileConfiguration.unusedPorts
                is StagedRtuProfileConfig -> profileConfiguration.unusedPorts
                else -> null
            }
            val currentPortStatus: HashMap<String, Boolean> = unusedPorts!!
            for ((port, status) in ControlMote.getAllUnusedPorts()) {
                if (!currentPortStatus.containsKey(port)) {
                    currentPortStatus[port] = status
                }
            }
            val cmDevicePortsList = Domain.cmBoardDevice.getPortsDomainNameWithPhysicalPoint()

            val cmPortsDisplayNameWithDomainName = ControlMote.getCmPortsDisplayNameWithDomainName()
            val savingTime = measureTimeMillis {
                runBlocking {
                    val deferredResults = currentPortStatus.map { (unusedPort, unusedPortState) ->
                        async(Dispatchers.Default) {
                            try {
                                val unusedPortDomainName = cmPortsDisplayNameWithDomainName[unusedPort] ?: return@async
                                val rawPoint = cmDevicePortsList[unusedPortDomainName] ?: return@async
                                val isPortUsedInAlgo = isPortUsedInAlgo(hayStack, unusedPort)
                                CcuLog.d(L.TAG_CCU_DOMAIN, "$unusedPort is used? $isPortUsedInAlgo")
                                when {
                                    unusedPortState && !rawPoint.markers.contains(Tags.UNUSED) -> {
                                        CcuLog.d(L.TAG_CCU_DOMAIN, "Adding writable tag - ${rawPoint.id}")
                                        rawPoint.markers.add(Tags.WRITABLE)
                                        rawPoint.markers.add(Tags.UNUSED)
                                        hayStack.updatePoint(rawPoint, rawPoint.id)
                                    }
                                    (!unusedPortState && rawPoint.markers.contains(Tags.UNUSED)) -> {
                                        CcuLog.d(L.TAG_CCU_DOMAIN, "Removing writable tag - ${rawPoint.id}")
                                        hayStack.clearAllAvailableLevelsInPoint(rawPoint.id)
                                        rawPoint.markers.remove(Tags.WRITABLE)
                                        rawPoint.markers.remove(Tags.UNUSED)
                                        hayStack.updatePoint(rawPoint, rawPoint.id)
                                        hayStack.writeHisValById(rawPoint.id, 0.0)
                                    }
                                }
                            } catch (e: Exception) {
                                CcuLog.e(L.TAG_CCU_DOMAIN, "Error processing port $unusedPort", e)
                            }
                        }
                    }
                    deferredResults.awaitAll()
                }
            }
            CcuLog.i(L.TAG_CCU_DOMAIN, "Saved unused ports in $savingTime ms")
        }

        private fun isPortUsedInAlgo(hayStack: CCUHsApi, unusedPort: String): Boolean {
            val domainName = HSUtil.getKeyByValue(ControlMote.getSystemEquipPointsDomainNameWithCmPortsDisName(),unusedPort)
            return if (domainName != null) {
                hayStack.readDefaultVal("domainName == \""+domainName+"\" and equipRef == \""+Domain.systemEquip.equipRef+"\"") > 0
            } else {
                CcuLog.e(
                    L.TAG_CCU_ERROR,
                    "Domain name is null while checking unused port: $unusedPort used in Algo"
                )
                true
            }
        }
        fun saveUnUsedPortStatus(
            profileConfiguration: Any,
            deviceAddress: Short,
            hayStack: CCUHsApi
        ) {
            val currentPortStatus = when (profileConfiguration) {
                is VavProfileConfiguration -> profileConfiguration.unusedPorts
                is AcbProfileConfiguration -> profileConfiguration.unusedPorts
                is DabProfileConfiguration -> profileConfiguration.unusedPorts
                is SseProfileConfiguration -> profileConfiguration.unusedPorts
                else -> null
            }

            val devicePorts = DeviceUtil.getPortsForDevice(deviceAddress, hayStack)
            devicePorts?.forEach { devicePort ->
                currentPortStatus?.get(devicePort.displayName)?.let { portValue ->
                    // In terminal profile, while saving unused ports, we need to check if the port is used in the algo
                    if (portValue && !devicePort.markers.contains(Tags.UNUSED) && !devicePort.enabled) {
                        devicePort.markers.add(Tags.WRITABLE)
                        devicePort.markers.add(Tags.UNUSED)
                    } else if ((!portValue || devicePort.enabled) && devicePort.markers.contains(
                            Tags.UNUSED)) {
                        hayStack.clearAllAvailableLevelsInPoint(devicePort.id)
                        devicePort.markers.remove(Tags.WRITABLE)
                        devicePort.markers.remove(Tags.UNUSED)
                        hayStack.writeHisValById(devicePort.id, 0.0)
                    }
                    hayStack.updatePoint(devicePort, devicePort.id)
                }
            }
        }

        fun initializeUnUsedPorts(
            deviceAddress: Short,
            hayStack: CCUHsApi
        ): HashMap<String, Boolean> {
            val devicePorts = DeviceUtil.getPortsForDevice(deviceAddress, hayStack)
            return if (devicePorts != null) {
                devicePorts.filter { disabledPort ->
                    !disabledPort.enabled
                }.associate { disabledPort ->
                    disabledPort.displayName to disabledPort.markers.contains(Tags.WRITABLE)
                }.toMutableMap() as HashMap<String, Boolean>
            } else {
                hashMapOf()
            }
        }

        fun setPortState(portName: String, state: Boolean, profileConfiguration: Any) : HashMap<String, Boolean>{
            val unusedPorts = when (profileConfiguration) {
                is DabModulatingRtuProfileConfig -> profileConfiguration.unusedPorts
                is ModulatingRtuProfileConfig -> profileConfiguration.unusedPorts
                is StagedVfdRtuProfileConfig -> profileConfiguration.unusedPorts
                is StagedRtuProfileConfig -> profileConfiguration.unusedPorts
                else -> null
            }

            if(unusedPorts != null && unusedPorts.containsKey(portName) && state) {
                unusedPorts.remove(portName)
            } else if(unusedPorts != null && !unusedPorts.containsKey(portName) && !state) {
                unusedPorts[portName] = false
            }
            CcuLog.i(L.TAG_CCU_DOMAIN, "unused ports: $unusedPorts")
            return unusedPorts!!
        }
    }
}