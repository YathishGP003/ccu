package a75f.io.logic.bo.building.mystat.profiles.packageunit.hpu

import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.mystat.profiles.packageunit.MyStatPackageUnitProfile

/**
 * Created by Manjunath K on 16-01-2025.
 */

class MyStatHpuProfile: MyStatPackageUnitProfile() {
    override fun updateZonePoints() {
        TODO("Not yet implemented")
    }

    override fun getProfileType(): ProfileType {
        TODO("Not yet implemented")
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        TODO("Not yet implemented")
    }
}