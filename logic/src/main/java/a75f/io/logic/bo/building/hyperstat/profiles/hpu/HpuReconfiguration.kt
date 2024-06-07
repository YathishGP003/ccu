package a75f.io.logic.bo.building.hyperstat.profiles.hpu

import a75f.io.api.haystack.Equip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th1InAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th2InAssociation

/**
 * Created by Manjunath K on 26-09-2022.
 */

class HpuReconfiguration {
    companion object {
        fun configAssociationPoint(
            portType: Port,
            updatedConfigValue: Double,
            equipPoint: Equip, ) {
            
            val equip = HyperStatHpuEquip.getHyperStatEquipRef(equipPoint.group.toShort())
            val updatedConfiguration = updateAssociation(equip.getConfiguration(), portType, updatedConfigValue.toInt())
            equip.updateConfiguration(updatedConfiguration)
        }

        fun updateConfiguration(updatedConfigValue: Double, equipPoint: Equip, portType: Port) {
            val equip = HyperStatHpuEquip.getHyperStatEquipRef(equipPoint.group.toShort())
            val updatedConfiguration = updateConfig(equip.getConfiguration(), portType, updatedConfigValue)
            equip.updateConfiguration(updatedConfiguration)
        }

        private fun updateConfig(
            current: HyperStatHpuConfiguration,
            portType: Port,
            status: Double
        ): HyperStatHpuConfiguration {
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
            current: HyperStatHpuConfiguration,
            portType: Port,
            association: Int
        ): HyperStatHpuConfiguration {
            val config = getConfigCopy(current)
            when (portType) {
                Port.RELAY_ONE -> config.relay1State =
                    config.relay1State.copy(association = HpuRelayAssociation.values()[association])
                Port.RELAY_TWO -> config.relay2State =
                    config.relay2State.copy(association = HpuRelayAssociation.values()[association])
                Port.RELAY_THREE -> config.relay3State =
                    config.relay3State.copy(association = HpuRelayAssociation.values()[association])
                Port.RELAY_FOUR -> config.relay4State =
                    config.relay4State.copy(association = HpuRelayAssociation.values()[association])
                Port.RELAY_FIVE -> config.relay5State =
                    config.relay5State.copy(association = HpuRelayAssociation.values()[association])
                Port.RELAY_SIX -> config.relay6State =
                    config.relay6State.copy(association = HpuRelayAssociation.values()[association])
                Port.ANALOG_OUT_ONE -> config.analogOut1State =
                    config.analogOut1State.copy(association = HpuAnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_TWO -> config.analogOut2State =
                    config.analogOut2State.copy(association = HpuAnalogOutAssociation.values()[association])
                Port.ANALOG_OUT_THREE -> config.analogOut3State =
                    config.analogOut3State.copy(association = HpuAnalogOutAssociation.values()[association])
                Port.ANALOG_IN_ONE -> config.analogIn1State =
                    config.analogIn1State.copy(association = AnalogInAssociation.values()[association])
                Port.ANALOG_IN_TWO -> config.analogIn2State =
                    config.analogIn2State.copy(association = AnalogInAssociation.values()[association])
                Port.TH1_IN -> config.thermistorIn1State =
                    config.thermistorIn1State.copy(association = Th1InAssociation.values()[association])
                Port.TH2_IN -> config.thermistorIn2State =
                    config.thermistorIn2State.copy(association = Th2InAssociation.values()[association])

                else -> {}

            }
            return config
        }

        private fun getConfigCopy(original: HyperStatHpuConfiguration): HyperStatHpuConfiguration {
            return HyperStatHpuConfiguration().apply {
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