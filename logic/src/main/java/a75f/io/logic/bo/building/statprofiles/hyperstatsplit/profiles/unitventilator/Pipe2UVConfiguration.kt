package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.unitVentilator.Pipe2UVEquip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.Collections

/**
 * Author: Manjunath Kundaragi
 * Created on: 07-08-2025
 */
class Pipe2UVConfiguration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String,
    profileType: ProfileType, model: SeventyFiveFProfileDirective
) : UnitVentilatorConfiguration(
    nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model
)  {
    lateinit var analogOut1Voltage: AnalogOutVoltage
    lateinit var analogOut2Voltage: AnalogOutVoltage
    lateinit var analogOut3Voltage: AnalogOutVoltage
    lateinit var analogOut4Voltage: AnalogOutVoltage

    override fun getActiveConfiguration(): UnitVentilatorConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val pipe2Equip = Pipe2UVEquip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        getActiveEnableConfigs(pipe2Equip)
        getActiveAssociationConfigs(pipe2Equip)
        getGenericZoneConfigs(pipe2Equip)
        getAnalogOutConfigs(pipe2Equip)
        getUvBasedConfig(pipe2Equip)
        equipId = pipe2Equip.equipRef
        isDefault = false
        return this
    }

    override fun getDefaultConfiguration(): UnitVentilatorConfiguration {
        val config = super.getDefaultConfiguration() as Pipe2UVConfiguration
        return  config.apply {
            getAnalogOut1Voltage()
            getAnalogOut2Voltage()
            getAnalogOut3Voltage()
            getAnalogOut4Voltage()
            controlVia = getDefaultValConfig(DomainName.controlVia, model)
            saTempering = getDefaultEnableConfig(DomainName.enableSaTemperingControl, model)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return super.getValueConfigs().toMutableList().apply {
            Collections.addAll(addValueConfig(analogOut1Voltage,this))
            Collections.addAll(addValueConfig(analogOut2Voltage,this))
            Collections.addAll(addValueConfig(analogOut3Voltage,this))
            Collections.addAll(addValueConfig(analogOut4Voltage,this))
        }
    }

    private fun addValueConfig(analogOutVoltage: AnalogOutVoltage, list:MutableList<ValueConfig>): MutableList<ValueConfig> {
        return list.apply {
            add(analogOutVoltage.fanMin)
            add(analogOutVoltage.fanMax)
            add(analogOutVoltage.fanAtLow)
            add(analogOutVoltage.fanAtMedium)
            add(analogOutVoltage.fanAtHigh)
            add(analogOutVoltage.oaoDamperMinVoltage)
            add(analogOutVoltage.oaoDamperMaxVoltage)
            add(analogOutVoltage.dcvModulationMinVoltage)
            add(analogOutVoltage.dcvModulationMaxVoltage)
            add(analogOutVoltage.waterValveMin)
            add(analogOutVoltage.waterValveMax)
            add(analogOutVoltage.faceAndBypassDamperMin)
            add(analogOutVoltage.faceAndBypassDamperMax)
        }
    }

    private fun getAnalogOutConfigs(equip:Pipe2UVEquip){

        analogOut1Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog1AtMinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog1AtMaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog1AtMinDcvModulation, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog1AtMaxDcvModulation, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog1MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog1MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog1MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog1MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            fanAtLow.currentVal = getDefault(equip.analog1FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog1FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog1FanHigh, equip, fanAtHigh)

            waterValveMin.currentVal = getDefault(equip.analog1MinWaterValve, equip, waterValveMin)
            waterValveMax.currentVal = getDefault(equip.analog1MaxWaterValve, equip, waterValveMax)
        }

        analogOut2Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog2AtMinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog2AtMaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog2AtMinDcvModulation, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog2AtMaxDcvModulation, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog2MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog2MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog2MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog2MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            fanAtLow.currentVal = getDefault(equip.analog2FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog2FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog2FanHigh, equip, fanAtHigh)

            waterValveMin.currentVal = getDefault(equip.analog2MinWaterValve, equip, waterValveMin)
            waterValveMax.currentVal = getDefault(equip.analog2MaxWaterValve, equip, waterValveMax)
        }

        analogOut3Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog3AtMinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog3AtMaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog3AtMinDcvModulation, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog3AtMaxDcvModulation, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog3MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog3MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog3MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog3MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            fanAtLow.currentVal = getDefault(equip.analog3FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog3FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog3FanHigh, equip, fanAtHigh)

            waterValveMin.currentVal = getDefault(equip.analog3MinWaterValve, equip, waterValveMin)
            waterValveMax.currentVal = getDefault(equip.analog3MaxWaterValve, equip, waterValveMax)
        }

        analogOut4Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog4AtMinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog4AtMaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog4AtMinDcvModulation, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog4AtMaxDcvModulation, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog4MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog4MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog4MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog4MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            fanAtLow.currentVal = getDefault(equip.analog4FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog4FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog4FanHigh, equip, fanAtHigh)

            waterValveMin.currentVal = getDefault(equip.analog4MinWaterValve, equip, waterValveMin)
            waterValveMax.currentVal = getDefault(equip.analog4MaxWaterValve, equip, waterValveMax)
        }


        outsideDamperMinOpenDuringRecirc.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringRecirculation,
            equip,
            outsideDamperMinOpenDuringRecirc
        )
        outsideDamperMinOpenDuringConditioning.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringConditioning,
            equip,
            outsideDamperMinOpenDuringConditioning
        )
        outsideDamperMinOpenDuringFanLow.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringFanLow,
            equip,
            outsideDamperMinOpenDuringFanLow
        )
        outsideDamperMinOpenDuringFanMedium.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringFanMedium,
            equip,
            outsideDamperMinOpenDuringFanMedium
        )
        outsideDamperMinOpenDuringFanHigh.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringFanHigh,
            equip,
            outsideDamperMinOpenDuringFanHigh
        )
        zoneCO2DamperOpeningRate.currentVal =
            getDefault(equip.co2DamperOpeningRate, equip, zoneCO2DamperOpeningRate)


    }

    fun isAnyRelayMappedAndEnabled(pipe2UVRelayControls: Pipe2UVRelayControls): Boolean {
        return getRelayEnabledAssociations().any { (enabled, association) ->
            enabled && association == pipe2UVRelayControls.ordinal
        }
    }

    fun isAnyAnalogMappedAndEnabled(pipe2UvAnalogOutControls: Pipe2UvAnalogOutControls): Boolean {
        return getAnalogEnabledAssociations().any { (enabled, association) ->
            enabled && association == pipe2UvAnalogOutControls.ordinal
        }
    }

    private  fun getAnalogOut1Voltage() {
         analogOut1Voltage= AnalogOutVoltage (
             oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog1MinOAODamper, model),
             oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog1MaxOAODamper, model),
             fanMin = getDefaultValConfig(DomainName.analog1MinFanSpeed, model),
             fanMax = getDefaultValConfig(DomainName.analog1MaxFanSpeed, model),
             fanAtLow = getDefaultValConfig(DomainName.analog1FanLow, model),
             fanAtMedium = getDefaultValConfig(DomainName.analog1FanMedium, model),
             fanAtHigh = getDefaultValConfig(DomainName.analog1FanHigh, model),
             dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog1MinDCVDamper, model),
             dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog1MaxDCVDamper, model),
             waterValveMin = getDefaultValConfig(DomainName.analog1MinWaterValve, model),
             waterValveMax = getDefaultValConfig(DomainName.analog1MaxWaterValve, model),
             faceAndBypassDamperMin =
                 getDefaultValConfig(DomainName.analog1MinFaceBypassDamper, model),
             faceAndBypassDamperMax =
                 getDefaultValConfig(DomainName.analog1MaxFaceBypassDamper, model)
        )
    }

    private fun getAnalogOut2Voltage() {
        analogOut2Voltage = AnalogOutVoltage(
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog2MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog2MaxOAODamper, model),
            fanMin = getDefaultValConfig(DomainName.analog2MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog2MaxFanSpeed, model),
            fanAtLow = getDefaultValConfig(DomainName.analog2FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog2FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog2FanHigh, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog2MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog2MaxDCVDamper, model),
            waterValveMin = getDefaultValConfig(DomainName.analog2MinWaterValve, model),
            waterValveMax = getDefaultValConfig(DomainName.analog2MaxWaterValve, model),
            faceAndBypassDamperMin =
                getDefaultValConfig(DomainName.analog2MinFaceBypassDamper, model),
            faceAndBypassDamperMax =
                getDefaultValConfig(DomainName.analog2MaxFaceBypassDamper, model)
        )
    }

    private fun getAnalogOut3Voltage() {
        analogOut3Voltage = AnalogOutVoltage(
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog3MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog3MaxOAODamper, model),
            fanMin = getDefaultValConfig(DomainName.analog3MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog3MaxFanSpeed, model),
            fanAtLow = getDefaultValConfig(DomainName.analog3FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog3FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog3FanHigh, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog3MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog3MaxDCVDamper, model),
            waterValveMin = getDefaultValConfig(DomainName.analog3MinWaterValve, model),
            waterValveMax = getDefaultValConfig(DomainName.analog3MaxWaterValve, model),
            faceAndBypassDamperMin =
                getDefaultValConfig(DomainName.analog3MinFaceBypassDamper, model),
            faceAndBypassDamperMax =
                getDefaultValConfig(DomainName.analog3MaxFaceBypassDamper, model)
        )
    }

    private fun getAnalogOut4Voltage(){
        analogOut4Voltage = AnalogOutVoltage(
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog4MinOAODamper, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog4MaxOAODamper, model),
            fanMin = getDefaultValConfig(DomainName.analog4MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog4MaxFanSpeed, model),
            fanAtLow = getDefaultValConfig(DomainName.analog4FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog4FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog4FanHigh, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog4MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog4MaxDCVDamper, model),
            waterValveMin = getDefaultValConfig(DomainName.analog4MinWaterValve, model),
            waterValveMax = getDefaultValConfig(DomainName.analog4MaxWaterValve, model),
            faceAndBypassDamperMin =
                getDefaultValConfig(DomainName.analog4MinFaceBypassDamper, model),
            faceAndBypassDamperMax =
                getDefaultValConfig(DomainName.analog4MaxFaceBypassDamper, model)
        )
    }

    override fun getRelayMap(): Map<String, Boolean> {
        val relayStatus = mutableMapOf<String, Boolean>()
        relayStatus[DomainName.relay1] = isRelayExternallyMapped(relay1Enabled, relay1Association)
        relayStatus[DomainName.relay2] = isRelayExternallyMapped(relay2Enabled, relay2Association)
        relayStatus[DomainName.relay3] = isRelayExternallyMapped(relay3Enabled, relay3Association)
        relayStatus[DomainName.relay4] = isRelayExternallyMapped(relay4Enabled, relay4Association)
        relayStatus[DomainName.relay5] = isRelayExternallyMapped(relay5Enabled, relay5Association)
        relayStatus[DomainName.relay6] = isRelayExternallyMapped(relay6Enabled, relay6Association)
        relayStatus[DomainName.relay7] = isRelayExternallyMapped(relay7Enabled, relay7Association)
        relayStatus[DomainName.relay8] = isRelayExternallyMapped(relay8Enabled, relay8Association)
        return relayStatus
    }

    private fun isRelayExternallyMapped(
        relay1Enabled: EnableConfig,
        relay1Association: AssociationConfig
    ): Boolean {
        return relay1Enabled.enabled && relay1Association.associationVal == Pipe2UVRelayControls.EXTERNALLY_MAPPED.ordinal
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogStatus = mutableMapOf<String, Pair<Boolean, String>>()
        analogStatus[DomainName.analog1Out] = Pair(
            isAnalogExternallyMapped(analogOut1Enabled, analogOut1Association),
            analogType(analogOut1Enabled)
        )
        analogStatus[DomainName.analog2Out] = Pair(
            isAnalogExternallyMapped(analogOut2Enabled, analogOut2Association),
            analogType(analogOut2Enabled)
        )
        analogStatus[DomainName.analog3Out] = Pair(
            isAnalogExternallyMapped(analogOut3Enabled, analogOut3Association),
            analogType(analogOut3Enabled)
        )
        analogStatus[DomainName.analog4Out] = Pair(
            isAnalogExternallyMapped(analogOut4Enabled, analogOut4Association),
            analogType(analogOut4Enabled)
        )
        return analogStatus
    }

    private fun isAnalogExternallyMapped(
        analog1Enabled: EnableConfig,
        analogAssociation: AssociationConfig
    ): Boolean {
        return analog1Enabled.enabled && analogAssociation.associationVal == Pipe2UvAnalogOutControls.EXTERNALLY_MAPPED.ordinal
    }

    private fun analogType(analog: EnableConfig): String {
        return when (analog) {
            analogOut1Enabled -> getPortType(analogOut1Association, analogOut1Voltage)
            analogOut2Enabled -> getPortType(analogOut2Association, analogOut2Voltage)
            analogOut3Enabled -> getPortType(analogOut3Association, analogOut3Voltage)
            analogOut4Enabled -> getPortType(analogOut4Association, analogOut4Voltage)
            else -> "2-10v"
        }
    }

    private fun getPortType(
        analogAssociation: AssociationConfig,
        analogOutVoltage: AnalogOutVoltage
    ): String {
        val porType = when (analogAssociation.associationVal) {
            Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE.ordinal -> getVoltageValueString(
                analogOutVoltage.waterValveMin,
                analogOutVoltage.waterValveMax
            )

            Pipe2UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal -> getVoltageValueString(
                analogOutVoltage.faceAndBypassDamperMin,
                analogOutVoltage.faceAndBypassDamperMax
            )

            Pipe2UvAnalogOutControls.FAN_SPEED.ordinal -> getVoltageValueString(
                analogOutVoltage.fanMin,
                analogOutVoltage.fanMax
            )

            Pipe2UvAnalogOutControls.OAO_DAMPER.ordinal -> getVoltageValueString(
                analogOutVoltage.oaoDamperMinVoltage,
                analogOutVoltage.oaoDamperMaxVoltage
            )

            Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal -> getVoltageValueString(
                analogOutVoltage.dcvModulationMinVoltage,
                analogOutVoltage.dcvModulationMaxVoltage
            )

            else -> {
                "2-10v"
            }
        }
        return porType
    }

    private fun getVoltageValueString(minVoltage: ValueConfig, maxVoltage: ValueConfig): String {
        return "${minVoltage.currentVal} - ${maxVoltage.currentVal}v"
    }

    private fun getAnalogOutVoltageRange(
        associationVal: Int,
        voltage: AnalogOutVoltage
    ): String {
        return when (associationVal) {
            Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE.ordinal -> {
                voltageRange(voltage.waterValveMin.currentVal.toInt(), voltage.waterValveMax.currentVal.toInt())
            }


            Pipe2UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal -> {
                voltageRange(voltage.faceAndBypassDamperMin.currentVal.toInt(), voltage.faceAndBypassDamperMax.currentVal.toInt())
            }

            Pipe2UvAnalogOutControls.FAN_SPEED.ordinal -> {
                voltageRange(voltage.fanMin.currentVal.toInt(), voltage.fanMax.currentVal.toInt())
            }

            Pipe2UvAnalogOutControls.OAO_DAMPER.ordinal -> {
                voltageRange(voltage.oaoDamperMinVoltage.currentVal.toInt(), voltage.oaoDamperMaxVoltage.currentVal.toInt())
            }

            Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal -> {
                voltageRange(voltage.dcvModulationMinVoltage.currentVal.toInt(), voltage.dcvModulationMaxVoltage.currentVal.toInt())
            }

            else -> "2-10v"
        }
    }
    private fun voltageRange(min: Int, max: Int) = min.toString()+"-"+max.toString() + "v"

    override fun analogOut1TypeToString(): String {
        return getAnalogOutVoltageRange(analogOut1Association.associationVal, analogOut1Voltage)
    }

    override fun analogOut2TypeToString(): String {
        return getAnalogOutVoltageRange(analogOut2Association.associationVal, analogOut2Voltage)
    }

    override fun analogOut3TypeToString(): String {
        return getAnalogOutVoltageRange(analogOut3Association.associationVal, analogOut3Voltage)
    }

    override fun analogOut4TypeToString(): String {
        return getAnalogOutVoltageRange(analogOut4Association.associationVal, analogOut4Voltage)
    }

    fun getFanConfiguration(port: Port): AnalogOutVoltage {
        return when (port) {
            Port.ANALOG_OUT_ONE -> analogOut1Voltage
            Port.ANALOG_OUT_TWO -> analogOut2Voltage
            Port.ANALOG_OUT_THREE -> analogOut3Voltage
            Port.ANALOG_OUT_FOUR -> analogOut4Voltage
            else -> analogOut1Voltage
        }
    }

    override fun getHighestFanStageCount(): Int {
        val stageOrdinal = getHighestStage(
            if (isAnyRelayMappedToFanLowVentilation()) {
                Pipe2UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal
            } else {
                Pipe2UVRelayControls.FAN_LOW_SPEED.ordinal
            },
            Pipe2UVRelayControls.FAN_MEDIUM_SPEED.ordinal,
            Pipe2UVRelayControls.FAN_HIGH_SPEED.ordinal
        )
        return when (stageOrdinal) {
            Pipe2UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal -> 1
            Pipe2UVRelayControls.FAN_LOW_SPEED.ordinal -> 1
            Pipe2UVRelayControls.FAN_MEDIUM_SPEED.ordinal -> 2
            Pipe2UVRelayControls.FAN_HIGH_SPEED.ordinal -> 3
            else -> 0
        }
    }

    private fun isAnyRelayMappedToFanLowVentilation(): Boolean {
        return isAnyRelayEnabledAndMapped(Pipe2UVRelayControls.FAN_LOW_SPEED_VENTILATION)
    }

    fun isAnyRelayEnabledAndMapped(mapping: Pipe2UVRelayControls): Boolean {
        return getRelayEnabledAssociations().any { (enabled, type) -> enabled && type == mapping.ordinal }
    }

    fun getLowestFanSelected(): Int {
        val lowestSelected = if (isAnyRelayMappedToFanLowVentilation()) {
            getLowestStage(
                Pipe2UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal,
                Pipe2UVRelayControls.FAN_MEDIUM_SPEED.ordinal,
                Pipe2UVRelayControls.FAN_HIGH_SPEED.ordinal
            )
        } else {
            getLowestStage(
                Pipe2UVRelayControls.FAN_LOW_SPEED.ordinal,
                Pipe2UVRelayControls.FAN_MEDIUM_SPEED.ordinal,
                Pipe2UVRelayControls.FAN_HIGH_SPEED.ordinal,
            )
        }
        if (lowestSelected == -1) return -1
        return Pipe2UVRelayControls.values()[lowestSelected].ordinal
    }
    
    override fun toString(): String {
        return super.toString().apply {
            "satTempering=$saTempering, controlVia=$controlVia, analogOut1Voltage=$analogOut1Voltage, analogOut2Voltage=$analogOut2Voltage, analogOut3Voltage=$analogOut3Voltage, analogOut4Voltage=$analogOut4Voltage"
        }
    }
}

