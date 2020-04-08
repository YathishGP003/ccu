package a75f.io.device.bacnet;

import android.util.Log;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BinaryValueObject;
import com.renovo.bacnet4j.obj.MultistateValueObject;
import com.renovo.bacnet4j.obj.NotificationClassObject;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

import a75f.io.api.haystack.CCUHsApi;

public class BACnetAlerts {
    NotificationClassObject FATAL = null;
    NotificationClassObject ERROR = null;
    NotificationClassObject WARN = null;
    NotificationClassObject PRIORITY = null;

    public BACnetAlerts(LocalDevice localDevice)
    {
        try {
            if (!localDevice.checkObjectByID(BACnetUtils.ALERT_FATAL)) {
                FATAL = new NotificationClassObject(localDevice, BACnetUtils.ALERT_FATAL, BACnetUtils.ALERT_FATAL_TITLE, 80, 80, 95, new EventTransitionBits(true, true, true));
                FATAL.writeProperty(null, new PropertyValue(PropertyIdentifier.description, new CharacterString("FATAL ALARM - 75F" )));
            }else{
                FATAL = (NotificationClassObject)localDevice.getObjectByID(BACnetUtils.ALERT_FATAL);
            }
            if (!localDevice.checkObjectByID(BACnetUtils.ALERT_ERROR)) {
                ERROR = new NotificationClassObject(localDevice, BACnetUtils.ALERT_ERROR, BACnetUtils.ALERT_ERROR_TITLE, 150, 150, 190, new EventTransitionBits(true, true, true));
                ERROR.writeProperty(null, new PropertyValue(PropertyIdentifier.description, new CharacterString("ERROR ALARM - 75F" )));
            }else {
                ERROR = (NotificationClassObject)localDevice.getObjectByID(BACnetUtils.ALERT_ERROR);
            }
            if (!localDevice.checkObjectByID(BACnetUtils.ALERT_WARN)) {
                WARN = new NotificationClassObject(localDevice, BACnetUtils.ALERT_WARN, BACnetUtils.ALERT_WARN_TITLE, 220, 220, 220, new EventTransitionBits(true, true, true));
                WARN.writeProperty(null, new PropertyValue(PropertyIdentifier.description, new CharacterString("WARN ALARM - 75F" )));
            }else {
                WARN = (NotificationClassObject)localDevice.getObjectByID(BACnetUtils.ALERT_WARN);
            }if (!localDevice.checkObjectByID(BACnetUtils.ALERT_PRIORITY)) {
                PRIORITY = new NotificationClassObject(localDevice, BACnetUtils.ALERT_PRIORITY, BACnetUtils.ALERT_PRIORITY_TITLE, 150, 80, 220, new EventTransitionBits(true, true, true));
                PRIORITY.writeProperty(null, new PropertyValue(PropertyIdentifier.description, new CharacterString("PRIORITY ALARM - 75F" )));
            }else {
                PRIORITY = (NotificationClassObject)localDevice.getObjectByID(BACnetUtils.ALERT_PRIORITY);
            }

            //Alert Conditions
            getBatteryLevel(localDevice);
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }


    private void getBatteryLevel(LocalDevice localDevice){
        Double batterLevel =  (Double) CCUHsApi.getInstance().readHisValByQuery("battery and level");
        Double status =  (Double) CCUHsApi.getInstance().readHisValByQuery("charging and status");
        Double isConnected =  (Double) CCUHsApi.getInstance().readHisValByQuery("power and connected");
        Log.i("Bacnet","Battery Level:"+batterLevel+" Status:"+status+" isConnected:"+isConnected);
        MultistateValueObject deviceBattery = null;
        UnsignedInteger presentValue = null;
        try {
            if (!localDevice.checkObjectByID(BACnetUtils.BATTERY_STATUS)) {

                BinaryValueObject binaryValueObject = new BinaryValueObject(localDevice,101,"BV for Alarm", BinaryPV.active,false);
                binaryValueObject.writeProperty(null, new PropertyValue(PropertyIdentifier.description, new CharacterString("BV Object to test Alarm")));
                binaryValueObject.supportIntrinsicReporting(0,BACnetUtils.ALERT_WARN,BinaryPV.inactive,new EventTransitionBits(true,true,true),NotifyType.alarm,0);
                binaryValueObject.supportCovReporting();

                Log.i("Bacnet", "Checking Battery:" + BACnetUtils.BATTERY_STATUS);
                deviceBattery = new MultistateValueObject(localDevice,BACnetUtils.BATTERY_STATUS, "CCU Battery", 3,null,1, false);
                deviceBattery.writeProperty(null, new PropertyValue(PropertyIdentifier.stateText, new BACnetArray<>(new CharacterString("Healthy-75%"), new CharacterString("ERROR-50%"), new CharacterString("FATAL-25%"))));
                deviceBattery.writePropertyInternal(PropertyIdentifier.numberOfStates, new UnsignedInteger(3));
                deviceBattery.writeProperty(null, new PropertyValue(PropertyIdentifier.description, new CharacterString("CCU Battery")));
                deviceBattery.supportCovReporting();
                BACnetArray<UnsignedInteger> alarmValues = new BACnetArray<>(new UnsignedInteger(2));
                BACnetArray<UnsignedInteger> faultValues = new BACnetArray<>(new UnsignedInteger(3));
                deviceBattery.supportIntrinsicReporting(1,BACnetUtils.ALERT_PRIORITY,alarmValues,faultValues, new EventTransitionBits(true,true,true), NotifyType.alarm,1);
                //if(status == 1.0){
                if(batterLevel > 75.0){
                    //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(220), new UnsignedInteger(220), new UnsignedInteger(220)));
                    //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_WARN));
                    //deviceBattery.writeProperty(null, new PropertyValue(PropertyIdentifier.notificationClass, WARN.getId()));
                    presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                    if(!presentValue.equals(new UnsignedInteger(1))) {
                        deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(1));
                        deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Battery is Healthy"));
                    }
                }if(batterLevel<75.0 && batterLevel > 50.0){
                    //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(220), new UnsignedInteger(220), new UnsignedInteger(220)));
                    //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, WARN.getId());
                    //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_WARN));
                    //deviceBattery.writeProperty(null, new PropertyValue(PropertyIdentifier.notificationClass, WARN.getId()));
                    presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                    if(!presentValue.equals(new UnsignedInteger(1))) {
                        deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Info - Battery is Healthy"));
                        deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(1));
                    }
                }if(batterLevel<50.0 && batterLevel > 25.0){
                    //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(150), new UnsignedInteger(150), new UnsignedInteger(190)));
                    //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, ERROR.getId());
                    //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_ERROR));
                    //deviceBattery.writeProperty(null, new PropertyValue(PropertyIdentifier.notificationClass, ERROR.getId()));
                    presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                    if(!presentValue.equals(new UnsignedInteger(2))) {
                        deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Error - Battery is less than 50%"));
                        deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(2));
                    }
                }if(batterLevel<25.0){
                    //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(80), new UnsignedInteger(80), new UnsignedInteger(95)));
                    //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_FATAL));
                    //deviceBattery.writeProperty(null, new PropertyValue(PropertyIdentifier.notificationClass, FATAL.getId()));
                    presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                    if(!presentValue.equals(new UnsignedInteger(3))) {
                        deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Fatal - Battery is less than 25%"));
                        deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(3));
                    }
                }
                   /* }else {
                        deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(5));
                    }*/
            }
            else {
                deviceBattery = (MultistateValueObject)localDevice.getObjectByID(BACnetUtils.BATTERY_STATUS);
                if(deviceBattery.readProperty(PropertyIdentifier.outOfService).equals(Boolean.FALSE)) {
                    //if(status == 1.0){
                    if (batterLevel > 75.0) {
                        //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(220), new UnsignedInteger(220), new UnsignedInteger(220)));
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, WARN.getId());
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_WARN));
                        presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                        if (!presentValue.equals(new UnsignedInteger(1))) {
                            deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Info - Battery is Healthy"));
                            deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(1));
                            Log.i("Bacnet", "Battery Level Updating Battery:" + batterLevel + "-1");
                        }
                    }
                    if (batterLevel < 75.0 && batterLevel > 50.0) {
                        //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(220), new UnsignedInteger(220), new UnsignedInteger(220)));
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, WARN.getId());
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_WARN));
                        presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                        if (!presentValue.equals(new UnsignedInteger(1))) {
                            deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Info - Battery is Healthy"));
                            deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(1));
                            Log.i("Bacnet", "Battery Level Updating Battery:" + batterLevel + "-1");
                        }
                    }
                    if (batterLevel < 50.0 && batterLevel > 25.0) {
                        //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(150), new UnsignedInteger(150), new UnsignedInteger(190)));
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, ERROR.getId());
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_ERROR));
                        presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                        if (!presentValue.equals(new UnsignedInteger(2))) {
                            deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Error - Battery is less than 50%"));
                            deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(2));
                            Log.i("Bacnet", "Battery Level Updating Battery:" + batterLevel + "-2");
                        }
                    }
                    if (batterLevel < 25.0) {
                        //WARN.writePropertyInternal(PropertyIdentifier.priority, new BACnetArray<>(new UnsignedInteger(80), new UnsignedInteger(80), new UnsignedInteger(95)));
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, FATAL.getId());
                        //deviceBattery.writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(BACnetUtils.ALERT_FATAL));
                        presentValue = deviceBattery.readProperty(PropertyIdentifier.presentValue);
                        if (!presentValue.equals(new UnsignedInteger(3))) {
                            deviceBattery.writePropertyInternal(PropertyIdentifier.eventMessageTexts, new CharacterString("Fatal - Battery is less than 25%"));
                            deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(3));

                            Log.i("Bacnet", "Battery Level Updating Battery:" + batterLevel + "-3");
                        }
                    }
                }
                /*}else {
                    deviceBattery.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(5));
                    Log.i("Bacnet", "Battery Level Updating Battery:" + batterLevel+"-5");
                }*/
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }

    }
}
