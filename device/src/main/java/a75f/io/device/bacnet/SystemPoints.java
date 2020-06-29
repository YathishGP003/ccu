package a75f.io.device.bacnet;

import android.util.Log;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AnalogValueObject;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.BinaryValueObject;
import com.renovo.bacnet4j.obj.MultistateValueObject;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.enumerated.EngineeringUnits;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;

public class SystemPoints {

    //7 - Relay
    public static MultistateValueObject relay1; //01
    public static MultistateValueObject relay2; //02
    public static MultistateValueObject relay3; //03
    public static MultistateValueObject relay4; //04
    public static MultistateValueObject relay5; //05
    public static MultistateValueObject relay6; //06
    public static MultistateValueObject relay7; //07

    //4 - Analog Out
    public static AnalogValueObject analogOut1;
    public static AnalogValueObject analogOut2;
    public static AnalogValueObject analogOut3;
    public static AnalogValueObject analogOut4;

    //2 - Analog In
    MultistateValueObject analogIn1;
    MultistateValueObject analogIn2;

    //2 - Thermister Analog
    AnalogValueObject thermister1;
    AnalogValueObject thermister2;

    VavStagedRtu vavStagedRtu;

    public SystemPoints() {
    }

    public SystemPoints(LocalDevice localDevice) {
        ProfileType profileType = L.ccu().systemProfile.getProfileType();
        int addressNumber = Integer.parseInt(localDevice.getId().getInstanceNumber() + "00");
        String ccuName = CCUHsApi.getInstance().read("device and ccu").get("dis").toString();
        int id = Globals.getInstance().getApplicationContext().getResources().getIdentifier("hvac_stage_selector", "array", Globals.getInstance().getApplicationContext().getPackageName());
        List<String> listOptions = Arrays.asList(Globals.getInstance().getApplicationContext().getResources().getStringArray(id));
        List<CharacterString> relayOptions = new ArrayList<>();
        for (String optionItem : listOptions) {
            relayOptions.add(new CharacterString(optionItem));
        }
        for (int relayNum = 1; relayNum <= 7; relayNum++) {
            if (checkRelay(relayNum) > 0) {
                Log.i("Bacnet", "Systempoints = relay= " + relayNum + "," + profileType.name());
                if ((profileType == ProfileType.SYSTEM_VAV_ANALOG_RTU || profileType == ProfileType.SYSTEM_DAB_ANALOG_RTU) && ((relayNum == 3) || (relayNum == 7))) {
                    Stage stage = getFullyModulatingRelayConfigOutput(relayNum);
                    createRelay(localDevice, addressNumber, ccuName, relayOptions, relayNum, stage.name());
                } else {
                    Stage stage = Stage.values()[(int) getRelayConfigAssociation("relay" + relayNum)];
                    createRelay(localDevice, addressNumber, ccuName, relayOptions, relayNum, stage.name());
                }
            } else {
                if (localDevice.checkObjectByIDandType((addressNumber + relayNum), ObjectType.multiStateValue)) {
                    deleteObject(localDevice, localDevice.getObjectByID(addressNumber + relayNum));
                }
            }
        }
        if (checkAnalog(1) > 0) {
            createAnalogOut1(localDevice, addressNumber, ccuName);
        } else {
            if (localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut1)) {
                deleteObject(localDevice, localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut1));
            }
        }
        if (checkAnalog(2) > 0) {
            createAnalogOut2(localDevice, addressNumber, ccuName);
        } else {
            if (localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut2)) {
                deleteObject(localDevice, localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut2));
            }
        }
        if (checkAnalog(3) > 0) {
            createAnalogOut3(localDevice, addressNumber, ccuName);
        } else {
            if (localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut3)) {
                deleteObject(localDevice, localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut3));
            }
        }
        if (checkAnalog(4) > 0) {
            createAnalogOut4(localDevice, addressNumber, ccuName);
        } else {
            if (localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut4)) {
                deleteObject(localDevice, localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut4));
            }
        }
    }


    public double checkRelay(int relayNo) {
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap configPoint = hayStack.read("point and system and config and output and enabled and relay" + relayNo);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public double getRelayConfigAssociation(String config) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and association and " + config);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }

    public Stage getFullyModulatingRelayConfigOutput(int relayNum) {
        Stage stage = Stage.FAN_1;
        if (relayNum == 7) {
            double curHumidifierType = CCUHsApi.getInstance().readDefaultVal("point and system and config and relay7 and humidifier and type");
            if (curHumidifierType == 0.0)
                stage = Stage.HUMIDIFIER;
            else
                stage = Stage.DEHUMIDIFIER;
        }
        return stage;
    }

    public double getRelay(int relayNo) {
        try {
            return CCUHsApi.getInstance().readHisValByQuery("point and his and system and state and relay" + relayNo);
        } catch (Exception e) {
            return 0;
        }
    }


    public double checkAnalog(int analogNo) {
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap configPoint = hayStack.read("point and system and config and output and enabled and analog" + analogNo);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }


    public double getAnalog(int analogNo) {
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            String tags = null;
            if (analogNo == 1) {
                tags = "analog" + analogNo + " and cooling";
            }
            if (analogNo == 2) {
                tags = "analog" + analogNo + " and fan";
            }
            if (analogNo == 3) {
                tags = "analog" + analogNo + " and heating";
            }
            if (analogNo == 4) {
                tags = "analog" + analogNo + " and heating";
            }
            HashMap configPoint = hayStack.read("point and system and config and " + tags);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public static double getAnalogOut(String analog) {
        return CCUHsApi.getInstance().readHisValByQuery("point and his and system and out and " + analog);
    }

    public double getAnalogMin(int analogNo) {
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            String tags = null;
            if (analogNo == 1) {
                tags = "analog" + analogNo + " and cooling and min";
            }
            if (analogNo == 2) {
                tags = "analog" + analogNo + " and fan and min";
            }
            if (analogNo == 3) {
                tags = "analog" + analogNo + " and heating and min";
            }
            if (analogNo == 4) {
                tags = "analog" + analogNo + " and heating and min";
            }
            HashMap configPoint = hayStack.read("point and system and config and " + tags);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public double getAnalogMax(int analogNo) {
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            String tags = null;
            if (analogNo == 1) {
                tags = "analog" + analogNo + " and cooling and max";
            }
            if (analogNo == 2) {
                tags = "analog" + analogNo + " and fan and max";
            }
            if (analogNo == 3) {
                tags = "analog" + analogNo + " and heating and max";
            }
            if (analogNo == 4) {
                tags = "analog" + analogNo + " and heating and max";
            }
            HashMap configPoint = hayStack.read("point and system and config and " + tags);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public double checkAnalogIn(int analogNo) {
        try {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            HashMap configPoint = hayStack.read("point and system and config and input and enabled and analog" + analogNo);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public double getAnalogIn(int analogNo) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and input and association and analog" + analogNo);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }

    public void createRelay(LocalDevice localDevice, int addressNumber, String ccuName, List<CharacterString> relayOptions, int relayNumber, String relayOp) {
        try {
            BACnetArray<CharacterString> relayModes = new BACnetArray<>(relayOptions);
            if (!localDevice.checkObjectByIDandType((addressNumber + relayNumber), ObjectType.multiStateValue)) {
                relay1 = new MultistateValueObject(localDevice, addressNumber + relayNumber, ccuName + "_relay" + relayNumber, relayModes.getCount(), relayModes, 1, false);
                relay1.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("75F System Relay " + relayNumber + " " + relayOp)));
                relay1.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger((int) getRelay(relayNumber)));
                relay1.supportCovReporting();
                incrementDBRevision(localDevice);
            } else {
                relay1 = (MultistateValueObject) localDevice.getObjectByIDandType((addressNumber + relayNumber), ObjectType.multiStateValue);
                relay1.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger((int) getRelay(relayNumber)));
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void createRelay7(LocalDevice localDevice, int addressNumber, String ccuName, List<CharacterString> relayOptions, int relayNumber, String relayOp) {
        try {
            BinaryValueObject relayProp = new BinaryValueObject(localDevice, addressNumber + relayNumber, ccuName + "_relay" + relayNumber, getRelay(relayNumber) > 0 ? BinaryPV.active : BinaryPV.inactive, false);

        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void createAnalogOut1(LocalDevice localDevice, int addressNumber, String ccuName) {
        try {
            if (!localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut1)) {
                analogOut1 = new AnalogValueObject(localDevice, addressNumber + BACnetUtils.analogOut1, ccuName + "_analogout1", (float) getAnalogOut("analog1"), EngineeringUnits.volts, false);
                analogOut1.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("75F System Analog Out 1 - Cooling")));
                analogOut1.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(1)));
                analogOut1.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(1)));
                incrementDBRevision(localDevice);
            } else {
                analogOut1 = (AnalogValueObject) localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut1);
                analogOut1.writePropertyInternal(PropertyIdentifier.presentValue, new Real((float) getAnalogOut("analog1")));
                analogOut1.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(1)));
                analogOut1.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(1)));
            }

        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void createAnalogOut2(LocalDevice localDevice, int addressNumber, String ccuName) {
        try {
            if (!localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut2)) {
                analogOut2 = new AnalogValueObject(localDevice, addressNumber + BACnetUtils.analogOut2, ccuName + "_analogout2", (float) getAnalogOut("analog2"), EngineeringUnits.volts, false);
                analogOut2.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("75F System Analog Out 2 - Fan Speed")));
                analogOut2.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(2)));
                analogOut2.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(2)));
                incrementDBRevision(localDevice);
            } else {
                analogOut2 = (AnalogValueObject) localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut2);
                analogOut2.writePropertyInternal(PropertyIdentifier.presentValue, new Real((float) getAnalogOut("analog2")));
                analogOut2.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(2)));
                analogOut2.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(2)));
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void createAnalogOut3(LocalDevice localDevice, int addressNumber, String ccuName) {
        try {
            if (!localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut3)) {
                analogOut3 = new AnalogValueObject(localDevice, addressNumber + BACnetUtils.analogOut3, ccuName + "_analogout3", (float) getAnalogOut("analog3"), EngineeringUnits.volts, false);
                analogOut3.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("75F System Analog Out 3 - Heating")));
                analogOut3.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(3)));
                analogOut3.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(3)));
                incrementDBRevision(localDevice);
            } else {
                analogOut3 = (AnalogValueObject) localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut3);
                analogOut3.writePropertyInternal(PropertyIdentifier.presentValue, new Real((float) getAnalogOut("analog3")));
                analogOut3.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(3)));
                analogOut3.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(3)));
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void createAnalogOut4(LocalDevice localDevice, int addressNumber, String ccuName) {
        try {
            if (!localDevice.checkObjectByID(addressNumber + BACnetUtils.analogOut4)) {
                analogOut4 = new AnalogValueObject(localDevice, addressNumber + BACnetUtils.analogOut4, ccuName + "_analogout4", (float) getAnalogOut("analog4"), EngineeringUnits.volts, false);
                analogOut4.writeProperty(new ValueSource(), new PropertyValue(PropertyIdentifier.description, new CharacterString("75F System Analog Out 4 - Outside Air")));
                analogOut4.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(4)));
                analogOut4.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(4)));
                incrementDBRevision(localDevice);
            } else {
                analogOut4 = (AnalogValueObject) localDevice.getObjectByID(addressNumber + BACnetUtils.analogOut4);
                analogOut4.writePropertyInternal(PropertyIdentifier.presentValue, new Real((float) getAnalogOut("analog4")));
                analogOut4.writePropertyInternal(PropertyIdentifier.lowLimit, new Real((int) getAnalogMin(4)));
                analogOut4.writePropertyInternal(PropertyIdentifier.highLimit, new Real((int) getAnalogMax(4)));
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void deleteObject(LocalDevice localDevice, BACnetObject baCnetObject) {
        try {
            localDevice.removeObject(baCnetObject.getId());
            localDevice.incrementDatabaseRevision();
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }
    }

    public void incrementDBRevision(LocalDevice localDevice) {
        if (localDevice != null) localDevice.incrementDatabaseRevision();
    }

    public void clearSystemPoints(LocalDevice localDevice) {

        try {
            Log.i("Bacnet", "Deleting the objects");
            if (relay1 != null) {
                localDevice.removeObject(relay1.getId());
            }
            if (relay2 != null) {
                localDevice.removeObject(relay2.getId());
            }
            if (relay3 != null) {
                localDevice.removeObject(relay3.getId());
            }
            if (relay4 != null) {
                localDevice.removeObject(relay4.getId());
            }
            if (relay5 != null) {
                localDevice.removeObject(relay5.getId());
            }
            if (relay6 != null) {
                localDevice.removeObject(relay6.getId());
            }
            if (relay7 != null) {
                localDevice.removeObject(relay7.getId());
            }
            if (analogOut1 != null) {
                localDevice.removeObject(analogOut1.getId());
            }
            if (analogOut2 != null) {
                localDevice.removeObject(analogOut2.getId());
            }
            if (analogOut3 != null) {
                localDevice.removeObject(analogOut3.getId());
            }
            if (analogOut4 != null) {
                localDevice.removeObject(analogOut4.getId());
            }
        } catch (BACnetServiceException e) {
            e.printStackTrace();
        }

    }
}
