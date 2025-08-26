package a75f.io.sanity.framework

import a75f.io.sanity.cases.DeviceEntityCheck
import a75f.io.sanity.cases.DuplicatePointCheck
import a75f.io.sanity.cases.NamedScheduleValidation
import a75f.io.sanity.cases.PartialDeviceCreationCheck
import a75f.io.sanity.cases.PartialEquipCreationCheck
import a75f.io.sanity.cases.SchedulableAndTunersCheck
import a75f.io.sanity.cases.ScheduleRefValidation

object SanitySuiteRegistry {
    val sanitySuites = HashMap<String, SanitySuite>()

    fun addSuite(suite: SanitySuite) {
        sanitySuites[suite.name] = suite
    }

    fun removeSuite(suite: SanitySuite) {
        sanitySuites.remove(suite.name)
    }

    fun getSuiteByName(name: String): SanitySuite? = sanitySuites[name]

    init {
        // Register default sanity suites
        val defaultSanitySuite = SanitySuite("DefaultSanitySuite")
        defaultSanitySuite.addCase(SchedulableAndTunersCheck())
        defaultSanitySuite.addCase(DeviceEntityCheck())
        defaultSanitySuite.addCase(DuplicatePointCheck())
        defaultSanitySuite.addCase(ScheduleRefValidation())
        defaultSanitySuite.addCase(NamedScheduleValidation())
        defaultSanitySuite.addCase(PartialEquipCreationCheck())
        defaultSanitySuite.addCase(PartialDeviceCreationCheck())
        addSuite(defaultSanitySuite)

    }
}