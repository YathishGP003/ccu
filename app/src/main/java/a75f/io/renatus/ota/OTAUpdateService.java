package a75f.io.renatus.ota;

import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_ENDED;
import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_STARTED;
import static a75f.io.renatus.ota.OtaCache.CMD_LEVEL;
import static a75f.io.renatus.ota.OtaCache.CMD_TYPE;
import static a75f.io.renatus.ota.OtaCache.FIRMWARE_VERSION;
import static a75f.io.renatus.ota.OtaCache.ID;
import static a75f.io.renatus.ota.OtaCache.MESSAGE_ID;
import static a75f.io.renatus.ota.OtaCache.RETRY_COUNT;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.common.io.Files;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.alerts.AlertGenerateHandler;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbFirmwareMetadataMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbFirmwarePacketMessage_t;
import a75f.io.device.serial.CmToCcuOtaStatus_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwarePacketRequest_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwareUpdateAckMessage_t;
import a75f.io.device.serial.FirmwareComponentType_t;
import a75f.io.device.serial.FirmwareDeviceType_t;
import a75f.io.device.serial.MessageConstants;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SnRebootIndicationMessage_t;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.util.ByteArrayUtils;
import a75f.io.logic.diag.otastatus.OtaState;
import a75f.io.logic.diag.otastatus.OtaStatus;
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint;
import a75f.io.usbserial.UsbService;

public class OTAUpdateService extends IntentService {

    //Constants
    public static final String METADATA_FILE_FORMAT = ".meta";
    public static final String BINARY_FILE_FORMAT = ".bin";

    private static final String TAG = "OTA_PROCESS";

    private static final String DOWNLOAD_BASE_URL = "http://updates.75fahrenheit.com/";
    private static final File DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private static long RETRY_TIME = 5 * 60000;
    private static long DELAY = 2 * 60000;
    private static int NO_OF_RETRY = 5;
    //Update variables
    private static ArrayList<Integer> mLwMeshAddresses;
    private static int mCurrentLwMeshAddress;

    private static int mLastSentPacket = -1;
    private static ArrayList<byte[]> packets;      //Where the decomposed binary is stored in memory
    private static short mVersionMajor = -1;
    private static short mVersionMinor = -1;

    private static int mUpdateLength = -1;         //Binary length (bytes)
    private static byte[] mFirmwareSignature = {};
    private static FirmwareComponentType_t mFirmwareDeviceType;

    private static long mMetadataDownloadId = -1;
    private static long mBinaryDownloadId = -1;

