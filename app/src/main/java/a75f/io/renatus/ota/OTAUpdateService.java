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
import a75f.io.logger.CcuLog;
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

    private static final long RETRY_TIME = 5 * 60000;
    private static final  long DELAY = 2 * 60000;
    private static final int NO_OF_RETRY = 5;
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
    private static FirmwareComponentType_t mFirmwareDeviceTypeFromMeta;

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
        if(intent == null){
            return;
        }
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
            moveUpdateToNextNode();
        }
        /* The OTA update is in progress, and is being notified from the CM */
        else if(action.equals(Globals.IntentActions.LSERIAL_MESSAGE_OTA)) {

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

            CcuLog.d(TAG, "[DOWNLOAD] Download failed, reason: " + reason);
            if (currentOtaRequest != null) {
                OtaCache cache = new OtaCache();
                cache.updateRetryCount(currentOtaRequest);
            }
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_FIRMWARE_DOWNLOAD_FAILED, mCurrentLwMeshAddress);
            resetUpdateVariables();
            completeUpdate();
            return;
        }

            c.close();

            if (id == mMetadataDownloadId) {
                CcuLog.d(TAG, "[DOWNLOAD] Metadata downloaded");

                runMetadataCheck(mVersionMajor, mVersionMinor, mFirmwareDeviceType);
            } else if (id == mBinaryDownloadId) {
                CcuLog.d(TAG, "[DOWNLOAD] Binary downloaded");

                runBinaryCheck(mVersionMajor, mVersionMinor, mFirmwareDeviceType);
            }
        }
    }
    private void handleOtaUpdateAck(byte[] eventBytes) {
        CmToCcuOverUsbFirmwareUpdateAckMessage_t msg = new CmToCcuOverUsbFirmwareUpdateAckMessage_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        CcuLog.i(TAG,"msg.lwMeshAddress.get()  "+msg.lwMeshAddress.get()+"   "+mCurrentLwMeshAddress);
        if(msg.lwMeshAddress.get() == mCurrentLwMeshAddress) {
            CcuLog.d(TAG, "[UPDATE] CM has acknowledged update");
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

            CcuLog.d(TAG, "[UPDATE] CM asks for packet " + msg.sequenceNumber.get() + " of "
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
        if (cache.getDeviceType() != -1) {
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
                if (!isExtracted || !versionMatches((short) versionMajor, (short) versionMinor)) {
                    CcuLog.d(TAG, "[METADATA] Incorrect firmware metadata, downloading correct metadata");
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
        } else {
            CcuLog.i(L.TAG_CCU_OTA_PROCESS, " OTA request device is not found..!");
        }
    }

    private void handleCmToDeviceProgress(byte[] eventBytes){
        LSerial.fromBytes(eventBytes, CmToCcuOtaStatus_t.class);
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        CmToCcuOtaStatus_t msg = new CmToCcuOtaStatus_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        CcuLog.i(L.TAG_CCU_OTA_PROCESS, " CM to Device process : State : "+msg.currentState.get() + " Data : "+msg.data.get());
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
            CcuLog.d(TAG, " SmartNode device type is null" );
            return;
        }
        CcuLog.d(TAG, "Node: "+msg.smartNodeAddress.get() + " Device :"+msg.smartNodeDeviceType.get() + " Device Rebooted");
        if (msg.smartNodeDeviceType.get() != FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE
                && mCurrentLwMeshAddress != msg.smartNodeAddress.get()) {
            CcuLog.d(TAG, mCurrentLwMeshAddress+ " != "+msg.smartNodeAddress.get());
            return;
        }
        CcuLog.d(TAG, "mUpdateInProgress : "+mUpdateInProgress);
       if (( msg.smartNodeDeviceType.get() == FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE ||
                (msg.smartNodeAddress.get() == mCurrentLwMeshAddress)) && mUpdateInProgress) {
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT));

            short versionMajor = msg.smartNodeMajorFirmwareVersion.get();
            short versionMinor = msg.smartNodeMinorFirmwareVersion.get();
            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            String ccuName = ccu.get("dis").toString();
            AlertGenerateHandler.handleMessage(FIRMWARE_OTA_UPDATE_ENDED, "Firmware OTA update for"+" "+ccuName+" "+
                    "ended for "+mFirmwareDeviceType.getUpdateFileName()+" "+ mCurrentLwMeshAddress+" "+"with version"+" "+versionMajor +
                    // changed Smart node to Smart Device as it is indicating the general name (US:9387)
                    "." + versionMinor);
           CcuLog.d(TAG, mUpdateWaitingToComplete + " - "+versionMajor+" - "+versionMinor);
            if (mUpdateWaitingToComplete && versionMatches(versionMajor, versionMinor)) {
                CcuLog.d(TAG, "[UPDATE] [SUCCESSFUL]"
                        + " [Node Address:" + mCurrentLwMeshAddress + "]"   // updated to Node address from SN as
                        // Node address is more generic and mCurrentLwMeshAddress contains generic node address (US:9387)
                        + " [PACKETS:" + mLastSentPacket
                        + "] Updated to target: " + versionMajor + "." + versionMinor);
                OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_SUCCEEDED, mCurrentLwMeshAddress);
            } else {
                CcuLog.d(TAG, "[UPDATE] [FAILED]"
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
        CcuLog.i(TAG, "handleOtaUpdateStartRequest: called started a request");
        deleteFilesByDeviceType(DOWNLOAD_DIR);
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
        }else if(firmwareVersion.startsWith("ConnectModule_") || firmwareVersion.startsWith("connect_module_")){
            mFirmwareDeviceType = FirmwareComponentType_t.CONNECT_MODULE_DEVICE_TYPE;
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
        CcuLog.d(TAG, "[VALIDATION] Validating update instructions: " + filename);
        if (mUpdateInProgress) {
            CcuLog.d(TAG, "[VALIDATION] Update already in progress");
            return;
        }

        if (!validVersion((short) versionMajor, (short) versionMinor)) {
            CcuLog.d(TAG, "[VALIDATION] Invalid version: " + versionMajor + "." + versionMinor);
            resetUpdateVariables();
            return;
        }

        CcuLog.d(TAG, "[VALIDATION] Valid version");

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
                HashMap equipment =  CCUHsApi.getInstance().readEntity("equip and oao and not hyperstatsplit");

                if(!equipment.isEmpty()){
                    Device OAOdevice = HSUtil.getDevice(Short.parseShort(equipment.get(Tags.GROUP).toString()));
                    if(OAOdevice.getMarkers().contains( deviceType.getHsMarkerName() )) {
                        CcuLog.d(TAG, "[VALIDATION] Adding OAO device " + OAOdevice.getAddr() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(OAOdevice.getAddr()));
                    }
                }


                if(deviceList.containsKey( deviceType.getHsMarkerName())){
                        mLwMeshAddresses.add( 99 + L.ccu().getSmartNodeAddressBand());
                    }

                for(Floor floor : HSUtil.getFloors()) {
                    for(Zone zone : HSUtil.getZones(floor.getId())) {
                        for(Device device : HSUtil.getDevices(zone.getId())) {

                            if(shouldBeCheckedForUpdate(device, deviceType)) {
                                CcuLog.d(TAG, "[VALIDATION] Adding device " + device.getAddr() + " to update");
                                mLwMeshAddresses.add(Integer.parseInt(device.getAddr()));
                            }

                        }
                    }
                }
                break;

            case "zone":
                //update all nodes in the same zone as the specified node
                for(Device device : HSUtil.getDevices("@"+id)) {
                    if(shouldBeCheckedForUpdate(device, deviceType)) {
                        CcuLog.d(TAG, "[VALIDATION] Adding device " + device.getAddr() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(device.getAddr()));
                    }
                }
                break;

            case "equip":
            case "module":
                //update just the one node
                Equip equip = HSUtil.getEquipInfo("@"+id);
                Device device = HSUtil.getDevice(Short.parseShort(equip.getGroup()));
                    if(shouldBeCheckedForUpdate(device, deviceType)) {
                        CcuLog.d(TAG, "[VALIDATION] Adding device " + equip.getGroup() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(equip.getGroup()));
                    }
                break;
        }
        if(mLwMeshAddresses.isEmpty()) {
            CcuLog.d(TAG, "[VALIDATION] Could not find device " + id + " at level " + updateLevel);

            resetUpdateVariables();
            otaRequestProcessInProgress = false;
            CcuLog.d(TAG, "[RESET] OTA Resetting the Update Variables & moving for next request");
            processOtaRequest();
            return;
        }
        OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_REQUEST_RECEIVED,mLwMeshAddresses);
        moveUpdateToNextNode();

        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_START));
    }

    /*
        Determine whether a device *may* require updating based on its Haystack tags.

        At this stage, we only have the firmware filename and the device's Haystack tags. This isn't enough info to determine exactly
        which devices need updating. So for now, all devices that might match to a given file name are added.

        (Ex. if filename is "HyperStat_", all devices with tag "hyperstat" and "hyperstatsplit" are added, because we don't yet know
        if the file contains HyperStat or HyperLite firmware.)

        If the FirmwareDeviceType in the firmware metadata does not match the actual device, it will be skipped during the runMetadataCheck() step.
     */
    private boolean shouldBeCheckedForUpdate(Device device, FirmwareComponentType_t deviceType) {
        if (device.getMarkers().contains(deviceType.getHsMarkerName())) {
            return true;
        } else if (deviceType.getHsMarkerName().equals(Tags.HYPERSTAT) && device.getMarkers().contains(Tags.HYPERSTATSPLIT)) {
            return true;
        }
        return false;
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

        CcuLog.d(TAG, "[METADATA] Running metadata check on file: " + filename);
        File metadata = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, METADATA_FILE_FORMAT);

        if (metadata == null) {
            CcuLog.d(TAG, "[METADATA] File not found, downloading metadata");
            mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
            return;

        } else {
            CcuLog.d(TAG, "[METADATA] Extracting firmware metadata");
            boolean isExtracted = extractFirmwareMetadata(metadata);
            if (!isExtracted || !versionMatches( (short) versionMajor, (short) versionMinor) ) {
                CcuLog.d(TAG, "[METADATA] Incorrect firmware metadata, downloading correct metadata");
                mMetadataDownloadId = startFileDownload(filename, deviceType, METADATA_FILE_FORMAT);
                return;
            }
            OtaCache cache = new OtaCache();
            cache.removeRequest(currentOtaRequest);

        }

        if (skipCurrentDeviceBasedOnMetadata()) {
            CcuLog.d(TAG, "[METADATA] Metadata contents do not match device type, moving to next node");
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_DEV_TYPE, mCurrentLwMeshAddress);
            moveUpdateToNextNode();
        } else {
            CcuLog.d(TAG, "[METADATA] Metadata passed check, starting binary check");
            runBinaryCheck(versionMajor, versionMinor, deviceType);
        }

    }

    // Currently, the only devices that require the device type to be checked against metadata are HyperStat and HyperStat Split (HyperLite)

    // All other types of firmware are not checked since the device type is already known from the request type, and
    // backward-compatibility issues could result if other types of firmware didn't have this info in their metadata.
    private boolean skipCurrentDeviceBasedOnMetadata() {
        Device device = HSUtil.getDevice((short)mCurrentLwMeshAddress);
        if (device == null) { return true; }

        if (mFirmwareDeviceTypeFromMeta == FirmwareComponentType_t.HYPER_STAT_DEVICE_TYPE) {
            if (!device.getMarkers().contains(Tags.HYPERSTAT)) return true;
        } else if (mFirmwareDeviceTypeFromMeta == FirmwareComponentType_t.HYPERSTAT_SPLIT_DEVICE_TYPE) {
            if (!(device.getMarkers().contains(Tags.HYPERSTATSPLIT) || device.getMarkers().contains(Tags.HYPERSTAT))) return true;
        }

        return false;
    }

    /**
     * Checks if the binary file is present, and if it matches the metadata
     * If those conditions aren't met, downloads new binary file
     * If they are met, continues to moving the binary file into RAM
     */
    private void runBinaryCheck(int versionMajor, int versionMinor, FirmwareComponentType_t deviceType) {
        String filename = makeFileName(versionMajor, versionMinor, deviceType);

        CcuLog.d(TAG, "[BINARY] Running binary check on file: " + filename);
        File binary = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, BINARY_FILE_FORMAT);

        if (binary == null) {
            CcuLog.d(TAG, "[BINARY] File not found, downloading binary");
            mBinaryDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
            return;

        } else {
            CcuLog.d(TAG, "[BINARY] Checking binary length match: " + binary.length() + ", " + mUpdateLength);
            if (binary.length() != mUpdateLength) {
                CcuLog.d(TAG, "[BINARY] Incorrect firmware binary, downloading correct binary");
                mMetadataDownloadId = startFileDownload(filename, deviceType, BINARY_FILE_FORMAT);
                return;
            }
        }

        CcuLog.d(TAG, "[BINARY] Binary passed check");
        CcuLog.d(TAG, "[STARTUP] Starting to update device with address " + mCurrentLwMeshAddress);

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
        CcuLog.d(TAG, "[DOWNLOAD] Starting download of file " + filePathUrl);

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
                CcuLog.d(TAG,"Compare files: " + file.getName() + ", " + fileName);
                if(file.getName().startsWith(fileName) && file.getName().endsWith(fileFormat))
                    return file;
            }
            return null;

        } catch (ArrayIndexOutOfBoundsException e) {
            CcuLog.e(TAG, "[STARTUP] File not found in " + dir);
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
            CcuLog.e(TAG, "[STARTUP] I/O exception while importing update file: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            CcuLog.e(TAG, "[STARTUP] File is too large: " + e.getMessage());
            e.printStackTrace();
        }

        ArrayList<byte[]> rPackets = new ArrayList<>();
        int len = fileByteArray.length;
        int packetNum = len / packetLength;

        if ((packetNum * packetLength) < len) {
            CcuLog.d(TAG, "[STARTUP] Added packet: " + packetNum + ", to " + (packetNum + 1) + "]");
            packetNum++;
        }

        CcuLog.d(TAG, "[STARTUP] File imported, number of packets: " + packetNum);

        for (int i = 0; i < packetNum; i++) {
            byte[] packet = new byte[packetLength];

            if (len - packetLength * i > packetLength) {
                System.arraycopy(fileByteArray, i * packetLength, packet, 0, packetLength);
            } else {
                System.arraycopy(fileByteArray, i * packetLength, packet, 0, len - packetLength * i);
            }
            rPackets.add(packet);
        }

        CcuLog.d(TAG, "[STARTUP] Packets generated: " + rPackets.size());

        return rPackets;
    }

    /**
     * Loads a binary image into RAM and parses it into packets in preparation for an update
     *
     * @param file       The file to be loaded and processed
     * @param deviceType The type of device being updated
     */
    private void setUpdateFile(File file, FirmwareComponentType_t deviceType) {
        CcuLog.d(TAG, "[STARTUP] Moving binary file to RAM");
        packets = importFile(file, MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE);

        if (packets == null) {
            CcuLog.d(TAG, "[STARTUP] Failed to move binary file to RAM");
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CCU_TO_CM_FAILED, mCurrentLwMeshAddress);
            resetUpdateVariables();
            return;
        }

        CcuLog.d(TAG, "[STARTUP] Successfully moved binary file to RAM, sending metadata");

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
                        // This is currently only used for HyperStat and HyperStat Split
                        mFirmwareDeviceTypeFromMeta = FirmwareComponentType_t.values()[reader.nextInt()];
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
            CcuLog.e(TAG, "[METADATA] Unable to parse file: File Not Found");
            json.delete();
            return false;
        } catch (IOException e) {
            json.delete();
            CcuLog.e(TAG, "[METADATA] Unable to parse file: IO Error");
            return false;
        }

        if (BuildConfig.DEBUG) {
            CcuLog.d(TAG, "[METADATA] [Node address:" + mCurrentLwMeshAddress
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

        FirmwareComponentType_t deviceTypeToSend = (firmware.equals(FirmwareComponentType_t.HYPER_STAT_DEVICE_TYPE) && mFirmwareDeviceTypeFromMeta.equals(FirmwareComponentType_t.HYPERSTAT_SPLIT_DEVICE_TYPE)) ? mFirmwareDeviceTypeFromMeta : firmware;
        message.metadata.deviceType.set(deviceTypeToSend);

        message.metadata.majorVersion.set(mVersionMajor);
        message.metadata.minorVersion.set(mVersionMinor);
        message.metadata.lengthInBytes.set(mUpdateLength);

        message.metadata.setSignature(mFirmwareSignature);
        if (BuildConfig.DEBUG) {
            CcuLog.d(TAG,
                    "[METADATA] [Node Address:" + mCurrentLwMeshAddress + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(message.getByteBuffer().array(), true) + "]");
        }

        try {
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            CcuLog.e(TAG, "[METADATA] FAILED TO SEND");
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
            CcuLog.d(TAG, "[UPDATE] Received packet request from address " + lwMeshAddress + ", instead of " + mCurrentLwMeshAddress);
            return;
        }
        if (mUpdateWaitingToComplete && packetNumber == packets.size() /*- 2*/) {
            CcuLog.d(TAG, "[UPDATE] Received packet request while waiting for the update to complete");
            return;
        }
        if (packetNumber < 0 || packetNumber > packets.size()) {
            CcuLog.d(TAG, "[UPDATE] Received request for invalid packet: " + packetNumber);
            return;
        }
        if (!mUpdateWaitingToComplete && (packetNumber == (packets.size() - 1))) {
            CcuLog.d(TAG, "[UPDATE] Received request for final packet");
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
                    CcuLog.d(TAG,
                            "[UPDATE] [Node Address:" + lwMeshAddress + "]" + "PS:"+packets.size()+","+message.packet.length+","+message.sequenceNumber.get()+" [PN:" + mLastSentPacket
                            + "] [DATA: " + ByteArrayUtils.byteArrayToHexString(packets.get(packetNumber), true) +  "]");
                }
            }
        }

        try {
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            CcuLog.e(TAG, "[UPDATE] [Node Address:" + lwMeshAddress + "] [PN:" + packetNumber + "] [FAILED]");
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
        CcuLog.d(TAG, "[RESET] Reset OTA update status");

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
        CcuLog.d(TAG, "[RESET] OTA Request process completed moving for next request");
        processOtaRequest();

    }



    /**
     * Push OTA request to Queue
     */
    void addRequest(Intent otaRequest){

        CcuLog.i(TAG, "addRequest: "+ otaRequestsQueue);
        if (otaRequest != null) {
            otaRequestsQueue.add(otaRequest);
            OtaCache cache = new OtaCache();
            cache.saveRequest(otaRequest);
             startRetryHandler();
            CcuLog.i(TAG, "Current Requests size : "+otaRequestsQueue.size());
            processOtaRequest();

        }
    }

    void processOtaRequest(){
        CcuLog.i(TAG,"processOtaRequest Called " + otaRequestsQueue.size() + " otaRequestProcessInProgress"+otaRequestProcessInProgress );
        try {
            if (!otaRequestsQueue.isEmpty() && !otaRequestProcessInProgress){
                handleOtaUpdateStartRequest(Objects.requireNonNull(otaRequestsQueue.poll()));
            }
        } catch (Exception e){
            e.printStackTrace();
            CcuLog.i(TAG,"Failed to take request from queue "+ e.getMessage());
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
                                if (!checkDuplicateRequest(intent)) {
                                    otaRequestsQueue.add(intent);
                                }
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

    boolean checkDuplicateRequest(Intent intentDetails){
        for (Intent intent : otaRequestsQueue) {
            String currentOtaRequest = intent.getStringExtra(MESSAGE_ID);
            if (currentOtaRequest.equalsIgnoreCase(intentDetails.getStringExtra(MESSAGE_ID))){
                return true;
            }
        }
        return false;
    }
    private void deleteFilesByDeviceType(File dir){
        try {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }
    void stopRetryHandler() {
        CcuLog.i(TAG, "Retry handler stopped");
        if(retryHandler != null ) {
            retryHandler.cancel();
        }
        retryHandlerStarted = false;
    }

    public static boolean isCmOtaInProgress(){
        return  (mCurrentLwMeshAddress ==  (L.ccu().getSmartNodeAddressBand() + 99));
    }
}
