package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon
import a75f.io.api.haystack.*
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconEquip.Companion.getHyperStatSplitEquipRef

/**
 * Created by Manjunath K for HyperStat CPU on 26-10-2021.
 * Created by Nick P for HyperStat Split CPU/Economiser on 07-24-2023.
 */
class CpuEconReconfiguration {

    companion object {
        fun configAssociationPoint(portType: Port, updatedConfigValue: Double, equipPoint: Equip, ) {
            val equip =  getHyperStatSplitEquipRef(equipPoint.group.toShort())
            val updatedConfiguration =  updateAssociation(equip.getConfiguration(),portType,updatedConfigValue.toInt()  )
            equip.updateConfiguration(updatedConfiguration)
        }
        fun updateConfiguration(updatedConfigValue: Double, equipPoint: Equip, portType: Port){
           val equip =  getHyperStatSplitEquipRef(equipPoint.group.toShort())
           val updatedConfiguration =  updateConfig(equip.getConfiguration(),portType,updatedConfigValue)
           equip.updateConfiguration(updatedConfiguration)
        }
        private fun updateConfig(current: HyperStatSplitCpuEconConfiguration, portType: Port, status: Double): HyperStatSplitCpuEconConfiguration{
            val config = getConfigCopy(current)
            when(portType){
                Port.RELAY_ONE -> config.relay1State = config.relay1State.copy(enabled = (status == 1.0))
                Port.RELAY_TWO -> config.relay2State = config.relay2State.copy(enabled = (status == 1.0))
                Port.RELAY_THREE -> config.relay3State = config.relay3State.copy(enabled = (status == 1.0))
                Port.RELAY_FOUR -> config.relay4State = config.relay4State.copy(enabled = (status == 1.0))
                Port.RELAY_FIVE -> config.relay5State = config.relay5State.copy(enabled = (status == 1.0))
                Port.RELAY_SIX -> config.relay6State = config.relay6State.copy(enabled = (status == 1.0))
                Port.RELAY_SEVEN -> config.relay7State = config.relay7State.copy(enabled = (status == 1.0))
                Port.RELAY_EIGHT -> config.relay8State = config.relay8State.copy(enabled = (status == 1.0))
                Port.ANALOG_OUT_ONE -> config.analogOut1State = config.analogOut1State.copy(enabled = status == 1.0)
                Port.ANALOG_OUT_TWO -> config.analogOut2State = config.analogOut2State.copy(enabled = status == 1.0)
                Port.ANALOG_OUT_THREE -> config.analogOut3State = config.analogOut3State.copy(enabled = status == 1.0)
                Port.ANALOG_OUT_FOUR -> config.analogOut4State = config.analogOut4State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_ONE -> config.universalIn1State = config.universalIn1State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_TWO -> config.universalIn2State = config.universalIn2State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_THREE -> config.universalIn3State = config.universalIn3State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_FOUR -> config.universalIn4State = config.universalIn4State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_FIVE -> config.universalIn5State = config.universalIn5State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_SIX -> config.universalIn6State = config.universalIn6State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_SEVEN -> config.universalIn7State = config.universalIn7State.copy(enabled = status == 1.0)
                Port.UNIVERSAL_IN_EIGHT -> config.universalIn8State = config.universalIn8State.copy(enabled = status == 1.0)
                else -> {}

            }
            return config
        }
        private fun updateAssociation(current: HyperStatSplitCpuEconConfiguration, portType: Port, association: Int): HyperStatSplitCpuEconConfiguration{
            val config = getConfigCopy(current)
            when(portType){
                Port.RELAY_ONE -> config.relay1State = config.relay1State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_TWO -> config.relay2State = config.relay2State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_THREE -> config.relay3State = config.relay3State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_FOUR -> config.relay4State = config.relay4State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_FIVE -> config.relay5State = config.relay5State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_SIX -> config.relay6State = config.relay6State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_SEVEN -> config.relay7State = config.relay7State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.RELAY_EIGHT -> config.relay8State = config.relay8State.copy(association = CpuEconRelayAssociation.values()[association])
                Port.ANALOG_OUT_ONE -> config.analogOut1State = config.analogOut1State.copy(association =  CpuEconAnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_TWO -> config.analogOut2State = config.analogOut2State.copy(association = CpuEconAnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_THREE -> config.analogOut3State = config.analogOut3State.copy(association = CpuEconAnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_FOUR -> config.analogOut4State = config.analogOut4State.copy(association = CpuEconAnalogOutAssociation.values()[association])
                Port.UNIVERSAL_IN_ONE -> config.universalIn1State = config.universalIn1State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_TWO -> config.universalIn2State = config.universalIn2State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_THREE -> config.universalIn3State = config.universalIn3State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_FOUR -> config.universalIn4State = config.universalIn4State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_FIVE -> config.universalIn5State = config.universalIn5State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_SIX -> config.universalIn6State = config.universalIn6State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_SEVEN -> config.universalIn7State = config.universalIn7State.copy(association = UniversalInAssociation.values()[association])
                Port.UNIVERSAL_IN_EIGHT -> config.universalIn8State = config.universalIn8State.copy(association = UniversalInAssociation.values()[association])
                else -> {}

            }
            return config
        }

        private fun getConfigCopy(original:HyperStatSplitCpuEconConfiguration ): HyperStatSplitCpuEconConfiguration {
            return HyperStatSplitCpuEconConfiguration().apply {
                temperatureOffset = original.temperatureOffset
                isEnableAutoForceOccupied = original.isEnableAutoForceOccupied
                isEnableAutoAway = original.isEnableAutoAway
                address0State = original.address0State
                address1State = original.address1State
                address2State = original.address2State
                address3State = original.address3State
                relay1State = original.relay1State
                relay2State = original.relay2State
                relay3State = original.relay3State
                relay4State = original.relay4State
                relay5State = original.relay5State
                relay6State = original.relay6State
                relay7State = original.relay7State
                relay8State = original.relay8State
                analogOut1State = original.analogOut1State
                analogOut2State = original.analogOut2State
                analogOut3State = original.analogOut3State
                analogOut4State = original.analogOut4State
                universalIn1State = original.universalIn1State
                universalIn2State = original.universalIn2State
                universalIn3State = original.universalIn3State
                universalIn4State = original.universalIn4State
                universalIn5State = original.universalIn5State
                universalIn6State = original.universalIn6State
                universalIn7State = original.universalIn7State
                universalIn8State = original.universalIn8State

                zoneCO2DamperOpeningRate = original.zoneCO2DamperOpeningRate
                zoneCO2Threshold = original.zoneCO2Threshold
                zoneCO2Target = original.zoneCO2Target
                zoneVOCThreshold = original.zoneVOCThreshold
                zoneVOCTarget = original.zoneVOCTarget
                zonePm2p5Threshold = original.zonePm2p5Threshold
                zonePm2p5Target = original.zonePm2p5Target
            }
        }

    }
}