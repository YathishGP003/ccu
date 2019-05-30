package a75f.io.device.mesh;


import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;

import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import com.google.gson.stream.JsonReader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import a75f.io.device.DeviceConstants;

public class OTAUpdateService extends Service {

    public static final String METADATA_FILE_NAME =".meta" ;    //"meta.data";
    public static final String BINARY_FILE_NAME = ".bin";       //"image.bin";

    //Tags
    static final String TAG = "OTAUpdateService";
    static final int PACKET_LENGTH = 32;
    //static final int PACKET_LENGTH_CM4 = 128;
    //static final String DOWNLOAD_BASE_URL = "http://updates.75fahrenheit.com/sn_fw/";
    static final String DOWNLOAD_BASE_URL = "http://updates.75fahrenheit.com/";
    private static final File DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
    private static final int MASK_8 = 0xFF;
    private static final int MASK_4 = 0xF;
    private static Looper mLooper;
    private static Messenger mMessenger;
    private int mLastSentPacket = -1;
    private ArrayList<byte[]> packets;      //Where the decomposed binary is stored in memory
    private short mVersionMajor = -1;       //Major version number (0-255)
    private short mVersionMinor = -1;       //Minor version number (0-255)

    //Constant fields
    private String mVersion = ""; //initial smart node factory reset version
    private int mUpdateLength = -1;         //Firmware length (bytes)
    private byte[] mFirmwareSignature = {}; //Firmware key
    private int mLwMeshAddress = -1;
    private boolean mUpdateInProgress = false;
    private boolean mUpdateWaitingToComplete = false;
    private boolean mBinaryIsDownloaded = false;
    private boolean mMetadataIsDownloaded = false;
    private DeviceConstants.OTA_FIRMWARE_COMPONENT mFirmwareInfo;
    private int[] mUpdatableNodes;
    private int mLastNodeUpdated;
    private static boolean isTimerStarted = false;
    private static CountDownTimer timer;
    //static final String DEVICE_APP_TAG = "75F Smart Node";

    public OTAUpdateService() {}

