package a75f.io.renatus.profiles.profileUtils

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.renatus.profiles.acb.AcbProfileViewModel
import a75f.io.renatus.profiles.system.StagedRtuProfileViewModel
import a75f.io.renatus.profiles.system.VavModulatingRtuViewModel
import a75f.io.renatus.profiles.vav.VavProfileViewModel
import android.util.Log
import java.util.*

open class UnusedPortsModel {
    companion object {
        fun saveConfiguration(viewModel: Any, firstUnusedPort: String, it: Boolean) {
            when (viewModel) {
                is StagedRtuProfileViewModel -> {
                    viewModel.viewState.unusedPortState[firstUnusedPort] = it
                    viewModel.saveConfiguration()
                }
                is VavModulatingRtuViewModel -> {
                    viewModel.viewState.unusedPortState[firstUnusedPort] = it
                    viewModel.saveConfiguration()
                }
                is VavProfileViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                }
                is AcbProfileViewModel -> {
                    viewModel.profileConfiguration.unusedPorts[firstUnusedPort] = it
                }
            }
        }

        fun saveUnUsedPortStatusOfSystemProfile(profileConfiguration: Any, hayStack: CCUHsApi) {
            val timeStart = System.currentTimeMillis()
            val unusedPorts = when (profileConfiguration) {
                is ModulatingRtuProfileConfig -> profileConfiguration.unusedPorts
                is StagedVfdRtuProfileConfig -> profileConfiguration.unusedPorts
                is StagedRtuProfileConfig -> profileConfiguration.unusedPorts
                else -> null
            }
            val currentPortStatus: HashMap<String, Boolean> = unusedPorts!!
            val cmDevicePortsList = Domain.cmBoardDevice.getPortsDomainNameWithPhysicalPoint()

            val cmPortsDisplayNameWithDomainName = ControlMote.getCmPortsDisplayNameWithDomainName()
            for ((unusedPort, unusedPortState) in currentPortStatus) {
                val unusedPortDomainName = cmPortsDisplayNameWithDomainName[unusedPort] ?: continue
                val rawPoint = cmDevicePortsList[unusedPortDomainName] ?: continue
                val isPortUsedInAlgo = isPortUsedInAlgo(hayStack, unusedPort)

                when {
                    unusedPortState && !isPortUsedInAlgo && !rawPoint.markers.contains(Tags.WRITABLE) -> {
                        rawPoint.markers.add(Tags.WRITABLE)
                        hayStack.updatePoint(rawPoint, rawPoint.id)
                    }
                    (!unusedPortState && rawPoint.markers.contains(Tags.WRITABLE)) || isPortUsedInAlgo -> {
                        hayStack.clearAllAvailableLevelsInPoint(rawPoint.id)
                        rawPoint.markers.remove(Tags.WRITABLE)
                        hayStack.updatePoint(rawPoint, rawPoint.id)
                        hayStack.writeHisValById(rawPoint.id, 0.0)
                    }
                }
            }
            CcuLog.i("UnusedPortsModel", "Time taken to save unused port status: ${System.currentTimeMillis() - timeStart} ms")
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
                else -> null
            }

            val devicePorts = DeviceUtil.getPortsForDevice(deviceAddress, hayStack)
            devicePorts?.forEach { devicePort ->
                currentPortStatus?.get(devicePort.displayName)?.let { portValue ->
                    if (portValue && !devicePort.markers.contains(Tags.WRITABLE)) {
                        devicePort.markers.add(Tags.WRITABLE)
                    } else if ((!portValue || devicePort.enabled) && devicePort.markers.contains(
                            Tags.WRITABLE)) {
                        hayStack.clearAllAvailableLevelsInPoint(devicePort.id)
                        devicePort.markers.remove(Tags.WRITABLE)
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
    }
}