package a75f.io.modbusbox;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
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

    public ArrayList<EquipmentDevice> parseEquips(Context c) {
        ArrayList<EquipmentDevice> assetEquipments = new ArrayList<>();

        try {
            String[] fileList;
            fileList = c.getAssets().list("modbus");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(c, "modbus/" + filename);
                assetEquipments.add(parseModbusDataFromString(equipJson));
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
                jsonObjects =  new String(buffer, "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObjects;
    }

    public ArrayList<EquipmentDevice> parseEneryMeterSystemEquips(Context c) {
        ArrayList<EquipmentDevice> assetEquipments = new ArrayList<>();

        try {
            String[] fileList;
            fileList = c.getAssets().list("modbus-em-system");
            for (String filename : fileList) {
                String equipJson = readFileFromAssets(c, "modbus-em-system/" + filename);
                assetEquipments.add(parseModbusDataFromString(equipJson));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return assetEquipments;
    }
}
