package a75f.io.logic.bo.building.hyperstat.profiles.cpu


data class Th1InState(
   val enabled: Boolean,
   val association: Th1InAssociation
)

data class Th2InState(
   val enabled: Boolean,
   val association: Th2InAssociation
)

data class AnalogInState(
   val enabled: Boolean,
   val association: AnalogInAssociation
)

// Order is important -- see comment above.
enum class Th1InAssociation {
   AIRFLOW_TEMPERATURE,
   GENERIC_FAULT_NC,
   GENERIC_FAULT_NO
}

// Order is important -- see comment above.
enum class Th2InAssociation {
   DOOR_WINDOW_SENSOR,
   GENERIC_FAULT_NC,
   GENERIC_FAULT_NO
}

// Order is important -- see comment above.
enum class AnalogInAssociation {
   CURRENT_TX_0_10,
   CURRENT_TX_0_20,
   CURRENT_TX_0_50,
   KEY_CARD_SENSOR,
   DOOR_WINDOW_SENSOR
}

