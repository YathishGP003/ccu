package a75f.io.device.bacnet;

import android.util.Log;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AnalogValueObject;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.BinaryValueObject;
import com.renovo.bacnet4j.obj.GroupObject;
import com.renovo.bacnet4j.obj.NotificationClassObject;
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
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

public class ZonePoints {


    public static BinaryValueObject occupancyStatus; //setback, setpoint, precondition, forcedoccupied, vacation
    public static BinaryValueObject conditioningStatus; //deadband,cooling,heating,tempdead
    public static GroupObject zoneGroup;
    public static BACnetObject globalZoneGroup = null;
    public SequenceOf<ReadAccessSpecification> readAccessSpecifications= new SequenceOf<>();
    public DeviceObjectPropertyReference deviceObjectPropertyReference = null;
    public SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>();

    public ZonePoints() {
    }
    public ZonePoints(LocalDevice localDevice,Zone zone,String objectName)
    {
        Log.i("ad","LocalDevice:"+localDevice.getInstanceNumber());
        Device zoneDevice =  HSUtil.getDevices(zone.getId()).get(0);
        //We will handle only single module per zone
        if(zoneDevice!=null) {
            String zoneName = zone.getDisplayName();
            String zoneDescription = objectName;
            int zoneAddress = Integer.parseInt(zoneDevice.getAddr());
            Equip zoneEquip =HSUtil.getEquipFromZone(zone.getId());
            if (!zoneEquip.getMarkers().contains("pid") && !zoneEquip.getMarkers().contains("emr")) {
                updateCurrentTemp(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                updateDesiredTemp(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                if (zoneEquip.getMarkers().contains("vav") || zoneEquip.getMarkers().contains("dab")) {
                    updateDamperPosition(localDevice, zoneAddress, zoneName, zoneDescription, zoneDevice);
                }
            }
            if(zoneDevice.getMarkers().contains("smartstat")) {
                updateSensorData(localDevice,zoneAddress,zoneName,zoneDescription,zoneDevice);
            }
        }
    }


    public void updateCurrentTemp(LocalDevice localDevice, int zoneAddress,String zoneName, String zoneDescription,Device zoneDevice){
        try {
            AnalogValueObject currentTemperature;
            NotificationClassObject notifClass;
            int addressNumber = Integer.parseInt(zoneAddress+"00");
            int instanceID = addressNumber + BACnetUtils.currentTemp;
            Log.i("Bacnet","Checking Current Temp:"+instanceID);
            if (!localDevice.checkObjectByID(instanceID)) {
                Log.i("Bacnet","Creating Current Temp:"+instanceID);
                currentTemperature = new AnalogValueObject(localDevice, instanceID, zoneName + "_currentTemp", (float) getZoneAvgCurrentTemp(zoneDevice), EngineeringUnits.degreesFahrenheit, false);
                currentTemperature.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,(float)getMaxBuildingLimits(),(float)getMinBuildingLimits(),1,
                        (float)getMaxBuildingLimits()+10,(float)getMinBuildingLimits()-10,new LimitEnable(true,true), new EventTransitionBits(true,true,true), NotifyType.alarm,0);
                currentTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Current Temperature of " + zoneDescription)));
                //currentTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.covIncrement, new Real(BACnetUtils.incrementValue)));

                //currentTemperature.supportWritable();
                //currentTemperature.makePresentValueReadOnly();
                currentTemperature.supportCovReporting(BACnetUtils.currentTempCOV);

                //currentTemperature.setOverridden(true);

                //TODO need to update the upper and lower limit for current temp which is building limit breach concepts.
                //int trendAddressNumber = Integer.parseInt(zoneAddress+""+currentTemperature.getInstanceId());
                //LinkedListLogBuffer logBuffer = new LinkedListLogBuffer<LogRecord>();
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
                //TrendLogObject trendLogObject = (TrendLogObject)localDevice.getObjectByID(instanceID);
                //trendLogObject.withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);
                //trendLogObject.withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(0.5f)));
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
            //double hdb = StandaloneTunerUtil.getStandaloneHeatingDeadband(zoneDevice.getEquipRef());
            //double cdb = StandaloneTunerUtil.getStandaloneCoolingDeadband(zoneDevice.getEquipRef());
            double hdb = getDeadband(zoneDevice,"heating");
            double cdb = getDeadband(zoneDevice,"cooling");
            Log.i("BacnetDB","Heating Deadband:"+hdb+" Cooling Deadband:"+cdb+" EquipRef:"+zoneDevice.getEquipRef());
            if (!localDevice.checkObjectByID(instanceCoolID)) {
                Log.i("Bacnet","Creating Cooling Desired Temp:"+instanceCoolID);
                //Todo re-verfify deadbands for heating and cooling DT
                AnalogValueObject desiredTemperature = new AnalogValueObject(localDevice, instanceCoolID, zoneName + "_coolingDesiredTemp", (float)getDesiredTemp(zoneDevice,"cooling"), EngineeringUnits.degreesFahrenheit, false);
                desiredTemperature.supportCommandable(72);
                //notifClass = new NotificationClassObject(localDevice, ncIDCooling, zoneName+"_NC_coolingDesiredTemp", 100, 5, 200, new EventTransitionBits(true, true, true));
                desiredTemperature.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,(float) (getMaxUserTempLimits(zoneDevice.getEquipRef(), cdb,"cooling")+cdb),getMinUserTempLimits(zoneDevice.getEquipRef(), hdb,"cooling"),(float)cdb,
                        BACnetUtils.buildingLimitMax,BACnetUtils.buildingLimitMin,new LimitEnable(true,true), new EventTransitionBits(true,true,true), NotifyType.alarm,0);

                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Cooling Desired Temperature of " + zoneDescription)));
                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.covIncrement, new Real(BACnetUtils.incrementValue)));
                //desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.relinquishDefault, new Real(72)));
                //desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.priorityArray, new PriorityArray()));
                //desiredTemperature.writePropertyInternal(PropertyIdentifier.relinquishDefault,new Real (72));
                desiredTemperature.supportCovReporting(0.5f);
                TrendLogObject trendObject = new TrendLogObject(localDevice, desiredTemperature.getInstanceId() , zoneName + "_coolingDesiredTemp_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), desiredTemperature.getId(), PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(0.5f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);


                Log.i("Bacnet","Creating notifClass for coolingDesiredTemp:"+desiredTemperature.getInstanceId());
            } else{
                setDesiredTemperature(localDevice,zoneAddress,zoneDevice, "cooling");
            }

            if (!localDevice.checkObjectByID(instanceHeatID)) {
                Log.i("Bacnet","Creating Heating Desired Temp:"+instanceHeatID);
                AnalogValueObject desiredTemperature = new AnalogValueObject(localDevice, instanceHeatID, zoneName + "_heatingDesiredTemp", (float)getDesiredTemp(zoneDevice,"heating"), EngineeringUnits.degreesFahrenheit, false);
                desiredTemperature.supportCommandable(72);
                //notifClass2 = new NotificationClassObject(localDevice, ncIDHeating, zoneName+"_NC_heatingDesiredTemp", 100, 5, 200, new EventTransitionBits(true, true, true));
                desiredTemperature.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,getMinUserTempLimits(zoneDevice.getEquipRef(), cdb,"heating"),(float) (getMaxUserTempLimits(zoneDevice.getEquipRef(), hdb,"heating")-hdb),(float)hdb,
                        BACnetUtils.buildingLimitMax,BACnetUtils.buildingLimitMin,new LimitEnable(true,true), new EventTransitionBits(true,true,true), NotifyType.alarm,0);
                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Heating Desired Temperature of " + zoneDescription)));
                desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.covIncrement, new Real(BACnetUtils.incrementValue)));
                //desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.relinquishDefault, new Real(72)));
                //desiredTemperature.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.priorityArray, new PriorityArray()));
                //desiredTemperature.writePropertyInternal(PropertyIdentifier.relinquishDefault,new Real (72));
                //desiredTemperature.writePropertyInternal(PropertyIdentifier.priorityArray,new PriorityArray());
                desiredTemperature.supportCovReporting(0.5f);
                TrendLogObject trendObject = new TrendLogObject(localDevice, desiredTemperature.getInstanceId(), zoneName + "_heatingDesiredTemp_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), desiredTemperature.getId(), PropertyIdentifier.presentValue), BACnetUtils.logInterval, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(0.5f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);

                Log.i("Bacnet","Creating notifClass for heatingDesiredTemp:"+desiredTemperature.getInstanceId());
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
                    //baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.deadband, new Real((int) 2)));
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
                    //baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.deadband, new Real((int) 2)));
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
                //damperPosition.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.deadband, new Real(5)));
                damperPosition.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("Damper position of " + zoneDescription)));
                damperPosition.supportCommandable(1);
                //damperPosition.setOverridden(true);

                TrendLogObject trendObject = new TrendLogObject(localDevice, damperPosition.getInstanceId(), zoneName + "_damperPos_trend", new LinkedListLogBuffer<LogRecord>(), true, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), damperPosition.getId(), PropertyIdentifier.presentValue), 0, false, BACnetUtils.bufferSize)
                        .withCov(BACnetUtils.covResubscriptionInterval, new ClientCov( new Real(1f)))
                        .withPolled(BACnetUtils.logInterval, TimeUnit.SECONDS, true, 2, TimeUnit.SECONDS);

                trendObject.writePropertyInternal(PropertyIdentifier.eventState,EventState.normal);
                trendObject.makePropertyReadOnly(PropertyIdentifier.logDeviceObjectProperty);

                readAccessSpecifications.add(new ReadAccessSpecification(damperPosition.getId(),
                        new SequenceOf<>( new PropertyReference(PropertyIdentifier.presentValue))));
                //readAccessSpecifications.add(new ReadAccessSpecification(trendObject.getId(),
                        //new SequenceOf<>( new PropertyReference(PropertyIdentifier.presentValue))));

                listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(damperPosition.getId(), PropertyIdentifier.presentValue, null, null));
                //listOfObjectPropertyReferences.add(new DeviceObjectPropertyReference(trendObject.getId(), PropertyIdentifier.presentValue, null, null));
                //localDevice.incrementDatabaseRevision(); //Todo Increase Database Revision
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
                baCnetObject.setOverridden(false);// to Writteable
                if(baCnetObject.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    baCnetObject.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.presentValue, new Real((int) damperPos)));
                }
                baCnetObject.setOverridden(true);// to make not-writteable
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
                double sensorVal = 0.0;
                EngineeringUnits engUnits = EngineeringUnits.noUnits;
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

   public double getCurrentTemp(Device equip){
       try {
           double currentTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and current and air and temp and equipRef == \"" + equip.getEquipRef() + "\"");
           return currentTemp;
       } catch (Exception e) {
           e.printStackTrace();
           return 0;
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
       double noTempSensor = 0;
       double buildingLimitMax =  TunerUtil.readBuildingTunerValByQuery("building and limit and max");
       double buildingLimitMin =  TunerUtil.readBuildingTunerValByQuery("building and limit and min");

       double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
       //int numOfDevices = HSUtil.getDevices(zone.getId()).size();
       //for (Device d: HSUtil.getDevices(zone.getId())) {
           double avgTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor and current and equipRef == \"" + d.getEquipRef() + "\"");
           if ((avgTemp <= (buildingLimitMax + tempDeadLeeway)) && (avgTemp >= (buildingLimitMin - tempDeadLeeway))) {
               currentTempSensor = (currentTempSensor + avgTemp);
           } else {
               noTempSensor++;
           }
       //}
       if (currentTempSensor > 0 /*&&  numOfDevices>1*/) {
           //currentTempSensor = currentTempSensor / (numOfDevices - noTempSensor);
           DecimalFormat decimalFormat = new DecimalFormat("#.#");
           currentTempSensor = Double.parseDouble(decimalFormat.format(currentTempSensor));
       }
       if(currentTempSensor > 0) {
           return currentTempSensor;
       }
        return 0;
   }
   public double getDesiredTemp(Device equip){
       try {
           double desiredTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and desired and air and temp and average and equipRef == \""+equip.getEquipRef()+"\"");
           return desiredTemp;
       } catch (Exception e) {
           e.printStackTrace();
           return 0;
       }
   }
    public double getDesiredTemp(Device equip, String tags){
        try {
            double desiredTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and desired and air and temp and "+tags+" and equipRef == \""+equip.getEquipRef()+"\"");
            return desiredTemp;
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
    public double getZoneDeadband(String tags,Device equip){
        try {
            double desiredTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and deadband and air and temp and average and equipRef == \""+equip.getEquipRef()+"\"");
            return desiredTemp;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    public double getUserLimits(String tags,Device equip){
        try {
            double desiredTemp = CCUHsApi.getInstance().readHisValByQuery("zone and point and deadband and air and temp and average and equipRef == \""+equip.getEquipRef()+"\"");
            return desiredTemp;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
   public double getDamperPos(Device equip){
       try {
           double damperPos = CCUHsApi.getInstance().readHisValByQuery("zone and point and damper and base and equipRef == \"" + equip.getEquipRef() + "\"");
           return damperPos;
       } catch (Exception e) {
           e.printStackTrace();
           return 0;
       }
   }

    private static float getMaxUserTempLimits(String equipId, double deadband, String tag){
        float maxVal = (float) TunerUtil.readBuildingTunerValByQuery("user and limit and max and "+tag) ;//(float)StandaloneTunerUtil.readTunerValByQuery("zone and "+tag+" and user and limit and max",equipId);
        return maxVal; /*+ (float)deadband;*/
    }

    private static float getMinUserTempLimits(String equipId, double deadband, String tag){
        float minVal =  (float) TunerUtil.readBuildingTunerValByQuery("user and limit and min and "+tag);//(float)StandaloneTunerUtil.readTunerValByQuery("zone and "+tag+" and user and limit and min",equipId);
        return minVal;/* - (float)deadband;*/
    }

   public void deleteZonePoints(LocalDevice localDevice, int zoneAddress){
       try {
           Log.i("BACnet","Zone Points Remove Module:"+zoneAddress);
           zoneAddress = Integer.parseInt(zoneAddress+"00");
           int dtCoolObjectID = zoneAddress + BACnetUtils.desiredTempCooling;
           int dtHeatObjectID = zoneAddress + BACnetUtils.desiredTempHeating;
           int ctObjectID = zoneAddress + BACnetUtils.currentTemp;
           int damperObjectID = zoneAddress + BACnetUtils.damperPos;

           int dtTrendObjectID = Integer.parseInt(dtCoolObjectID + "0") + BACnetUtils.desiredTempCooling;
           int dtHeatTrendObjectID = Integer.parseInt(dtHeatObjectID + "0") + BACnetUtils.desiredTempHeating;
           int ctTrendObjectID = Integer.parseInt(ctObjectID + "0") + BACnetUtils.currentTemp;
           //ToDo app crashing when tried to delete TREND OBJECT
           /*try {
               if (localDevice.checkObjectByID(dtTrendObjectID)) {
                   Log.i("Bacnet","dt Trend Deleted:"+dtTrendObjectID);
                   TrendLogObject trendLogObject = (TrendLogObject)localDevice.getObjectByID(dtTrendObjectID);
                   trendLogObject.setEnabled(false);
                   trendLogObject.setDeletable(true);
                   localDevice.removeObjectByID(dtTrendObjectID);
               }
               if (localDevice.checkObjectByID(ctTrendObjectID)) {
                   Log.i("Bacnet","Ct Trend Deleted:"+ctTrendObjectID);
                   TrendLogObject trendLogObject = (TrendLogObject)localDevice.getObjectByID(dtTrendObjectID);
                   trendLogObject.setEnabled(false);
                   trendLogObject.setDeletable(true);
                   localDevice.removeObjectByID(ctTrendObjectID);
               }
           } catch (InterruptedException e) {
               e.printStackTrace();
           }*/

           if (localDevice.checkObjectByID(dtCoolObjectID)) {
               localDevice.removeObjectByID(dtCoolObjectID);
               Log.i("Bacnet","Dt Deleted:"+dtCoolObjectID);
           }
           if (localDevice.checkObjectByID(dtHeatObjectID)) {
               localDevice.removeObjectByID(dtHeatObjectID);
               Log.i("Bacnet","Dt Deleted:"+dtHeatObjectID);
           }
           if (localDevice.checkObjectByID(ctObjectID)) {
               localDevice.removeObjectByID(ctObjectID);
               Log.i("Bacnet","Ct Deleted:"+ctObjectID);
           }
           if (localDevice.checkObjectByID(damperObjectID)) {
               localDevice.removeObjectByID(damperObjectID);
               Log.i("Bacnet","Damper Deleted:"+damperObjectID);
           }
           localDevice.incrementDatabaseRevision();
       } catch (BACnetServiceException e) {
           e.printStackTrace();
       }
   }
}
