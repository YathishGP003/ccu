package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.DabModulatingRtuSystemEquip
import a75f.io.domain.equips.VavModulatingRtuSystemEquip
import a75f.io.domain.util.CommonQueries
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.bo.haystack.device.ControlMote.getAllUnusedPorts
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
    /*lateinit var analog2MinFan: ValueConfig
    lateinit var analog2MaxFan: ValueConfig
    lateinit var analog3MinHeating: ValueConfig
    lateinit var analog3MaxHeating: ValueConfig
    lateinit var analogOut4MinCoolingLoop: ValueConfig
    lateinit var analogOut4MaxCoolingLoop: ValueConfig*/


    override fun getDefaultConfiguration(): DabModulatingRtuProfileConfig {
        super.getDefaultConfiguration()
        /*analog1OutputEnable = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analog2OutputEnable = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analog3OutputEnable = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)
        analog4OutputEnable = getDefaultEnableConfig(DomainName.analog4OutputEnable, model)
        relay3OutputEnable = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay7OutputEnable = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)
        adaptiveDeltaEnable = getDefaultEnableConfig(DomainName.adaptiveDeltaEnable, model)
        maximizedExitWaterTempEnable = getDefaultEnableConfig(DomainName.maximizedExitWaterTempEnable, model)
        dcwbEnable = getDefaultEnableConfig(DomainName.dcwbEnable, model)

        analog1OutputAssociation = getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model)
        analog2OutputAssociation = getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model)
        analog3OutputAssociation = getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model)
        analog4OutputAssociation = getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)

        //analog4Association = getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model)
        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)*/

        /*analogOut1CoolingMin = getDefaultValConfig(DomainName.analog1MinCooling, model)
        analogOut1CoolingMax = getDefaultValConfig(DomainName.analog1MaxCooling, model)
        analogOut2StaticPressureMin = getDefaultValConfig(DomainName.analog2MinStaticPressure, model)
        analogOut2StaticPressureMax = getDefaultValConfig(DomainName.analog2MaxStaticPressure, model)
        analogOut3HeatingMin = getDefaultValConfig(DomainName.analog3MinHeating, model)
        analogOut3HeatingMax = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        analogOut4FreshAirMin = getDefaultValConfig(DomainName.analog4MinOutsideDamper, model)
        analogOut4FreshAirMax = getDefaultValConfig(DomainName.analog4MaxOutsideDamper, model)*/
        adaptiveDeltaEnable = getDefaultEnableConfig(DomainName.adaptiveDeltaEnable, model)
        maximizedExitWaterTempEnable = getDefaultEnableConfig(DomainName.maximizedExitWaterTempEnable, model)
        dcwbEnable = getDefaultEnableConfig(DomainName.dcwbEnable, model)
        chilledWaterTargetDelta = getDefaultValConfig(DomainName.chilledWaterTargetDelta, model)
        chilledWaterExitTemperatureMargin = getDefaultValConfig(DomainName.chilledWaterExitTemperatureMargin, model)
        chilledWaterExitTemperatureTarget = getDefaultValConfig(DomainName.chilledWaterExitTemperatureTarget, model)
        chilledWaterMaxFlowRate = getDefaultValConfig(DomainName.chilledWaterMaxFlowRate, model)
        analog1ValveClosedPosition = getDefaultValConfig(DomainName.analog1ValveClosedPosition, model)
        analog1ValveFullPosition = getDefaultValConfig(DomainName.analog1ValveFullPosition, model)
        /*analog2MinFan = getDefaultValConfig(DomainName.analog2MinFan, model)
        analog2MaxFan = getDefaultValConfig(DomainName.analog2MaxFan, model)
        analog3MinHeating = getDefaultValConfig(DomainName.analog3MinHeating, model)
        analog3MaxHeating = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        analogOut4MinCoolingLoop = getDefaultValConfig(DomainName.analogOut4MinCoolingLoop, model)
        analogOut4MaxCoolingLoop = getDefaultValConfig(DomainName.analogOut4MaxCoolingLoop, model)*/

        //unusedPorts = getAllUnusedPorts()
        //isDefault = true
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

        updateAnalogActiveConfig(dabModulatingRtuSystemEquip)

        analog1ValveClosedPosition.currentVal = dabModulatingRtuSystemEquip.analog1ValveClosedPosition.readDefaultVal()
        analog1ValveFullPosition.currentVal = dabModulatingRtuSystemEquip.analog1ValveFullPosition.readDefaultVal()

        /*if(dabModulatingRtuSystemEquip.analog1OutputEnable.readDefaultVal() > 0 && !dcwbEnable.enabled) {
            analogOut1CoolingMin.currentVal = dabModulatingRtuSystemEquip.analog1MinCooling.readDefaultVal()
            analogOut1CoolingMax.currentVal = dabModulatingRtuSystemEquip.analog1MaxCooling.readDefaultVal()
        } else if(dabModulatingRtuSystemEquip.analog1OutputEnable.readDefaultVal() > 0 && dcwbEnable.enabled) {
            analogOut1CoolingMin.currentVal = dabModulatingRtuSystemEquip.analog1MinCooling.readDefaultVal()
            analogOut1CoolingMax.currentVal = dabModulatingRtuSystemEquip.analog1MaxCooling.readDefaultVal()
            analog1ValveClosedPosition.currentVal = dabModulatingRtuSystemEquip.analog1ValveClosedPosition.readDefaultVal()
            analog1ValveFullPosition.currentVal = dabModulatingRtuSystemEquip.analog1ValveFullPosition.readDefaultVal()
        } else {
            analogOut1CoolingMin = getDefaultValConfig(DomainName.analog1MinCooling, model)
            analogOut1CoolingMax = getDefaultValConfig(DomainName.analog1MaxCooling, model)
        }

        if(dabModulatingRtuSystemEquip.analog2OutputEnable.readDefaultVal() > 0) {
            analog2MinFan.currentVal = dabModulatingRtuSystemEquip.analog2MinFan.readDefaultVal()
            analog2MaxFan.currentVal = dabModulatingRtuSystemEquip.analog2MaxFan.readDefaultVal()
        } else{
            analogOut2StaticPressureMin = getDefaultValConfig(DomainName.analog2MinFan, model)
            analogOut2StaticPressureMax = getDefaultValConfig(DomainName.analog2MinFan, model)
        }

        if(dabModulatingRtuSystemEquip.analog3OutputEnable.readDefaultVal() > 0) {
            analog3MinHeating.currentVal = dabModulatingRtuSystemEquip.analog3MinHeating.readDefaultVal()
            analog3MaxHeating.currentVal = dabModulatingRtuSystemEquip.analog3MaxHeating.readDefaultVal()
        } else {
            analogOut3HeatingMin = getDefaultValConfig(DomainName.analog3MinHeating, model)
            analogOut3HeatingMax = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        }
*/
        /*if(dabModulatingRtuSystemEquip.analog4OutputEnable.readDefaultVal() > 0 && dcwbEnable.enabled
            && dabModulatingRtuSystemEquip.analog4OutputAssociation.readDefaultVal() > 0) {
            // When the analog4OutputAssociation is mapped to damper
            analogOut4FreshAirMin.currentVal = dabModulatingRtuSystemEquip.analog4MinOutsideDamper.readDefaultVal()
            analogOut4FreshAirMax.currentVal = dabModulatingRtuSystemEquip.analog4MaxOutsideDamper.readDefaultVal()
        } else if (dabModulatingRtuSystemEquip.analog4OutputEnable.readDefaultVal() > 0 && dcwbEnable.enabled) {
            // When the analog4OutputAssociation is mapped to cooling
            analogOut4MinCoolingLoop.currentVal = dabModulatingRtuSystemEquip.analogOut4MinCoolingLoop.readDefaultVal()
            analogOut4MaxCoolingLoop.currentVal = dabModulatingRtuSystemEquip.analogOut4MaxCoolingLoop.readDefaultVal()
        } else {
            analogOut4FreshAirMin = getDefaultValConfig(DomainName.analogOut4MinCoolingLoop, model)
            analogOut4FreshAirMax = getDefaultValConfig(DomainName.analogOut4MaxCoolingLoop, model)
        }
*/
        unusedPorts = getCMUnusedPorts(Domain.hayStack)
        isDefault = false
        return this
    }


    /*override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(analog1OutputAssociation)
            add(analog2OutputAssociation)
            add(analog3OutputAssociation)
            add(analog4OutputAssociation)
            add(relay3Association)
            add(relay7Association)
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(analogOut1CoolingMin)
            add(analogOut1CoolingMax)
            add(analogOut2StaticPressureMin)
            add(analogOut2StaticPressureMax)
            add(analogOut3HeatingMin)
            add(analogOut3HeatingMax)
            add(analogOut4FreshAirMin)
            add(analogOut4FreshAirMax)
        }
    }*/

    override fun getEnableConfigs(): List<EnableConfig> {
        return super.getEnableConfigs() + mutableListOf<EnableConfig>().apply {
            add(adaptiveDeltaEnable)
            add(maximizedExitWaterTempEnable)
            add(dcwbEnable)
        }
    }

    private fun updateAnalogActiveConfig(dabModulatingRtuSystemEquip : DabModulatingRtuSystemEquip) {
        analog1OutMinMaxConfig.apply {
            when (analog1OutputAssociation.associationVal) {
                0 -> {
                    fanSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog1MinFan.readDefaultVal().toInt()
                    fanSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog1MaxFan.readDefaultVal().toInt()
                }

                1 -> {
                    compressorSpeedConfig.min =
                        dabModulatingRtuSystemEquip.analog1MinCompressorSpeed.readDefaultVal()
                            .toInt()
                    compressorSpeedConfig.max =
                        dabModulatingRtuSystemEquip.analog1MaxCompressorSpeed.readDefaultVal()
                            .toInt()
                }

                2 -> {
                    outsideAirDamperConfig.min =
                        dabModulatingRtuSystemEquip.analog1MinOutsideDamper.readDefaultVal().toInt()
                    outsideAirDamperConfig.max =
                        dabModulatingRtuSystemEquip.analog1MaxOutsideDamper.readDefaultVal().toInt()
                }

                3 -> {
                    coolingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog1MinCooling.readDefaultVal().toInt()
                    coolingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog1MaxCooling.readDefaultVal().toInt()
                }

                4 -> {
                    heatingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog1MinHeating.readDefaultVal().toInt()
                    heatingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog1MaxHeating.readDefaultVal().toInt()
                }

                5 -> {
                    chilledWaterValveConfig.min =
                        dabModulatingRtuSystemEquip.analog1MinCHWValve.readDefaultVal()
                            .toInt()
                    chilledWaterValveConfig.max =
                        dabModulatingRtuSystemEquip.analog1MaxCHWValve.readDefaultVal()
                            .toInt()
                }
            }
        }

        analog2OutMinMaxConfig.apply {
            when (analog2OutputAssociation.associationVal) {
                0 -> {
                    fanSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog2MinFan.readDefaultVal().toInt()
                    fanSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog2MaxFan.readDefaultVal().toInt()
                }

                1 -> {
                    compressorSpeedConfig.min =
                        dabModulatingRtuSystemEquip.analog2MinCompressorSpeed.readDefaultVal()
                            .toInt()
                    compressorSpeedConfig.max =
                        dabModulatingRtuSystemEquip.analog2MaxCompressorSpeed.readDefaultVal()
                            .toInt()
                }

                2 -> {
                    outsideAirDamperConfig.min =
                        dabModulatingRtuSystemEquip.analog2MinOutsideDamper.readDefaultVal().toInt()
                    outsideAirDamperConfig.max =
                        dabModulatingRtuSystemEquip.analog2MaxOutsideDamper.readDefaultVal().toInt()
                }

                3 -> {
                    coolingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog2MinCooling.readDefaultVal().toInt()
                    coolingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog2MaxCooling.readDefaultVal().toInt()
                }

                4 -> {
                    heatingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog2MinHeating.readDefaultVal().toInt()
                    heatingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog2MaxHeating.readDefaultVal().toInt()
                }

                5 ->  {
                    chilledWaterValveConfig.min =
                        dabModulatingRtuSystemEquip.analog2MinCHWValve.readDefaultVal().toInt()
                    chilledWaterValveConfig.max =
                        dabModulatingRtuSystemEquip.analog2MaxCHWValve.readDefaultVal().toInt()
                }
            }
        }

        analog3OutMinMaxConfig.apply {
            when (analog3OutputAssociation.associationVal) {
                0 -> {
                    fanSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog3MinFan.readDefaultVal().toInt()
                    fanSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog3MaxFan.readDefaultVal().toInt()
                }

                1 -> {
                    compressorSpeedConfig.min =
                        dabModulatingRtuSystemEquip.analog3MinCompressorSpeed.readDefaultVal()
                            .toInt()
                    compressorSpeedConfig.max =
                        dabModulatingRtuSystemEquip.analog3MaxCompressorSpeed.readDefaultVal()
                            .toInt()
                }

                2 -> {
                    outsideAirDamperConfig.min =
                        dabModulatingRtuSystemEquip.analog3MinOutsideDamper.readDefaultVal().toInt()
                    outsideAirDamperConfig.max =
                        dabModulatingRtuSystemEquip.analog3MaxOutsideDamper.readDefaultVal().toInt()
                }

                3 -> {
                    coolingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog3MinCooling.readDefaultVal().toInt()
                    coolingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog3MaxCooling.readDefaultVal().toInt()
                }

                4 -> {
                    heatingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog3MinHeating.readDefaultVal().toInt()
                    heatingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog3MaxHeating.readDefaultVal().toInt()
                }

                5 -> {
                    chilledWaterValveConfig.min =
                        dabModulatingRtuSystemEquip.analog3MinCHWValve.readDefaultVal().toInt()
                    chilledWaterValveConfig.max =
                        dabModulatingRtuSystemEquip.analog3MaxCHWValve.readDefaultVal().toInt()
                }
            }
        }

        analog4OutMinMaxConfig.apply {
            when (analog4OutputAssociation.associationVal) {
                0 -> {
                    fanSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog4MinFan.readDefaultVal().toInt()
                    fanSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog4MaxFan.readDefaultVal().toInt()
                }

                1 -> {
                    compressorSpeedConfig.min =
                        dabModulatingRtuSystemEquip.analog4MinCompressorSpeed.readDefaultVal()
                            .toInt()
                    compressorSpeedConfig.max =
                        dabModulatingRtuSystemEquip.analog4MaxCompressorSpeed.readDefaultVal()
                            .toInt()
                }

                2 -> {
                    outsideAirDamperConfig.min =
                        dabModulatingRtuSystemEquip.analog4MinOutsideDamper.readDefaultVal().toInt()
                    outsideAirDamperConfig.max =
                        dabModulatingRtuSystemEquip.analog4MaxOutsideDamper.readDefaultVal().toInt()
                }

                3 -> {
                    coolingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog4MinCooling.readDefaultVal().toInt()
                    coolingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog4MaxCooling.readDefaultVal().toInt()
                }

                4 -> {
                    heatingSignalConfig.min =
                        dabModulatingRtuSystemEquip.analog4MinHeating.readDefaultVal().toInt()
                    heatingSignalConfig.max =
                        dabModulatingRtuSystemEquip.analog4MaxHeating.readDefaultVal().toInt()
                }

                5 -> {
                    chilledWaterValveConfig.min =
                        dabModulatingRtuSystemEquip.analog4MinCHWValve.readDefaultVal().toInt()
                    chilledWaterValveConfig.max =
                        dabModulatingRtuSystemEquip.analog4MaxCHWValve.readDefaultVal().toInt()
                }
            }
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