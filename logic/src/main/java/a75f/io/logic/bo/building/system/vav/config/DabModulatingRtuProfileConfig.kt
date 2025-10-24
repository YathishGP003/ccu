package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.DabModulatingRtuSystemEquip
import a75f.io.domain.util.CommonQueries
import a75f.io.logic.bo.haystack.device.ControlMote.getCMUnusedPorts
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class DabModulatingRtuProfileConfig(override val model: SeventyFiveFProfileDirective) : ModulatingRtuProfileConfig(model) {

    lateinit var adaptiveDeltaEnable: EnableConfig
    lateinit var maximizedExitWaterTempEnable: EnableConfig
    lateinit var dcwbEnable: EnableConfig
    lateinit var chilledWaterTargetDelta: ValueConfig
    lateinit var chilledWaterExitTemperatureMargin: ValueConfig
    lateinit var chilledWaterExitTemperatureTarget: ValueConfig
    lateinit var chilledWaterMaxFlowRate: ValueConfig
    lateinit var analog1ValveClosedPosition: ValueConfig
    lateinit var analog1ValveFullPosition: ValueConfig

    override fun getDefaultConfiguration(): DabModulatingRtuProfileConfig {
        super.getDefaultConfiguration()
        adaptiveDeltaEnable = getDefaultEnableConfig(DomainName.adaptiveDeltaEnable, model)
        maximizedExitWaterTempEnable = getDefaultEnableConfig(DomainName.maximizedExitWaterTempEnable, model)
        dcwbEnable = getDefaultEnableConfig(DomainName.dcwbEnable, model)
        chilledWaterTargetDelta = getDefaultValConfig(DomainName.chilledWaterTargetDelta, model)
        chilledWaterExitTemperatureMargin = getDefaultValConfig(DomainName.chilledWaterExitTemperatureMargin, model)
        chilledWaterExitTemperatureTarget = getDefaultValConfig(DomainName.chilledWaterExitTemperatureTarget, model)
        chilledWaterMaxFlowRate = getDefaultValConfig(DomainName.chilledWaterMaxFlowRate, model)
        analog1ValveClosedPosition = getDefaultValConfig(DomainName.analog1ValveClosedPosition, model)
        analog1ValveFullPosition = getDefaultValConfig(DomainName.analog1ValveFullPosition, model)
        return this
    }

    override fun getActiveConfiguration(): DabModulatingRtuProfileConfig {

        val equip = Domain.hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (equip.isEmpty()) {
            return this
        }
        val dabModulatingRtuSystemEquip = DabModulatingRtuSystemEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        analog1OutputEnable.enabled =
            dabModulatingRtuSystemEquip.analog1OutputEnable.readDefaultVal() > 0
        analog2OutputEnable.enabled =
            dabModulatingRtuSystemEquip.analog2OutputEnable.readDefaultVal() > 0
        analog3OutputEnable.enabled =
            dabModulatingRtuSystemEquip.analog3OutputEnable.readDefaultVal() > 0
        analog4OutputEnable.enabled =
            dabModulatingRtuSystemEquip.analog4OutputEnable.readDefaultVal() > 0
        relay3OutputEnable.enabled =
            dabModulatingRtuSystemEquip.relay3OutputEnable.readDefaultVal() > 0
        relay7OutputEnable.enabled =
            dabModulatingRtuSystemEquip.relay7OutputEnable.readDefaultVal() > 0
        adaptiveDeltaEnable.enabled =
            dabModulatingRtuSystemEquip.adaptiveDeltaEnable.readDefaultVal() > 0
        maximizedExitWaterTempEnable.enabled =
            dabModulatingRtuSystemEquip.maximizedExitWaterTempEnable.readDefaultVal() > 0
        dcwbEnable.enabled =
            dabModulatingRtuSystemEquip.dcwbEnable.readDefaultVal() > 0

        thermistor1Enabled.enabled = dabModulatingRtuSystemEquip.thermistor1InputEnable.readDefaultVal() > 0
        thermistor2Enabled.enabled = dabModulatingRtuSystemEquip.thermistor2InputEnable.readDefaultVal() > 0
        analogIn1Enabled.enabled = dabModulatingRtuSystemEquip.analog1InputEnable.readDefaultVal() > 0
        analogIn2Enabled.enabled = dabModulatingRtuSystemEquip.analog2InputEnable.readDefaultVal() > 0

        chilledWaterTargetDelta.currentVal = if(dcwbEnable.enabled && adaptiveDeltaEnable.enabled) {
            dabModulatingRtuSystemEquip.chilledWaterTargetDelta.readDefaultVal()
        } else {
            getDefaultValConfig(DomainName.chilledWaterTargetDelta, model).currentVal
        }
        chilledWaterMaxFlowRate.currentVal = if(dcwbEnable.enabled) {
            dabModulatingRtuSystemEquip.chilledWaterMaxFlowRate.readDefaultVal()
        } else {
            getDefaultValConfig(DomainName.chilledWaterMaxFlowRate, model).currentVal
        }
        chilledWaterExitTemperatureMargin.currentVal = if(dcwbEnable.enabled && !adaptiveDeltaEnable.enabled) {
            dabModulatingRtuSystemEquip.chilledWaterExitTemperatureMargin.readDefaultVal()
        } else {
            getDefaultValConfig(DomainName.chilledWaterExitTemperatureMargin, model).currentVal
        }

        analog1OutputAssociation.associationVal = if (analog1OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.analog1OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model).associationVal
        }

        analog2OutputAssociation.associationVal = if (analog2OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.analog2OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model).associationVal
        }

        analog3OutputAssociation.associationVal = if (analog3OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.analog3OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model).associationVal
        }

        analog3OutputAssociation.associationVal = if (analog3OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.analog3OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model).associationVal
        }

        analog4OutputAssociation.associationVal = if (analog4OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.analog4OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model).associationVal
        }

        relay3Association.associationVal = if (relay3OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.relay3OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model).associationVal
        }

        relay7Association.associationVal = if (relay7OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.relay7OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model).associationVal
        }

        analog1ValveClosedPosition.currentVal = if (dcwbEnable.enabled) {
            dabModulatingRtuSystemEquip.analog1ValveClosedPosition.readDefaultVal()
        } else {
            getDefaultValConfig(DomainName.analog1ValveClosedPosition, model).currentVal
        }
        analog1ValveFullPosition.currentVal = if (dcwbEnable.enabled) {
            dabModulatingRtuSystemEquip.analog1ValveFullPosition.readDefaultVal()
        } else {
            getDefaultValConfig(DomainName.analog1ValveFullPosition, model).currentVal
        }

        thermistor1InAssociation.associationVal = if (thermistor1Enabled.enabled) {
            dabModulatingRtuSystemEquip.thermistor1InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model).associationVal
        }

        thermistor2InAssociation.associationVal = if (thermistor2Enabled.enabled) {
            dabModulatingRtuSystemEquip.thermistor2InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model).associationVal
        }
        updateAnalogActiveConfig(dabModulatingRtuSystemEquip)

        unusedPorts = getCMUnusedPorts(Domain.hayStack)
        isDefault = false
        return this
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return super.getEnableConfigs() + mutableListOf<EnableConfig>().apply {
            add(adaptiveDeltaEnable)
            add(maximizedExitWaterTempEnable)
            add(dcwbEnable)
        }
    }

    private fun updateAnalogActiveConfig(dabModulatingRtuSystemEquip: DabModulatingRtuSystemEquip) {
        analog1OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MinFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MaxFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MinCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MaxCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MinOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )

            outsideAirDamperConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MaxOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MinCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MaxCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MinHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MaxHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )

            chilledWaterValveConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MinCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.min
                )
            chilledWaterValveConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog1MaxCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.max
                )

        }
        analog2OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MinFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MaxFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MinCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MaxCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MinOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MaxOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MinCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MaxCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MinHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MaxHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )

            chilledWaterValveConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MinCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.min
                )
            chilledWaterValveConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog2MaxCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.max
                )
        }

        analog3OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MinFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MaxFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MinCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MaxCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MinOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MaxOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MinCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MaxCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MinHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MaxHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )

            chilledWaterValveConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MinCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.min
                )
            chilledWaterValveConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog3MaxCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.max
                )
        }


        analog4OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MinFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MaxFan,
                    dabModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MinCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MaxCompressorSpeed,
                    dabModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MinOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MaxOutsideDamper,
                    dabModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MinCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MaxCooling,
                    dabModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MinHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MaxHeating,
                    dabModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )

            chilledWaterValveConfig.min =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MinCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.min
                )
            chilledWaterValveConfig.max =
                getDefault(
                    dabModulatingRtuSystemEquip.analog4MaxCHWValve,
                    dabModulatingRtuSystemEquip,
                    chilledWaterValveConfig.max
                )
        }

    }

    override fun getValueConfigs(): List<ValueConfig> {
        return super.getValueConfigs() + mutableListOf<ValueConfig>().apply {
            add(chilledWaterTargetDelta)
            add(chilledWaterExitTemperatureMargin)
            add(chilledWaterMaxFlowRate)
            add(analog1ValveClosedPosition)
            add(analog1ValveFullPosition)
        }
    }
    fun getDefault(point: Point, equip: DabModulatingRtuSystemEquip, valueConfig: Int): Int {
        return if (Domain.readPointForEquip(point.domainName, equip.equipRef).isEmpty())
            valueConfig
        else
            point.readDefaultVal().toInt()
    }

    override fun toString(): String {
        return super.toString()+" DabModulatingRtuProfileConfig(model=$model, " +
                "adaptiveDeltaEnable=${adaptiveDeltaEnable.enabled}, " +
                "maximizedExitWaterTempEnable=${maximizedExitWaterTempEnable.enabled}, " +
                "dcwbEnable=${dcwbEnable.enabled}, " +
                "chilledWaterTargetDelta=${chilledWaterTargetDelta.currentVal}, " +
                "chilledWaterExitTemperatureMargin=${chilledWaterExitTemperatureMargin.currentVal}, " +
                "chilledWaterExitTemperatureTarget=${chilledWaterExitTemperatureTarget.currentVal}, " +
                "chilledWaterMaxFlowRate=${chilledWaterMaxFlowRate.currentVal}, "+
                "analog1ValveClosedPosition=${analog1ValveClosedPosition.currentVal}, " +
                "analog1ValveFullPosition=${analog1ValveFullPosition.currentVal} "
    }

}