package a75f.io.logic.bo.building.hyperstat.profiles.pipe2

import a75f.io.api.haystack.Equip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation

/**
 * Created by Manjunath K on 26-09-2022.
 */

class Pipe2Reconfiguration {
    companion object {
        fun configAssociationPoint(
            portType: Port,
            updatedConfigValue: Double,
            equipPoint: Equip, ) {
            
            val equip = HyperStatPipe2Equip.getHyperStatEquipRef(equipPoint.group.toShort())
            val updatedConfiguration = updateAssociation(equip.getConfiguration(), portType, updatedConfigValue.toInt())
            equip.updateConfiguration(updatedConfiguration)
        }

        fun updateConfiguration(updatedConfigValue: Double, equipPoint: Equip, portType: Port) {
            val equip = HyperStatPipe2Equip.getHyperStatEquipRef(equipPoint.group.toShort())
            val updatedConfiguration = updateConfig(equip.getConfiguration(), portType, updatedConfigValue)
            equip.updateConfiguration(updatedConfiguration)
        }

        private fun updateConfig(
            current: HyperStatPipe2Configuration,
            portType: Port,
            status: Double
        ): HyperStatPipe2Configuration {
            val config = getConfigCopy(current)
            when (portType) {
                Port.RELAY_ONE -> config.relay1State =
                    config.relay1State.copy(enabled = (status == 1.0))
                Port.RELAY_TWO -> config.relay2State =
                    config.relay2State.copy(enabled = (status == 1.0))
                Port.RELAY_THREE -> config.relay3State =
                    config.relay3State.copy(enabled = (status == 1.0))
                Port.RELAY_FOUR -> config.relay4State =
                    config.relay4State.copy(enabled = (status == 1.0))
                Port.RELAY_FIVE -> config.relay5State =
                    config.relay5State.copy(enabled = (status == 1.0))
                Port.RELAY_SIX -> config.relay6State =
                    config.relay6State.copy(enabled = (status == 1.0))
                Port.ANALOG_OUT_ONE -> config.analogOut1State =
                    config.analogOut1State.copy(enabled = status == 1.0)
                Port.ANALOG_OUT_TWO -> config.analogOut2State =
                    config.analogOut2State.copy(enabled = status == 1.0)
                Port.ANALOG_OUT_THREE -> config.analogOut3State =
                    config.analogOut3State.copy(enabled = status == 1.0)
                Port.ANALOG_IN_ONE -> config.analogIn1State =
                    config.analogIn1State.copy(enabled = status == 1.0)
                Port.ANALOG_IN_TWO -> config.analogIn2State =
                    config.analogIn2State.copy(enabled = status == 1.0)
                Port.TH1_IN -> config.thermistorIn1State =
                    config.thermistorIn1State.copy(enabled = status == 1.0)
                Port.TH2_IN -> config.thermistorIn2State =
                    config.thermistorIn2State.copy(enabled = status == 1.0)
                else -> {}

            }
            return config
        }

        private fun updateAssociation(
            current: HyperStatPipe2Configuration,
            portType: Port,
            association: Int
        ): HyperStatPipe2Configuration {
            val config = getConfigCopy(current)
            when (portType) {
                Port.RELAY_ONE -> config.relay1State =
                    config.relay1State.copy(association = Pipe2RelayAssociation.values()[association])
                Port.RELAY_TWO -> config.relay2State =
                    config.relay2State.copy(association = Pipe2RelayAssociation.values()[association])
                Port.RELAY_THREE -> config.relay3State =
                    config.relay3State.copy(association = Pipe2RelayAssociation.values()[association])
                Port.RELAY_FOUR -> config.relay4State =
                    config.relay4State.copy(association = Pipe2RelayAssociation.values()[association])
                Port.RELAY_FIVE -> config.relay5State =
                    config.relay5State.copy(association = Pipe2RelayAssociation.values()[association])
                Port.RELAY_SIX -> config.relay6State =
                    config.relay6State.copy(association = Pipe2RelayAssociation.values()[association])
                Port.ANALOG_OUT_ONE -> config.analogOut1State =
                    config.analogOut1State.copy(association = Pipe2AnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_TWO -> config.analogOut2State =
                    config.analogOut2State.copy(association = Pipe2AnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_THREE -> config.analogOut3State =
                    config.analogOut3State.copy(association = Pipe2AnalogOutAssociation.values()[association])
                Port.ANALOG_IN_ONE -> config.analogIn1State =
                    config.analogIn1State.copy(association = AnalogInAssociation.values()[association])
                Port.ANALOG_IN_TWO -> config.analogIn2State =
                    config.analogIn2State.copy(association = AnalogInAssociation.values()[association])
                else -> {}

            }
            return config
        }

        private fun getConfigCopy(original: HyperStatPipe2Configuration): HyperStatPipe2Configuration {
            return HyperStatPipe2Configuration().apply {
                temperatureOffset = original.temperatureOffset
                isEnableAutoForceOccupied = original.isEnableAutoForceOccupied
                isEnableAutoAway = original.isEnableAutoAway
                relay1State = original.relay1State
                relay2State = original.relay2State
                relay3State = original.relay3State
                relay4State = original.relay4State
                relay5State = original.relay5State
                relay6State = original.relay6State
                analogOut1State = original.analogOut1State
                analogOut2State = original.analogOut2State
                analogOut3State = original.analogOut3State
                analogIn1State = original.analogIn1State
                analogIn2State = original.analogIn2State
                thermistorIn1State = original.thermistorIn1State
                thermistorIn2State = original.thermistorIn2State
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