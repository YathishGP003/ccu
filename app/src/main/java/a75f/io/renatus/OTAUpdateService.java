package a75f.io.renatus;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.common.io.Files;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.alerts.AlertGenerateHandler;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbFirmwareMetadataMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbFirmwarePacketMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwarePacketRequest_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwareUpdateAckMessage_t;
import a75f.io.device.serial.FirmwareDeviceType_t;
import a75f.io.device.serial.MessageConstants;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SnRebootIndicationMessage_t;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.bo.util.ByteArrayUtils;
import a75f.io.usbserial.UsbService;

import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_ENDED;
import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_STARTED;

public class OTAUpdateService extends IntentService {

    //Constants
    public static final String METADATA_FILE_FORMAT = ".meta";
    public static final String BINARY_FILE_FORMAT = ".bin";

    private static final String TAG = "OTAUpdateService";

    private static final String DOWNLOAD_BASE_URL = "http://updates.75fahrenheit.com/";
    private static final File DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    //Update variables
    private static ArrayList<Integer> mLwMeshAddresses;
    private static int mCurrentLwMeshAddress;

    private static int mLastSentPacket = -1;
    private static ArrayList<byte[]> packets;      //Where the decomposed binary is stored in memory
    private static short mVersionMajor = -1;
    private static short mVersionMinor = -1;

    private static int mUpdateLength = -1;         //Binary length (bytes)
    private static byte[] mFirmwareSignature = {};
    private static FirmwareDeviceType_t mFirmwareDeviceType;

    private static long mMetadataDownloadId = -1;
    private static long mBinaryDownloadId = -1;

    private static boolean mUpdateInProgress = false;
    private static boolean mUpdateWaitingToComplete = false;

    public OTAUpdateService() {
        super("OTAUpdateService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String action = intent.getAction();
        Log.i(Globals.TAG, "onHandleIntent  action  "+action);
        if(action == null) {
            return;
        }

        /* The service has been launched from an activity */
        if(action.equals(Globals.IntentActions.ACTIVITY_MESSAGE)) {
            handleOtaUpdateStartRequest(intent);
        }
        /* An activity has requested the OTA update to end (for debugging) */
        else if(action.equals(Globals.IntentActions.ACTIVITY_RESET)) {
            Log.i(Globals.TAG, "completeUpdate : calling  ACTIVITY_RESET ");
            resetUpdateVariables();
            completeUpdate();
        }
        /* The service has been launched from a PubNub notification */
        else if(action.equals(Globals.IntentActions.PUBNUB_MESSAGE)) {
            Log.i(Globals.TAG, " PUBNUB_MESSAGE BC Received ");
            handleOtaUpdateStartRequest(intent);
        }
        /* The service has been launched in response to a CM disconnect */
        else if(action.equals(UsbService.ACTION_USB_DETACHED)) {
            Log.i(Globals.TAG, "completeUpdate : calling  ACTION_USB_DETACHED ");
            resetUpdateVariables();
            completeUpdate();
        }
        /* The service has been launched in response to a file download */
        else if(action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            handleFileDownloadComplete(intent);
        }
        /* The service has been launched in response to OTA update timeout */
        else if(action.equals(Globals.IntentActions.OTA_UPDATE_TIMED_OUT)) {
            Log.i(Globals.TAG, "completeUpdate : calling  OTA_UPDATE_TIMED_OUT ");
            resetUpdateVariables();
            completeUpdate();
        }
        /* The OTA update is in progress, and is being notified from the CM */
        else if(action.equals(Globals.IntentActions.LSERIAL_MESSAGE)) {
            MessageType eventType = (MessageType) intent.getSerializableExtra("eventType");
            byte[] eventBytes = intent.getByteArrayExtra("eventBytes");
            Log.i(Globals.TAG, "onHandleIntent  eventType  "+eventType);
            switch(eventType) {
                case CM_TO_CCU_OVER_USB_FIRMWARE_UPDATE_ACK:
                    handleOtaUpdateAck(eventBytes);
                    break;

                case CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST:
                    handlePacketRequest(eventBytes);
                    break;

                case CM_TO_CCU_OVER_USB_SN_REBOOT:
                    handleNodeReboot(eventBytes);
                    break;
            }
        }
    }

    private void handleFileDownloadComplete(Intent intent) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        if(id == -1) {
            return;
        }

        Cursor c = downloadManager.query(new DownloadManager.Query().setFilterById(id));
        c.moveToFirst();

        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
        if(status != DownloadManager.STATUS_SUCCESSFUL) {
            int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
            c.close();

            Log.d(TAG, "[DOWNLOAD] Download failed, reason: " + reason);

            //TODO retry a couple of times, depending on error
            Log.i(Globals.TAG, " Download failed, reason: so reseting the status by calling  completeUpdate ");
            resetUpdateVariables();
            completeUpdate();
            return;
        }

        c.close();

        if(id == mMetadataDownloadId) {
            Log.d(TAG, "[DOWNLOAD] Metadata downloaded");

            runMetadataCheck(DOWNLOAD_DIR, mVersionMajor, mVersionMinor, mFirmwareDeviceType);
        }

        else if(id == mBinaryDownloadId) {
            Log.d(TAG, "[DOWNLOAD] Binary downloaded");

            runBinaryCheck(DOWNLOAD_DIR, mVersionMajor, mVersionMinor, mFirmwareDeviceType);
        }
    }

