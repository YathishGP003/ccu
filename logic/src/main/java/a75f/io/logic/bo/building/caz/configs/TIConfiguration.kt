package a75f.io.logic.bo.building.caz.configs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.TIEquip
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class TIConfiguration(
    nodeAddress: Int,
    nodeType: String,
    priority: Int,
    roomRef: String,
    floorRef: String,
    profileType: ProfileType,
    val model: SeventyFiveFProfileDirective
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var zonePriority: ValueConfig
    lateinit var roomTemperatureType: ValueConfig
    lateinit var temperatureOffset: ValueConfig
    lateinit var supplyAirTemperatureType: ValueConfig


    override fun getDependencies(): List<ValueConfig> {
        return emptyList()
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf(
            zonePriority,
            roomTemperatureType,
            temperatureOffset,
            supplyAirTemperatureType
        )
    }

    fun getActiveConfiguration(): TIConfiguration {

        var tiRawEquip =
            Domain.hayStack.readEntity("domainName == \"${ModelNames.ti}\" and group == \"$nodeAddress\"")
        // Remove the bellow code after migration all the hyperStat cpu modules
        if (tiRawEquip.isEmpty()) {
            tiRawEquip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        }
        if (tiRawEquip.isEmpty()) {
            return this
        }

        val tiEquip = TIEquip(tiRawEquip[Tags.ID].toString())
        val configuration = this.getDefaultConfiguration()

        configuration.zonePriority.currentVal = tiEquip.zonePriority.readDefaultVal()
        configuration.temperatureOffset.currentVal = tiEquip.temperatureOffset.readDefaultVal()
        configuration.roomTemperatureType.currentVal = tiEquip.roomTemperatureType.readDefaultVal()
        configuration.supplyAirTemperatureType.currentVal = tiEquip.supplyAirTemperatureType.readDefaultVal()

        configuration.isDefault = false
        return configuration
    }


    fun getDefaultConfiguration(): TIConfiguration {
        zonePriority = getDefaultValConfig(DomainName.zonePriority, model)
        roomTemperatureType = getDefaultValConfig(DomainName.roomTemperatureType, model)
        supplyAirTemperatureType = getDefaultValConfig(DomainName.supplyAirTempType, model)
        temperatureOffset = getDefaultValConfig(DomainName.temperatureOffset, model)
        isDefault = true
        return this
    }

    enum class RoomTempType {
        SENSOR_BUS_TEMPERATURE, TH1, TH2
    }

    enum class SupplyTempType {
        NONE, TH1, TH2
    }

    // Framework will not update the physical ref for TI profile because TI profile has
    // reverse configuration like physical point mapping is dynamic and logical point mapping is static.
    // So, we need to update the physical point ref manually.
    fun updatePhysicalPointRef( equipRef: String, deviceRef: String) {
        fun updatePhysicalPointRef(logicalDomainName: String, physicalPointDomain: String, portEnabled: Boolean, port: String) {
            val logicalPoint = Point(logicalDomainName, equipRef)
            if (logicalPoint.pointExists()) {
                val physicalPoint =
                    Domain.hayStack.readEntity("point and domainName == \"$physicalPointDomain\" and deviceRef == \"$deviceRef\"")
                if (physicalPoint.isNotEmpty()) {
                    val rowPoint = RawPoint.Builder().setHashMap(physicalPoint as HashMap)
                    rowPoint.setPointRef(logicalPoint.id)
                    rowPoint.setEnabled(portEnabled)
                    rowPoint.setPort(port)
                    CCUHsApi.getInstance().updatePoint(rowPoint.build(), rowPoint.build().id)
                }
            }
        }

        when (roomTemperatureType.currentVal.toInt()) {
            RoomTempType.SENSOR_BUS_TEMPERATURE.ordinal -> {
                updatePhysicalPointRef(
                    DomainName.currentTemp,
                    DomainName.currentTemp,
                    true,
                    Port.SENSOR_RT.toString()
                )
                 updatePhysicalPointRef(
                    DomainName.roomTemperature,
                    DomainName.th1In,
                     false,
                    Port.TH1_IN.toString()
                )

                updatePhysicalPointRef(
                    DomainName.roomTemperature,
                    DomainName.th2In,
                    false,
                    Port.TH2_IN.toString()
                )
            }

            RoomTempType.TH1.ordinal -> {
                updatePhysicalPointRef(
                    DomainName.currentTemp,
                    DomainName.currentTemp,
                    false,
                    Port.SENSOR_RT.toString()
                )
                updatePhysicalPointRef(
                    DomainName.roomTemperature,
                    DomainName.th1In,
                    true,
                    Port.TH2_IN.toString()
                )
                updatePhysicalPointRef(
                    DomainName.roomTemperature,
                    DomainName.th2In,
                    false,
                    Port.TH2_IN.toString()
                )
            }

            RoomTempType.TH2.ordinal -> {
                updatePhysicalPointRef(
                    DomainName.currentTemp,
                    DomainName.currentTemp,
                    false,
                    Port.SENSOR_RT.toString()
                )
                updatePhysicalPointRef(
                    DomainName.roomTemperature,
                    DomainName.th1In,
                    false,
                    Port.TH1_IN.toString()
                )
                updatePhysicalPointRef(
                    DomainName.roomTemperature,
                    DomainName.th2In,
                    true,
                    Port.TH2_IN.toString()
                )
            }
            else -> {}
        }
        when (supplyAirTemperatureType.currentVal.toInt()) {

            SupplyTempType.NONE.ordinal -> {
                Domain.writeHisValByDomain(DomainName.dischargeAirTemperature, 0.0)
            }
            SupplyTempType.TH1.ordinal -> {
                updatePhysicalPointRef(
                    DomainName.dischargeAirTemperature,
                    DomainName.th1In,
                    true,
                    Port.TH1_IN.toString()
                )
            }

            SupplyTempType.TH2.ordinal -> {
                updatePhysicalPointRef(
                    DomainName.dischargeAirTemperature,
                    DomainName.th2In,
                    true,
                    Port.TH2_IN.toString()
                )
            }

            else -> {}
        }
    }


}