    /**
     * Helper method for concatenating byte arrays
     *
     * @param bytes The byte arrays to be added
     * @return The concatenated arrays
     */
    private static byte[] addBytes(byte[]... bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] b : bytes) {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write to output stream.");
            }
        }

        return outputStream.toByteArray();
    }

    /**
     * Utility methods to convert a hex string to a byte array
     *
     * @param s The string to be converted
     * @return A byte array of the converted string
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * A utility methods to convert a byte array to a hex string
     * Based on the javax.xml implementation, with modifications
     *
     * @param data   The array to be converted
     * @param spaces A flag for whether the output should have spaces between each byte
     * @return A hex string of the converted byte array
     */
    private static String byteArrayToHexString(byte[] data, boolean spaces) {
        StringBuilder r = new StringBuilder();
        for (byte b : data) {
            r.append(HEX_CODE[(b >> 4) & MASK_4]);
            r.append(HEX_CODE[(b & MASK_4)]);
            if (spaces) {
                r.append(' ');
            }
        }
        return r.toString();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("OTAService", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mLooper = thread.getLooper();

        mLastSentPacket = -1;
        mMessenger = new Messenger(new IncomingHandler(mLooper, this));

        try {
            LSerial.getInstance().attachClient(mMessenger);


        } catch (Exception e) {
            Log.e(TAG, "Failed to register messenger");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int node = intent.getIntExtra("lwMeshAddress", -1);
        String firmwareInfo = intent.getStringExtra("firmwareInfo");
        int versionMajor = intent.getIntExtra("versionMajor", -1);
        int versionMinor = intent.getIntExtra("versionMinor", -1);

        deleteAllFiles();

        mVersionMajor = (short) versionMajor;
        mVersionMinor = (short) versionMinor;

        //mVersion = "SmartNode_v"+versionMajor+"."+versionMinor;
        if(firmwareInfo.startsWith("SmartNode_")) {
            mVersion = firmwareInfo;
            mFirmwareInfo = DeviceConstants.OTA_FIRMWARE_COMPONENT.SMARTNODE;
            startUpdate(node, versionMajor, versionMinor, firmwareInfo, DeviceConstants.OTA_FIRMWARE_COMPONENT.SMARTNODE);
        }else if(firmwareInfo.startsWith("Itm_") || firmwareInfo.startsWith("itm_")) {
            mVersion = firmwareInfo;
            if(firmwareInfo.startsWith("Itm_"))
                mVersion = firmwareInfo.replace("I","i");
            mFirmwareInfo = DeviceConstants.OTA_FIRMWARE_COMPONENT.ITM;
            startUpdate(node, versionMajor, versionMinor, mVersion, DeviceConstants.OTA_FIRMWARE_COMPONENT.ITM);
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /**
     * Starts downloading the binary file
     */
    void startBinaryDownload(String filename, DeviceConstants.OTA_FIRMWARE_COMPONENT firmware) {
        Log.d(TAG, "{DOWNLOAD] Starting binary download");
        switch (firmware){
            case SMART_NODE:
                new DownloadFileFromURL().execute((DOWNLOAD_BASE_URL+"sn_fw/"+filename + BINARY_FILE_NAME), filename.concat(BINARY_FILE_NAME));
                break;
            case ITM:
                new DownloadFileFromURL().execute((DOWNLOAD_BASE_URL+"itm_fw/"+filename + BINARY_FILE_NAME), filename.concat(BINARY_FILE_NAME));
                break;
        }
    }

    /**
     * Starts downloading the metadata file
     */
    void startMetadataDownload(String filename, CCUConstants.OTA_FIRMWARE_COMPONENT firmware) {
        Log.d(TAG, "{DOWNLOAD] Starting metadata download");
        switch (firmware){
            case SMARTNODE:
                new DownloadFileFromURL().execute((DOWNLOAD_BASE_URL+"sn_fw/"+filename + METADATA_FILE_NAME), filename.concat(METADATA_FILE_NAME));
                break;
            case ITM:
                new DownloadFileFromURL().execute((DOWNLOAD_BASE_URL+"itm_fw/"+filename + METADATA_FILE_NAME), filename.concat(METADATA_FILE_NAME));
                break;
        }
    }

    /**
     * Clears update variables
     */
    private void resetUpdate() {
        mLwMeshAddress = -1;
        mUpdateInProgress = false;
        if(packets != null)packets.clear();
        isTimerStarted = false;
        mVersionMajor = -1;
        mVersionMinor = -1;
        mLastSentPacket = -1;
        mUpdateLength = -1;
        mFirmwareSignature = new byte[0];
        Log.d(TAG, "[RESET] Resetting update status");
    }

    private void pauseUpdate() {
        int address = mLwMeshAddress;
        short versionMajor = mVersionMajor;
        short versionMinor = mVersionMinor;
        resetUpdate();
        mLwMeshAddress = address;
        mVersionMajor = versionMajor;
        mVersionMinor = versionMinor;
        //mVersion = "SmartNode_v"+mVersionMajor+"."+mVersionMinor;
    }

    /**
     * Starts an OTA update for the specified smart node, to the specified version
     * Fails if the address is invalid, if there is currently an update in progress, or if the files
     * do not exist
     *
     * @param address The address of the Smart Node to be updated
     */
    private void startUpdate(int address, int versionMajor, int versionMinor, String filename, CCUConstants.OTA_FIRMWARE_COMPONENT firmware) {
        Log.d(TAG, "{VALIDATION] Validating update instructions="+filename);
        if (mUpdateInProgress) {
            Log.d(TAG, "[VALIDATION] Update already in progress");
            return;
        }

        if (address < 0) {
            Log.d(TAG, "[VALIDATION] Invalid address: " + address);
            resetUpdate();
            return;
        }

        if (!validVersion((short) versionMajor, (short) versionMinor)) {
            Log.d(TAG, "[VALIDATION] Invalid version: " + versionMajor + "." + versionMinor);
            resetUpdate();
            return;
        }

        Log.d(TAG, "{VALIDATION] Valid address and version");

        mLwMeshAddress = address;
        boolean isSystemDevice = false;
        if(DCVSensorData.getHandle().isPaired() && (address == DCVSensorData.getHandle().getSensorAddress()))
            runMetadataCheck(DOWNLOAD_DIR, versionMajor, versionMinor, filename,firmware);
        else if(COSensorData.getHandle().isPaired() && (address == COSensorData.getHandle().getSensorAddress()))
            runMetadataCheck(DOWNLOAD_DIR, versionMajor, versionMinor, filename,firmware);
        else if(NO2SensorData.getHandle().isPaired() && (address == NO2SensorData.getHandle().getSensorAddress()))
            runMetadataCheck(DOWNLOAD_DIR, versionMajor, versionMinor, filename,firmware);
        else if(PressureSensorData.getHandle().isPaired() && (address == PressureSensorData.getHandle().getSensorAddress()))
            runMetadataCheck(DOWNLOAD_DIR, versionMajor, versionMinor, filename,firmware);
        else {
            runMetadataCheck(DOWNLOAD_DIR, versionMajor, versionMinor, filename, firmware);

        }
    }

    /**
     * Checks if a metadata file is present, and if it matches the requested version number.
     * Starts a download task for the new metadata file if those conditions aren't met
     * Moves on to checking the binary file if they are
     *
     * @param dir          The directory to search
     * @param versionMajor The expected major version
     * @param versionMinor The expected minor version
     */
    private void runMetadataCheck(File dir, int versionMajor, int versionMinor, String filename, CCUConstants.OTA_FIRMWARE_COMPONENT firmware) {
        Log.d(TAG, "{METADATA] Running metadata check="+mVersion+","+filename);
        File metadata = findMetadataFile(dir,filename);

        if (metadata == null) {
            Log.d(TAG, "{METADATA] File not found, downloading metadata");
            mMetadataIsDownloaded = false;
            startMetadataDownload(filename,firmware);
            pauseUpdate();
            return;
        } else {
            Log.d(TAG, "{METADATA] Extracting firmware metadata");
            boolean isExtraccted = extractFirmwareMetadata(metadata);
            if (!isExtraccted || !versionMatches((short) versionMajor, (short) versionMinor) ) {
                mMetadataIsDownloaded = false;
                Log.d(TAG, "[STARTUP] Incorrect firmware metadata, downloading correct firmware.");
                startMetadataDownload(filename, firmware);
                pauseUpdate();
                return;
            }
        }

        Log.d(TAG, "{METADATA] Metadata passed check, starting binary check");
        runBinaryCheck(dir,filename,firmware);
    }

    /**
     * Checks if the binary file is present, and if it matches the metadata
     * If those conditions aren't met, downloads new binary file
     * If they are met, continues to moving the binary file into RAM
     *
     * @param dir The directory to search for the binary file
     */
    private void runBinaryCheck(File dir, String filename, CCUConstants.OTA_FIRMWARE_COMPONENT firmware) {
        Log.d(TAG, "{BINARY] Running binary check="+mVersion+","+filename);
        File binary = findBinaryFile(dir, filename);

        if (binary == null) {
            Log.d(TAG, "{BINARY] Binary file not found, starting binary download");
            startBinaryDownload(filename,firmware);
            //pauseUpdate();
            return;
        } else {
            Log.d(TAG, "{BINARY] Checking binary length="+binary.length()+","+mUpdateLength);
            if (binary.length() != mUpdateLength) {
                mBinaryIsDownloaded = false;
                Log.d(TAG, "[STARTUP] Incorrect firmware binary, downloading correct firmware.");
                startBinaryDownload(filename,firmware);
                return;
            }
        }

        Log.d(TAG, "{BINARY] Binary passed check");

        Log.d(TAG, "[STARTUP] [SN:" + mLwMeshAddress + "]");

        CCUKinveyInterface.uploadErrorConditions(new Alert(Alert.AlertType.FIRMWARE_OTA_UPDATE_STARTED, "SYSTEM").setArgs(AlertsData.getCCUName(), mLwMeshAddress,mVersionMajor+"."+mVersionMinor),mLwMeshAddress);
        mUpdateInProgress = true;
        mLastSentPacket = -1;

        setUpdateFile(binary, firmware);
    }

    private void deleteAllFiles(){
        for(File file:DOWNLOAD_DIR.listFiles()){
            if(file.getName().startsWith("SmartNode_v"))
                file.delete();
        }
    }
    /**
     * Finds a metadata file, if it exists, in the specified directory
     *
     * @param dir The directory to search
     * @return The file, if found, or null, if not
     */
    private File findMetadataFile(File dir, final String version) {
        try {
            for(File file: dir.listFiles()){
                Log.e(TAG,"findMetaDataFile="+file.getName()+","+version+","+file.getName().startsWith(version));
                if(file.getName().startsWith(version) && file.getName().endsWith(".meta"))
                    return file;
            }
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "[STARTUP] Metadata not found in " + dir);
            return null;
        }
    }

    /**
     * Finds a binary file, if it exists, in the specified directory
     *
     * @param dir The directory to search
     * @return The file, if found, or null, if not
     */
    private File findBinaryFile(File dir, final String version) {
        try {
            for(File file: dir.listFiles()){
                Log.e(TAG,"findBinaryDataFile="+file.getName()+","+version+","+file.getName().startsWith(version));
                if(file.getName().startsWith(version) && file.getName().endsWith(".bin"))
                    return file;
            }
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "[STARTUP] Binary not found in " + dir);
            return null;
        }
    }

    /**
     * Sends the corresponding packet over serial to the CM
     *
     * @param lwMeshAddress The address to send the update packet to
     * @param packetNumber  The packet to send
     */
    private void sendPacket(int lwMeshAddress, int packetNumber) {

        if (lwMeshAddress != mLwMeshAddress) {
            Log.d(TAG, "[UPDATE] [SN:" + mLwMeshAddress + "] WRONG ADDRESS " + lwMeshAddress);
            return;
        }
        if (mUpdateWaitingToComplete && packetNumber == packets.size() /*- 2*/) {
            Log.d(TAG, "[UPDATE] Received packet request while waiting for the update to complete");
            return;
        }
        if (packetNumber < 0 || packetNumber > packets.size()) {
            Log.d(TAG, "[UPDATE] [SN:" + mLwMeshAddress + "] INVALID PACKET " + packetNumber);
            return;
        }



        if (!mUpdateWaitingToComplete && (packetNumber == (packets.size() - 1))) {
            Log.d(TAG, "[UPDATE] Received request for final packet");
            mUpdateWaitingToComplete = true;
        }

        byte[] msgType = {(byte) SerialService.MESSAGETYPE.CCU_TO_CM_OVER_USB_FIRMWARE_PACKET.ordinal()};
        byte[] address = Shorts.toByteArray((short) (lwMeshAddress));
        byte x = address[0];
        address[0] = address[1];
        address[1] = x;
        byte[] sequenceNumber = Shorts.toByteArray((short) packetNumber);
        x = sequenceNumber[0];
        sequenceNumber[0] = sequenceNumber[1];
        sequenceNumber[1] = x;
        byte[] packet = addBytes(msgType, address, sequenceNumber, packets.get(packetNumber));

        if (packetNumber > mLastSentPacket) {
            mLastSentPacket = packetNumber;
            if (CCUApp.DEBUG) {
                if (packetNumber % 100 == 0) {
                    Log.d(TAG, "[UPDATE] [SN:" + lwMeshAddress + "]"+"PS:"+packets.size()+","+packet.length+","+sequenceNumber+" [PN:" + mLastSentPacket
                            + "] [DATA: " + byteArrayToHexString(packet, true) +  "]");
                }
            }
        }

        try {
            SerialService.getInstance().sendSerialToCM(packet);

        } catch (Exception e) {
            Log.e(TAG, "[UPDATE] [SN:" + lwMeshAddress + "] [PN:" + packetNumber + "] [FAILED]");
        }
    }

    /**
     * Sends the firmware metadata to the CM
     */
    private void sendFirmwareMetadata(CCUConstants.OTA_FIRMWARE_COMPONENT firmware) {
        byte[] msgType = {(byte) SerialService.MESSAGETYPE.CCU_TO_CM_OVER_USB_FIRMWARE_METADATA.ordinal()};
        byte[] address = Shorts.toByteArray((short) (mLwMeshAddress));
        byte x = address[0];
        address[0] = address[1];
        address[1] = x;
        byte[] device = {(byte) (CCUConstants.OTA_FIRMWARE_COMPONENT.SMARTNODE.ordinal() & MASK_8)};
        switch(firmware){
            case SMARTNODE:
                device = new byte[]{(byte) (CCUConstants.OTA_FIRMWARE_COMPONENT.SMARTNODE.ordinal() & MASK_8)};
                break;
            case ITM:
                device = new byte[]{(byte) (CCUConstants.OTA_FIRMWARE_COMPONENT.ITM.ordinal() & MASK_8)};
                break;
            case SMARTSTAT_BACK:
                device = new byte[]{(byte) (CCUConstants.OTA_FIRMWARE_COMPONENT.SMARTSTAT_BACK.ordinal() & MASK_8)};
                break;
            case HIA:
                device = new byte[]{(byte) (CCUConstants.OTA_FIRMWARE_COMPONENT.HIA.ordinal() & MASK_8)};
                break;
        }
        byte[] major = {(byte) (mVersionMajor & MASK_8)};
        byte[] minor = {(byte) (mVersionMinor & MASK_8)};
        byte[] length = Ints.toByteArray(mUpdateLength);
        x = length[0];
        byte y = length[1];
        byte z = length[2];
        length[0] = length[3];
        length[1] = z;
        length[2] = y;
        length[3] = x;

        byte[] packet = addBytes(msgType, address, device, major, minor, length, mFirmwareSignature);

        if (CCUApp.DEBUG) {
            Log.d(TAG, "[METADATA] [SN:" + mLwMeshAddress + "] [DATA: " + byteArrayToHexString(packet, true) + "]");
        }

        try {
            SerialService.getInstance().sendSerialToCM(packet);
            timerForOtaNonResponse();
        } catch (Exception e) {
            Log.e(TAG, "[METADATA] FAILED TO SEND");
        }
    }

    /**
     * Extracts the firmware metadata from the json file
     *
     * @param json The json file with the information
     */
    private boolean extractFirmwareMetadata(File json) {
        //setTestData();

        try {
            JsonReader reader = new JsonReader(new FileReader(json));

            reader.beginObject();

            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "deviceType":
                        //Currently does not store device type, as we know it's a smart node
                        reader.nextInt();
                        break;
                    case "versionMajor":
                        mVersionMajor = (short) reader.nextInt();
                        break;
                    case "versionMinor":
                        mVersionMinor = (short) reader.nextInt();
                        break;
                    case "updateLength":
                        mUpdateLength = reader.nextInt();
                        break;
                    case "firmwareSignature":
                        String firmwareUpdateString = reader.nextString();
                        mFirmwareSignature = hexStringToByteArray(firmwareUpdateString);
                        break;
                    default:
                        reader.skipValue();
                }
            }

            reader.endObject();
            reader.close();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "[METADATA] Unable to parse file: File Not Found");
            json.delete();
            return false;
        } catch (IOException e) {
            json.delete();
            Log.e(TAG, "[METADATA] Unable to parse file: IO Error");
            return false;
        }

        if (CCUApp.DEBUG) {
            Log.d(TAG, "[METADATA] [SN:" + mLwMeshAddress
                    + "] [VER:" + mVersionMajor + "." + mVersionMinor
                    + "] [LEN:" + mUpdateLength
                    + "] [SIG:" + byteArrayToHexString(mFirmwareSignature, true) + "]");
        }
        return true;
    }

    /**
     * Checks if the given version matches the version that is being updated to
     *
     * @param major The major version
     * @param minor The minor version
     * @return Whether the major and minor versions match the update
     */
    private boolean versionMatches(short major, short minor) {
        if (major != mVersionMajor) {
            return false;
        } else if (minor != mVersionMinor) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the specified version is valid
     *
     * @param major The major version
     * @param minor The minor version
     * @return True if it is a valid version, false if not
     */
    private boolean validVersion(short major, short minor) {
        if (major < 0) {
            return false;
        }

        if (minor < 0) {
            return false;
        }

        return true;
    }

    /**
     * Processes the binary image file to be sent to CM and SN
     *
     * @param file The file to be processed
     */
    private void setUpdateFile(File file, CCUConstants.OTA_FIRMWARE_COMPONENT firmware) {
        Log.d(TAG, "{STARTUP] Moving binary file to RAM");
        packets = importFile(file, PACKET_LENGTH);

        if (packets == null) {
            Log.d(TAG, "[STARTUP] Failed to move binary file to RAM.");
            resetUpdate();
            return;
        }

        Log.d(TAG, "[STARTUP] Successfully moved binary file to RAM, sending metadata");
        sendFirmwareMetadata(firmware);
    }

    /**
     * Splits a byte array into arrays of a given length
     *
     * @param file         A Java.Io.File to be converted into packets
     * @param packetLength The length of the packets to be generated
     * @return An ArrayList of byte arrays, each of length packetLength
     */
    private ArrayList<byte[]> importFile(File file, int packetLength) {
        if (file == null) {
            return null;
        }

        byte[] fileByteArray = new byte[(int) file.length()];

        try {
            fileByteArray = Files.toByteArray(file);
        } catch (IOException e) {
            Log.e(TAG, "[I/O Exception while reading update file]" + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "[File is too large]" + e.getMessage());
            e.printStackTrace();
        }

        ArrayList<byte[]> rPackets = new ArrayList<>();
        int len = fileByteArray.length;
        int packetNum = len / packetLength;

        if ((packetNum * packetLength) < len) {
            Log.d(TAG, "[STARTUP] [Added packet: " + packetNum + ", to " + (packetNum + 1) + "]");
            packetNum++;
        }

        Log.d(TAG, "[STARTUP] [Number of packets:" + packetNum + "]");

        for (int i = 0; i < packetNum; i++) {
            byte[] packet = new byte[packetLength];

            if (len - packetLength * i > packetLength) {
                System.arraycopy(fileByteArray, i * packetLength, packet, 0, packetLength);
            } else {
                System.arraycopy(fileByteArray, i * packetLength, packet, 0, len - packetLength * i);
            }
            rPackets.add(packet);
        }

        Log.d(TAG, "[STARTUP] [Packets generated:" + rPackets.size() + "]");

        return rPackets;
    }

    /**
     * Handles incoming messaged from the SerialService
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<OTAUpdateService> mSnOtaUpdateServiceReference;

        IncomingHandler(Looper looper, OTAUpdateService OtaUpdateService) {
            super(looper);
            mSnOtaUpdateServiceReference = new WeakReference<>(OtaUpdateService);
        }

        @Override
        public void handleMessage(Message msg) {
            OTAUpdateService OtaUpdateService = mSnOtaUpdateServiceReference.get();
            if (OtaUpdateService != null) {
                SerialService.MESSAGETYPE nMsgType = SerialService.MESSAGETYPE.values()[msg.what];
                Bundle bundle;
                switch (nMsgType) {
                    case CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST:
                        if(timer != null){
                            timer.cancel();
                            isTimerStarted = false;
                        }
                        bundle = msg.getData();
                        snOtaUpdateService.sendPacket(bundle.getInt("lwMeshAddress"), bundle.getInt("sequenceNumber"));
                        break;
                    /*case CM_ERROR_REPORT:
                        bundle = msg.getData();
                        if(snOtaUpdateService.mLwMeshAddress == bundle.getInt("lwMeshAddress")) {
                            snOtaUpdateService.resetUpdate();
                            if (timer != null) {
                                timer.cancel();
                                isTimerStarted = false;
                            }
                        }
                        break;*/
                    case CM_TO_CCU_OVER_USB_DEVICE_REBOOT:
                        if(timer != null){
                            timer.cancel();
                            isTimerStarted = false;
                        }
                        bundle = msg.getData();
                        if (bundle.getInt("lwMeshAddress") == snOtaUpdateService.mLwMeshAddress
                                && snOtaUpdateService.mUpdateInProgress) {
                            short versionMajor = (short) bundle.getInt("versionMajor");
                            short versionMinor = (short) bundle.getInt("versionMinor");
                            CCUKinveyInterface.uploadErrorConditions(new Alert(Alert.AlertType.FIRMWARE_OTA_UPDATE_ENDED, "SYSTEM").setArgs(AlertsData.getCCUName(),snOtaUpdateService.mLwMeshAddress, versionMajor+"."+versionMinor),snOtaUpdateService.mLwMeshAddress);
                            if (snOtaUpdateService.mUpdateWaitingToComplete
                                    && snOtaUpdateService.versionMatches(versionMajor, versionMinor)) {
                                Log.d(TAG, "[UPDATE] [SUCCESSFUL]"
                                        + " [SN:" + snOtaUpdateService.mLwMeshAddress + "]"
                                        + " {PACKETS:" + snOtaUpdateService.mLastSentPacket
                                        + "] Updated to target: " + versionMajor + "." + versionMinor);
                                snOtaUpdateService.resetUpdate();
                                SerialService.getInstance().dettachClient(mMessenger);

                                snOtaUpdateService.stopSelf();


                            } else {
                                Log.d(TAG, "[UPDATE] [FAILED]"
                                        + " [SN:" + snOtaUpdateService.mLwMeshAddress + "]"
                                        + " {PACKETS:" + snOtaUpdateService.mLastSentPacket + "]"
                                        + " [TARGET: " + snOtaUpdateService.mVersionMajor
                                        + "." + snOtaUpdateService.mVersionMinor
                                        + "] [ACTUAL: " + versionMajor + "." + versionMinor + "]");
                                snOtaUpdateService.resetUpdate();
                                SerialService.getInstance().dettachClient(mMessenger);
                                snOtaUpdateService.stopSelf();
                            }
                        }
                        break;
                    default:
                        break;

                }
            }
        }
    }

    /**
     * Downloads a file in the background given a URL
     */
    private class DownloadFileFromURL extends AsyncTask<String, String, String> {
        private final int buffSize = 1024;
        private final int port = 8192;

        /**
         * Sets up a URL connection and downloads a file given the URL and saves it to the file name
         *
         * @param fUrl furl[0] should be the URL to download from, fUrl[1] should be the name the file will have on the file system
         * @return Nothing
         */
        @Override
        protected String doInBackground(String... fUrl) {
            Log.d(TAG, "[DOWNLOAD] [URL: " + fUrl[0] + "]" + " [FILENAME: " + fUrl[1] + "]"+","+mMetadataIsDownloaded+","+mBinaryIsDownloaded);

            if (mMetadataIsDownloaded && mBinaryIsDownloaded) {
                return null;
            }

            int count;
            try {
                URL url = new URL(fUrl[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                InputStream input = new BufferedInputStream(url.openStream(), port);

                OutputStream output = new FileOutputStream(DOWNLOAD_DIR.toString() + "/" + fUrl[1]);

                byte[] data = new byte[buffSize];

                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e(TAG, "[ERROR] " + "[ " + e.getMessage() + "]");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "[DOWNLOAD] Completed="+mMetadataIsDownloaded+","+mBinaryIsDownloaded);

            if (!mMetadataIsDownloaded) {
                Log.d(TAG, "[DOWNLOAD] Metadata downloaded");
                mMetadataIsDownloaded = true;
                runMetadataCheck(DOWNLOAD_DIR, mVersionMajor, mVersionMinor, mVersion,mFirmwareInfo);
            } else if (!mBinaryIsDownloaded) {
                Log.d(TAG, "[DOWNLOAD] Binary downloaded");
                mBinaryIsDownloaded = true;
                runBinaryCheck(DOWNLOAD_DIR, mVersion,mFirmwareInfo);
            }
        }
    }

    private void timerForOtaNonResponse() {
        if (!isTimerStarted) {
            isTimerStarted = true;
            timer = new CountDownTimer(300000, 20000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    Log.d("OTA", "timerForOtaNotUpdate timer = " + millisUntilFinished + "," + (millisUntilFinished / 20000));
                }

                @Override
                public void onFinish() {
                    resetUpdate();

                    CCUKinveyInterface.uploadErrorConditions(new Alert(Alert.AlertType.FIRMWARE_OTA_UPDATE_ENDED, "SYSTEM").setArgs(AlertsData.getCCUName(), mLwMeshAddress,mVersionMajor+"."+mVersionMinor),mLwMeshAddress);
                    if(timer != null )timer.cancel();
                    isTimerStarted = false;
                }
            }.start();
        }
    }
}
