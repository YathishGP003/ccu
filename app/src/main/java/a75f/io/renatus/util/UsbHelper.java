package a75f.io.renatus.util;

import static a75f.io.logic.L.TAG_CCU_BACNET_MSTP;
import static a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_MSTP_CONFIGURATION;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.logger.CcuLog;

public class UsbHelper {
    private static final String TAG = "UsbHelper";


    public static void runChmodUsbDevices() {
        CcuLog.e(TAG_CCU_BACNET_MSTP, "-##-runChmodUsbDevices--");
        try {
            // Start root shell
            Process process = Runtime.getRuntime().exec("su");

            String[] commands = {
                    "chmod 755 /dev/bus/usb\n",
                    "chmod 755 /dev/bus/usb/*\n",
                    "chmod 666 /dev/bus/usb/*/*\n",
                    "exit\n"
            };

            // Send commands to the shell
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                os.writeBytes(command);
            }
            os.flush();

            // Wait for the command to complete
            process.waitFor();

            // Log exit code
            CcuLog.d(TAG_CCU_BACNET_MSTP, "chmod executed with exit code: " + process.exitValue());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            CcuLog.e(TAG_CCU_BACNET_MSTP, "Error executing chmod", e);
        }
    }

    public static void listUsbDevices(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            CcuLog.d(TAG, "UsbManager not available");
            return;
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            CcuLog.d(TAG, "Device: " + device.getDeviceName());
            CcuLog.d(TAG, "Vendor ID: " + device.getVendorId() + ", Product ID: " + device.getProductId());
        }
    }

    public static String runAsRoot(String command) {
        CcuLog.d(TAG, "UsbManager runAsRoot command --> " + command);
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            // Read standard output
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Log errors, if any
            while ((line = errorReader.readLine()) != null) {
                CcuLog.d(TAG, "stderr --> " + line);
            }

            process.waitFor();
            os.close();
            reader.close();
            errorReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        CcuLog.d(TAG, "UsbManager runAsRoot output --> " + output.toString().trim());
        return output.toString().trim();
    }

    public static void getPortAddressMstpDevices(Context context){
        File devDirectory = new File("/dev");
        File[] files = devDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.startsWith("ttyUSB")) {
                    updatePreference("/dev/" + fileName, context);
                    break;
                }
            }
        }
    }

    private static void updatePreference(String port, Context context){
        CcuLog.d(TAG, "UsbManager updatePreference port--> " + port);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String mstpConfig = sharedPreferences.getString(BACNET_MSTP_CONFIGURATION, "");
        if(!mstpConfig.isEmpty()){
            Gson gson = new Gson();
            DataMstpObj dataMstpObj = gson.fromJson(mstpConfig, DataMstpObj.class);
            dataMstpObj.getDataMstp().setMstpPortAddress(port);

            String jsonString = new Gson().toJson(dataMstpObj);
            CcuLog.d(TAG, "MSTP output @@-->" + jsonString);
            sharedPreferences.edit().putString(BACNET_MSTP_CONFIGURATION, jsonString).apply();
        }
    }
}

