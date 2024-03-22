package a75f.io.logic.bo.building.bypassdamper

import a75.io.algos.ControlLoop
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.BypassDamperEquip
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.ZoneProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Damper
import a75f.io.logic.tuners.TunerUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class BypassDamperProfile(equipRef: String, addr: Short): ZoneProfile() {

    var bdEquip : BypassDamperEquip = BypassDamperEquip(equipRef)
    var nodeAddr : Short = addr
    //var equipRef : String? = null
    var bypassLoop : ControlLoop = ControlLoop()
    var bypassDamper : Damper = Damper()

    var proportionalKFactor = 0.5
    var integralKFactor = 0.5
    var proportionalSpread = 0.4
    var integralMaxTimeout = 30

    init {
        CcuLog.i("CCU_BYPASS", "Bypass Damper Profile Init")

        if (bdEquip.proportionalKFactor.readPriorityVal() > 0.0) proportionalKFactor = bdEquip.proportionalKFactor.readPriorityVal()
        if (bdEquip.integralKFactor.readPriorityVal() > 0.0) integralKFactor = bdEquip.integralKFactor.readPriorityVal()
        if (bdEquip.expectedPressureError.readPriorityVal() > 0.0) proportionalSpread = bdEquip.expectedPressureError.readPriorityVal()
        if (bdEquip.bypassDamperIntegralTime.readPriorityVal().toInt() > 0) integralMaxTimeout = bdEquip.bypassDamperIntegralTime.readPriorityVal().toInt()

        bypassLoop.setProportionalGain(proportionalKFactor)
        bypassLoop.setIntegralGain(integralKFactor)
        bypassLoop.setProportionalSpread(proportionalSpread)
        bypassLoop.setIntegralMaxTimeout(integralMaxTimeout)
        bypassLoop.reset()

        bypassDamper.minPosition = bdEquip.damperMinPosition.readPriorityVal().toInt()
        bypassDamper.maxPosition = bdEquip.damperMaxPosition.readPriorityVal().toInt()

        CcuLog.i("CCU_BYPASS", "Bypass Damper Profile Init Done")

    }

    override fun getEquip(): Equip? {
        val equip = CCUHsApi.getInstance().readHDict("equip and group == \"$nodeAddr\"")
        return Equip.Builder().setHDict(equip).build()
    }

    fun getEquipRef(): String? {
        val equip = CCUHsApi.getInstance().readHDict("equip and group == \"$nodeAddr\"")
        return equip.get("id").toString()
    }

    override fun updateZonePoints() {
        if (mInterface != null) mInterface.refreshView()
        CcuLog.i("CCU_BYPASS", "--->Bypass Damper Profile<---"+nodeAddr)

        initLoopVariables()

        var bypassLoopOp = 0.0

        if (isZoneDead) {
            CcuLog.d("CCU_BYPASS", "Bypass Damper: Zone is dead")
            return
        } else {
            val systemFanLoopOp = hayStack.readHisValByQuery("point and fan and system and loop and output and not tuner")

            val staticPressureSensor = bdEquip.ductStaticPressureSensor.readHisVal()
            val staticPressureSp = bdEquip.ductStaticPressureSetpoint.readPriorityVal()
            if (systemFanLoopOp > 0.0) {
                if (!bypassLoop.enabled) bypassLoop.setEnabled()

                bypassLoopOp = bypassLoop.getLoopOutput(staticPressureSensor, staticPressureSp)
                if (bypassLoopOp < 0.0) bypassLoopOp = 0.0
                if (bypassLoopOp > 100.0) bypassLoopOp = 100.0

            } else {
                if (bypassLoop.enabled) bypassLoop.setDisabled()
                CcuLog.i("CCU_BYPASS", "systemFanLoop is zero, bypass damper set to minimum")
                bypassLoop.reset()
            }
            bypassDamper.currentPosition = (bypassDamper.minPosition + (bypassLoopOp/100) * (bypassDamper.maxPosition - bypassDamper.minPosition)).toInt()


            CcuLog.d("CCU_BYPASS", "Bypass Damper: systemFanLoopOp " + systemFanLoopOp + ", staticPressure " + staticPressureSensor + ", staticPressureSp: " + staticPressureSp + ", bypassLoopOp: " + bypassLoopOp + ", bypassDamperPos: " + bypassDamper.currentPosition)
            bypassLoop.dumpWithTag("CCU_BYPASS")
        }

        bdEquip.bypassDamperLoopOutput.writeHisVal(bypassLoopOp)
        bdEquip.bypassDamperPos.writeHisVal(bypassDamper.currentPosition.toDouble())

    }

    override fun isZoneDead(): Boolean {
        val equip = equip
        if (equip == null) {
            CcuLog.e("CCU_BYPASS", "Profile does not have linked equip , assume zone is dead")
            return true
        }
        val point = CCUHsApi.getInstance()
            .readEntity("point and (heartbeat or heartBeat) and equipRef == \"" + equip.id + "\"")
        if (!point.isEmpty()) {
            val hisItem = CCUHsApi.getInstance().curRead(point["id"].toString())
            if (hisItem == null) {
                CcuLog.e(
                    "CCU_BYPASS",
                    "Equip dead! , Heartbeat does not exist for " + equip.displayName
                )
                return true
            }
            var zoneDeadTime = TunerUtil.readTunerValByQuery(
                "zone and dead and time",
                equip.id
            )
            if (zoneDeadTime == 0.0) {
                CcuLog.e(
                    "CCU_BYPASS",
                    "Invalid value for zoneDeadTime tuner, use default " + equip.displayName
                )
                zoneDeadTime = 15.0
            }
            if (System.currentTimeMillis() - hisItem.dateInMillis > zoneDeadTime * 60 * 1000) {
                CcuLog.e(
                    "CCU_BYPASS",
                    "Equip dead! , Heartbeat " + hisItem.date.toString() + " " + equip.displayName + " " + zoneDeadTime
                )
                return true
            }
        }

        return false
    }

    private fun initLoopVariables() {
        bypassDamper.minPosition = bdEquip.damperMinPosition.readPriorityVal().toInt()
        bypassDamper.maxPosition = bdEquip.damperMaxPosition.readPriorityVal().toInt()
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.BYPASS_DAMPER
    }

    override fun <T : BaseProfileConfiguration?> getProfileConfiguration(address: Short): T {
        return null as T
    }

    override fun getDomainProfileConfiguration() : ProfileConfiguration {
        return BypassDamperProfileConfiguration(nodeAddr.toInt(), NodeType.SMART_NODE.name, ZonePriority.NONE.ordinal, "SYSTEM", "SYSTEM", ProfileType.BYPASS_DAMPER, ModelLoader.getSmartNodeBypassDamperModelDef() as SeventyFiveFProfileDirective).getActiveConfiguration()
    }

    override fun getNodeAddresses() : Set<Short> {
        val nodeSet: MutableSet<Short?> = HashSet<Short?>()
        nodeSet.add(nodeAddr)
        return nodeSet as Set<Short>
    }

}