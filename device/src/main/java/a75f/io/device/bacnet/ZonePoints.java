package a75f.io.device.bacnet;

import android.util.Log;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AnalogValueObject;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.TrendLogObject;
import com.renovo.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.renovo.bacnet4j.type.constructed.ClientCov;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.LimitEnable;
import com.renovo.bacnet4j.type.constructed.LogRecord;
import com.renovo.bacnet4j.type.constructed.PropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.ReadAccessSpecification;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.EngineeringUnits;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Real;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.bo.building.SensorType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.tuners.TunerUtil;

public class ZonePoints {
    public SequenceOf<ReadAccessSpecification> readAccessSpecifications= new SequenceOf<>();
    public SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>();

    public ZonePoints() {
    }
    public ZonePoints(LocalDevice localDevice,Zone zone,String zoneDescription)
    {
        Log.i("ad","LocalDevice:"+localDevice.getInstanceNumber());
        Device zoneDevice =  HSUtil.getDevices(zone.getId()).get(0);
        //We will handle only single module per zone
        if(zoneDevice!=null) {
            String zoneName = zone.getDisplayName();
            int zoneAddress = Integer.parseInt(zoneDevice.getAddr());
            Equip zoneEquip =HSUtil.getEquipFromZone(zone.getId());
            if (!zoneEquip.getMarkers().contains("pid") && !zoneEquip.getMarkers().contains("emr")) {
                updateCurrentTemp(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                updateDesiredTemp(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                if (zoneEquip.getMarkers().contains("vav") || zoneEquip.getMarkers().contains("dab")) {
                    updateDamperPosition(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                }
            }
            if (zoneEquip.getMarkers().contains("vav")) {
                updateSupplyAirTemperature(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                updateReheatCoil(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
            }
            if(zoneDevice.getMarkers().contains("smartstat")) {
                updateSensorData(localDevice,zoneAddress,zoneName, zoneDescription,zoneDevice);
            }
        }
    }


    public void updateCurrentTemp(LocalDevice localDevice, int zoneAddress,String zoneName, String zoneDescription,Device zoneDevice){
        try {
            AnalogValueObject currentTemperature;
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.currentTemp;
            Log.i("Bacnet","Checking Current Temp:"+instanceID);
            if (!localDevice.checkObjectByIDandType(instanceID,ObjectType.analogValue)) {
                Log.i("Bacnet","Creating Current Temp:"+instanceID);
                currentTemperature = new AnalogValueObject(localDevice, instanceID, zoneName + "_currentTemp", (float) getZoneAvgCurrentTemp(zoneDevice), EngineeringUnits.degreesFahrenheit, false);
                currentTemperature.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,(float)getMaxBuildingLimits(),(float)getMinBuildingLimits(),1,
                        (float)getMaxBuildingLimits()+10,(float)getMinBuildingLimits()-10,new LimitEnable(true,true), new EventTransitionBits(true,true,true), NotifyType.alarm,0);
                currentTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Current Temperature of " + zoneDescription)));

                currentTemperature.supportCovReporting(BACnetUtils.currentTempCOV);
                TrendLogObject trendObject = new TrendLogObject(localDevice, instanceID /*trendAddressNumber + BACnetUtils.currentTemp*/, zoneName + "_currentTemp_trend",
                        new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), currentTemperature.getId(), PropertyIdentifier.presentValue),
                        BACnetUtils.logInterval, false, BACnetUtils.bufferSize);
                trendObject.withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);
                trendObject.withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(BACnetUtils.currentTempCOV)));
                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);
                Log.i("Bacnet","Creating notifClass for Current Temp:"+instanceID);

            }else{
                setPresentValue(localDevice,zoneAddress,zoneDevice);
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void setPresentValue(LocalDevice localDevice, int zoneAddress, Device zone){
        try {
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.currentTemp;
            if (localDevice.checkObjectByID(instanceID)) {
                AnalogValueObject baCnetObject = (AnalogValueObject)localDevice.getObjectByID(instanceID);
                double currentTemp = getZoneAvgCurrentTemp(zone);
                Log.i("Bacnet","Updating Current Temp:"+instanceID+" Value:"+currentTemp);
                if(baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    baCnetObject.writePropertyInternal(PropertyIdentifier.presentValue, new Real((float) currentTemp));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateDesiredTemp(LocalDevice localDevice, int zoneAddress,String zoneName, String zoneDescription,Device zoneDevice){
        try {
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceCoolID = addressNumber + BACnetUtils.desiredTempCooling;
            int instanceHeatID = addressNumber + BACnetUtils.desiredTempHeating;
            double hdb = getDeadband(zoneDevice,"heating");
            double cdb = getDeadband(zoneDevice,"cooling");
            Log.i("BacnetDB","Heating Deadband:"+hdb+" Cooling Deadband:"+cdb+" EquipRef:"+zoneDevice.getEquipRef());
            if (!localDevice.checkObjectByIDandType(instanceCoolID,ObjectType.analogValue)) {
                Log.i("Bacnet","Creating Cooling Desired Temp:"+instanceCoolID);
                //Todo re-verfify deadbands for heating and cooling DT
                AnalogValueObject desiredTemperature = new AnalogValueObject(localDevice, instanceCoolID, zoneName + "_coolingDesiredTemp", (float)getDesiredTemp(zoneDevice,"cooling"), EngineeringUnits.degreesFahrenheit, false);
                desiredTemperature.supportCommandable(72);
                desiredTemperature.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,(float) (getMaxUserTempLimits("cooling")+cdb),getMinUserTempLimits("cooling"),(float)cdb,
                        BACnetUtils.buildingLimitMax,BACnetUtils.buildingLimitMin,new LimitEnable(true,true), new EventTransitionBits(true,true,true), NotifyType.alarm,0);

                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Cooling Desired Temperature of " + zoneDescription)));
                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.covIncrement, new Real(BACnetUtils.incrementValue)));
                desiredTemperature.supportCovReporting(0.5f);
                TrendLogObject trendObject = new TrendLogObject(localDevice, desiredTemperature.getInstanceId() , zoneName + "_coolingDesiredTemp_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), desiredTemperature.getId(), PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(0.5f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);
            } else{
                setDesiredTemperature(localDevice,zoneAddress,zoneDevice, "cooling");
            }

            if (!localDevice.checkObjectByIDandType(instanceHeatID,ObjectType.analogValue)) {
                Log.i("Bacnet","Creating Heating Desired Temp:"+instanceHeatID);
                AnalogValueObject desiredTemperature = new AnalogValueObject(localDevice, instanceHeatID, zoneName + "_heatingDesiredTemp", (float)getDesiredTemp(zoneDevice,"heating"), EngineeringUnits.degreesFahrenheit, false);
                desiredTemperature.supportCommandable(72);
                desiredTemperature.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,getMinUserTempLimits("heating"),(float) (getMaxUserTempLimits("heating")-hdb),(float)hdb,
                        BACnetUtils.buildingLimitMax,BACnetUtils.buildingLimitMin,new LimitEnable(true,true), new EventTransitionBits(true,true,true), NotifyType.alarm,0);
                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Heating Desired Temperature of " + zoneDescription)));
                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.covIncrement, new Real(BACnetUtils.incrementValue)));
                desiredTemperature.supportCovReporting(0.5f);
                TrendLogObject trendObject = new TrendLogObject(localDevice, desiredTemperature.getInstanceId(), zoneName + "_heatingDesiredTemp_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), desiredTemperature.getId(), PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(0.5f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);
            } else{
                setDesiredTemperature(localDevice,zoneAddress,zoneDevice, "heating");
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void setDesiredTemperature(LocalDevice localDevice, int zoneAddress, Device equip, String dtemp){
        try {
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            if(dtemp.contains("cooling")) {
                int instanceCoolID = addressNumber + BACnetUtils.desiredTempCooling;
                if (localDevice.checkObjectByID(instanceCoolID)) {
                    BACnetObject baCnetObject = localDevice.getObjectByID(instanceCoolID);
                    double desiredCoolingTemp = getDesiredTemp(equip, dtemp);
                    double cdb = getDeadband(equip,"cooling");
                    Log.i("Bacnet", "Updating Desired Temp:" + instanceCoolID + " Value:" + desiredCoolingTemp);
                    if(baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                        baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real((float) desiredCoolingTemp)));
                        baCnetObject.writePropertyInternal(PropertyIdentifier.deadband,new Real((float)cdb));
                    }
                }
            }
            if(dtemp.contains("heating")) {
                int instanceHeatID = addressNumber + BACnetUtils.desiredTempHeating;
                if (localDevice.checkObjectByID(instanceHeatID)) {
                    BACnetObject baCnetObject = localDevice.getObjectByID(instanceHeatID);
                    double desiredHeatingTemp = getDesiredTemp(equip, "heating");
                    double hdb = getDeadband(equip,"heating");
                    Log.i("Bacnet", "Updating Desired Temp:" + instanceHeatID + " Value:" + desiredHeatingTemp);
                    if(baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                        baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real((float) desiredHeatingTemp)));
                        baCnetObject.writePropertyInternal(PropertyIdentifier.deadband,new Real((float)hdb));
                    }
                }
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void updateDamperPosition(LocalDevice localDevice, int zoneAddress,String zoneName, String zoneDescription,Device zoneDevice){
        try {
            AnalogValueObject damperPosition;
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.damperPos;
            if (!localDevice.checkObjectByID(instanceID)) {
                Log.i("Bacnet","Creating Damper Position:"+instanceID);
                damperPosition = new AnalogValueObject(localDevice, instanceID, zoneName + "_damperPos", (float)getDamperPos(zoneDevice), EngineeringUnits.percent, false);
                damperPosition.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Damper position of " + zoneDescription)));
                damperPosition.supportCommandable(1);

                TrendLogObject trendObject = new TrendLogObject(localDevice, damperPosition.getInstanceId(), zoneName + "_damperPos_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), damperPosition.getId(), PropertyIdentifier.presentValue), 0, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);

                readAccessSpecifications.add(new ReadAccessSpecification(damperPosition.getId(),
                        new SequenceOf<>( new PropertyReference(PropertyIdentifier.presentValue))));

                listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(damperPosition.getId(), PropertyIdentifier.presentValue, null, null));
            }
            else{
                setDamperPosition(localDevice,zoneAddress,zoneDevice);
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void setDamperPosition(LocalDevice localDevice, int zoneAddress, Device equip){
        try {
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.damperPos;
            if (localDevice.checkObjectByID(instanceID)) {
                BACnetObject baCnetObject = localDevice.getObjectByID(instanceID);
                double damperPos = getDamperPos(equip);
                Log.i("Bacnet","Updating Damper Position:"+instanceID+" Value:"+damperPos);
                baCnetObject.setOverridden(false);
                if(baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real((int) damperPos)));
                }
                baCnetObject.setOverridden(true);
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void updateSensorData(LocalDevice localDevice, int zoneAddress,String zoneName, String zoneDescription,Device zoneDevice){
        try {
            AnalogValueObject sensorTypeName;
            int addressNumber = Integer.parseInt(zoneAddress+"00");

            for(int i =1 ;i < SensorType.values().length; i++) {
                int instanceID = addressNumber + BACnetUtils.HUMIDITY_SENSOR_VALUE + i - 1;
                double sensorVal;
                EngineeringUnits engUnits;
                if (!localDevice.checkObjectByID(instanceID)) {
                    Log.i("Bacnet", "Creating Sensor Data for:"+SensorType.values()[i].getSensorPort()+"," + instanceID);
                    switch (SensorType.values()[i]){
                        case ENERGY_METER_LOW:
                        case ENERGY_METER_HIGH:
                            continue;
                        case HUMIDITY:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.percent;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Humidity value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID /*trendAddressNumber + BACnetUtils.currentTemp*/, zoneName + "_humiditySensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);


                            break;
                        case CO2:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.partsPerMillion;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Co2 Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_co2Sensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case CO:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.partsPerMillion;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("CO Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_coSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case NO:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.partsPerMillion;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("NO Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_noSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case VOC:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.partsPerBillion;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("VOC Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_vocSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case PRESSURE:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.inches;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Pressure Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_pressureSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case OCCUPANCY:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.noUnits;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Occupancy Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_occupancySensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case SOUND:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.decibels;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Sound Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_soundSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case CO2_EQUIVALENT:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.partsPerMillion;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Co2Equivalent Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_co2EqvSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case ILLUMINANCE:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.luxes;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Illuminance Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_illuminanceSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                        case UVI:
                            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[i].getSensorPort().getPortSensor()+" and equipRef == \"" + zoneDevice.getEquipRef() + "\"");
                            engUnits = EngineeringUnits.noUnits;
                            sensorTypeName = new AnalogValueObject(localDevice, instanceID, zoneName + "_"+SensorType.values()[i].getSensorPort().getPortSensor(), (float) sensorVal, engUnits, false);
                            sensorTypeName.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("UVI Sensor value of " + zoneDescription)));
                            sensorTypeName.supportCommandable(1);
                            sensorTypeName.setOverridden(true);
                            new TrendLogObject(localDevice, instanceID, zoneName + "_uviSensor_trend",
                                    new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                                    new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), sensorTypeName.getId(),
                                            PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                                    .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                                    .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS)
                                    .writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                            break;
                    }
                } else {
                    initSensorValues(localDevice, zoneAddress, zoneDevice, i);
                }
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void initSensorValues(LocalDevice localDevice, int zoneAddress, Device zone, int sensornumber){
        try {
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            double sensorVal = 0.0;
            sensorVal = CCUHsApi.getInstance().readHisValByQuery("point and sensor and current and his and "+ SensorType.values()[sensornumber].getSensorPort().getPortSensor()+" and equipRef == \"" + zone.getEquipRef() + "\"");
            int instanceID = addressNumber + BACnetUtils.HUMIDITY_SENSOR_VALUE + sensornumber - 1;
            if (localDevice.checkObjectByID(instanceID)) {
                BACnetObject baCnetObject = localDevice.getObjectByID(instanceID);
                Log.i("Bacnet","Updating Sensor Value Id:"+instanceID+" Value:"+sensorVal+","+SensorType.values()[sensornumber].getSensorPort().getPortSensor());
                baCnetObject.setOverridden(false);// to Writteable
                baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real((float)sensorVal)));
                baCnetObject.setOverridden(true);// to make not-writteable
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void updateSupplyAirTemperature(LocalDevice localDevice, int zoneAddress,String zoneName, String zoneDescription,Device zoneDevice){
        try {
            AnalogValueObject supplyAirTemperatureObject;
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.supplyAirTemperature;
            if (!localDevice.checkObjectByID(instanceID)) {
                Log.i("Bacnet","Creating supply Air Temp"+instanceID);
                supplyAirTemperatureObject = new AnalogValueObject(localDevice, instanceID, zoneName + "_supplyAirTemperature", (float)getSupplyAirTemp(zoneDevice), EngineeringUnits.degreesFahrenheit, false);
                supplyAirTemperatureObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Supply Air Temperature of " + zoneDescription)));
                supplyAirTemperatureObject.supportCommandable(1);

                TrendLogObject trendObject = new TrendLogObject(localDevice, supplyAirTemperatureObject.getInstanceId(), zoneName + "_supplyAirTemperature_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), supplyAirTemperatureObject.getId(), PropertyIdentifier.presentValue), 0, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);

                readAccessSpecifications.add(new ReadAccessSpecification(supplyAirTemperatureObject.getId(),
                        new SequenceOf<>( new PropertyReference(PropertyIdentifier.presentValue))));

                listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(supplyAirTemperatureObject.getId(), PropertyIdentifier.presentValue, null, null));
            }
            else{
                setSupplyAirPresentValue(localDevice,zoneAddress,zoneDevice);
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void setSupplyAirPresentValue(LocalDevice localDevice, int zoneAddress, Device zone){
        try {
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.supplyAirTemperature;
            if (localDevice.checkObjectByID(instanceID)) {
                AnalogValueObject baCnetObject = (AnalogValueObject)localDevice.getObjectByID(instanceID);
                double supplyAirTemp = getSupplyAirTemp(zone);
                Log.i("Bacnet","Updating supplyAirTemp:"+instanceID+" Value:"+supplyAirTemp);
                if(baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    baCnetObject.writePropertyInternal(PropertyIdentifier.presentValue, new Real((float) supplyAirTemp));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void updateReheatCoil(LocalDevice localDevice, int zoneAddress, String zoneName, String zoneDescription, Device zoneDevice) {
        try {
            AnalogValueObject reheatCoilAV;
            int addressNumber = Integer.parseInt(zoneAddress + "00");
            int instanceID = addressNumber + BACnetUtils.reheatCoil;
            if (!localDevice.checkObjectByID(instanceID)) {
                Log.i("Bacnet", "Creating Reheat Coil:" + instanceID);
                ReheatType reheatType = getReheatCoilType(zoneDevice.getEquipRef());
                reheatCoilAV = new AnalogValueObject(localDevice, instanceID, zoneName + "_reheatCoil_"+reheatType.displayName, (float) getReheatCoil(zoneDevice), EngineeringUnits.percent, false);
                reheatCoilAV.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Reheat Coil of " + zoneDescription)));
                reheatCoilAV.supportCommandable(1);
                TrendLogObject trendObject = new TrendLogObject(localDevice, reheatCoilAV.getInstanceId(), zoneName + "_reheatCoil_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), reheatCoilAV.getId(), PropertyIdentifier.presentValue), 0, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov(new Real(1f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);
                readAccessSpecifications.add(new ReadAccessSpecification(reheatCoilAV.getId(),
                        new SequenceOf<>(new PropertyReference(PropertyIdentifier.presentValue))));
                listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(reheatCoilAV.getId(), PropertyIdentifier.presentValue, null, null));
            } else {
                setReheatCoil(localDevice, zoneAddress, zoneDevice);
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void setReheatCoil(LocalDevice localDevice, int zoneAddress, Device equip) {
        try {
            int addressNumber = Integer.parseInt(zoneAddress + "00");
            int instanceID = addressNumber + BACnetUtils.reheatCoil;
            if (localDevice.checkObjectByID(instanceID)) {
                BACnetObject baCnetObject = localDevice.getObjectByID(instanceID);
                double reheatCoil = getReheatCoil(equip);
                Log.i("Bacnet", "Updating Reheat Coil:" + instanceID + " Value:" + reheatCoil);
                baCnetObject.setOverridden(false);
                if (baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real((int) reheatCoil)));
                }
                baCnetObject.setOverridden(true);
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }
    public double getMaxBuildingLimits(){
        return TunerUtil.readBuildingTunerValByQuery("building and limit and max");
   }
    public double getMinBuildingLimits(){
        return TunerUtil.readBuildingTunerValByQuery("building and limit and min");
    }
   public double getZoneAvgCurrentTemp(Device d){
       double currentTempSensor = 0;
       double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
       double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");

       double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
       double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + d.getEquipRef() + "\"");
       if ((avgTemp <= (buildingLimitMax + tempDeadLeeway)) && (avgTemp >= (buildingLimitMin - tempDeadLeeway))) {
           currentTempSensor = (currentTempSensor + avgTemp);
       }
       if (currentTempSensor > 0 ) {
           DecimalFormat decimalFormat = new DecimalFormat("#.#");
           currentTempSensor = Double.parseDouble(decimalFormat.format(currentTempSensor));
       }
       if(currentTempSensor > 0) {
           return currentTempSensor;
       }
        return 0;
   }
    public double getDesiredTemp(Device equip, String tags){
        try {
            return CCUHsApi.getInstance().readHisValByQuery("zone and point and desired and air and temp and "+tags+" and equipRef == \""+equip.getEquipRef()+"\"");
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public double getDeadband(Device equip, String tags){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = CCUHsApi.getInstance().read("point and tuner and deadband and base and "+tags+" and equipRef == \""+equip.getEquipRef()+"\"");
        if((cdb != null) && (cdb.get("id") != null) ) {

            ArrayList values = hayStack.readPoint(cdb.get("id").toString());
            if (values != null && values.size() > 0) {
                for (int l = 1; l <= values.size(); l++) {
                    HashMap valMap = ((HashMap) values.get(l - 1));
                    if (valMap.get("val") != null) {
                        return Double.parseDouble(valMap.get("val").toString());
                    }
                }
            }
        }
        return 0;
    }
   public double getDamperPos(Device equip){
       try {
           return CCUHsApi.getInstance().readHisValByQuery("zone and point and damper and base and equipRef == \"" + equip.getEquipRef() + "\"");
       } catch (Exception e) {
           e.printStackTrace();
           return 0;
       }
   }
    public double getSupplyAirTemp(Device equip){
        try {
            return CCUHsApi.getInstance().readHisValByQuery("zone and point and entering and air and temp and sensor and equipRef == \"" + equip.getEquipRef() + "\"");
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public double getReheatCoil(Device equip) {
        try {
            return CCUHsApi.getInstance().readHisValByQuery("point and zone and reheat and cmd and equipRef == \"" + equip.getEquipRef() + "\"");
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    private static ReheatType getReheatCoilType(String equipRef){
        double reheatTypeValue = CCUHsApi.getInstance().readDefaultVal("point and zone and reheat and type and config and equipRef == \"" + equipRef + "\"");
        return ReheatType.values()[(int)reheatTypeValue];
    }
    private static float getMaxUserTempLimits(String tag){
        return (float) TunerUtil.readBuildingTunerValByQuery("user and limit and max and "+tag);
    }

    private static float getMinUserTempLimits(String tag){
        return (float) TunerUtil.readBuildingTunerValByQuery("user and limit and min and "+tag);
    }

    public void deleteZonePoints(LocalDevice localDevice, short zoneAddress) {
        Device zoneDevice = HSUtil.getDevice(zoneAddress);
        if (zoneDevice != null) {
            Equip zoneEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + zoneAddress + "\"")).build();
            if (!zoneEquip.getMarkers().contains("pid") && !zoneEquip.getMarkers().contains("emr")) {
                int prefixAddress = Integer.parseInt((int) zoneAddress + "00");
                boolean commonPointsDeleted = deleteCommonZonePoints(localDevice, prefixAddress);
                boolean deleteZonePoints = false;
                if (zoneEquip.getMarkers().contains("vav")) {
                    deleteZonePoints = deleteVAVObjects(localDevice, prefixAddress);
                }
                if (zoneEquip.getMarkers().contains("smartstat")) {
                    deleteZonePoints = deleteSmartStatPoints(localDevice, prefixAddress);
                }
                if (zoneEquip.getMarkers().contains("sse")) {
                    deleteZonePoints = true;
                }
                if (commonPointsDeleted && deleteZonePoints) {
                    localDevice.incrementDatabaseRevision();
                }
            }
        }
    }


    public boolean deleteVAVObjects(LocalDevice localDevice, int prefixAddress) {
        int supplyAirObjectID = prefixAddress + BACnetUtils.supplyAirTemperature;
        int reheatCoilObjectID = prefixAddress + BACnetUtils.reheatCoil;
        boolean isDeleted = false;
        try {
            if (localDevice.checkObjectByID(supplyAirObjectID)) {
                BACnetObject trendSupplyAir = localDevice.getObjectByIDandType(supplyAirObjectID, ObjectType.trendLog);
                localDevice.removeByObject(trendSupplyAir);
                localDevice.removeObjectByID(supplyAirObjectID);
                isDeleted = true;
            }
            if (localDevice.checkObjectByID(reheatCoilObjectID)) {
                BACnetObject trendReheat = localDevice.getObjectByIDandType(reheatCoilObjectID, ObjectType.trendLog);
                localDevice.removeByObject(trendReheat);
                localDevice.removeObjectByID(reheatCoilObjectID);
                isDeleted = true;
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
        return isDeleted;
    }

    public boolean deleteCommonZonePoints(LocalDevice localDevice, int prefixAddress) {
        int dtCoolObjectID = prefixAddress + BACnetUtils.desiredTempCooling;
        int dtHeatObjectID = prefixAddress + BACnetUtils.desiredTempHeating;
        int ctObjectID = prefixAddress + BACnetUtils.currentTemp;
        int damperObjectID = prefixAddress + BACnetUtils.damperPos;
        boolean isDeleted = false;
        try {
            if (localDevice.checkObjectByID(dtCoolObjectID)) {
                BACnetObject trendDtCool = localDevice.getObjectByIDandType(dtCoolObjectID, ObjectType.trendLog);
                localDevice.removeByObject(trendDtCool);
                localDevice.removeObjectByID(dtCoolObjectID);
                isDeleted = true;
            }
            if (localDevice.checkObjectByID(dtHeatObjectID)) {
                BACnetObject trendDtHeat = localDevice.getObjectByIDandType(dtHeatObjectID, ObjectType.trendLog);
                localDevice.removeByObject(trendDtHeat);
                localDevice.removeObjectByID(dtHeatObjectID);
                isDeleted = true;
            }
            if (localDevice.checkObjectByID(ctObjectID)) {
                BACnetObject trendCurrentTemp = localDevice.getObjectByIDandType(ctObjectID, ObjectType.trendLog);
                localDevice.removeByObject(trendCurrentTemp);
                localDevice.removeObjectByID(ctObjectID);
                isDeleted = true;
            }
            if (localDevice.checkObjectByID(damperObjectID)) {
                BACnetObject trendDamper = localDevice.getObjectByIDandType(damperObjectID, ObjectType.trendLog);
                localDevice.removeByObject(trendDamper);
                localDevice.removeObjectByID(damperObjectID);
                isDeleted = true;
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
        return isDeleted;
    }

    public boolean deleteSmartStatPoints(LocalDevice localDevice, int prefixAddress) {
        boolean isDeleted = false;
        for (int i = 1; i < SensorType.values().length; i++) {
            int instanceID = prefixAddress + BACnetUtils.HUMIDITY_SENSOR_VALUE + i - 1;
            try {
                if (localDevice.checkObjectByID(instanceID)) {
                    BACnetObject trendObject = localDevice.getObjectByIDandType(instanceID, ObjectType.trendLog);
                    localDevice.removeByObject(trendObject);
                    localDevice.removeObjectByID(instanceID);
                    isDeleted = true;
                } else {
                    isDeleted = false;
                }
            } catch (BACnetServiceException e) {
                e.printStackTrace();
            }
        }
        return isDeleted;
    }
}
