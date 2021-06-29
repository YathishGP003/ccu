package a75f.io.modbusbox;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import a75f.io.api.haystack.modbus.EquipmentDevice;

public class ModbusParser {

    public ArrayList<EquipmentDevice> parseAllEquips(Context c) {
        ArrayList<EquipmentDevice> allEquips = parseEquips(c);
        return allEquips;
    }

    public static final int MODBUS=1;
    public static final int BTU=2;
    public static final int EM_SYSTEM=3;
    public static final int EM_ZONE=4;

    public ArrayList<EquipmentDevice> parseEquips(Context c) {
        ArrayList<EquipmentDevice> assetEquipments = new ArrayList<>();

        try {
            String[] fileList;
            fileList = c.getAssets().list("modbus");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(c, "modbus/" + filename);
                assetEquipments.add(parseModbusDataFromString(equipJson));
            }
            assetEquipments.addAll(readExternalJSONFromDir(c,"/sdcard/ccu/modbus",this.MODBUS));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return assetEquipments;
    }


    public EquipmentDevice parseModbusDataFromString(String json) {
        EquipmentDevice equipmentDevice = null;
        try {
            Gson gson = new Gson();
            //ObjectMapper objectMapper = new ObjectMapper();
            //objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            //objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            //equipmentDevice = objectMapper.readValue(json, EquipmentDevice.class);
            equipmentDevice = gson.fromJson(json, EquipmentDevice.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return equipmentDevice;
    }


    public String readFileFromAssets(Context ctx, String pathToJson) {
        InputStream rawInput;
        ByteArrayOutputStream rawOutput = null;
        String jsonObjects = "";
        try {
            InputStream is = ctx.getAssets().open(pathToJson);
            int size = is.available();
            if (size > 0) {
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonObjects = new String(buffer, "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObjects;
    }

    /**
     * Read all the Energy Meter details JSON file
     */
    public ArrayList<EquipmentDevice> parseEneryMeterEquips(Context c) {
        ArrayList<EquipmentDevice> assetEquipments = new ArrayList<>();

        try {
            String[] fileList;
            fileList = c.getAssets().list("modbus-em-zone");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(c, "modbus-em-zone/" + filename);
                assetEquipments.add(parseModbusDataFromString(equipJson));
            }
            assetEquipments.addAll(readExternalJSONFromDir(c,"/sdcard/ccu/modbus",this.EM_ZONE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return assetEquipments;
    }

    /**
     * Read all the Energy Meter details JSON file
     */
    public ArrayList<EquipmentDevice> parseEneryMeterSystemEquips(Context c) {
        ArrayList<EquipmentDevice> energyMeterDevices = new ArrayList<>();
        try {
            String[] fileList = c.getAssets().list("modbus-em-system");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(c, "modbus-em-system/" + filename);
                energyMeterDevices.add(parseModbusDataFromString(equipJson));
            }
            energyMeterDevices.addAll(readExternalJSONFromDir(c,"/sdcard/ccu/modbus",this.EM_SYSTEM));
        } catch (IOException e) {
            Log.e("MODBUS PARSER", "File path does not exist");
            e.printStackTrace();
        }
        return energyMeterDevices;
    }

    /**
     * Read all the BTU Meter details JSON file
     *
     * @param context
     * @return
     */
    public ArrayList<EquipmentDevice> readBTUMeterDeviceDetails(Context context) {
        ArrayList<EquipmentDevice> btuMeterDevices = new ArrayList<>();

        try {
            String[] fileList;
            fileList = context.getAssets().list("modbus-btu");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(context, "modbus-btu/" + filename);
                btuMeterDevices.add(parseModbusDataFromString(equipJson));
            }
            btuMeterDevices.addAll(readExternalJSONFromDir(context,"/sdcard/ccu/modbus",this.BTU));
        } catch (IOException e) {
            Log.e("MODBUS PARSER", "File path does not exist");
            e.printStackTrace();
        }
        return btuMeterDevices;
    }

    private ArrayList<EquipmentDevice> readExternalJSONFromDir(Context c, String filePath,int type){
        ArrayList<EquipmentDevice> filterDevices = new ArrayList<EquipmentDevice>();
        File modbusJsonFolder = new File(filePath);
        if(modbusJsonFolder!=null && modbusJsonFolder.exists() && modbusJsonFolder.isDirectory()
                && modbusJsonFolder.listFiles()!=null && modbusJsonFolder.listFiles().length>0) {
            File listOfFile[] = modbusJsonFolder.listFiles();
            for (int i = 0; i < listOfFile.length; i++) {
                try {
                    if(listOfFile[i].isFile())
                    {
                        EquipmentDevice device =parseModbusDataFromString(readFileFromFolder(c, listOfFile[i]));
                        if(type == MODBUS && device.getEquipType()!="BTU" && device.getEquipType()!="EMR" && device.getEquipType()!="EMR_ZONE")
                            filterDevices.add(device);
                        else if(type == BTU && device.getEquipType()!="BTU")
                            filterDevices.add(device);
                        else if(type == EM_SYSTEM && device.getEquipType()!="EMR")
                            filterDevices.add(device);
                        else if(type == EM_ZONE && device.getEquipType()!="EMR_ZONE")
                            filterDevices.add(device);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            modbusJsonFolder.mkdir();
        }
        return filterDevices;
    }



    public String readFileFromFolder(Context ctx, File file) {
        String jsonObjects = "";
        try {
            InputStream is = new FileInputStream(file);
            int size = is.available();
            if (size > 0) {
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonObjects = new String(buffer, "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObjects;
    }

}
