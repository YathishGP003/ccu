package a75f.io.modbusbox;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a75f.io.api.haystack.modbus.EquipmentDevice;

public class ModbusParser {

    enum MBCategory{
        MODBUS,BTU,EM_SYSTEM,EM_ZONE
    }

    // Hold Existing Device ID to avoid duplication
    ArrayList<String> deviceIDList = new ArrayList<>();
    public ArrayList<EquipmentDevice> parseAllEquips(Context c) {
        ArrayList<EquipmentDevice> allEquips = parseEquips(c);
        return allEquips;
    }

    public ArrayList<EquipmentDevice> parseEquips(Context c) {
        ArrayList<EquipmentDevice> assetEquipments = new ArrayList<>();

        try {
            String[] fileList;
            fileList = c.getAssets().list("modbus");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(c, "modbus/" + filename);
                EquipmentDevice device =parseModbusDataFromString(equipJson);
                deviceIDList.add(device.getModbusEquipIdId());
                assetEquipments.add(device);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return assetEquipments;
    }


    public EquipmentDevice parseModbusDataFromString(String json) {
        EquipmentDevice equipmentDevice = null;
        if(isValidJSON(json)) {
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
                EquipmentDevice device =parseModbusDataFromString(equipJson);
                deviceIDList.add(device.getModbusEquipIdId());
                assetEquipments.add(device);
            }
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
                EquipmentDevice device =parseModbusDataFromString(equipJson);
                deviceIDList.add(device.getModbusEquipIdId());
                energyMeterDevices.add(device);
            }
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
                EquipmentDevice device =parseModbusDataFromString(equipJson);
                deviceIDList.add(device.getModbusEquipIdId());
                btuMeterDevices.add(device);
            }

        } catch (IOException e) {
            Log.e("MODBUS PARSER", "File path does not exist");
            e.printStackTrace();
        }
        return btuMeterDevices;
    }


    public ArrayList<EquipmentDevice> readExternalJSONFromDir(String filePath,MBCategory type){
        ArrayList<EquipmentDevice> filterDevices = new ArrayList<EquipmentDevice>();
        File modbusJsonFolder = new File(filePath);
        if(modbusJsonFolder!=null && modbusJsonFolder.exists() && modbusJsonFolder.isDirectory()
                && modbusJsonFolder.listFiles()!=null && modbusJsonFolder.listFiles().length>0) {
            File listOfFile[] = modbusJsonFolder.listFiles();
            for (int i = 0; i < listOfFile.length; i++) {
                try {
                    if(listOfFile[i].isFile())
                    {
                        EquipmentDevice device =parseModbusDataFromString(readFileFromFolder(listOfFile[i]));
                        if(type == MBCategory.MODBUS && device.getEquipType()!="BTU" && device.getEquipType()!="EMR" && device.getEquipType()!="EMR_ZONE")
                            filterDevices.add(device);
                        else if(type == MBCategory.BTU && device.getEquipType()!="BTU")
                            filterDevices.add(device);
                        else if(type == MBCategory.EM_SYSTEM && device.getEquipType()!="EMR")
                            filterDevices.add(device);
                        else if(type == MBCategory.EM_ZONE && device.getEquipType()!="EMR_ZONE")
                            filterDevices.add(device);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            modbusJsonFolder.mkdirs();
        }
        return filterDevices;
    }



    public String readFileFromFolder(File file) {
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

    private boolean isValidJSON(String rawJson){
        try {
            JSONObject jsonObject = new JSONObject(rawJson);

            if(deviceIDList.contains(jsonObject.getString("modbusEquipId (_id)")))

            // Check for Equipe Type tag
            if (isValidTAG(jsonObject.getString("equipType"))) {



            }else{
                return false;
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return  true;
    }


    private boolean isValidTAG(String tag){
        // Check for Special Char
        Pattern validPattern = Pattern.compile("[^A-Za-z0-9]");
        Matcher matcher = validPattern.matcher(tag);
        boolean isContainsSpecialChar = matcher.find();

        // Check for Caps
        boolean isStartWithCaps = Character.isUpperCase(tag.charAt(0));

        return  (!isContainsSpecialChar)&&(!isStartWithCaps);
    }

}
