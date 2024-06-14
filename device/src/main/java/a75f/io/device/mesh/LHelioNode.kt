package a75f.io.device.mesh

import a75f.io.api.haystack.RawPoint
import a75f.io.device.serial.SmartNodeControls_t
import a75f.io.domain.api.DomainName
import org.javolution.io.Struct.Unsigned8

class LHelioNode : LSmartNode() {
    companion object {
        private const val ANALOG_OUT_FOUR = "ANALOG_OUT_FOUR"
        private const val RELAY_THREE = "RELAY_THREE"
        private const val RELAY_FOUR = "RELAY_FOUR"
        private const val ANALOG_OUT_THREE = "ANALOG_OUT_THREE"
        fun getHelioNodePort(controls: SmartNodeControls_t, p: RawPoint): Unsigned8? {
            val domainName = p.domainName
            if (domainName != null) {
                when (domainName) {
                    DomainName.analog1Out -> return controls.analogOut1
                    DomainName.analog2Out -> return controls.analogOut2
                    DomainName.analog3Out -> return controls.analogOut3
                    DomainName.analog4Out -> return controls.analogOut4
                    DomainName.relay1 -> return controls.digitalOut1
                    DomainName.relay2 -> return controls.digitalOut2
                    DomainName.relay3 -> return controls.digitalOut3
                    DomainName.relay4 -> return controls.digitalOut4
                }
            }
            val port = p.port
            return if (port != null) {
                when (port) {
                    ANALOG_OUT_ONE -> controls.analogOut1
                    ANALOG_OUT_TWO -> controls.analogOut2
                    ANALOG_OUT_THREE -> controls.analogOut3
                    ANALOG_OUT_FOUR -> controls.analogOut4
                    RELAY_ONE -> controls.digitalOut1
                    RELAY_TWO -> controls.digitalOut2
                    RELAY_THREE -> controls.digitalOut3
                    RELAY_FOUR -> controls.digitalOut4
                    else -> null
                }
            } else null
        }
    }
}