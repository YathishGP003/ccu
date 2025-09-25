package a75f.io.logic.bo.building.statprofiles.util

/**
 * Created by Manjunath K on 02-01-2023.
 * Updated for CPU with Economiser by Nick P on 07-24-2023.
 */

const val HYPERSTATSPLIT: String = "Hyperstatsplit"
const val HSSPLIT_CPUECON: String = "cpu"
const val HSSPLIT_FULL: String = "HyperStat Split"
const val CPUECON_FULL: String = "CPU & Economiser"
const val PIPE4_ECON :String  = "4 Pipe & Economizer"
const val PIPE2_ECON :String = "2 Pipe & Economizer"

const val TRUE = "true"
const val ON = "on"
const val COOLING = "cooling"
const val HEATING = "heating"
const val TEMPDEAD = "tempdead"


// Drop down options for  Relay options
const val FAN_ENABLED = "fanEnable"
const val OCCUPIED_ENABLED = "occupiedEnable"
const val HUMIDIFIER = "humidifier"
const val DEHUMIDIFIER = "dehumidifier"
const val OAO_DAMPER = "oaoDamper"

const val HYPERSTAT: String = "HyperStat"
const val HSCPU: String = "Conventional Package Unit"

const val FAN_LOWEST_STAGE = 0
const val FAN_MEDIUM_STAGE = 1
const val FAN_HIGHEST_STAGE = 2

enum class AuxActiveStages {
    NONE, AUX1, AUX2, BOTH
}


