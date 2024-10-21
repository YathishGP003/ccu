package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.VavAcbEquip
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.VavEquip
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.building.vav.AcbProfileConfiguration

class ACBConfigHandler {
    companion object {
        // "analogType" tag is used by control message code and cannot easily be replaced with a domain name query.
        // We are setting this value upon equip creation/reconfiguration for now.
        fun setOutputTypes(hayStack: CCUHsApi, config: AcbProfileConfiguration) {
            val device = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")

            // Set
            val relay1 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay1 + "\"");
            var relay1Point = RawPoint.Builder().setHDict(relay1).setEnabled(true)
            hayStack.updatePoint(relay1Point.setType("Relay N/C").build(), relay1.get("id").toString())

            var relay2 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay2 + "\"");
            var relay2Point = RawPoint.Builder().setHDict(relay2)
            hayStack.updatePoint(relay2Point.setType("Relay N/C").build(), relay2.get("id").toString())

            var analogOut1 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1Out + "\"");
            var analog1Point = RawPoint.Builder().setHDict(analogOut1)
            hayStack.updatePoint(analog1Point.setType(getDamperTypeString(config)).build(), analogOut1.get("id").toString())

            val analogOut2 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog2Out + "\"");

            val valueType = config.valveType.currentVal.toInt() - 1
            val analog2OpEnabled = valueType == ReheatType.ZeroToTenV.ordinal ||
                    valueType == ReheatType.TwoToTenV.ordinal ||
                    valueType == ReheatType.TenToZeroV.ordinal ||
                    valueType == ReheatType.TenToTwov.ordinal  ||
                    valueType == ReheatType.Pulse.ordinal

            val analog2Point = RawPoint.Builder().setHDict(analogOut2)
                .setType(getValveTypeString(config)).setEnabled(analog2OpEnabled).build()

            hayStack.updatePoint(analog2Point, analogOut2.get("id").toString())

        }

        // This logic will break if the "damperType" point enum is changed
        private fun getDamperTypeString(config: AcbProfileConfiguration) : String {
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
        private fun getValveTypeString(config: AcbProfileConfiguration) : String {
            return when(config.valveType.currentVal.toInt()) {
                1 -> "0-10v"
                2 -> "2-10v"
                3 -> "10-2v"
                4 -> "10-0v"
                5 -> "Pulsed Electric"
                else -> { "0" }
            }
        }

        fun updateCondensateSensor(hayStack: CCUHsApi, config: AcbProfileConfiguration) {
            val device = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")
            var th2In = hayStack.readEntity("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.th2In + "\"")
            var th2InPoint = RawPoint.Builder().setHashMap(th2In)

            if (config.condensateSensorType.enabled) {
                // N/C Condensation Sensor
                val condensateNcPoint = hayStack.readEntity("point and domainName == \"" + DomainName.condensateNC + "\" and group == \"" + config.nodeAddress + "\"")
                if (condensateNcPoint.containsKey("id")) {
                    hayStack.updatePoint(th2InPoint.setPointRef(condensateNcPoint.get("id").toString()).build(), th2In.get("id").toString())
                }
            } else {
                // N/O Condensation Sensor
                val condensateNoPoint = hayStack.readEntity("point and domainName == \"" + DomainName.condensateNO + "\" and group == \"" + config.nodeAddress + "\"")
                if (condensateNoPoint.containsKey("id")) {
                    hayStack.updatePoint(th2InPoint.setPointRef(condensateNoPoint.get("id").toString()).build(), th2In.get("id").toString())
                }
            }

        }

        fun updateRelayAssociation(hayStack: CCUHsApi, config: AcbProfileConfiguration) {
            val device = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")
            var relay1Map = hayStack.readEntity("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay1 + "\"")
            var relay1 = RawPoint.Builder().setHashMap(relay1Map)

            if (config.relay1Association.associationVal > 0) {
                // N/O Valve
                val valveNoPoint = hayStack.read("point and domainName == \"" + DomainName.chilledWaterValveIsolationCmdPointNO + "\" and group == \"" + config.nodeAddress + "\"")
                if (valveNoPoint.containsKey("id")) {
                    hayStack.updatePoint(relay1.setPointRef(valveNoPoint.get("id").toString()).build(), relay1Map.get("id").toString())
                }
            } else {
                // N/C Valve
                val valveNcPoint = hayStack.read("point and domainName == \"" + DomainName.chilledWaterValveIsolationCmdPointNC + "\" and group == \"" + config.nodeAddress + "\"")
                if (valveNcPoint.containsKey("id")) {
                    hayStack.updatePoint(relay1.setPointRef(valveNcPoint.get("id").toString()).build(), relay1Map.get("id").toString())
                }
            }

        }

        // Previously, maxVal of (heating or cooling) Min CFM setpoint was set to the value of
        // the corresponding Max CFM setpoint. To recreate this logic, we need to manually edit the maxVal
        // tag on these points after they are created.
        fun setMinCfmSetpointMaxVals(hayStack: CCUHsApi, config: AcbProfileConfiguration) {
            val equip = hayStack.readEntity("equip and group == \"" + config.nodeAddress + "\"")
            val vavEquip = VavAcbEquip(equip.get("id").toString())

            if (vavEquip.enableCFMControl.readDefaultVal() > 0.0) {
                val maxCoolingCfm = vavEquip.maxCFMCooling.readDefaultVal()
                val maxReheatingCfm = vavEquip.maxCFMReheating.readDefaultVal()

                val minCoolingCfmMap = hayStack.readEntity("point and domainName == \"" + DomainName.minCFMCooling + "\"" + " and equipRef == \"" + vavEquip.equipRef + "\"")
                val minCoolingCfmPoint = Point.Builder().setHashMap(minCoolingCfmMap).setMaxVal(maxCoolingCfm.toString()).build()
                hayStack.updatePoint(minCoolingCfmPoint, minCoolingCfmMap.get("id").toString())

                val minReheatingCfmMap = hayStack.readEntity("point and domainName == \"" + DomainName.minCFMReheating + "\"" + " and equipRef == \"" + vavEquip.equipRef + "\"")
                val minReheatingCfmPoint = Point.Builder().setHashMap(minReheatingCfmMap).setMaxVal(maxReheatingCfm.toString()).build()
                hayStack.updatePoint(minReheatingCfmPoint, minReheatingCfmMap.get("id").toString())
            }

        }

        // AirflowCfmProportionalRange tuner is very config-specific. Appropriate value depends on the CFM setpoints.
        // We set this value to 1.5x the Max Cooling CFM, which is the ballpark value used by U.S. Support during commissioning.
        fun setAirflowCfmProportionalRange(hayStack: CCUHsApi, config: AcbProfileConfiguration) {
            val equip = hayStack.readEntity("equip and group == \"" + config.nodeAddress + "\"")
            val vavEquip = VavEquip(equip.get("id").toString())

            if (vavEquip.enableCFMControl.readDefaultVal() > 0.0) {
                vavEquip.vavAirflowCFMProportionalRange.writeVal(8, 1.5 * vavEquip.maxCFMCooling.readPriorityVal())
            }
        }

    }
}