    private static boolean mUpdateInProgress = false;
    private static boolean mUpdateWaitingToComplete = false;
    private static boolean otaRequestProcessInProgress = false;
    private static String currentOtaRequest;
    private static String currentRunningRequestType;
    public static final Queue<Intent> otaRequestsQueue = new LinkedList<>();
    private boolean retryHandlerStarted = false;
    private Timer retryHandler;
    public OTAUpdateService() {
        super("OTAUpdateService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String action = intent.getAction();
        if(action == null) {
            return;
        }

        /* The service has been launched from an activity */
        if(action.equals(Globals.IntentActions.ACTIVITY_MESSAGE)) {
            addRequest(intent);
        }
        /* An activity has requested the OTA update to end (for debugging) */
        else if(action.equals(Globals.IntentActions.ACTIVITY_RESET)) {
            resetUpdateVariables();
            completeUpdate();
        }
        /* The service has been launched from a PubNub notification */
        else if(action.equals(Globals.IntentActions.PUBNUB_MESSAGE)) {
            addRequest(intent);
        }
        /* The service has been launched in response to a CM disconnect */
        else if(action.equals(UsbService.ACTION_USB_DETACHED)) {
            if (!OTAUpdateService.isCmOtaInProgress()) {
                resetUpdateVariables();
                completeUpdate();
            }
        }
        /* The service has been launched in response to a file download */
        else if(action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            handleFileDownloadComplete(intent);
        }
        /* The service has been launched in response to OTA update timeout */
        else if(action.equals(Globals.IntentActions.OTA_UPDATE_TIMED_OUT)) {
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_TIMEOUT, mCurrentLwMeshAddress);
            resetUpdateVariables();
            completeUpdate();
        }
        /* The OTA update is in progress, and is being notified from the CM */
        else if(action.equals(Globals.IntentActions.LSERIAL_MESSAGE)) {

            MessageType eventType = (MessageType) intent.getSerializableExtra("eventType");
            byte[] eventBytes = intent.getByteArrayExtra("eventBytes");

            switch(eventType) {
                case CM_TO_CCU_OVER_USB_FIRMWARE_UPDATE_ACK:
                    handleOtaUpdateAck(eventBytes);
                    break;

                case CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST:
                    handlePacketRequest(eventBytes);
                    break;

                case CM_TO_CCU_OTA_STATUS:
                    handleCmToDeviceProgress(eventBytes);
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

        if (id == -1) {
            return;
        }

        Cursor c = downloadManager.query(new DownloadManager.Query().setFilterById(id));

        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));


            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                c.close();

            Log.d(TAG, "[DOWNLOAD] Download failed, reason: " + reason);
            OtaCache cache = new OtaCache();
            cache.updateRetryCount(currentOtaRequest);

            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_FIRMWARE_DOWNLOAD_FAILED, mCurrentLwMeshAddress);
            resetUpdateVariables();
            completeUpdate();
            return;
        }

            c.close();

            if (id == mMetadataDownloadId) {
                Log.d(TAG, "[DOWNLOAD] Metadata downloaded");

                runMetadataCheck(mVersionMajor, mVersionMinor, mFirmwareDeviceType);
            } else if (id == mBinaryDownloadId) {
                Log.d(TAG, "[DOWNLOAD] Binary downloaded");

                runBinaryCheck(mVersionMajor, mVersionMinor, mFirmwareDeviceType);
            }
        }
    }
    private void handleOtaUpdateAck(byte[] eventBytes) {
        CmToCcuOverUsbFirmwareUpdateAckMessage_t msg = new CmToCcuOverUsbFirmwareUpdateAckMessage_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        Log.i(TAG,"msg.lwMeshAddress.get()  "+msg.lwMeshAddress.get()+"   "+mCurrentLwMeshAddress);
        if(msg.lwMeshAddress.get() == mCurrentLwMeshAddress) {
            Log.d(TAG, "[UPDATE] CM has acknowledged update");
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_CM_ACK));
            // We will receive multiple packets ack so always update the value need to check how to do this
        }
    }

    private void updateOtaStatusToOtaStarted() {
        if(OtaStatusDiagPoint.Companion.getCurrentOtaStatus(mCurrentLwMeshAddress) != OtaStatus.OTA_CCU_TO_CM_UPDATE_STARTED.ordinal()) {
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CCU_TO_CM_UPDATE_STARTED, mCurrentLwMeshAddress);
        }
    }
    private void handlePacketRequest(byte[] eventBytes) {
        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_PACKET_REQ));
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        if ( mUpdateLength == -1 ){
            downloadFileIfMissing();
        } else {
            CmToCcuOverUsbFirmwarePacketRequest_t msg = new CmToCcuOverUsbFirmwarePacketRequest_t();
            msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

            Log.d(TAG, "[UPDATE] CM asks for packet " + msg.sequenceNumber.get() + " of "
                    + (mUpdateLength / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE));

            sendPacket(msg.lwMeshAddress.get(), msg.sequenceNumber.get());
            OtaStatusDiagPoint.Companion.updateCcuToCmOtaProgress(
                    (mUpdateLength / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE),
                    msg.sequenceNumber.get(),
                    msg.lwMeshAddress.get());
        }
    }


    private void downloadFileIfMissing() {
        OtaCache cache = new OtaCache();
        FirmwareComponentType_t deviceType = FirmwareComponentType_t.values()[cache.getDeviceType()];
        int versionMinor = cache.getMinorVersion();
        int versionMajor = cache.getMajorVersion();
        mCurrentLwMeshAddress = cache.getRunningNodeAddress();
        Set<String> meshList = cache.getMeshList();
        meshList.forEach(i -> mLwMeshAddresses.add(Integer.parseInt(i)));
        String filename = makeFileName(versionMajor, versionMinor, deviceType);

        File metadata = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, METADATA_FILE_FORMAT);
        if (metadata == null) {
            mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
            return;
        } else {
            boolean isExtracted = extractFirmwareMetadata(metadata);
            if (!isExtracted || !versionMatches( (short) versionMajor, (short) versionMinor) ) {
                Log.d(TAG, "[METADATA] Incorrect firmware metadata, downloading correct metadata");
                mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
                return;
            }
        }
        File binary = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, BINARY_FILE_FORMAT);
        if (binary == null) {
            mBinaryDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
        } else {
            packets = importFile(binary, MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE);
        }
    }

    private void handleCmToDeviceProgress(byte[] eventBytes){
        LSerial.fromBytes(eventBytes, CmToCcuOtaStatus_t.class);
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        CmToCcuOtaStatus_t msg = new CmToCcuOtaStatus_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        Log.i(L.TAG_CCU_OTA_PROCESS, " CM to Device process : State : "+msg.currentState.get() + " Data : "+msg.data.get());
        if ( msg.currentState.get() == OtaState.FIRMWARE_RECEIVED.ordinal()
                || msg.currentState.get() == OtaState.FIRMWARE_COPY_AVAILABLE.ordinal()) {
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CCU_TO_CM_FIRMWARE_RECEIVED, mCurrentLwMeshAddress);
            short currentBand = L.ccu().getSmartNodeAddressBand();
            if (mCurrentLwMeshAddress != (currentBand + 99)){
                OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CM_TO_DEVICE_PACKET_STARTED, mCurrentLwMeshAddress);
            }
            mUpdateWaitingToComplete = true;
        } else if ( msg.currentState.get() == OtaState.CM_DEVICE_IN_PROGRESS.ordinal()) {
            OtaStatusDiagPoint.Companion.updateCmToDeviceOtaProgress(
                    mUpdateLength / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE,
                    msg.data.get(), mCurrentLwMeshAddress
            );
        } else if ( msg.currentState.get() == OtaState.CM_DEVICE_TIMEOUT.ordinal()) {
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_TIMED_OUT));
        }
    }

    private void handleNodeReboot(byte[] eventBytes) {

        SnRebootIndicationMessage_t msg = new SnRebootIndicationMessage_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        if(msg.smartNodeDeviceType == null){
            Log.d(TAG, " SmartNode device type is null" );
            return;
        }
        Log.d(TAG, "Node: "+msg.smartNodeAddress.get() + " Device :"+msg.smartNodeDeviceType.get() + " Device Rebooted");
        if (msg.smartNodeDeviceType.get() != FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE
                && mCurrentLwMeshAddress != msg.smartNodeAddress.get()) {
            Log.d(TAG, mCurrentLwMeshAddress+ " != "+msg.smartNodeAddress.get());
            return;
        }
        Log.d(TAG, "mUpdateInProgress : "+mUpdateInProgress);
       if (( msg.smartNodeDeviceType.get() == FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE ||
                (msg.smartNodeAddress.get() == mCurrentLwMeshAddress)) && mUpdateInProgress) {
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT));

            short versionMajor = msg.smartNodeMajorFirmwareVersion.get();
            short versionMinor = msg.smartNodeMinorFirmwareVersion.get();
            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            String ccuName = ccu.get("dis").toString();
            AlertGenerateHandler.handleMessage(FIRMWARE_OTA_UPDATE_ENDED, "Firmware OTA update for"+" "+ccuName+" "+
                    "ended for smart device"+" "+ msg.smartNodeAddress.get()+" "+"with version"+" "+versionMajor +
                    // changed Smart node to Smart Device as it is indicating the general name (US:9387)
                    "." + versionMinor);
            Log.d(TAG, mUpdateWaitingToComplete + " - "+versionMajor+" - "+versionMinor);
            if (mUpdateWaitingToComplete && versionMatches(versionMajor, versionMinor)) {
                Log.d(TAG, "[UPDATE] [SUCCESSFUL]"
                        + " [Node Address:" + mCurrentLwMeshAddress + "]"   // updated to Node address from SN as
                        // Node address is more generic and mCurrentLwMeshAddress contains generic node address (US:9387)
                        + " [PACKETS:" + mLastSentPacket
                        + "] Updated to target: " + versionMajor + "." + versionMinor);
                OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_SUCCEEDED, mCurrentLwMeshAddress);
            } else {
                Log.d(TAG, "[UPDATE] [FAILED]"
                        + " [Node Address:" + mCurrentLwMeshAddress + "]"
                        + " [PACKETS:" + mLastSentPacket + "]"
                        + " [TARGET: " + mVersionMajor
                        + "." + mVersionMinor
                        + "] [ACTUAL: " + versionMajor + "." + versionMinor + "]");
                OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CM_TO_DEVICE_FAILED, mCurrentLwMeshAddress);
            }
            OtaStatus nodeStatus = DeviceUtil.getNodeStatus(msg.nodeStatus.get());
            if (nodeStatus != OtaStatus.NO_INFO)
                OtaStatusDiagPoint.Companion.updateOtaStatusPoint(nodeStatus, mCurrentLwMeshAddress);

            moveUpdateToNextNode();
        }
    }

    /**
     * Takes an Intent and parses its extra data to determine what type of device is being
     * updated, and to what version
     *
     * @param intent The Intent which started this OTA update
     */
    private void handleOtaUpdateStartRequest(Intent intent) {
        otaRequestProcessInProgress = true;
        Log.i(TAG, "handleOtaUpdateStartRequest: called started a request");
        String id = intent.getStringExtra(ID);
        String firmwareVersion = intent.getStringExtra(FIRMWARE_VERSION);
        String cmdLevel = intent.getStringExtra(CMD_LEVEL);
        currentOtaRequest = intent.getStringExtra(MESSAGE_ID);
        currentRunningRequestType = intent.getStringExtra(CMD_TYPE);

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
        if(firmwareVersion.startsWith("SmartNode_")) {
            mFirmwareDeviceType = FirmwareComponentType_t.SMART_NODE_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType, currentRunningRequestType, currentOtaRequest);
        }
        else if(firmwareVersion.startsWith("HelioNode_")) {
            mFirmwareDeviceType = FirmwareComponentType_t.HELIO_NODE_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType, currentRunningRequestType, currentOtaRequest);
        }
        else if(firmwareVersion.startsWith("ITM_")) {
            mFirmwareDeviceType = FirmwareComponentType_t.ITM_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType, currentRunningRequestType, currentOtaRequest);
        } else if(firmwareVersion.startsWith("HyperStat_")) {
            mFirmwareDeviceType = FirmwareComponentType_t.HYPER_STAT_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType, currentRunningRequestType, currentOtaRequest);
        }else if(firmwareVersion.startsWith("CM_")){
            mFirmwareDeviceType = FirmwareComponentType_t.CONTROL_MOTE_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType,  currentRunningRequestType, currentOtaRequest);
        }else{
            otaRequestProcessInProgress = false;
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
    private void   startUpdate(String id, String updateLevel, int versionMajor, int versionMinor,
                               FirmwareComponentType_t deviceType, String requestType, String currentRequest) {

        String filename = makeFileName(versionMajor, versionMinor, deviceType);
        Log.d(TAG, "[VALIDATION] Validating update instructions: " + filename);
        if (mUpdateInProgress) {
            Log.d(TAG, "[VALIDATION] Update already in progress");
            return;
        }

        if (!validVersion((short) versionMajor, (short) versionMinor)) {
            Log.d(TAG, "[VALIDATION] Invalid version: " + versionMajor + "." + versionMinor);
            resetUpdateVariables();
            return;
        }

        Log.d(TAG, "[VALIDATION] Valid version");

        if(mLwMeshAddresses == null) {
            mLwMeshAddresses = new ArrayList<>();
        }
        mLwMeshAddresses.clear();
        //determine how many devices are going to be updated
        switch(updateLevel) {
            case "site":
            case "system":
                //update everything
                HashMap<Object, Object> deviceList= CCUHsApi.getInstance().readEntity("device and cm");
                HashMap equipment =  CCUHsApi.getInstance().readEntity("equip and oao");

                if(!equipment.isEmpty()){
                    Device OAOdevice = HSUtil.getDevice(Short.parseShort(equipment.get(Tags.GROUP).toString()));
                    if(OAOdevice.getMarkers().contains( deviceType.getHsMarkerName() )) {
                        Log.d(TAG, "[VALIDATION] Adding OAO device " + OAOdevice.getAddr() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(OAOdevice.getAddr()));
                    }
                }


                if(deviceList.containsKey( deviceType.getHsMarkerName())){
                        mLwMeshAddresses.add( 99 + L.ccu().getSmartNodeAddressBand());
                    }

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
        if(mLwMeshAddresses.isEmpty()) {
            Log.d(TAG, "[VALIDATION] Could not find device " + id + " at level " + updateLevel);

            resetUpdateVariables();
            otaRequestProcessInProgress = false;
            Log.d(TAG, "[RESET] OTA Resetting the Update Variables & moving for next request");
            processOtaRequest();
            return;
        }
        OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_REQUEST_RECEIVED,mLwMeshAddresses);
        moveUpdateToNextNode();

        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_START));
    }


    /**
     * Checks if a metadata file is present, and if it matches the requested version number.
     * Starts a download task for the new metadata file if those conditions aren't met
     * Moves on to checking the binary file if they are
     *
     * @param versionMajor The expected major version
     * @param versionMinor The expected minor version
     */
    private void runMetadataCheck(int versionMajor, int versionMinor, FirmwareComponentType_t deviceType) {

        String filename = makeFileName(versionMajor, versionMinor, deviceType);

        Log.d(TAG, "[METADATA] Running metadata check on file: " + filename);
        File metadata = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, METADATA_FILE_FORMAT);

        if (metadata == null) {
            Log.d(TAG, "[METADATA] File not found, downloading metadata");
            mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
            return;

        } else {
            Log.d(TAG, "[METADATA] Extracting firmware metadata");
            boolean isExtracted = extractFirmwareMetadata(metadata);
            if (!isExtracted || !versionMatches( (short) versionMajor, (short) versionMinor) ) {
                Log.d(TAG, "[METADATA] Incorrect firmware metadata, downloading correct metadata");
                mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
                return;
            }
            OtaCache cache = new OtaCache();
            cache.removeRequest(currentOtaRequest);

        }

        Log.d(TAG, "[METADATA] Metadata passed check, starting binary check");
        runBinaryCheck(versionMajor, versionMinor, deviceType);
    }

    /**
     * Checks if the binary file is present, and if it matches the metadata
     * If those conditions aren't met, downloads new binary file
     * If they are met, continues to moving the binary file into RAM
     */
    private void runBinaryCheck(int versionMajor, int versionMinor, FirmwareComponentType_t deviceType) {
        String filename = makeFileName(versionMajor, versionMinor, deviceType);

        Log.d(TAG, "[BINARY] Running binary check on file: " + filename);
        File binary = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, BINARY_FILE_FORMAT);

        if (binary == null) {
            Log.d(TAG, "[BINARY] File not found, downloading binary");
            mBinaryDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
            return;

        } else {
            Log.d(TAG, "[BINARY] Checking binary length match: " + binary.length() + ", " + mUpdateLength);
            if (binary.length() != mUpdateLength) {
                Log.d(TAG, "[BINARY] Incorrect firmware binary, downloading correct binary");
                mMetadataDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
                return;
            }
        }

        Log.d(TAG, "[BINARY] Binary passed check");
        Log.d(TAG, "[STARTUP] Starting to update device with address " + mCurrentLwMeshAddress);

        //TODO notify something (PubNub?) that an update has started
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        String ccuName = ccu.get("dis").toString();

        AlertGenerateHandler.handleMessage(FIRMWARE_OTA_UPDATE_STARTED, "Firmware OTA update for"+" "+ccuName+" "+
                "started for "+deviceType.getUpdateFileName()+" "+mCurrentLwMeshAddress+" "+"with version"+" "+versionMajor + "." + versionMinor);
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
    private long startFileDownload(String filename, FirmwareComponentType_t deviceType, String fileFormat) {
        String filePathSystem = DOWNLOAD_DIR.toString() + "/" + filename + fileFormat;
        String filePathUrl = DOWNLOAD_BASE_URL + deviceType.getUpdateUrlDirectory() + filename + fileFormat;
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
    private void deleteFilesByDeviceType(File dir, FirmwareComponentType_t deviceType){
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
    private void setUpdateFile(File file, FirmwareComponentType_t deviceType) {
        Log.d(TAG, "[STARTUP] Moving binary file to RAM");
        packets = importFile(file, MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE);

        if (packets == null) {
            Log.d(TAG, "[STARTUP] Failed to move binary file to RAM");
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CCU_TO_CM_FAILED, mCurrentLwMeshAddress);
            resetUpdateVariables();
            return;
        }

        Log.d(TAG, "[STARTUP] Successfully moved binary file to RAM, sending metadata");

        OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_REQUEST_IN_PROGRESS, mCurrentLwMeshAddress);
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
            Log.d(TAG, "[METADATA] [Node address:" + mCurrentLwMeshAddress
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
    private void sendFirmwareMetadata(FirmwareComponentType_t firmware) {
        updateOtaStatusToOtaStarted();
        CcuToCmOverUsbFirmwareMetadataMessage_t message = new CcuToCmOverUsbFirmwareMetadataMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_FIRMWARE_METADATA);
        message.lwMeshAddress.set(mCurrentLwMeshAddress);

        message.metadata.deviceType.set(firmware);
        message.metadata.majorVersion.set(mVersionMajor);
        message.metadata.minorVersion.set(mVersionMinor);
        message.metadata.lengthInBytes.set(mUpdateLength);

        message.metadata.setSignature(mFirmwareSignature);
        if (BuildConfig.DEBUG) {
            Log.d(TAG,
                    "[METADATA] [Node Address:" + mCurrentLwMeshAddress + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(message.getByteBuffer().array(), true) + "]");
        }

        try {
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
                    Log.d(TAG,
                            "[UPDATE] [Node Address:" + lwMeshAddress + "]" + "PS:"+packets.size()+","+message.packet.length+","+message.sequenceNumber.get()+" [PN:" + mLastSentPacket
                            + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(packets.get(packetNumber), true) +  "]");
                }
            }
        }

        try {
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            Log.e(TAG, "[UPDATE] [Node Address:" + lwMeshAddress + "] [PN:" + packetNumber + "] [FAILED]");
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

    private String makeFileName(int versionMajor, int versionMinor, FirmwareComponentType_t deviceType) {
        return deviceType.getUpdateFileName() + "_v" + versionMajor + "." + versionMinor;
    }

    private void moveUpdateToNextNode() {
        resetUpdateVariables();
        if(mLwMeshAddresses.isEmpty()) {
            completeUpdate();
            return;
        }
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        mCurrentLwMeshAddress = mLwMeshAddresses.get(0);
        OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_UPDATE_STARTED, mCurrentLwMeshAddress);
        mLwMeshAddresses.remove(0);

        OtaCache cache = new OtaCache();
        cache.saveRunningDeviceDetails(mCurrentLwMeshAddress,mFirmwareDeviceType.ordinal(),mVersionMajor,mVersionMinor,mLwMeshAddresses);
        runMetadataCheck(mVersionMajor, mVersionMinor, mFirmwareDeviceType);
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
        deleteFilesByDeviceType(DOWNLOAD_DIR, mFirmwareDeviceType);
        new OtaCache().saveRunningDeviceDetails(-1,-1,-1,-1, new ArrayList<>());
        mVersionMajor = -1;
        mVersionMinor = -1;

        mUpdateLength = -1;

        mMetadataDownloadId = -1;
        mBinaryDownloadId = -1;

        mUpdateInProgress = false;
        Intent completeIntent = new Intent(Globals.IntentActions.OTA_UPDATE_COMPLETE);
        sendBroadcast(completeIntent);
        otaRequestProcessInProgress = false;
        Log.d(TAG, "[RESET] OTA Request process completed moving for next request");
        processOtaRequest();

    }



    /**
     * Push OTA request to Queue
     */
    void addRequest(Intent otaRequest){

        Log.i(TAG, "addRequest: "+ otaRequestsQueue);
        if (otaRequest != null) {
            otaRequestsQueue.add(otaRequest);
            OtaCache cache = new OtaCache();
            cache.saveRequest(otaRequest);
             startRetryHandler();
            Log.i(TAG, "Current Requests size : "+otaRequestsQueue.size());
            processOtaRequest();

        }
    }

    void processOtaRequest(){
        Log.i(TAG,"processOtaRequest Called " + otaRequestsQueue.size() + " otaRequestProcessInProgress"+otaRequestProcessInProgress );
        try {
            if (!otaRequestsQueue.isEmpty() && !otaRequestProcessInProgress){
                handleOtaUpdateStartRequest(Objects.requireNonNull(otaRequestsQueue.poll()));
            }
        } catch (Exception e){
            e.printStackTrace();
            Log.i(TAG,"Failed to take request from queue "+ e.getMessage());
        }
    }

    private void startRetryHandler() {
        if (!retryHandlerStarted) {
            retryHandlerStarted = true;
            retryHandler = new Timer();
            TimerTask otaTimeOutTask = new TimerTask() {
                @Override
                public void run() {
                    OtaCache cache = new OtaCache();
                    LinkedTreeMap<String, LinkedTreeMap<String,String>> otaRequests = cache.getRequestMap();
                    if (!otaRequests.isEmpty()) {
                        otaRequests.forEach((msgId, request) -> {
                            if (Integer.parseInt(Objects.requireNonNull(request.get(RETRY_COUNT))) < NO_OF_RETRY) {
                                Intent intent = cache.getIntentFromRequest(request);
                                otaRequestsQueue.add(intent);
                            } else {
                                cache.removeRequest(msgId);
                            }
                        });
                        processOtaRequest();
                    } else {
                        stopRetryHandler();
                    }
                }
            };
            retryHandler.schedule(otaTimeOutTask,   DELAY, RETRY_TIME);
        }
    }
    void stopRetryHandler() {
        Log.i(TAG, "Retry handler stopped");
        if(retryHandler != null ) {
            retryHandler.cancel();
        }
        retryHandlerStarted = false;
    }

    public static boolean isCmOtaInProgress(){
        return  (mCurrentLwMeshAddress ==  (L.ccu().getSmartNodeAddressBand() + 99));
    }
}
