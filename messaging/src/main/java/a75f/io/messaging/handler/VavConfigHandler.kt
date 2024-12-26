package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.VavEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.building.vav.VavProfileConfiguration

class VavConfigHandler {
    companion object {
        // "analogType" tag is used by control message code and cannot easily be replaced with a domain name query.
        // We are setting this value upon equip creation/reconfiguration for now.
        fun setVavOutputTypes(hayStack: CCUHsApi, config: VavProfileConfiguration) {
            val device = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")

            val reheatType = config.reheatType.currentVal.toInt() - 1
            val reheatCmdPoint = hayStack.readEntity("point and group == \"" + config.nodeAddress + "\" and domainName == \"" + DomainName.reheatCmd + "\"")

            // Relay 1 enabled if Reheat Type is 1-Stage or 2-Stage (2-Stage is only an option for VAV No Fan)
            val relay1OpEnabled = reheatType == ReheatType.OneStage.ordinal || reheatType == ReheatType.TwoStage.ordinal
            val relay1 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay1 + "\"")
            val relay1Point = RawPoint.Builder().setHDict(relay1).setType(if (relay1OpEnabled) "Relay N/C" else "Relay N/O").setEnabled(relay1OpEnabled).build()
            if (reheatType == ReheatType.OneStage.ordinal || reheatType == ReheatType.TwoStage.ordinal) {
                relay1Point.pointRef = reheatCmdPoint["id"].toString()
            } else {
                relay1Point.pointRef = null
            }
            hayStack.updatePoint(relay1Point, relay1["id"].toString())

            // Relay 2 is always enabled for VAV Series and Parallel Fan. For VAV No Fan, enabled only when Reheat Type is 2-Stage.

            val relay2OpEnabled = ((config.profileType != ProfileType.VAV_REHEAT.name) || reheatType == ReheatType.TwoStage.ordinal)
            val relay2 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay2 + "\"")
            val relay2Point = RawPoint.Builder().setHDict(relay2).setType(if (relay2OpEnabled) "Relay N/C" else "Relay N/O").setEnabled(relay2OpEnabled)
            if (relay2OpEnabled){
                relay2Point.setPointRef(reheatCmdPoint["id"].toString())
            } else {
                relay2Point.setPointRef(null)
            }
            hayStack.updatePoint(relay2Point.build(), relay2["id"].toString())

            val analogOut1 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.analog1Out + "\"")
            val analog1Point = RawPoint.Builder().setHDict(analogOut1)
            hayStack.updatePoint(analog1Point.setType(getDamperTypeString(config)).build(), analogOut1["id"].toString())

            val analog2OpEnabled = reheatType == ReheatType.ZeroToTenV.ordinal ||
                    reheatType == ReheatType.TwoToTenV.ordinal ||
                    reheatType == ReheatType.TenToZeroV.ordinal ||
                    reheatType == ReheatType.TenToTwov.ordinal ||
                    reheatType == ReheatType.Pulse.ordinal
            val analogOut2 = hayStack.readHDict("point and deviceRef == \""+ device["id"]
                    +"\" and domainName == \"" + DomainName.analog2Out + "\"")
            val analog2Point = RawPoint.Builder().setHDict(analogOut2)
                .setType(getReheatTypeString(config)).setEnabled(analog2OpEnabled).build()
            hayStack.updatePoint(analog2Point, analogOut2["id"].toString())

            val analogIn1 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.analog1In + "\"")
            val analog1InPoint = RawPoint.Builder().setHDict(analogIn1)
            hayStack.updatePoint(analog1InPoint.setType(getDamperTypeString(config)).build(), analogIn1["id"].toString())

        }

        // This logic will break if the "damperType" point enum is changed
        private fun getDamperTypeString(config: VavProfileConfiguration) : String {
            return when(config.damperType.currentVal.toInt()) {
                0 -> "0-10v"
                1 -> "2-10v"
                2 -> "10-2v"
                3 -> "10-0v"
                4 -> "Smart Damper"
                5 -> "0-5v"
                else -> { "0-10v" }
            }
        }

        // This logic will break if the "reheatType" enum is changed
        private fun getReheatTypeString(config: VavProfileConfiguration) : String {
            return when(config.reheatType.currentVal.toInt()) {
                1 -> "0-10v"
                2 -> "2-10v"
                3 -> "10-2v"
                4 -> "10-0v"
                5 -> "Pulsed Electric"
                else -> { "0-10v" }
            }
        }

        // Previously, maxVal of (heating or cooling) Min CFM setpoint was set to the value of
        // the corresponding Max CFM setpoint. To recreate this logic, we need to manually edit the maxVal
        // tag on these points after they are created.
        fun setMinCfmSetpointMaxVals(hayStack: CCUHsApi, config: VavProfileConfiguration) {
            val equip = hayStack.readEntity("equip and group == \"" + config.nodeAddress + "\"")
            val vavEquip = VavEquip(equip["id"].toString())

            if (vavEquip.enableCFMControl.readDefaultVal() > 0.0) {
                val maxCoolingCfm = vavEquip.maxCFMCooling.readDefaultVal()
                val maxReheatingCfm = vavEquip.maxCFMReheating.readDefaultVal()

                val minCoolingCfmMap = hayStack.readEntity("point and domainName == \"" + DomainName.minCFMCooling + "\"" + " and equipRef == \"" + vavEquip.equipRef + "\"")
                val minCoolingCfmPoint = Point.Builder().setHashMap(minCoolingCfmMap).setMaxVal(maxCoolingCfm.toString()).build()
                hayStack.updatePoint(minCoolingCfmPoint, minCoolingCfmMap["id"].toString())

                val minReheatingCfmMap = hayStack.readEntity("point and domainName == \"" + DomainName.minCFMReheating + "\"" + " and equipRef == \"" + vavEquip.equipRef + "\"")
                val minReheatingCfmPoint = Point.Builder().setHashMap(minReheatingCfmMap).setMaxVal(maxReheatingCfm.toString()).build()
                hayStack.updatePoint(minReheatingCfmPoint, minReheatingCfmMap["id"].toString())
            }

        }

        // AirflowCfmProportionalRange tuner is very config-specific. Appropriate value depends on the CFM setpoints.
        // We set this value to 1.5x the Max Cooling CFM, which is the ballpark value used by U.S. Support during commissioning.
        fun setAirflowCfmProportionalRange(hayStack: CCUHsApi, config: VavProfileConfiguration) {
            val equip = hayStack.readEntity("equip and group == \"" + config.nodeAddress + "\"")
            val vavEquip = VavEquip(equip["id"].toString())

            if (vavEquip.enableCFMControl.readDefaultVal() > 0.0) {
                vavEquip.vavAirflowCFMProportionalRange.writeVal(10, 1.5 * vavEquip.maxCFMCooling.readPriorityVal())
            }
        }


    }
}
