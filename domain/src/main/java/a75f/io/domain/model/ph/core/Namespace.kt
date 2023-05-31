package a75f.io.domain.model.ph.core

class Namespace {
    companion object {
        const val PH = "ph"
        const val PH75F = "ph75f"
        const val BMS75F = "bms75f"
        const val MODBUS75F = "modbus75f"
        const val EXTERNAL75f = "external75f"
        const val default = PH75F
        val values = setOf(PH, PH75F, BMS75F, MODBUS75F, EXTERNAL75f)
        private val displayNameLookup = mapOf(
            PH to "Native",
            PH75F to "Domain",
            BMS75F to "75F",
            MODBUS75F to "Modbus",
            EXTERNAL75f to "External"
        )
        @JvmStatic fun isPhNative(ns: String) = (ns == PH)
        @JvmStatic fun is75f(ns: String) = (ns == PH75F)
        @JvmStatic fun isValid(ns: String) = values.contains(ns)
        @JvmStatic fun getDisplayName(ns: String) = displayNameLookup[ns] ?: throw IllegalArgumentException("\"$ns\" is not a valid Namespace")
    }
}
