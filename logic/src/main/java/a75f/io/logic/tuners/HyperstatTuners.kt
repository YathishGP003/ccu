package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_ANALOG_SPEED_MULTIPLIER
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_ANALOG_SPEED_MULTIPLIER_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_COOLING_DEADBAND
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_COOLING_DEADBAND_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_COOLING_DEADBAND_MULTIPLIER
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_COOLING_DEADBAND_MULTIPLIER_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_HEATING_DEADBAND
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_HEATING_DEADBAND_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_HEATING_DEADBAND_MULTIPLIER
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_HEATING_DEADBAND_MULTIPLIER_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_HUMIDITY_HISTERESIS
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_HUMIDITY_HISTERESIS_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_INTEGRAL_KFACTOR
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_INTEGRAL_KFACTOR_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_PROPORTIONAL_KFACTOR
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_PROPORTIONAL_KFACTOR_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_RELAY_ACTIVATION_HISTERESIS
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_RELAY_ACTIVATION_HISTERESIS_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_TEMPERATURE_INTEGRAL_TIME
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_TEMPERATURE_INTEGRAL_TIME_DEFAULT
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE
import a75f.io.logic.tuners.TunersUtil.HyperstatTunerConstants.Companion.HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE_DEFAULT
import android.annotation.SuppressLint

/**
 * @author Manjunath K
 * Created on 28-07-2021.
 */
class HyperstatTuners {