    private void handleOtaUpdateAck(byte[] eventBytes) {
        CmToCcuOverUsbFirmwareUpdateAckMessage_t msg = new CmToCcuOverUsbFirmwareUpdateAckMessage_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        if(msg.lwMeshAddress.get() == mCurrentLwMeshAddress) {
            Log.d(TAG, "[UPDATE] CM has acknowledged update");
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_CM_ACK));
        }
    }

    private void handlePacketRequest(byte[] eventBytes) {
        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_PACKET_REQ));

        CmToCcuOverUsbFirmwarePacketRequest_t msg = new CmToCcuOverUsbFirmwarePacketRequest_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        Log.d(TAG, "[UPDATE] CM asks for packet " + msg.sequenceNumber.get() + " of "
                + (mUpdateLength / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE) );

        Log.i(Globals.TAG, "[UPDATE] CM asks for packet " + msg.sequenceNumber.get() + " of "
                + (mUpdateLength / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE) );

        sendPacket(msg.lwMeshAddress.get(), msg.sequenceNumber.get());
    }

    private void handleNodeReboot(byte[] eventBytes) {
        Log.i(Globals.TAG, "handleNodeReboot called ");
        SnRebootIndicationMessage_t msg = new SnRebootIndicationMessage_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        if ( (msg.smartNodeAddress.get() == mCurrentLwMeshAddress) && mUpdateInProgress) {
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT));

            short versionMajor = msg.smartNodeMajorFirmwareVersion.get();
            short versionMinor = msg.smartNodeMinorFirmwareVersion.get();
            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            String ccuName = ccu.get("dis").toString();
            AlertGenerateHandler.handleMessage(FIRMWARE_OTA_UPDATE_ENDED, "Firmware OTA update for"+" "+ccuName+" "+"ended for smart node"+" "+ +msg.smartNodeAddress.get()+" "+"with version"+" "+versionMajor + "." + versionMinor);

            if (mUpdateWaitingToComplete && versionMatches(versionMajor, versionMinor)) {
                Log.d(TAG, "[UPDATE] [SUCCESSFUL]"
                        + " [SN:" + mCurrentLwMeshAddress + "]"
                        + " [PACKETS:" + mLastSentPacket
                        + "] Updated to target: " + versionMajor + "." + versionMinor);

                moveUpdateToNextNode();

            } else {
                Log.d(TAG, "[UPDATE] [FAILED]"
                        + " [SN:" + mCurrentLwMeshAddress + "]"
                        + " [PACKETS:" + mLastSentPacket + "]"
                        + " [TARGET: " + mVersionMajor
                        + "." + mVersionMinor
                        + "] [ACTUAL: " + versionMajor + "." + versionMinor + "]");

                moveUpdateToNextNode();
            }
        }
    }

    /**
     * Takes an Intent and parses its extra data to determine what type of device is being
     * updated, and to what version
     *
     * @param intent The Intent which started this OTA update
     */
    private void handleOtaUpdateStartRequest(Intent intent) {
        Log.i(Globals.TAG, " handleOtaUpdateStartRequest ");
        String id = intent.getStringExtra("id");
        String firmwareVersion = intent.getStringExtra("firmwareVersion");
        String cmdLevel = intent.getStringExtra("cmdLevel");

        if(id == null || firmwareVersion == null) {
            return;
        }

        try {
            String versionNumStr = firmwareVersion.split("_v")[1];                 // e.g. "SmartNode_v1.0" -> "1.0"

            int versionMajor = Integer.parseInt(versionNumStr.split("\\.")[0]);   // e.g. "1.0" -> 1
            int versionMinor = Integer.parseInt(versionNumStr.split("\\.")[1]);   // e.g. "1.0" -> 0

            mVersionMajor = (short) versionMajor;
            mVersionMinor = (short) versionMinor;

        } catch(NullPointerException e) {
            // Version parsing failed
            e.printStackTrace();
            return;
        }
        Log.i(Globals.TAG, " firmwareVersion  "+firmwareVersion+" cmdLevel : "+cmdLevel);
        if(firmwareVersion.startsWith("SmartNode_")) {
            mFirmwareDeviceType = FirmwareDeviceType_t.SMART_NODE_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType);
        }
        else if(firmwareVersion.startsWith("Itm_") || firmwareVersion.startsWith("itm_")) {
            mFirmwareDeviceType = FirmwareDeviceType_t.ITM_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType);
        } else if(firmwareVersion.startsWith("HyperStat_")) {
            mFirmwareDeviceType = FirmwareDeviceType_t.HYPER_STAT_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType);
        }
    }

    /**
     * Starts an OTA update for the specified device, to the specified version
     * Fails if the address is invalid, if there is currently an update in progress, or if the files
     * do not exist
     *
     * @param id      The guid of corresponding level
     * @param versionMajor The major version of the new firmware
     * @param versionMinor The minor version of the new firmware
     * @param deviceType   The type of device being updated
     */
    private void startUpdate(String id, String updateLevel, int versionMajor, int versionMinor, FirmwareDeviceType_t deviceType) {
        String filename = makeFileName(versionMajor, versionMinor, deviceType);
        Log.i(Globals.TAG, " filename   "+filename);
        Log.i(Globals.TAG, " mUpdateInProgress    "+mUpdateInProgress);
        Log.d(TAG, "[VALIDATION] Validating update instructions: " + filename);
        if (mUpdateInProgress) {
            Log.d(TAG, "[VALIDATION] Update already in progress");
            Log.i(Globals.TAG, " [VALIDATION] Update already in progress    ");
            return;
        }

        if (!validVersion((short) versionMajor, (short) versionMinor)) {
            Log.d(TAG, "[VALIDATION] Invalid version: " + versionMajor + "." + versionMinor);
            Log.i(Globals.TAG, "[VALIDATION] Invalid version: " + versionMajor + "." + versionMinor);
            resetUpdateVariables();
            return;
        }

        Log.d(TAG, "[VALIDATION] Valid version");
        Log.i(Globals.TAG, "[VALIDATION] Valid version  updateLevel : +"+updateLevel);

        if(mLwMeshAddresses == null) {
            mLwMeshAddresses = new ArrayList<>();
        }

        //determine how many devices are going to be updated
        switch(updateLevel) {
            case "site":
            case "system":
                //update everything
                for(Floor floor : HSUtil.getFloors()) {
                    for(Zone zone : HSUtil.getZones(floor.getId())) {
                        for(Device device : HSUtil.getDevices(zone.getId())) {

                            if(device.getMarkers().contains( deviceType.getHsMarkerName() )) {
                                Log.d(TAG, "[VALIDATION] Adding device " + device.getAddr() + " to update");
                                mLwMeshAddresses.add(Integer.parseInt(device.getAddr()));
                            }

                        }
                    }
                }
                break;

            case "zone":
                //update all nodes in the same zone as the specified node
                for(Device device : HSUtil.getDevices("@"+id)) {
                    if(device.getMarkers().contains( deviceType.getHsMarkerName() )) {
                        Log.d(TAG, "[VALIDATION] Adding device " + device.getAddr() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(device.getAddr()));
                    }
                }
                break;

            case "equip":
            case "module":
                //update just the one node
                Equip equip = HSUtil.getEquipInfo("@"+id);
                Device device = HSUtil.getDevice(Short.parseShort(equip.getGroup()));
                    if(device.getMarkers().contains( deviceType.getHsMarkerName() )) {
                        Log.d(TAG, "[VALIDATION] Adding device " + equip.getGroup() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(equip.getGroup()));
                    }
                break;
        }
        Log.i(Globals.TAG, "mLwMeshAddresses No Of device found"+mLwMeshAddresses.size());
        if(mLwMeshAddresses.isEmpty()) {
            Log.d(TAG, "[VALIDATION] Could not find device " + id + " at level " + updateLevel);
            resetUpdateVariables();
            return;
        }

        moveUpdateToNextNode();
        Log.i(Globals.TAG, "sendBroadcast for OTA_UPDATE_START ");
        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_START));
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
    private void runMetadataCheck(File dir, int versionMajor, int versionMinor, FirmwareDeviceType_t deviceType) {
        Log.i(Globals.TAG, "runMetadataCheck  called ");
        String filename = makeFileName(versionMajor, versionMinor, deviceType);

        Log.d(TAG, "[METADATA] Running metadata check on file: " + filename);
        Log.i(Globals.TAG, "[METADATA] Running metadata check on file: " + filename);
        File metadata = findFile(dir, filename, METADATA_FILE_FORMAT);

        if (metadata == null) {
            Log.d(TAG, "[METADATA] File not found, downloading metadata");
            Log.i(Globals.TAG, "[METADATA] File not found, downloading metadata");
            mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
            return;

        } else {
            Log.d(TAG, "[METADATA] Extracting firmware metadata");
            Log.i(Globals.TAG, "[METADATA] Extracting firmware metadata");
            boolean isExtracted = extractFirmwareMetadata(metadata);
            if (!isExtracted || !versionMatches( (short) versionMajor, (short) versionMinor) ) {
                Log.d(TAG, "[METADATA] Incorrect firmware metadata, downloading correct metadata");
                Log.i(Globals.TAG, "[METADATA] Incorrect firmware metadata, downloading correct metadata");
                mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
                return;
            }
        }

        Log.d(TAG, "[METADATA] Metadata passed check, starting binary check");
        Log.i(Globals.TAG, "[METADATA] Metadata passed check, starting binary check");
        runBinaryCheck(dir, versionMajor, versionMinor, deviceType);
    }

    /**
     * Checks if the binary file is present, and if it matches the metadata
     * If those conditions aren't met, downloads new binary file
     * If they are met, continues to moving the binary file into RAM
     *
     * @param dir The directory to search for the binary file
     */
    private void runBinaryCheck(File dir, int versionMajor, int versionMinor, FirmwareDeviceType_t deviceType) {
        String filename = makeFileName(versionMajor, versionMinor, deviceType);

        Log.d(TAG, "[BINARY] Running binary check on file: " + filename);
        Log.i(Globals.TAG, "[BINARY] Running binary check on file: " + filename);
        File binary = findFile(dir, filename, BINARY_FILE_FORMAT);

        if (binary == null) {
            Log.d(TAG, "[BINARY] File not found, downloading binary");
            Log.i(Globals.TAG, "[BINARY] File not found, downloading binary");
            mBinaryDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
            return;

        } else {
            Log.d(TAG, "[BINARY] Checking binary length match: " + binary.length() + ", " + mUpdateLength);
            Log.i(Globals.TAG, "[BINARY] Checking binary length match: " + binary.length() + ", " + mUpdateLength);
            if (binary.length() != mUpdateLength) {
                Log.d(TAG, "[BINARY] Incorrect firmware binary, downloading correct binary");
                Log.i(Globals.TAG,  "[BINARY] Incorrect firmware binary, downloading correct binary");
                mMetadataDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
                return;
            }
        }

        Log.d(TAG, "[BINARY] Binary passed check");
        Log.i(Globals.TAG, "[BINARY] Binary passed check");

        Log.d(TAG, "[STARTUP] Starting to update device with address " + mCurrentLwMeshAddress);
        Log.i(Globals.TAG, "[STARTUP] Starting to update device with address " + mCurrentLwMeshAddress);

        //TODO notify something (PubNub?) that an update has started
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        String ccuName = ccu.get("dis").toString();
        Log.i(Globals.TAG, "sending FIRMWARE_OTA_UPDATE_STARTED alert for" + mCurrentLwMeshAddress);
        AlertGenerateHandler.handleMessage(FIRMWARE_OTA_UPDATE_STARTED, "Firmware OTA update for"+" "+ccuName+" "+
                "started for "+deviceType.getUpdateFileName()+" "+mCurrentLwMeshAddress+" "+"with version"+" "+versionMajor + "." + versionMinor);
        Log.i(Globals.TAG, "reset mUpdateInProgress :" + mUpdateInProgress +" mLastSentPacket: "+mLastSentPacket);
        mUpdateInProgress = true;
        mLastSentPacket = -1;

        setUpdateFile(binary, deviceType);
    }

    /**
     * Starts downloading the specified file
     *
     * @param filename   The file name
     * @param deviceType The type of device this file is targeting
     * @param fileFormat The file extension type (e.g. ".meta" or ".bin")
     * @return The DownloadManager ID for this file
     */
    private long startFileDownload(String filename, FirmwareDeviceType_t deviceType, String fileFormat) {
        Log.i(Globals.TAG, "startFileDownload  called ");
        String filePathSystem = DOWNLOAD_DIR.toString() + "/" + filename + fileFormat;
        String filePathUrl = DOWNLOAD_BASE_URL + deviceType.getUpdateUrlDirectory() + filename + fileFormat;
        Log.i(Globals.TAG, "[DOWNLOAD] Starting download of file  "+filePathUrl);
        Log.d(TAG, "[DOWNLOAD] Starting download of file " + filePathUrl);

        DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(filePathUrl))
                .setTitle(filename + fileFormat)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationUri(Uri.fromFile(new File(filePathSystem)));

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        return downloadManager.enqueue(downloadRequest);
    }

    /**
     * Finds a file (if it exists) in the specified directory
     *
     * @param dir The directory to search
     * @return The file, if found, or null, if not
     */
    private File findFile(File dir, final String fileName, String fileFormat) {
        try {
            for(File file : dir.listFiles()) {
                Log.d(TAG,"Compare files: " + file.getName() + ", " + fileName);
                if(file.getName().startsWith(fileName) && file.getName().endsWith(fileFormat))
                    return file;
            }
            return null;

        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "[STARTUP] File not found in " + dir);
            return null;
        }
    }

    /**
     * Deletes all firmware files for the specified device type in the specified directory
     * @param dir        The directory where OTA update files are downloaded
     * @param deviceType The type of device whose files are to be deleted
     */
    private void deleteFilesByDeviceType(File dir, FirmwareDeviceType_t deviceType){
        Log.i(Globals.TAG, "deleteFilesByDeviceType: called ");
        try {
            for (File file : dir.listFiles()) {
                if (file.getName().startsWith(deviceType.getUpdateFileName() + "_v")) {
                    file.delete();
                }
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Splits a byte array into arrays of a given length
     *
     * @param file         A java.io.File to be converted into packets
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
            Log.e(TAG, "[STARTUP] I/O exception while importing update file: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "[STARTUP] File is too large: " + e.getMessage());
            e.printStackTrace();
        }

        ArrayList<byte[]> rPackets = new ArrayList<>();
        int len = fileByteArray.length;
        int packetNum = len / packetLength;

        if ((packetNum * packetLength) < len) {
            Log.d(TAG, "[STARTUP] Added packet: " + packetNum + ", to " + (packetNum + 1) + "]");
            packetNum++;
        }

        Log.d(TAG, "[STARTUP] File imported, number of packets: " + packetNum);

        for (int i = 0; i < packetNum; i++) {
            byte[] packet = new byte[packetLength];

            if (len - packetLength * i > packetLength) {
                System.arraycopy(fileByteArray, i * packetLength, packet, 0, packetLength);
            } else {
                System.arraycopy(fileByteArray, i * packetLength, packet, 0, len - packetLength * i);
            }
            rPackets.add(packet);
        }

        Log.d(TAG, "[STARTUP] Packets generated: " + rPackets.size());

        return rPackets;
    }

    /**
     * Loads a binary image into RAM and parses it into packets in preparation for an update
     *
     * @param file       The file to be loaded and processed
     * @param deviceType The type of device being updated
     */
    private void setUpdateFile(File file, FirmwareDeviceType_t deviceType) {
        Log.d(TAG, "[STARTUP] Moving binary file to RAM");
        Log.i(Globals.TAG, "[STARTUP] Moving binary file to RAM");
        packets = importFile(file, MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE);

        if (packets == null) {
            Log.d(TAG, "[STARTUP] Failed to move binary file to RAM");
            resetUpdateVariables();
            return;
        }

        Log.d(TAG, "[STARTUP] Successfully moved binary file to RAM, sending metadata");
        Log.i(Globals.TAG, "[STARTUP] Successfully moved binary file to RAM, sending metadata");
        sendFirmwareMetadata(deviceType);
    }

    /**
     * Extracts the firmware metadata from the json ".meta" file
     *
     * @param json The json file with the information
     * @return Whether the extraction was successful or not
     */
    private boolean extractFirmwareMetadata(File json) {
        try {
            JsonReader reader = new JsonReader(new FileReader(json));

            reader.beginObject();

            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "deviceType":
                        //Not particularly useful, since we need to know the device type before
                        //  downloading the firmware, to select between /sn_fw, /itm_fw, etc.
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
                        mFirmwareSignature = ByteArrayUtils.hexStringToByteArray(firmwareUpdateString);
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

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[METADATA] [SN:" + mCurrentLwMeshAddress
                    + "] [VER:" + mVersionMajor + "." + mVersionMinor
                    + "] [LEN:" + mUpdateLength
                    + "] [SIG:" + ByteArrayUtils.byteArrayToHexString(mFirmwareSignature, true) + "]");
        }
        return true;
    }

    /**
     * Sends the firmware metadata to the CM
     *
     * @param firmware The type of device being updated
     */
    private void sendFirmwareMetadata(FirmwareDeviceType_t firmware) {
        Log.i(Globals.TAG, "sendFirmwareMetadata "+firmware );
        CcuToCmOverUsbFirmwareMetadataMessage_t message = new CcuToCmOverUsbFirmwareMetadataMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_FIRMWARE_METADATA);
        message.lwMeshAddress.set(mCurrentLwMeshAddress);

        message.metadata.deviceType.set(firmware);
        message.metadata.majorVersion.set(mVersionMajor);
        message.metadata.minorVersion.set(mVersionMinor);
        message.metadata.lengthInBytes.set(mUpdateLength);

        message.metadata.setSignature(mFirmwareSignature);
        Log.i(Globals.TAG,
                "[METADATA] [SN:" + mCurrentLwMeshAddress + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(message.getByteBuffer().array(), true) + "]");
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[METADATA] [SN:" + mCurrentLwMeshAddress + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(message.getByteBuffer().array(), true) + "]");
        }

        try {
            Log.i(Globals.TAG, "sendStructToCM  "+message.toString() );
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            Log.e(TAG, "[METADATA] FAILED TO SEND");
        }
    }

    /**
     * Sends the corresponding packet over serial to the CM
     *
     * @param lwMeshAddress The address to send the update packet to
     * @param packetNumber  The packet to send
     */
    private void sendPacket(int lwMeshAddress, int packetNumber) {

        if (lwMeshAddress != mCurrentLwMeshAddress) {
            Log.d(TAG, "[UPDATE] Received packet request from address " + lwMeshAddress + ", instead of " + mCurrentLwMeshAddress);
            return;
        }
        if (mUpdateWaitingToComplete && packetNumber == packets.size() /*- 2*/) {
            Log.d(TAG, "[UPDATE] Received packet request while waiting for the update to complete");
            return;
        }
        if (packetNumber < 0 || packetNumber > packets.size()) {
            Log.d(TAG, "[UPDATE] Received request for invalid packet: " + packetNumber);
            return;
        }
        if (!mUpdateWaitingToComplete && (packetNumber == (packets.size() - 1))) {
            Log.d(TAG, "[UPDATE] Received request for final packet");
            mUpdateWaitingToComplete = true;
        }

        CcuToCmOverUsbFirmwarePacketMessage_t message = new CcuToCmOverUsbFirmwarePacketMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_FIRMWARE_PACKET);
        message.lwMeshAddress.set(lwMeshAddress);
        message.sequenceNumber.set(packetNumber);

        message.setPacket(packets.get(packetNumber));

        if (packetNumber > mLastSentPacket) {
            mLastSentPacket = packetNumber;
            if (BuildConfig.DEBUG) {
                if (packetNumber % 100 == 0) {
                    Log.d(TAG, "[UPDATE] [SN:" + lwMeshAddress + "]" + "PS:"+packets.size()+","+message.packet.length+","+message.sequenceNumber.get()+" [PN:" + mLastSentPacket
                            + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(packets.get(packetNumber), true) +  "]");

                    Log.i(Globals.TAG,
                            "[UPDATE] [SN:" + lwMeshAddress + "]" + "PS:"+packets.size()+","+message.packet.length+","+message.sequenceNumber.get()+" [PN:" + mLastSentPacket
                            + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(packets.get(packetNumber), true) +  "]");
                }
            }
        }

        try {
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            Log.e(TAG, "[UPDATE] [SN:" + lwMeshAddress + "] [PN:" + packetNumber + "] [FAILED]");
        }
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
        return (major >= 0) && (minor >= 0);
    }

    private String makeFileName(int versionMajor, int versionMinor, FirmwareDeviceType_t deviceType) {
        return deviceType.getUpdateFileName() + "_v" + versionMajor + "." + versionMinor;
    }

    private void moveUpdateToNextNode() {
        Log.i(Globals.TAG, "moveUpdateToNextNode called ");
        resetUpdateVariables();

        if(mLwMeshAddresses.isEmpty()) {
            Log.i(Globals.TAG, " moveUpdateToNextNode  mLwMeshAddresses.isEmpty() completeUpdate");
            completeUpdate();
            return;
        }

        mCurrentLwMeshAddress = mLwMeshAddresses.get(0);
        mLwMeshAddresses.remove(0);
        Log.i(Globals.TAG, "mCurrentLwMeshAddress "+mCurrentLwMeshAddress + " mLwMeshAddresses : "+mLwMeshAddresses.size());

        runMetadataCheck(DOWNLOAD_DIR, mVersionMajor, mVersionMinor, mFirmwareDeviceType);
    }

    /**
     * Clears update variables
     */
    private void resetUpdateVariables() {
        mCurrentLwMeshAddress = -1;

        mLastSentPacket = -1;

        mUpdateLength = -1;

        mMetadataDownloadId = -1;
        mBinaryDownloadId = -1;

        mUpdateWaitingToComplete = false;

        Log.d(TAG, "[RESET] Reset OTA update status");
    }

    /**
     *
     */
    private void completeUpdate() {
        Log.i(Globals.TAG, "completeUpdate : called ");
        deleteFilesByDeviceType(DOWNLOAD_DIR, mFirmwareDeviceType);

        mVersionMajor = -1;
        mVersionMinor = -1;

        mUpdateLength = -1;

        mMetadataDownloadId = -1;
        mBinaryDownloadId = -1;

        mUpdateInProgress = false;
        Log.i(Globals.TAG, "broadcasting OTA_UPDATE_COMPLETE ");
        Intent completeIntent = new Intent(Globals.IntentActions.OTA_UPDATE_COMPLETE);
        sendBroadcast(completeIntent);

        Log.d(TAG, "[RESET] No nodes remain, OTA update is complete");
    }
}
