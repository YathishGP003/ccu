package a75f.io.modbusbox;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;

public class ModbusParser {

    enum MBCategory{
        MODBUS,BTU,EM_SYSTEM,EM_ZONE
    }

    // Hold Existing Devices to avoid duplication
    private List<EquipmentDevice> deviceList = new ArrayList<>();
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
                deviceList.add(device);
                assetEquipments.add(device);
            }
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
    private EquipmentDevice parseModbusDevice(String json) throws JsonParseException {
        EquipmentDevice equipmentDevice = null;
        if(isValidJSON(json)) {
            equipmentDevice = new Gson().fromJson(json, EquipmentDevice.class);
        }else{
            Log.i("CCU_MODBUS", "INVALID JSON TAG FOR EXTERNAL JSON");
        }
        return equipmentDevice;
    }


    private String readFileFromAssets(Context ctx, String pathToJson) {
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
                deviceList.add(device);
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
                deviceList.add(device);
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
                deviceList.add(device);
                btuMeterDevices.add(device);
            }

        } catch (IOException e) {
            Log.e("MODBUS PARSER", "File path does not exist");
            e.printStackTrace();
        }
        return btuMeterDevices;
    }


    public ArrayList<EquipmentDevice> readExternalJSONFromDir(String filePath,MBCategory type){
        ArrayList<EquipmentDevice> filterDevices = new ArrayList<>();
        File modbusJsonFolder = new File(filePath);
        if(modbusJsonFolder.listFiles()==null||modbusJsonFolder.listFiles().length==0)
            Log.i("CCU_MODBUS", "No External JSON files found for "+type);
        if(modbusJsonFolder!=null && modbusJsonFolder.exists() && modbusJsonFolder.isDirectory()
                && modbusJsonFolder.listFiles()!=null && modbusJsonFolder.listFiles().length>0) {
            File listOfFile[] = modbusJsonFolder.listFiles();
            for (int i = 0; i < listOfFile.length; i++) {
                try {
                    if(listOfFile[i].isFile())
                    {
                        EquipmentDevice device = parseModbusDevice(readFileFromFolder(listOfFile[i]));
                        if(device == null ) continue;
                        Log.i("CCU_MODBUS", "Valid JSON file found : "+device.getName());
                        if(type == MBCategory.MODBUS && device.getEquipType()!="BTU" &&
                                device.getEquipType()!="EMR" && device.getEquipType()!="EMR_ZONE")
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



    private String readFileFromFolder(File file) {
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

            // Check for duplicate Equip ID
            for (int i = 0; i < deviceList.size() ; i++) {
                if(deviceList.get(i).getModbusEquipIdId().equals(jsonObject.getString("modbusEquipId (_id)"))) {
                    Log.i("CCU_MODBUS", "Duplicate modbusEquipId: "+jsonObject.getString("modbusEquipId (_id)"));
                    return false;
                }
            }

            // Read Registers
            JSONArray registers = jsonObject.getJSONArray("registers");
            for (int i = 0; i <registers.length() ; i++) {

                    JSONObject registerData = registers.getJSONObject(i);
                    // Read register parameters
                    JSONArray params = registerData.getJSONArray("parameters");
                    if(!isContainsDuplicateParametersInSameJSON(params)) {
                        for (int j = 0; j < params.length(); j++) {

                            JSONObject parameter = params.getJSONObject(j);
                            JSONArray logicalPointTags = parameter.getJSONArray("logicalPointTags");

                            for (int k = 0; k < logicalPointTags.length(); k++) {

                                JSONObject tag = logicalPointTags.getJSONObject(k);

                                //Validate the logical Point Tag
                                if (!isValidTAG(tag.getString("tagName"))) {
                                    return false;
                                }
                            }
                        }
                    }else{
                        Log.i("CCU_MODBUS", params.toString()+"\n JSON Contains duplicate parameters");
                        return false;
                    }

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
        if(matcher.find())
        {
            Log.i("CCU_MODBUS", "Invalid: tag : "+tag+" Contains Special Character");
            return false;
        }

        // Check for Caps
        if(Character.isUpperCase(tag.charAt(0))){
            Log.i("CCU_MODBUS", "Invalid: tag : "+tag+" Should not start with upper case");
            return false;
        }

        if(Character.isDigit(tag.charAt(0))){
            Log.i("CCU_MODBUS", "Invalid: tag : "+tag+" Should not start with Number");
            return false;
        }

        return  true;
    }


    private boolean isContainsDuplicateParametersInSameJSON(JSONArray jsonArray){
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject paramIDetails = jsonArray.getJSONObject(i);
                for (int j = 0; j < jsonArray.length(); j++) {
                        JSONObject paramJDetails = jsonArray.getJSONObject(j);
                        if (i != j && paramIDetails.getString("parameterId").equals(paramJDetails.getString("parameterId"))) {
                            Log.i("CCU_MODBUS", "Duplicate parameter ID found: "+paramJDetails.getString("parameterId"));
                            return true;
                        }
                }
                if (isContainsDuplicateParamerInOtherJSON(paramIDetails.getString("parameterId"))) return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private boolean isContainsDuplicateParamerInOtherJSON(String paramID){
        for (int i = 0; i < deviceList.size(); i++) {
            EquipmentDevice device = deviceList.get(i);
            List<Register> registers= device.getRegisters();
            for (int j = 0; j <registers.size() ; j++) {
                List<Parameter> parameters= registers.get(j).getParameters();
                for (int k = 0; k <parameters.size() ; k++) {
                        if(parameters.get(k).parameterId.equals(paramID)){
                            Log.i("CCU_MODBUS", "Duplicate parameter ID found: "+paramID+" in "+device.getName());
                            return true;
                        }
                }
            }
        }
        return false;
    }

}
