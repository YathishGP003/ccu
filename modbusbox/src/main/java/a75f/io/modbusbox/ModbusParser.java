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
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;

public class ModbusParser {


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
    private EquipmentDevice parseModbusDevice(String json,String fileName) throws JsonParseException {
        EquipmentDevice equipmentDevice = null;
        if(isValidJSON(json,fileName)) {
            equipmentDevice = new Gson().fromJson(json, EquipmentDevice.class);
        }else{
            Log.i("CCU_MODBUS", "Invalid JSON File - "+fileName);
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


    public void readExternalJSONFromDir(String filePath,List<EquipmentDevice> modbus, List<EquipmentDevice> btuList,
                                                         List<EquipmentDevice> emSysList,List<EquipmentDevice> emzoneList){
        File modbusJsonFolder = new File(filePath);
        if(modbusJsonFolder.listFiles()==null||modbusJsonFolder.listFiles().length==0)
            Log.i("CCU_MODBUS", "No External JSON files found ");
        if(modbusJsonFolder!=null && modbusJsonFolder.exists() && modbusJsonFolder.isDirectory()
                && modbusJsonFolder.listFiles()!=null && modbusJsonFolder.listFiles().length>0) {
            File listOfFile[] = modbusJsonFolder.listFiles();
            for (int i = 0; i < listOfFile.length; i++) {
                try {
                    if(listOfFile[i].isFile())
                    {
                        EquipmentDevice device = parseModbusDevice(readFileFromFolder(listOfFile[i]), listOfFile[i].getName());
                        if(device == null ) continue;

                        Log.i("CCU_MODBUS", "Valid JSON file found : "+listOfFile[i].getName() + " EquipType :"+device.getEquipType());

                        if(device.getEquipType().equals(ModbusCategory.BTU.displayName)){
                            btuList.add(device);
                        }
                        else if(device.getEquipType().equals(ModbusCategory.EMR.displayName)){
                            emSysList.add(device);
                        }
                        else if(device.getEquipType().equals(ModbusCategory.EMR_ZONE.displayName)){
                            emzoneList.add(device);
                        }
                        else{
                            modbus.add(device);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            modbusJsonFolder.mkdirs();
        }

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

    private boolean isValidJSON(String rawJson,String fileName){
        boolean isValid= true;
        try {
            JSONObject jsonObject = new JSONObject(rawJson);

            // Check for duplicate Equip ID
            for (int i = 0; i < deviceList.size() ; i++) {
                if(deviceList.get(i).getModbusEquipIdId().equals(jsonObject.getString("modbusEquipId (_id)"))) {
                    Log.i("CCU_MODBUS", "Duplicate modbusEquipId - "+jsonObject.getString("modbusEquipId (_id)") +" found in - "+fileName+"" );
                    isValid = false;
                    break;
                }
            }

            // Read Registers
            JSONArray registers = jsonObject.getJSONArray("registers");

            if(isContainsDuplicateParametersInSameJSON(registers,fileName)){
                isValid = false;
            }

            for (int i = 0; i <registers.length() ; i++) {

                JSONObject registerData = registers.getJSONObject(i);
                // Read register parameters
                JSONArray params = registerData.getJSONArray("parameters");
                for (int j = 0; j < params.length(); j++) {

                    // Read Logical Points
                    JSONObject parameter = params.getJSONObject(j);
                    JSONArray logicalPointTags = parameter.getJSONArray("logicalPointTags");

                    for (int k = 0; k < logicalPointTags.length(); k++) {

                        JSONObject tag = logicalPointTags.getJSONObject(k);

                        //Validate the logical Point Tag
                        if (!isValidTAG(tag.getString("tagName"),fileName)) {
                            isValid =  false;
                        }
                    }
                }

            }

        }catch (Exception e){
            Log.i("CCU_MODBUS", "Exception "+e.getMessage());
            e.printStackTrace();
            //return false;
            isValid = false;
        }
        return  isValid;
    }


    private boolean isValidTAG(String tag,String fileName){
        boolean isValid = true;
        // Check for Special Char
        Pattern validPattern = Pattern.compile("[^A-Za-z0-9]");
        Matcher matcher = validPattern.matcher(tag);
        if(matcher.find())
        {
            Log.i("CCU_MODBUS", "Invalid: tag '"+tag+"' found in "+ fileName + "  Contains Special Character");
            isValid = false;
        }

        // Check for Caps
        if(Character.isUpperCase(tag.charAt(0))){
            Log.i("CCU_MODBUS", "Invalid: tag '"+tag+"' found in "+fileName +"   Should not start with upper case");
            isValid = false;
        }

        if(Character.isDigit(tag.charAt(0))){
            Log.i("CCU_MODBUS", "Invalid: tag '"+tag+"' found in "+fileName+"   Should not start with Number");
            isValid = false;
        }

        return  isValid;
    }


    private boolean isContainsDuplicateParametersInSameJSON(JSONArray jsonArray,String fileName){
        boolean isValid = false;
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray paramIDetails = jsonArray.getJSONObject(i).getJSONArray("parameters");

                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONArray paramJDetails = jsonArray.getJSONObject(j).getJSONArray("parameters");
                        if (i!=j&&paramIDetails.getJSONObject(0).getString("parameterId").equals(paramJDetails.getJSONObject(0).getString("parameterId"))) {
                            Log.i("CCU_MODBUS", "Same JSON has duplicate parameter ID "+paramJDetails.getJSONObject(0).getString("parameterId")+
                                    ",Param Name : "+paramJDetails.getJSONObject(0).getString("name")+ " found in "+fileName);
                            isValid = true;
                        }
                }
                if (isContainsDuplicateParamerInOtherJSON(paramIDetails.getJSONObject(0).getString("parameterId"),fileName)) isValid = true;
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.i("CCU_MODBUS", "Exception in isContainsDuplicateParametersInSameJSON"+e.getMessage());
            isValid = true;
        }
        return isValid;
    }

    private boolean isContainsDuplicateParamerInOtherJSON(String paramID,String fileName){
        boolean isContainDuplicate = false;
        for (int i = 0; i < deviceList.size(); i++) {
            EquipmentDevice device = deviceList.get(i);
            List<Register> registers= device.getRegisters();

            for (int j = 0; j <registers.size() ; j++) {

                List<Parameter> parameters= registers.get(j).getParameters();
                if(parameters.get(0).parameterId.equals(paramID)){
                    Log.i("CCU_MODBUS", "Duplicate parameter ID "+paramID+ ",Param Name : "+parameters.get(0).getName()+ " found in "+fileName);
                    isContainDuplicate =  true;
                    break;
                }

            }
        }
        return isContainDuplicate;
    }

}