    companion object {

        private var siteRef: String? = null

        @SuppressLint("StaticFieldLeak")
        private var hayStack: CCUHsApi? = null
        private var equipRef: String? = null
        private var equipDis: String? = null
        private var tz: String? = null
        private var roomRef: String? = null
        private var floorRef: String? = null
        // function which add tuner points to haystack

        private fun pushTunerPointToHaystack(hayStack: CCUHsApi, point: Point, defaultValue: Double) {
            val pointId: String = hayStack.addPoint(point)
            hayStack.writePointForCcuUser(
                pointId,
                TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                defaultValue,
                0
            )
            hayStack.writeHisValById(pointId, defaultValue)
        }


        // function which create tuner point
        private fun createTunerPoint(
            displayName: String, markers: Array<String>,
            min: String, max: String, incValue: String, unit: String
        ): Point {
            val point = Point.Builder()
                .setDisplayName("$equipDis-standalone$displayName")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setHisInterpolate("cov")
                .setMinVal(min).setMaxVal(max).setIncrementVal(incValue)
                .setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit(unit)
                .setTz(tz)

                // add common  markers
                .addMarker("hyperstat").addMarker("tuner").addMarker("default").addMarker("sp")
                .addMarker("writable").addMarker("his")
                .addMarker(Tags.STANDALONE)



            if (!roomRef.isNullOrEmpty()) point.setRoomRef(roomRef)
            if (!floorRef.isNullOrEmpty()) point.setFloorRef(floorRef)

            // add specific markers
            markers.forEach { point.addMarker(it) }
            return point.build()
        }

        // Adding Hyperstat Default tuners

        fun addHyperstatDefaultTuners(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String
        ) {

            //initialise the variables
            this.hayStack = hayStack
            this.siteRef = siteRef
            this.equipDis = equipDis
            this.equipRef = equipRef
            this.tz = tz
            val listOfTunerPoints: HashMap<Point, Double> = HashMap()

            // Create Tuner Points

            // coolingDeadband = 2 (°F)
            // Min = 0 ,  Max = 10.0 ,  Default = 2 , inc 0.5 unit = u00B0F
            /*
            val coolingDeadBandMarkers = arrayOf("cooling", "deadband")
            val hsCoolingDeadBandPoint = createTunerPoint(
                HYPERSTAT_COOLING_DEADBAND, coolingDeadBandMarkers, "0", "10.0", "0.5", "\u00B0F"
            )
            listOfTunerPoints[hsCoolingDeadBandPoint] = HYPERSTAT_COOLING_DEADBAND_DEFAULT

            */
            // coolingDeadbandMultiplier = 0.5
            // min = 0 , max = 5.0 , Default = 0.5 inc 0.1 unit = ?
            val hsCoolingDeadbandMultiplierMarkers = arrayOf("cooling", "deadband", "multiplier")
            val hsCoolingDeadbandMultiplierPoint = createTunerPoint(
                HYPERSTAT_COOLING_DEADBAND_MULTIPLIER, hsCoolingDeadbandMultiplierMarkers, "0", "5.0", "0.1", ""
            )
            listOfTunerPoints[hsCoolingDeadbandMultiplierPoint] = HYPERSTAT_COOLING_DEADBAND_MULTIPLIER_DEFAULT


            // temperatureProportionalRange = 2 (°F)
            // min = 0 , max = 10 default = 2 , inc = 1 , unit = "\u00B0F"
            val tempProportionalRangeMarkers = arrayOf("temp", "proportional", "pspread")
            val tempProportionalRangePoint = createTunerPoint(
                HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE, tempProportionalRangeMarkers, "0", "10.0", "1", "\u00B0F"
            )
            listOfTunerPoints[tempProportionalRangePoint] = HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE_DEFAULT
            // proportionalKFactor = 0.5
            // min 0.1 , max = 1.0 default = 0.5 , inc = 0.1 unit = ?
            val proportionalKFactorMarkers = arrayOf("proportional", "pgain")
            val proportionalKFactorPoint = createTunerPoint(
                HYPERSTAT_PROPORTIONAL_KFACTOR, proportionalKFactorMarkers, "0.1", "1.0", "0.1", ""
            )
            listOfTunerPoints[proportionalKFactorPoint] = HYPERSTAT_PROPORTIONAL_KFACTOR_DEFAULT

            //	relayActivationHysteresis = 10%
            //  Min = 0 , max = 100 , Default = 10 , inc = 1  unit = %
            val relayActivationHysteresisMarkers = arrayOf("relay", "activation", "hysteresis")
            val relayActivationHysteresisPoint = createTunerPoint(
                HYPERSTAT_RELAY_ACTIVATION_HISTERESIS, relayActivationHysteresisMarkers, "0", "100", "1", "%"
            )
            listOfTunerPoints[relayActivationHysteresisPoint] = HYPERSTAT_RELAY_ACTIVATION_HISTERESIS_DEFAULT

            //	analogFanSpeedMultiplier = 1
            //  min = 0.1 , max = 3.0 default = 1 , inc = 0.1  unit = ?
            val analogFanSpeedMultiplierMarkers = arrayOf("analog", "fan", "multiplier", "speed")
            val analogFanSpeedMultiplierPoint = createTunerPoint(
                HYPERSTAT_ANALOG_SPEED_MULTIPLIER, analogFanSpeedMultiplierMarkers, "0.1", "3.0", "0.1", ""
            )
            listOfTunerPoints[analogFanSpeedMultiplierPoint] = HYPERSTAT_ANALOG_SPEED_MULTIPLIER_DEFAULT

            // humidityHysteresis = 5%
            // min 0 , max = 100 , Default = 5 , inc = 1  unit = %
            val humidityHysteresisMarkers = arrayOf("humidity", "hysteresis")
            val humidityHysteresisPoint = createTunerPoint(
                HYPERSTAT_HUMIDITY_HISTERESIS, humidityHysteresisMarkers, "0", "100", "1", "%"
            )
            listOfTunerPoints[humidityHysteresisPoint] = HYPERSTAT_HUMIDITY_HISTERESIS_DEFAULT

            /*

            // autoAwayZoneSetbackTemp = 2 (°F)
            // Min = 0 ,  Max = 10.0 ,  Default = 2 , inc 0.5  unit = "\u00B0F"
            val autoAwayZoneSetbackTempMarkers = arrayOf("auto", "zone", "away", "setback", "temp")
            val autoAwayZoneSetbackTempPoint = createTunerPoint(
                HYPERSTAT_AUTO_AWAY_ZONE_STEPBACK_TEMP, autoAwayZoneSetbackTempMarkers, "0", "10.0", "0.5", "\u00B0F"
            )
            listOfTunerPoints[autoAwayZoneSetbackTempPoint] = HYPERSTAT_AUTO_AWAY_ZONE_STEPBACK_TEMP_DEFAULT

            */

            // heatingDeadband = 2 (°F)
            // min = 0 max = 10.0 inc = 0.5 default = 2.0 unit "\u00B0F"
           /* val heatingDeadbandMarkers = arrayOf("heating", "deadband")
            val heatingDeadbandPoint = createTunerPoint(
                HYPERSTAT_HEATING_DEADBAND, heatingDeadbandMarkers, "0", "10.0", "0.5", "\u00B0F"
            )
            listOfTunerPoints[heatingDeadbandPoint] = HYPERSTAT_HEATING_DEADBAND_DEFAULT
            */
            val heatingDeadbandMultiplierMarkers = arrayOf("heating", "deadband", "multiplier")
            val heatingDeadbandMultiplierPoint = createTunerPoint(
                HYPERSTAT_HEATING_DEADBAND_MULTIPLIER, heatingDeadbandMultiplierMarkers, "0", "5.0", "0.1", ""
            )
            listOfTunerPoints[heatingDeadbandMultiplierPoint] = HYPERSTAT_HEATING_DEADBAND_MULTIPLIER_DEFAULT

            val temperatureIntegralTimeMarkers = arrayOf("temp", "integral", "time", "itimeout")
            val temperatureIntegralTimePoint = createTunerPoint(
                HYPERSTAT_TEMPERATURE_INTEGRAL_TIME, temperatureIntegralTimeMarkers, "1", "60", "1", "m"
            )
            listOfTunerPoints[temperatureIntegralTimePoint] = HYPERSTAT_TEMPERATURE_INTEGRAL_TIME_DEFAULT

            // integralKFactor = 0.5
            // min = 0.1 , max = 1.0 inc = 0.1 default = 0.5 unit = ?
            val integralKFactorMarkers = arrayOf("proportional", "integral", "igain")
            val integralKFactorPoint = createTunerPoint(
                HYPERSTAT_INTEGRAL_KFACTOR, integralKFactorMarkers, "0.1", "1.0", "0.1", ""
            )
            listOfTunerPoints[integralKFactorPoint] = HYPERSTAT_INTEGRAL_KFACTOR_DEFAULT


            // Push Tuner points to haystack
            listOfTunerPoints.forEach { (point, defaultValue) ->
                pushTunerPointToHaystack(
                    hayStack,
                    point,
                    defaultValue
                )
            }

        }

        private fun addHyperstatModuleDefaultTuners(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String
        ) {

            //initialise the variables
            this.hayStack = hayStack
            this.siteRef = siteRef
            this.equipDis = equipDis
            this.equipRef = equipRef
            this.tz = tz
            val listOfTunerPoints: HashMap<Point, Double> = HashMap()

            // Create Tuner Points

            // coolingDeadband = 2 (°F)
            // Min = 0 ,  Max = 10.0 ,  Default = 2 , inc 0.5 unit = u00B0F
            val coolingDeadBandMarkers = arrayOf("cooling", "deadband","base")
            val hsCoolingDeadBandPoint = createTunerPoint(
                HYPERSTAT_COOLING_DEADBAND, coolingDeadBandMarkers, "0", "10.0", "0.5", "\u00B0F"
            )
            listOfTunerPoints[hsCoolingDeadBandPoint] = HYPERSTAT_COOLING_DEADBAND_DEFAULT

            // coolingDeadbandMultiplier = 0.5
            // min = 0 , max = 5.0 , Default = 0.5 inc 0.1 unit = ?
            val hsCoolingDeadbandMultiplierMarkers = arrayOf("cooling", "deadband", "multiplier")
            val hsCoolingDeadbandMultiplierPoint = createTunerPoint(
                HYPERSTAT_COOLING_DEADBAND_MULTIPLIER, hsCoolingDeadbandMultiplierMarkers, "0", "5.0", "0.1", ""
            )
            listOfTunerPoints[hsCoolingDeadbandMultiplierPoint] = HYPERSTAT_COOLING_DEADBAND_MULTIPLIER_DEFAULT


            // temperatureProportionalRange = 2 (°F)
            // min = 0 , max = 10 default = 2 , inc = 1 , unit = "\u00B0F"
            val tempProportionalRangeMarkers = arrayOf("temp", "proportional", "pspread")
            val tempProportionalRangePoint = createTunerPoint(
                HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE, tempProportionalRangeMarkers, "0", "10.0", "1", "\u00B0F"
            )
            listOfTunerPoints[tempProportionalRangePoint] = HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE_DEFAULT
            // proportionalKFactor = 0.5
            // min 0.1 , max = 1.0 default = 0.5 , inc = 0.1 unit = ?
            val proportionalKFactorMarkers = arrayOf("proportional", "pgain")
            val proportionalKFactorPoint = createTunerPoint(
                HYPERSTAT_PROPORTIONAL_KFACTOR, proportionalKFactorMarkers, "0.1", "1.0", "0.1", ""
            )
            listOfTunerPoints[proportionalKFactorPoint] = HYPERSTAT_PROPORTIONAL_KFACTOR_DEFAULT

            //	relayActivationHysteresis = 10%
            //  Min = 0 , max = 100 , Default = 10 , inc = 1  unit = %
            val relayActivationHysteresisMarkers = arrayOf("relay", "activation", "hysteresis")
            val relayActivationHysteresisPoint = createTunerPoint(
                HYPERSTAT_RELAY_ACTIVATION_HISTERESIS, relayActivationHysteresisMarkers, "0", "100", "1", "%"
            )
            listOfTunerPoints[relayActivationHysteresisPoint] = HYPERSTAT_RELAY_ACTIVATION_HISTERESIS_DEFAULT

            //	analogFanSpeedMultiplier = 1
            //  min = 0.1 , max = 3.0 default = 1 , inc = 0.1  unit = ?
            val analogFanSpeedMultiplierMarkers = arrayOf("analog", "fan", "multiplier", "speed")
            val analogFanSpeedMultiplierPoint = createTunerPoint(
                HYPERSTAT_ANALOG_SPEED_MULTIPLIER, analogFanSpeedMultiplierMarkers, "0.1", "3.0", "0.1", ""
            )
            listOfTunerPoints[analogFanSpeedMultiplierPoint] = HYPERSTAT_ANALOG_SPEED_MULTIPLIER_DEFAULT

            // humidityHysteresis = 5%
            // min 0 , max = 100 , Default = 5 , inc = 1  unit = %
            val humidityHysteresisMarkers = arrayOf("humidity", "hysteresis")
            val humidityHysteresisPoint = createTunerPoint(
                HYPERSTAT_HUMIDITY_HISTERESIS, humidityHysteresisMarkers, "0", "100", "1", "%"
            )
            listOfTunerPoints[humidityHysteresisPoint] = HYPERSTAT_HUMIDITY_HISTERESIS_DEFAULT

            /*

            // autoAwayZoneSetbackTemp = 2 (°F)
            // Min = 0 ,  Max = 10.0 ,  Default = 2 , inc 0.5  unit = "\u00B0F"
            val autoAwayZoneSetbackTempMarkers = arrayOf("auto", "zone", "away", "setback", "temp")
            val autoAwayZoneSetbackTempPoint = createTunerPoint(
                HYPERSTAT_AUTO_AWAY_ZONE_STEPBACK_TEMP, autoAwayZoneSetbackTempMarkers, "0", "10.0", "0.5", "\u00B0F"
            )
            listOfTunerPoints[autoAwayZoneSetbackTempPoint] = HYPERSTAT_AUTO_AWAY_ZONE_STEPBACK_TEMP_DEFAULT

            */

            // heatingDeadband = 2 (°F)
            // min = 0 max = 10.0 inc = 0.5 default = 2.0 unit "\u00B0F"
            val heatingDeadbandMarkers = arrayOf("heating", "deadband","base")
            val heatingDeadbandPoint = createTunerPoint(
                HYPERSTAT_HEATING_DEADBAND, heatingDeadbandMarkers, "0", "10.0", "0.5", "\u00B0F"
            )
            listOfTunerPoints[heatingDeadbandPoint] = HYPERSTAT_HEATING_DEADBAND_DEFAULT

            val heatingDeadbandMultiplierMarkers = arrayOf("heating", "deadband", "multiplier")
            val heatingDeadbandMultiplierPoint = createTunerPoint(
                HYPERSTAT_HEATING_DEADBAND_MULTIPLIER, heatingDeadbandMultiplierMarkers, "0", "5.0", "0.1", ""
            )
            listOfTunerPoints[heatingDeadbandMultiplierPoint] = HYPERSTAT_HEATING_DEADBAND_MULTIPLIER_DEFAULT

            val temperatureIntegralTimeMarkers = arrayOf("temp", "integral", "time", "itimeout")
            val temperatureIntegralTimePoint = createTunerPoint(
                HYPERSTAT_TEMPERATURE_INTEGRAL_TIME, temperatureIntegralTimeMarkers, "1", "60", "1", "m"
            )
            listOfTunerPoints[temperatureIntegralTimePoint] = HYPERSTAT_TEMPERATURE_INTEGRAL_TIME_DEFAULT

            // integralKFactor = 0.5
            // min = 0.1 , max = 1.0 inc = 0.1 default = 0.5 unit = ?
            val integralKFactorMarkers = arrayOf("proportional", "integral", "igain")
            val integralKFactorPoint = createTunerPoint(
                HYPERSTAT_INTEGRAL_KFACTOR, integralKFactorMarkers, "0.1", "1.0", "0.1", ""
            )
            listOfTunerPoints[integralKFactorPoint] = HYPERSTAT_INTEGRAL_KFACTOR_DEFAULT


            // Push Tuner points to haystack
            listOfTunerPoints.forEach { (point, defaultValue) ->
                pushTunerPointToHaystack(
                    hayStack,
                    point,
                    defaultValue
                )
            }


            val standaloneCoolingPreconditioningRate = Point.Builder()
                .setDisplayName("$equipDis-standaloneCoolingPreconditioningRate")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("minute")
                .setTz(tz)
                .build()
            val standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate)
            BuildingTunerUtil.updateTunerLevels(standaloneCoolingPreconditioningRateId, roomRef, hayStack)
            hayStack.writeHisValById(
                standaloneCoolingPreconditioningRateId,
                HSUtil.getPriorityVal(standaloneCoolingPreconditioningRateId)
            )

            val standaloneHeatingPreconditioningRate = Point.Builder()
                .setDisplayName("$equipDis-standaloneHeatingPreconditioningRate")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("minute")
                .setTz(tz)
                .build()
            val standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate)
            BuildingTunerUtil.updateTunerLevels(standaloneHeatingPreconditioningRateId, roomRef, hayStack)
            hayStack.writeHisValById(
                standaloneHeatingPreconditioningRateId,
                HSUtil.getPriorityVal(standaloneHeatingPreconditioningRateId)
            )


            val autoAwaySetback = Point.Builder()
                .setDisplayName("$equipDis-autoAwaySetback")
                .setSiteRef(siteRef)
                .setEquipRef(equipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                .addMarker("zone").addMarker("auto").addMarker("away").addMarker("setback").addMarker("sp")
                .addMarker(Tags.STANDALONE)
                .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
            val autoAwaySetbackId = hayStack.addPoint(autoAwaySetback);
            hayStack.writePointForCcuUser(autoAwaySetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0, 0);
            hayStack.writeHisValById(autoAwaySetbackId, 2.0);
        }

        fun addHyperstatModuleTuners(
            hayStack: CCUHsApi, siteRef: String, equipRef: String, equipDis: String, tz: String,
            roomRef: String, floorRef: String
        ) {
            this.roomRef = roomRef
            this.floorRef = floorRef
            ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipDis, equipRef, roomRef, floorRef, tz)
            addHyperstatModuleDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz)
        }

    }
}

