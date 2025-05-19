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

open class DabModulatingRtuProfileConfig(val model: SeventyFiveFProfileDirective) :
    ProfileConfiguration(99, "", 0, "SYSTEM", "SYSTEM", model.domainName) {

    lateinit var analog1OutputEnable: EnableConfig
    lateinit var analog2OutputEnable: EnableConfig
    lateinit var analog3OutputEnable: EnableConfig
    lateinit var analog4OutputEnable: EnableConfig
    lateinit var relay3OutputEnable: EnableConfig
    lateinit var relay7OutputEnable: EnableConfig
    lateinit var adaptiveDeltaEnable: EnableConfig
    lateinit var maximizedExitWaterTempEnable: EnableConfig
    lateinit var dcwbEnable: EnableConfig
    lateinit var relay7Association: AssociationConfig
    lateinit var analog4Association: AssociationConfig


    lateinit var analogOut1CoolingMin : ValueConfig
    lateinit var analogOut1CoolingMax : ValueConfig
    lateinit var analogOut2StaticPressureMin : ValueConfig
    lateinit var analogOut2StaticPressureMax : ValueConfig
    lateinit var  analogOut3HeatingMin : ValueConfig
    lateinit var analogOut3HeatingMax : ValueConfig
    lateinit var analogOut4FreshAirMin : ValueConfig
    lateinit var analogOut4FreshAirMax : ValueConfig
    lateinit var unusedPorts: HashMap<String, Boolean>
    lateinit var chilledWaterTargetDelta: ValueConfig
    lateinit var chilledWaterExitTemperatureMargin: ValueConfig
    lateinit var chilledWaterExitTemperatureTarget: ValueConfig
    lateinit var chilledWaterMaxFlowRate: ValueConfig
    lateinit var analog1ValveClosedPosition: ValueConfig
    lateinit var analog1ValveFullPosition: ValueConfig
    lateinit var analog2MinFan: ValueConfig
    lateinit var analog2MaxFan: ValueConfig
    lateinit var analog3MinHeating: ValueConfig
    lateinit var analog3MaxHeating: ValueConfig
    lateinit var analogOut4MinCoolingLoop: ValueConfig
    lateinit var analogOut4MaxCoolingLoop: ValueConfig


    open fun getDefaultConfiguration(): DabModulatingRtuProfileConfig {

        analog1OutputEnable = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analog2OutputEnable = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analog3OutputEnable = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)
        analog4OutputEnable = getDefaultEnableConfig(DomainName.analog4OutputEnable, model)
        relay3OutputEnable = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay7OutputEnable = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)
        adaptiveDeltaEnable = getDefaultEnableConfig(DomainName.adaptiveDeltaEnable, model)
        maximizedExitWaterTempEnable = getDefaultEnableConfig(DomainName.maximizedExitWaterTempEnable, model)
        dcwbEnable = getDefaultEnableConfig(DomainName.dcwbEnable, model)

        analog4Association = getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model)
        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)

        analogOut1CoolingMin = getDefaultValConfig(DomainName.analog1MinCooling, model)
        analogOut1CoolingMax = getDefaultValConfig(DomainName.analog1MaxCooling, model)
        analogOut2StaticPressureMin = getDefaultValConfig(DomainName.analog2MinStaticPressure, model)
        analogOut2StaticPressureMax = getDefaultValConfig(DomainName.analog2MaxStaticPressure, model)
        analogOut3HeatingMin = getDefaultValConfig(DomainName.analog3MinHeating, model)
        analogOut3HeatingMax = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        analogOut4FreshAirMin = getDefaultValConfig(DomainName.analog4MinOutsideDamper, model)
        analogOut4FreshAirMax = getDefaultValConfig(DomainName.analog4MaxOutsideDamper, model)
        chilledWaterTargetDelta = getDefaultValConfig(DomainName.chilledWaterTargetDelta, model)
        chilledWaterExitTemperatureMargin = getDefaultValConfig(DomainName.chilledWaterExitTemperatureMargin, model)
        chilledWaterExitTemperatureTarget = getDefaultValConfig(DomainName.chilledWaterExitTemperatureTarget, model)
        chilledWaterMaxFlowRate = getDefaultValConfig(DomainName.chilledWaterMaxFlowRate, model)
        analog1ValveClosedPosition = getDefaultValConfig(DomainName.analog1ValveClosedPosition, model)
        analog1ValveFullPosition = getDefaultValConfig(DomainName.analog1ValveFullPosition, model)
        analog2MinFan = getDefaultValConfig(DomainName.analog2MinFan, model)
        analog2MaxFan = getDefaultValConfig(DomainName.analog2MaxFan, model)
        analog3MinHeating = getDefaultValConfig(DomainName.analog3MinHeating, model)
        analog3MaxHeating = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        analogOut4MinCoolingLoop = getDefaultValConfig(DomainName.analogOut4MinCoolingLoop, model)
        analogOut4MaxCoolingLoop = getDefaultValConfig(DomainName.analogOut4MaxCoolingLoop, model)

        unusedPorts = getAllUnusedPorts()
        isDefault = true
        return this
    }

    open fun getActiveConfiguration(): DabModulatingRtuProfileConfig {

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

        analog4Association.associationVal = if (analog4OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.analog4OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.analog4OutputAssociation, model).associationVal
        }

        relay7Association.associationVal = if (relay7OutputEnable.enabled) {
            dabModulatingRtuSystemEquip.relay7OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model).associationVal
        }

        if(dabModulatingRtuSystemEquip.analog1OutputEnable.readDefaultVal() > 0 && !dcwbEnable.enabled) {
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

        if(dabModulatingRtuSystemEquip.analog4OutputEnable.readDefaultVal() > 0 && dcwbEnable.enabled
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

        unusedPorts = getCMUnusedPorts(Domain.hayStack)
        isDefault = false
        return this
    }


    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
            add(analog4Association)
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
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(analog1OutputEnable)
            add(analog2OutputEnable)
            add(analog3OutputEnable)
            add(analog4OutputEnable)
            add(relay3OutputEnable)
            add(relay7OutputEnable)
            add(adaptiveDeltaEnable)
            add(maximizedExitWaterTempEnable)
            add(dcwbEnable)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(chilledWaterTargetDelta)
            add(chilledWaterExitTemperatureMargin)
            add(chilledWaterMaxFlowRate)
            add(analog1ValveClosedPosition)
            add(analog1ValveFullPosition)
            add(analog2MinFan)
            add(analog2MaxFan)
            add(analog3MinHeating)
            add(analog3MaxHeating)
            add(analogOut4MinCoolingLoop)
            add(analogOut4MaxCoolingLoop)
            add(analogOut1CoolingMin)
            add(analogOut1CoolingMax)
            add(analogOut2StaticPressureMin)
            add(analogOut2StaticPressureMax)
            add(analogOut3HeatingMin)
            add(analogOut3HeatingMax)
            add(analogOut4FreshAirMin)
            add(analogOut4FreshAirMax)
        }
    }

    override fun toString(): String {
        return "analog1OutputEnable ${analog1OutputEnable.enabled} analog2OutputEnable ${analog2OutputEnable.enabled} analog3OutputEnable ${analog3OutputEnable.enabled}" +
                " analog4OutputEnable ${analog4OutputEnable.enabled} relay3OutputEnable ${relay3OutputEnable.enabled} relay7OutputEnable ${relay7OutputEnable.enabled}" +
                " relay7Association ${relay7Association.associationVal}" + " adaptiveDeltaEnable ${adaptiveDeltaEnable.enabled}" +
                " maximizedExitWaterTempEnable ${maximizedExitWaterTempEnable.enabled}" + " dcwbEnable ${dcwbEnable.enabled}"
    }

}