package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.VavModulatingRtuSystemEquip
import a75f.io.domain.util.CommonQueries
import a75f.io.logic.bo.building.hvac.ModulatingProfileAnalogMapping
import a75f.io.logic.bo.haystack.device.ControlMote.getAllUnusedPorts
import a75f.io.logic.bo.haystack.device.ControlMote.getCMUnusedPorts
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class ModulatingRtuProfileConfig(open val model: SeventyFiveFProfileDirective) :
    ProfileConfiguration(99, "", 0, "SYSTEM", "SYSTEM", model.domainName) {

    lateinit var analog1OutputEnable: EnableConfig
    lateinit var analog2OutputEnable: EnableConfig
    lateinit var analog3OutputEnable: EnableConfig
    lateinit var analog4OutputEnable: EnableConfig
    lateinit var relay3OutputEnable: EnableConfig
    lateinit var relay7OutputEnable: EnableConfig

    lateinit var analog1OutputAssociation: AssociationConfig
    lateinit var analog2OutputAssociation: AssociationConfig
    lateinit var analog3OutputAssociation: AssociationConfig
    lateinit var analog4OutputAssociation: AssociationConfig
    lateinit var relay3Association: AssociationConfig
    lateinit var relay7Association: AssociationConfig

    lateinit var thermistor1Enabled : EnableConfig
    lateinit var thermistor2Enabled : EnableConfig
    lateinit var analogIn1Enabled : EnableConfig
    lateinit var analogIn2Enabled : EnableConfig
    lateinit var thermistor1InAssociation: AssociationConfig
    lateinit var thermistor2InAssociation: AssociationConfig
    lateinit var analogIn1Association: AssociationConfig
    lateinit var analogIn2Association: AssociationConfig

    lateinit var unusedPorts: HashMap<String, Boolean>

    var analog1OutMinMaxConfig = ModulatingRtuAnalogOutMinMaxConfig()
    var analog2OutMinMaxConfig = ModulatingRtuAnalogOutMinMaxConfig()
    var analog3OutMinMaxConfig = ModulatingRtuAnalogOutMinMaxConfig()
    var analog4OutMinMaxConfig = ModulatingRtuAnalogOutMinMaxConfig()


    open fun getDefaultConfiguration(): ModulatingRtuProfileConfig {

        analog1OutputEnable = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analog2OutputEnable = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analog3OutputEnable = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)
        analog4OutputEnable = getDefaultEnableConfig(DomainName.analog4OutputEnable, model)
        relay3OutputEnable = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay7OutputEnable = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)

        analog1OutputAssociation = getDefaultAssociationConfig(DomainName.analog1OutputAssociation, model)
        analog2OutputAssociation = getDefaultAssociationConfig(DomainName.analog2OutputAssociation, model)
        analog3OutputAssociation = getDefaultAssociationConfig(DomainName.analog3OutputAssociation, model)
        analog4OutputAssociation = getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model)
        relay3Association = getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model)
        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)

        thermistor1Enabled = getDefaultEnableConfig(DomainName.thermistor1InputEnable, model)
        thermistor2Enabled = getDefaultEnableConfig(DomainName.thermistor2InputEnable, model)
        analogIn1Enabled = getDefaultEnableConfig(DomainName.analog1InputEnable, model)
        analogIn2Enabled = getDefaultEnableConfig(DomainName.analog2InputEnable, model)
        thermistor1InAssociation = getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model)
        thermistor2InAssociation = getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model)
        analogIn1Association = getDefaultAssociationConfig(DomainName.analog1InputAssociation, model)
        analogIn2Association = getDefaultAssociationConfig(DomainName.analog2InputAssociation, model)

        analog1OutMinMaxConfig.apply {
            fanSignalConfig.min = getDefaultValConfig(DomainName.analog1MinFan, model).currentVal.toInt()
            fanSignalConfig.max = getDefaultValConfig(DomainName.analog1MaxFan, model).currentVal.toInt()
            compressorSpeedConfig.min = getDefaultValConfig(DomainName.analog1MinCompressorSpeed, model).currentVal.toInt()
            compressorSpeedConfig.max = getDefaultValConfig(DomainName.analog1MaxCompressorSpeed, model).currentVal.toInt()
            outsideAirDamperConfig.min = getDefaultValConfig(DomainName.analog1MinOutsideDamper, model).currentVal.toInt()
            outsideAirDamperConfig.max = getDefaultValConfig(DomainName.analog1MaxOutsideDamper, model).currentVal.toInt()
            coolingSignalConfig.min = getDefaultValConfig(DomainName.analog1MinCooling, model).currentVal.toInt()
            coolingSignalConfig.max = getDefaultValConfig(DomainName.analog1MaxCooling, model).currentVal.toInt()
            heatingSignalConfig.min = getDefaultValConfig(DomainName.analog1MinHeating, model).currentVal.toInt()
            heatingSignalConfig.max = getDefaultValConfig(DomainName.analog1MaxHeating, model).currentVal.toInt()
        }

        analog2OutMinMaxConfig.apply {
            fanSignalConfig.min = getDefaultValConfig(DomainName.analog2MinFan, model).currentVal.toInt()
            fanSignalConfig.max = getDefaultValConfig(DomainName.analog2MaxFan, model).currentVal.toInt()
            compressorSpeedConfig.min = getDefaultValConfig(DomainName.analog2MinCompressorSpeed, model).currentVal.toInt()
            compressorSpeedConfig.max = getDefaultValConfig(DomainName.analog2MaxCompressorSpeed, model).currentVal.toInt()
            outsideAirDamperConfig.min = getDefaultValConfig(DomainName.analog2MinOutsideDamper, model).currentVal.toInt()
            outsideAirDamperConfig.max = getDefaultValConfig(DomainName.analog2MaxOutsideDamper, model).currentVal.toInt()
            coolingSignalConfig.min = getDefaultValConfig(DomainName.analog2MinCooling, model).currentVal.toInt()
            coolingSignalConfig.max = getDefaultValConfig(DomainName.analog2MaxCooling, model).currentVal.toInt()
            heatingSignalConfig.min = getDefaultValConfig(DomainName.analog2MinHeating, model).currentVal.toInt()
            heatingSignalConfig.max = getDefaultValConfig(DomainName.analog2MaxHeating, model).currentVal.toInt()
        }

        analog3OutMinMaxConfig.apply {
            fanSignalConfig.min = getDefaultValConfig(DomainName.analog3MinFan, model).currentVal.toInt()
            fanSignalConfig.max = getDefaultValConfig(DomainName.analog3MaxFan, model).currentVal.toInt()
            compressorSpeedConfig.min = getDefaultValConfig(DomainName.analog3MinCompressorSpeed, model).currentVal.toInt()
            compressorSpeedConfig.max = getDefaultValConfig(DomainName.analog3MaxCompressorSpeed, model).currentVal.toInt()
            outsideAirDamperConfig.min = getDefaultValConfig(DomainName.analog3MinOutsideDamper, model).currentVal.toInt()
            outsideAirDamperConfig.max = getDefaultValConfig(DomainName.analog3MaxOutsideDamper, model).currentVal.toInt()
            coolingSignalConfig.min = getDefaultValConfig(DomainName.analog3MinCooling, model).currentVal.toInt()
            coolingSignalConfig.max = getDefaultValConfig(DomainName.analog3MaxCooling, model).currentVal.toInt()
            heatingSignalConfig.min = getDefaultValConfig(DomainName.analog3MinHeating, model).currentVal.toInt()
            heatingSignalConfig.max = getDefaultValConfig(DomainName.analog3MaxHeating, model).currentVal.toInt()
        }

        analog4OutMinMaxConfig.apply {
            fanSignalConfig.min = getDefaultValConfig(DomainName.analog4MinFan, model).currentVal.toInt()
            fanSignalConfig.max = getDefaultValConfig(DomainName.analog4MaxFan, model).currentVal.toInt()
            compressorSpeedConfig.min = getDefaultValConfig(DomainName.analog4MinCompressorSpeed, model).currentVal.toInt()
            compressorSpeedConfig.max = getDefaultValConfig(DomainName.analog4MaxCompressorSpeed, model).currentVal.toInt()
            outsideAirDamperConfig.min = getDefaultValConfig(DomainName.analog4MinOutsideDamper, model).currentVal.toInt()
            outsideAirDamperConfig.max = getDefaultValConfig(DomainName.analog4MaxOutsideDamper, model).currentVal.toInt()
            coolingSignalConfig.min = getDefaultValConfig(DomainName.analog4MinCooling, model).currentVal.toInt()
            coolingSignalConfig.max = getDefaultValConfig(DomainName.analog4MaxCooling, model).currentVal.toInt()
            heatingSignalConfig.min = getDefaultValConfig(DomainName.analog4MinHeating, model).currentVal.toInt()
            heatingSignalConfig.max = getDefaultValConfig(DomainName.analog4MaxHeating, model).currentVal.toInt()
        }

        unusedPorts = getAllUnusedPorts()
        isDefault = true
        return this
    }

    open fun getActiveConfiguration(): ModulatingRtuProfileConfig {

        val equip = Domain.hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (equip.isEmpty()) {
            return this
        }
        val vavModulatingRtuSystemEquip = VavModulatingRtuSystemEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        analog1OutputEnable.enabled =
            vavModulatingRtuSystemEquip.analog1OutputEnable.readDefaultVal() > 0
        analog2OutputEnable.enabled =
            vavModulatingRtuSystemEquip.analog2OutputEnable.readDefaultVal() > 0
        analog3OutputEnable.enabled =
            vavModulatingRtuSystemEquip.analog3OutputEnable.readDefaultVal() > 0
        analog4OutputEnable.enabled =
            vavModulatingRtuSystemEquip.analog4OutputEnable.readDefaultVal() > 0
        relay3OutputEnable.enabled =
            vavModulatingRtuSystemEquip.relay3OutputEnable.readDefaultVal() > 0
        relay7OutputEnable.enabled =
            vavModulatingRtuSystemEquip.relay7OutputEnable.readDefaultVal() > 0

        thermistor1Enabled.enabled = vavModulatingRtuSystemEquip.thermistor1InputEnable.readDefaultVal() > 0
        thermistor2Enabled.enabled = vavModulatingRtuSystemEquip.thermistor2InputEnable.readDefaultVal() > 0
        analogIn1Enabled.enabled = vavModulatingRtuSystemEquip.analog1InputEnable.readDefaultVal() > 0
        analogIn2Enabled.enabled = vavModulatingRtuSystemEquip.analog2InputEnable.readDefaultVal() > 0

        relay3Association.associationVal = if (relay3OutputEnable.enabled) {
            vavModulatingRtuSystemEquip.relay3OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay3OutputAssociation, model).associationVal
        }

        relay7Association.associationVal = if (relay7OutputEnable.enabled) {
            vavModulatingRtuSystemEquip.relay7OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model).associationVal
        }

        thermistor1InAssociation.associationVal = if (thermistor1Enabled.enabled) {
            vavModulatingRtuSystemEquip.thermistor1InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.thermistor1InputAssociation, model).associationVal
        }
        thermistor2InAssociation.associationVal = if (thermistor2Enabled.enabled) {
            vavModulatingRtuSystemEquip.thermistor2InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.thermistor2InputAssociation, model).associationVal
        }
        analogIn1Association.associationVal = if (analogIn1Enabled.enabled) {
            vavModulatingRtuSystemEquip.analog1InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog1InputAssociation, model).associationVal
        }
        analogIn2Association.associationVal = if (analogIn2Enabled.enabled) {
            vavModulatingRtuSystemEquip.analog2InputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog2InputAssociation, model).associationVal
        }

        if (vavModulatingRtuSystemEquip.analog1OutputAssociation.readDefaultVal() > 0) {
            analog1OutputAssociation.associationVal =
                vavModulatingRtuSystemEquip.analog1OutputAssociation.readDefaultVal().toInt()
        } else {
            analog1OutputAssociation = getDefaultAssociationConfig(
                DomainName.analog1OutputAssociation,
                model
            )
        }
        if (vavModulatingRtuSystemEquip.analog2OutputAssociation.readDefaultVal() > 0) {
            analog2OutputAssociation.associationVal =
                vavModulatingRtuSystemEquip.analog2OutputAssociation.readDefaultVal().toInt()
        } else {
            analog2OutputAssociation = getDefaultAssociationConfig(
                DomainName.analog2OutputAssociation,
                model
            )
        }
        if (vavModulatingRtuSystemEquip.analog3OutputAssociation.readDefaultVal() > 0) {
            analog3OutputAssociation.associationVal =
                vavModulatingRtuSystemEquip.analog3OutputAssociation.readDefaultVal().toInt()
        } else {
            analog3OutputAssociation = getDefaultAssociationConfig(
                DomainName.analog3OutputAssociation,
                model
            )
        }

        if (vavModulatingRtuSystemEquip.analog4OutputAssociation.readDefaultVal() > 0) {
            analog4OutputAssociation.associationVal =
                vavModulatingRtuSystemEquip.analog4OutputAssociation.readDefaultVal().toInt()
        } else {
            analog4OutputAssociation = getDefaultAssociationConfig(
                DomainName.analog4OutputAssociation,
                model
            )
        }
        updateAnalogActiveConfig(vavModulatingRtuSystemEquip)
        unusedPorts = getCMUnusedPorts(Domain.hayStack)
        isDefault = false
        return this
    }

    private fun updateAnalogActiveConfig(vavModulatingRtuSystemEquip: VavModulatingRtuSystemEquip) {

        analog1OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MinFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MaxFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MinCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MaxCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MinOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MaxOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MinCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MaxCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MinHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog1MaxHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )
        }

        analog2OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MinFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MaxFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MinCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MaxCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MinOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MaxOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MinCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MaxCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MinHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog2MaxHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )
        }

        analog3OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MinFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MaxFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MinCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MaxCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MinOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MaxOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MinCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MaxCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MinHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog3MaxHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )
        }

        analog4OutMinMaxConfig.apply {
            fanSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MinFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.min
                )
            fanSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MaxFan,
                    vavModulatingRtuSystemEquip,
                    fanSignalConfig.max
                )

            compressorSpeedConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MinCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.min
                )
            compressorSpeedConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MaxCompressorSpeed,
                    vavModulatingRtuSystemEquip,
                    compressorSpeedConfig.max
                )

            outsideAirDamperConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MinOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.min
                )
            outsideAirDamperConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MaxOutsideDamper,
                    vavModulatingRtuSystemEquip,
                    outsideAirDamperConfig.max
                )

            coolingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MinCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.min
                )
            coolingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MaxCooling,
                    vavModulatingRtuSystemEquip,
                    coolingSignalConfig.max
                )

            heatingSignalConfig.min =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MinHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.min
                )
            heatingSignalConfig.max =
                getDefault(
                    vavModulatingRtuSystemEquip.analog4MaxHeating,
                    vavModulatingRtuSystemEquip,
                    heatingSignalConfig.max
                )
        }
    }



    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(analog1OutputAssociation)
            add(analog2OutputAssociation)
            add(analog3OutputAssociation)
            add(analog4OutputAssociation)
            add(relay3Association)
            add(relay7Association)
            add(thermistor1InAssociation)
            add(thermistor2InAssociation)
            add(analogIn1Association)
            add(analogIn2Association)
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            /*add(analogOut1CoolingMin)
            add(analogOut1CoolingMax)
            add(analogOut2StaticPressureMin)
            add(analogOut2StaticPressureMax)
            add(analogOut3HeatingMin)
            add(analogOut3HeatingMax)
            add(analogOut4FreshAirMin)
            add(analogOut4FreshAirMax)*/
        }
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(analog1OutputEnable)
            add(analog2OutputEnable)
            add(analog3OutputEnable)
            add(analog4OutputEnable)
            add(relay3OutputEnable)
            add(relay7OutputEnable)
            add(thermistor1Enabled)
            add(thermistor2Enabled)
            add(analogIn1Enabled)
            add(analogIn2Enabled)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            addAll(getAnalogOut1Values())
            addAll(getAnalogOut2Values())
            addAll(getAnalogOut3Values())
            addAll(getAnalogOut4Values())
        }
    }

    private fun getAnalogOut1Values() : List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            if (analog1OutputEnable.enabled) {
                if (analog1OutputAssociation.associationVal == 0) {
                    add(ValueConfig(DomainName.analog1MinFan, analog1OutMinMaxConfig.fanSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog1MaxFan, analog1OutMinMaxConfig.fanSignalConfig.max.toDouble()))
                } else if (analog1OutputAssociation.associationVal == 1) {
                    add(ValueConfig(DomainName.analog1MinCompressorSpeed, analog1OutMinMaxConfig.compressorSpeedConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog1MaxCompressorSpeed, analog1OutMinMaxConfig.compressorSpeedConfig.max.toDouble()))
                } else if (analog1OutputAssociation.associationVal == 2) {
                    add(ValueConfig(DomainName.analog1MinOutsideDamper, analog1OutMinMaxConfig.outsideAirDamperConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog1MaxOutsideDamper, analog1OutMinMaxConfig.outsideAirDamperConfig.max.toDouble()))
                } else if (analog1OutputAssociation.associationVal == 3) {
                    add(ValueConfig(DomainName.analog1MinCooling, analog1OutMinMaxConfig.coolingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog1MaxCooling, analog1OutMinMaxConfig.coolingSignalConfig.max.toDouble()))
                } else if (analog1OutputAssociation.associationVal == 4) {
                    add(ValueConfig( DomainName.analog1MinHeating, analog1OutMinMaxConfig.heatingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog1MaxHeating, analog1OutMinMaxConfig.heatingSignalConfig.max.toDouble()))
                } else if (analog1OutputAssociation.associationVal == 5) {
                    add(ValueConfig(DomainName.analog1MinCHWValve, analog1OutMinMaxConfig.chilledWaterValveConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog1MaxCHWValve, analog1OutMinMaxConfig.chilledWaterValveConfig.max.toDouble()))
                }
            }
        }
    }

    private fun getAnalogOut2Values() : List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            if (analog2OutputEnable.enabled) {
                if (analog2OutputAssociation.associationVal == 0) {
                    add(ValueConfig(DomainName.analog2MinFan, analog2OutMinMaxConfig.fanSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog2MaxFan, analog2OutMinMaxConfig.fanSignalConfig.max.toDouble()))
                } else if (analog2OutputAssociation.associationVal == 1) {
                    add(ValueConfig(DomainName.analog2MinCompressorSpeed, analog2OutMinMaxConfig.compressorSpeedConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog2MaxCompressorSpeed, analog2OutMinMaxConfig.compressorSpeedConfig.max.toDouble()))
                } else if (analog2OutputAssociation.associationVal == 2) {
                    add(ValueConfig(DomainName.analog2MinOutsideDamper, analog2OutMinMaxConfig.outsideAirDamperConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog2MaxOutsideDamper, analog2OutMinMaxConfig.outsideAirDamperConfig.max.toDouble()))
                } else if (analog2OutputAssociation.associationVal == 3) {
                    add(ValueConfig(DomainName.analog2MinCooling, analog2OutMinMaxConfig.coolingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog2MaxCooling, analog2OutMinMaxConfig.coolingSignalConfig.max.toDouble()))
                } else if (analog2OutputAssociation.associationVal == 4) {
                    add(ValueConfig( DomainName.analog2MinHeating, analog2OutMinMaxConfig.heatingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog2MaxHeating, analog2OutMinMaxConfig.heatingSignalConfig.max.toDouble()))
                } else if (analog2OutputAssociation.associationVal == 5) {
                    add(ValueConfig(DomainName.analog2MinCHWValve, analog2OutMinMaxConfig.chilledWaterValveConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog2MaxCHWValve, analog2OutMinMaxConfig.chilledWaterValveConfig.max.toDouble()))
                }
            }
        }
    }

    private fun getAnalogOut3Values() : List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            if (analog3OutputEnable.enabled) {
                if (analog3OutputAssociation.associationVal == 0) {
                    add(ValueConfig(DomainName.analog3MinFan, analog3OutMinMaxConfig.fanSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog3MaxFan, analog3OutMinMaxConfig.fanSignalConfig.max.toDouble()))
                } else if (analog3OutputAssociation.associationVal == 1) {
                    add(ValueConfig(DomainName.analog3MinCompressorSpeed, analog3OutMinMaxConfig.compressorSpeedConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog3MaxCompressorSpeed, analog3OutMinMaxConfig.compressorSpeedConfig.max.toDouble()))
                } else if (analog3OutputAssociation.associationVal == 2) {
                    add(ValueConfig(DomainName.analog3MinOutsideDamper, analog3OutMinMaxConfig.outsideAirDamperConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog3MaxOutsideDamper, analog3OutMinMaxConfig.outsideAirDamperConfig.max.toDouble()))
                } else if (analog3OutputAssociation.associationVal == 3) {
                    add(ValueConfig(DomainName.analog3MinCooling, analog3OutMinMaxConfig.coolingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog3MaxCooling, analog3OutMinMaxConfig.coolingSignalConfig.max.toDouble()))
                } else if (analog3OutputAssociation.associationVal == 4) {
                    add(ValueConfig(DomainName.analog3MinHeating, analog3OutMinMaxConfig.heatingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog3MaxHeating, analog3OutMinMaxConfig.heatingSignalConfig.max.toDouble()))
                } else if (analog3OutputAssociation.associationVal == 5) {
                    add(ValueConfig(DomainName.analog3MinCHWValve, analog3OutMinMaxConfig.chilledWaterValveConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog3MaxCHWValve, analog3OutMinMaxConfig.chilledWaterValveConfig.max.toDouble()))
                }
            }
        }
    }

    private fun getAnalogOut4Values() : List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            if (analog4OutputEnable.enabled) {
                if (analog4OutputAssociation.associationVal == 0) {
                    add(ValueConfig(DomainName.analog4MinFan, analog4OutMinMaxConfig.fanSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog4MaxFan, analog4OutMinMaxConfig.fanSignalConfig.max.toDouble()))
                } else if (analog4OutputAssociation.associationVal == 1) {
                    add(ValueConfig(DomainName.analog4MinCompressorSpeed, analog4OutMinMaxConfig.compressorSpeedConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog4MaxCompressorSpeed, analog4OutMinMaxConfig.compressorSpeedConfig.max.toDouble()))
                } else if (analog4OutputAssociation.associationVal == 2) {
                    add(ValueConfig(DomainName.analog4MinOutsideDamper, analog4OutMinMaxConfig.outsideAirDamperConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog4MaxOutsideDamper, analog4OutMinMaxConfig.outsideAirDamperConfig.max.toDouble()))
                } else if (analog4OutputAssociation.associationVal == 3) {
                    add(ValueConfig(DomainName.analog4MinCooling, analog4OutMinMaxConfig.coolingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog4MaxCooling, analog4OutMinMaxConfig.coolingSignalConfig.max.toDouble()))
                } else if (analog4OutputAssociation.associationVal == 4) {
                    add(ValueConfig( DomainName.analog4MinHeating, analog4OutMinMaxConfig.heatingSignalConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog4MaxHeating, analog4OutMinMaxConfig.heatingSignalConfig.max.toDouble()))
                } else if (analog4OutputAssociation.associationVal == 5) {
                    add(ValueConfig(DomainName.analog4MinCHWValve, analog4OutMinMaxConfig.chilledWaterValveConfig.min.toDouble()))
                    add(ValueConfig(DomainName.analog4MaxCHWValve, analog4OutMinMaxConfig.chilledWaterValveConfig.max.toDouble()))
                }
            }
        }
    }

    fun isAnyRelayEnabledAndMapped(mapping: Int): Boolean {
        return (relay3OutputEnable.enabled && relay3Association.associationVal == mapping) ||
                (relay7OutputEnable.enabled && relay7Association.associationVal == mapping)
    }
    fun isAnyAnalogMapped(mapping: Int) : Boolean{
        return (analog1OutputEnable.enabled && analog1OutputAssociation.associationVal == mapping) ||
                (analog2OutputEnable.enabled && analog2OutputAssociation.associationVal == mapping) ||
                (analog3OutputEnable.enabled && analog3OutputAssociation.associationVal == mapping) ||
                (analog4OutputEnable.enabled && analog4OutputAssociation.associationVal == mapping)
    }

    fun isAnyAnalogMappedToCompressor() : Boolean {
        return (analog1OutputEnable.enabled && analog1OutputAssociation.associationVal
                                == ModulatingProfileAnalogMapping.CompressorSpeed.ordinal)
                || (analog2OutputEnable.enabled && analog2OutputAssociation.associationVal
                                == ModulatingProfileAnalogMapping.CompressorSpeed.ordinal)
                || (analog3OutputEnable.enabled && analog3OutputAssociation.associationVal
                                == ModulatingProfileAnalogMapping.CompressorSpeed.ordinal)
                || (analog4OutputEnable.enabled && analog4OutputAssociation.associationVal
                                == ModulatingProfileAnalogMapping.CompressorSpeed.ordinal)
    }
    fun getDefault(point: Point, equip: VavModulatingRtuSystemEquip, valueConfig: Int): Int {
        return if (Domain.readPointForEquip(point.domainName, equip.equipRef).isEmpty())
            valueConfig
        else
            point.readDefaultVal().toInt()
    }

    override fun toString(): String {
        return "analog1OutputEnable ${analog1OutputEnable.enabled} analog2OutputEnable ${analog2OutputEnable.enabled} analog3OutputEnable ${analog3OutputEnable.enabled}" +
                " analog4OutputEnable ${analog4OutputEnable.enabled} relay3OutputEnable ${relay3OutputEnable.enabled} relay7OutputEnable ${relay7OutputEnable.enabled}" +
                " analog1OutputAssociation ${analog1OutputAssociation.associationVal} analog2OutputAssociation ${analog2OutputAssociation.associationVal}" +
                " analog3OutputAssociation ${analog3OutputAssociation.associationVal} analog4OutputAssociation ${analog4OutputAssociation.associationVal}" +
                " relay3Association ${relay3Association.associationVal} relay7Association ${relay7Association.associationVal}" +
                " thermistor1Enabled ${thermistor1Enabled.enabled} thermistor2Enabled ${thermistor2Enabled.enabled}" +
                " analog1OutMinMaxConfig $analog1OutMinMaxConfig" +
                " analog2OutMinMaxConfig $analog2OutMinMaxConfig" +
                " analog3OutMinMaxConfig $analog3OutMinMaxConfig" +
                " analog4OutMinMaxConfig $analog4OutMinMaxConfig"
    }

}