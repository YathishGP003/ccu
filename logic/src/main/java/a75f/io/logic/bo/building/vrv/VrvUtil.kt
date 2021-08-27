package a75f.io.logic.bo.building.vrv

enum class VrvOperationMode {
    OFF {
        override fun toString() = "Off"
    },
    FAN {
        override fun toString() = "Fan (Ventilation)"
    },
    HEAT {
        override fun toString() = "Heat only mode"
    },
    COOL {
        override fun toString() = "Cool only mode"
    },
    AUTO {
        override fun toString() = "Auto"
    }
}
enum class VrvFanSpeed {
    Auto, Low, Medium, High
}

enum class VrvAirflowDirection {
    Position0, Position1, Position2, Position3, Position4, Swing, Auto
}

enum class VrvMasterController {
    NOT_MASTER {
              override fun toString() = "Not-Master"
    },
    MASTER {
        override fun toString() = "Master"
    }
}