enum class Pipe2UvAnalogOutControls {
    WATER_MODULATING_VALVE, FACE_DAMPER_VALVE, FAN_SPEED, OAO_DAMPER, DCV_MODULATING_DAMPER, EXTERNALLY_MAPPED
}

enum class Pipe2UVRelayControls {
    FAN_LOW_SPEED_VENTILATION,
    FAN_LOW_SPEED,
    FAN_MEDIUM_SPEED,
    FAN_HIGH_SPEED,
    AUX_HEATING_STAGE1,
    AUX_HEATING_STAGE2,
    WATER_VALVE,
    FAN_ENABLED,
    OCCUPIED_ENABLED,
    FACE_BYPASS_DAMPER,
    DCV_DAMPER,
    HUMIDIFIER,
    DEHUMIDIFIER,
    EXTERNALLY_MAPPED
}

// Constants for Pipe2UVConfiguration instead of crating a new enum list for one enum value
const val SUPPLY_WATER_TEMPERATURE = 106


data class AnalogOutVoltage(
    var oaoDamperMinVoltage: ValueConfig, var oaoDamperMaxVoltage: ValueConfig,
    var fanMin: ValueConfig, var fanMax: ValueConfig,
    var fanAtLow: ValueConfig, var fanAtMedium: ValueConfig, var fanAtHigh: ValueConfig,
    var dcvModulationMinVoltage: ValueConfig, var dcvModulationMaxVoltage: ValueConfig,
    var waterValveMin : ValueConfig , var waterValveMax: ValueConfig,
    var faceAndBypassDamperMin: ValueConfig, var faceAndBypassDamperMax: ValueConfig
)


