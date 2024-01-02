package a75f.io.logic.bo.building.system.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain

import a75f.io.domain.api.DomainName.coolingLoopOutput
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.heatingLoopOutput
import a75f.io.domain.api.DomainName.useOutsideTempLockoutCooling
import a75f.io.domain.api.DomainName.useOutsideTempLockoutHeating
import a75f.io.domain.api.DomainName.vavAnalogFanSpeedMultiplier
import a75f.io.domain.api.DomainName.vavHumidityHysteresis
import a75f.io.domain.api.DomainName.vavOutsideTempCoolingLockout
import a75f.io.domain.api.DomainName.vavOutsideTempHeatingLockout
import a75f.io.domain.api.Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.BasicConfig
import a75f.io.logic.bo.building.system.TempDirection
import a75f.io.logic.bo.building.system.calculateDSPSetPoints
import a75f.io.logic.bo.building.system.calculateSATSetPoints
import a75f.io.logic.bo.building.system.getConditioningMode
import a75f.io.logic.bo.building.system.getExternalEquipId
import a75f.io.logic.bo.building.system.getTunerByDomainName
import a75f.io.logic.bo.building.system.handleDeHumidityOperation
import a75f.io.logic.bo.building.system.handleHumidityOperation
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.bo.building.system.operateDamper
import a75f.io.logic.bo.building.system.setOccupancyMode
import a75f.io.logic.bo.building.system.updatePointHistoryAndDefaultValue
import a75f.io.logic.bo.building.system.updatePointValue
import a75f.io.logic.bo.building.system.writePointForCcuUser
import a75f.io.logic.interfaces.ModbusWritableDataInterface
import android.content.Intent

/**
 * Created by Manjunath K on 26-12-2023.
 */

class VavExternalAhu: VavSystemProfile() {
    override fun doSystemControl() {
        TODO("Not yet implemented")
    }

    override fun addSystemEquip() {
        TODO("Not yet implemented")
    }

    override fun deleteSystemEquip() {
        TODO("Not yet implemented")
    }

    override fun isCoolingAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHeatingAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCoolingActive(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHeatingActive(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getProfileType(): ProfileType {
        TODO("Not yet implemented")
    }

    override fun getStatusMessage(): String {
        TODO("Not yet implemented")
    }

}