package a75f.io.logic.bo.building.system.vav.config

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.VavModulatingRtuSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.haystack.device.ControlMote
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

open class ModulatingRtuProfileConfig(val model: SeventyFiveFProfileDirective) :
    ProfileConfiguration(99, "", 0, "SYSTEM", "SYSTEM", model.domainName) {

    lateinit var analog1OutputEnable: EnableConfig
    lateinit var analog2OutputEnable: EnableConfig
    lateinit var analog3OutputEnable: EnableConfig
    lateinit var analog4OutputEnable: EnableConfig
    lateinit var relay3OutputEnable: EnableConfig
    lateinit var relay7OutputEnable: EnableConfig
    lateinit var relay7Association: AssociationConfig


    lateinit var analogOut1CoolingMin : ValueConfig
    lateinit var analogOut1CoolingMax : ValueConfig
    lateinit var analogOut2StaticPressureMin : ValueConfig
    lateinit var analogOut2StaticPressureMax : ValueConfig
    lateinit var  analogOut3HeatingMin : ValueConfig
    lateinit var analogOut3HeatingMax : ValueConfig
    lateinit var analogOut4FreshAirMin : ValueConfig
    lateinit var analogOut4FreshAirMax : ValueConfig
    lateinit var unusedPorts: HashMap<String, Boolean>


    open fun getDefaultConfiguration(): ModulatingRtuProfileConfig {

        analog1OutputEnable = getDefaultEnableConfig(DomainName.analog1OutputEnable, model)
        analog2OutputEnable = getDefaultEnableConfig(DomainName.analog2OutputEnable, model)
        analog3OutputEnable = getDefaultEnableConfig(DomainName.analog3OutputEnable, model)
        analog4OutputEnable = getDefaultEnableConfig(DomainName.analog4OutputEnable, model)
        relay3OutputEnable = getDefaultEnableConfig(DomainName.relay3OutputEnable, model)
        relay7OutputEnable = getDefaultEnableConfig(DomainName.relay7OutputEnable, model)

        relay7Association = getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model)

        analogOut1CoolingMin = getDefaultValConfig(DomainName.analog1MinCooling, model)
        analogOut1CoolingMax = getDefaultValConfig(DomainName.analog1MaxCooling, model)
        analogOut2StaticPressureMin = getDefaultValConfig(DomainName.analog2MinStaticPressure, model)
        analogOut2StaticPressureMax = getDefaultValConfig(DomainName.analog2MaxStaticPressure, model)
        analogOut3HeatingMin = getDefaultValConfig(DomainName.analog3MinHeating, model)
        analogOut3HeatingMax = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        analogOut4FreshAirMin = getDefaultValConfig(DomainName.analog4MinOutsideDamper, model)
        analogOut4FreshAirMax = getDefaultValConfig(DomainName.analog4MaxOutsideDamper, model)
        try {
            unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)
        } catch (e : NullPointerException) {
            unusedPorts = hashMapOf()
            CcuLog.e(Domain.LOG_TAG,"Failed to fetch CM Unused ports")
            e.printStackTrace()
        }
        isDefault = true
        return this
    }

    open fun getActiveConfiguration(): ModulatingRtuProfileConfig {

        val equip = Domain.hayStack.readEntity("system and equip and not modbus")
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

        relay7Association.associationVal = if (relay7OutputEnable.enabled) {
            vavModulatingRtuSystemEquip.relay7OutputAssociation.readDefaultVal().toInt()
        } else {
            getDefaultAssociationConfig(DomainName.relay7OutputAssociation, model).associationVal
        }

        if(vavModulatingRtuSystemEquip.analog1OutputEnable.readDefaultVal() > 0){
            analogOut1CoolingMin.currentVal = vavModulatingRtuSystemEquip.analog1MinCooling.readDefaultVal()
            analogOut1CoolingMax.currentVal = vavModulatingRtuSystemEquip.analog1MaxCooling.readDefaultVal()
        }else{
            analogOut1CoolingMin = getDefaultValConfig(DomainName.analog1MinCooling, model)
            analogOut1CoolingMax = getDefaultValConfig(DomainName.analog1MaxCooling, model)
        }

        if(vavModulatingRtuSystemEquip.analog2OutputEnable.readDefaultVal() > 0) {
            analogOut2StaticPressureMin.currentVal = vavModulatingRtuSystemEquip.analog2MinStaticPressure.readDefaultVal()
            analogOut2StaticPressureMax.currentVal = vavModulatingRtuSystemEquip.analog2MaxStaticPressure.readDefaultVal()
        }else{
            analogOut2StaticPressureMin = getDefaultValConfig(DomainName.analog2MinStaticPressure, model)
            analogOut2StaticPressureMax = getDefaultValConfig(DomainName.analog2MaxStaticPressure, model)
        }

        if(vavModulatingRtuSystemEquip.analog3OutputEnable.readDefaultVal() > 0) {
            analogOut3HeatingMin.currentVal = vavModulatingRtuSystemEquip.analog3MinHeating.readDefaultVal()
            analogOut3HeatingMax.currentVal = vavModulatingRtuSystemEquip.analog3MaxHeating.readDefaultVal()
        }else{
            analogOut3HeatingMin = getDefaultValConfig(DomainName.analog3MinHeating, model)
            analogOut3HeatingMax = getDefaultValConfig(DomainName.analog3MaxHeating, model)
        }

        if(vavModulatingRtuSystemEquip.analog4OutputEnable.readDefaultVal() > 0) {
            analogOut4FreshAirMin.currentVal = vavModulatingRtuSystemEquip.analog4MinOutsideDamper.readDefaultVal()
            analogOut4FreshAirMax.currentVal = vavModulatingRtuSystemEquip.analog4MaxOutsideDamper.readDefaultVal()
        }else{
            analogOut4FreshAirMin = getDefaultValConfig(DomainName.analog4MinOutsideDamper, model)
            analogOut4FreshAirMax = getDefaultValConfig(DomainName.analog4MaxOutsideDamper, model)
        }

        unusedPorts = ControlMote.getCMUnusedPorts(Domain.hayStack)

        isDefault = false
        return this
    }


    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
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
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
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

    override fun toString(): String {
        return "analog1OutputEnable ${analog1OutputEnable.enabled} analog2OutputEnable ${analog2OutputEnable.enabled} analog3OutputEnable ${analog3OutputEnable.enabled}" +
                " analog4OutputEnable ${analog4OutputEnable.enabled} relay3OutputEnable ${relay3OutputEnable.enabled} relay7OutputEnable ${relay7OutputEnable.enabled}" +
                " relay7Association ${relay7Association.associationVal}"
    }

}