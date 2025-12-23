package a75f.io.renatus.ota;

import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_ENDED;
import static a75f.io.alerts.AlertsConstantsKt.FIRMWARE_OTA_UPDATE_STARTED;
import static a75f.io.renatus.ota.OtaCache.CMD_LEVEL;
import static a75f.io.renatus.ota.OtaCache.CMD_TYPE;
import static a75f.io.renatus.ota.SeqCache.DEVICE_LIST;
import static a75f.io.renatus.ota.OtaCache.FIRMWARE_VERSION;
import static a75f.io.renatus.ota.OtaCache.ID;
import static a75f.io.renatus.ota.OtaCache.MESSAGE_ID;
import static a75f.io.renatus.ota.SeqCache.EMPTY_FIRMWARE_NAME;
import static a75f.io.renatus.ota.SeqCache.EMPTY_META_NAME;
import static a75f.io.renatus.ota.SeqCache.ERASE_SEQUENCE;
import static a75f.io.renatus.ota.SeqCache.ERASE_SEQUENCE_FALSE;
import static a75f.io.renatus.ota.SeqCache.ERASE_SEQUENCE_TRUE;
import static a75f.io.renatus.ota.SeqCache.META_NAME;
import static a75f.io.renatus.ota.SeqCache.NODE_ADDRESS;
import static a75f.io.renatus.ota.OtaCache.RETRY_COUNT;
import static a75f.io.renatus.ota.SeqCache.REMOVE_LIST;
import static a75f.io.renatus.ota.SeqCache.SEQ_NAME;
import static a75f.io.renatus.ota.SeqCache.SEQ_SERVER_ID;
import static a75f.io.renatus.ota.SeqCache.SEQ_VERSION;
import static a75f.io.renatus.ota.SeqCache.FIRMWARE_NAME;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.io.Files;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HTimeZone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
import a75f.io.device.DeviceConstants;
import a75f.io.device.alerts.AlertGenerateHandler;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.serial.CcuToCmOverUsbFirmwareMetadataMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbFirmwarePacketMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbFirmwarePcnMetadataMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbPcnFirmwareEraseMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbPcnFirmwarePacketMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbPcnSequenceMetaMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSequenceMetadataMessage_t;
import a75f.io.device.serial.CmToCcuOtaStatus_t;
import a75f.io.device.serial.CmToCcuOverUsbErrorReportMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwarePacketRequest_t;
import a75f.io.device.serial.CmToCcuOverUsbFirmwareUpdateAckMessage_t;
import a75f.io.device.serial.FirmwareComponentType_t;
import a75f.io.device.serial.FirmwareDeviceType_t;
import a75f.io.device.serial.MessageConstants;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.PcnRebootIndicationMessage_t;
import a75f.io.device.serial.SnRebootIndicationMessage_t;
import a75f.io.domain.api.Domain;
import a75f.io.domain.devices.ConnectNodeDevice;
import a75f.io.domain.devices.PCNDevice;
import a75f.io.domain.util.ModelNames;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil;
import a75f.io.logic.bo.building.pcn.PCNUtil;
import a75f.io.logic.bo.building.pointscheduling.model.CustomScheduleManager;
import a75f.io.logic.bo.util.ByteArrayUtils;
import a75f.io.logic.diag.otastatus.OtaState;
import a75f.io.logic.diag.otastatus.OtaStatus;
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint;
import a75f.io.logic.diag.otastatus.SequenceOtaState;
import a75f.io.logic.diag.otastatus.SequenceOtaStatus;
import a75f.io.renatus.util.HeartBeatUtil;
import a75f.io.usbserial.UsbService;

public class OTAUpdateService extends IntentService {

    //Constants
    public static final String METADATA_FILE_FORMAT = ".meta";
    public static final String BINARY_FILE_FORMAT = ".bin";
    public static final String MPY_FILE_FORMAT = ".mpy";

    private static final String TAG = "OTA_PROCESS";
    private static final int SEQ_TYPE = 0x03;
    private static final int NODE_STATUS_FW_OTA_VALUE_MASK = 0xF8;
    private static final int NODE_STATUS_VALUE_SEQ_OTA_SUCCESS = 0;

    private static final String DOWNLOAD_BASE_URL = "https://updates.75f.io/";
    private static final File DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private static final File DOWNLOAD_SEQ_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private static final long RETRY_TIME = 5 * 60000;
    private static final  long DELAY = 2 * 60000;
    private static final int NO_OF_RETRY = 5;
    //Update variables
    private static ArrayList<Integer> mLwMeshAddresses;
    private static ArrayList<Integer> mLwMeshSeqAddresses;
    private static int mCurrentLwMeshAddress;

    private static int mLastSentPacket = -1;
    private static ArrayList<byte[]> packets;      //Where the decomposed binary is stored in memory
    private static short mVersionMajor = -1;
    private static short mVersionMinor = -1;
    private static short mServerIdPcn = -1;

    private static int mUpdateLength = -1;         //Binary length (bytes)
    private static byte[] mFirmwareSignature = {};
    private static FirmwareComponentType_t mFirmwareDeviceType;
    private static FirmwareComponentType_t mFirmwareDeviceTypeFromMeta;

    private static int mslaveIdSq = -1;         //Used in case of sequence update for PCN
    private static int mUpdateLengthSq = -1;         //Binary length of sequence (bytes)
    private static byte[] mSequenceSignature = {};
    private static FirmwareComponentType_t mSequenceDeviceTypeFromMeta;
    private static String mSequenceName = "";
    private static String mSequenceMetaFileName = "";
    private static String mSequenceSeqFileName = "";
    private static short mSequenceVersion = -1;
    private static Boolean mSequenceEmptyRequest = false;

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
    static boolean ZoneAndModuleLevelUpdate = false;
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
            // Update sequence status
            moveUpdateToNextNode();
        }
        else if(action.equals(Globals.IntentActions.SEQUENCE_UPDATE_START)) {
            sequenceRequest(intent);
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

                case CM_ERROR_REPORT:
                    handleErrorReport(eventBytes);
                    break;

                case CM_TO_CCU_OVER_USB_SEQUENCE_PACKET_REQUEST:
                    handlePacketRequestSequence(eventBytes);
                    break;

                case CM_TO_CCU_OVER_USB_SEQUENCE_OTA_STATUS:
                    handleCmToDeviceSequenceProgress(eventBytes);
                    break;

                case PCN_MODBUS_SERVER_REBOOT:
                    handlePcnNodeReboot(eventBytes);
                    break;
            }
        }
    }

    private void handleErrorReport(byte[] eventBytes) {
        try {
            CmToCcuOverUsbErrorReportMessage_t msg = new CmToCcuOverUsbErrorReportMessage_t();
            msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);
            CcuLog.d(TAG, "Error Report from CM_to_CCU : Error type : " + msg.errorType.get() + " Detail " + msg.errorDetail.get());
            switch (msg.errorType.get()){
                case FIRMWARE_METADATA_INVALID:
                    CcuLog.d(TAG, "Error Handling from CM_to_CCU: Firmware metadata invalid");
                    moveUpdateToNextNode();
                    break;

                case CCU_UPDATE_NODE_NOT_IN_DBASE:
                    CcuLog.d(TAG, "Error Handling from CM_to_CCU: CCU update node not in database");
                    Handler handler = new Handler();
                    // Create a Runnable that will call sendFirmwareMetadata after the delay
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendFirmwareMetadata(mFirmwareDeviceType);
                            CcuLog.d(TAG, "Error Handling from CM_to_CCU: Sending firmware metadata after 1 minute");
                        }
                    }, 60000);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            CcuLog.e(TAG, "Error parsing error report message: " + e.getMessage());
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
        CcuLog.d(TAG, " CM has acknowledged update "+msg.messageType.get());

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

            // If PCN update is active, we need to append additional field
            if(mServerIdPcn != -1){
                sendPacketPcn(msg.lwMeshAddress.get(), msg.sequenceNumber.get());
            } else {
                sendPacket(msg.lwMeshAddress.get(), msg.sequenceNumber.get());
            }
            OtaStatusDiagPoint.Companion.updateCcuToCmOtaProgress(
                    (mUpdateLength / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE),
                    msg.sequenceNumber.get(),
                    msg.lwMeshAddress.get());
        }
    }

    /**
     * Handles a firmware packet request from the CM (Control Module) during OTA (Over-The-Air) update process.
     *
     * This function:
     * 1. Updates the last OTA activity timestamp
     * 2. Parses the incoming packet request message
     * 3. Logs the current update progress
     * 4. Initiates sending of the requested firmware packet
     * 5. Updates diagnostic points with progress information
     *
     * @param eventBytes The raw byte array containing the packet request message in little-endian format
     *
     * Message Structure (CmToCcuOverUsbFirmwarePacketRequest_t):
     * - lwMeshAddress: The Lightweight Mesh address of the target device
     * - sequenceNumber: The sequence number of the requested firmware packet
     *
     * Diagnostic Updates:
     * - Updates OTA progress information including:
     *   - Total packets expected (mUpdateLengthSq / FIRMWARE_UPDATE_PACKET_SIZE)
     *   - Current packet sequence number
     *   - Target device address
     *
     * Note: This function is part of the firmware update state machine and should only be called
     * during an active OTA update session.
     */
    private void handlePacketRequestSequence(byte[] eventBytes) {
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        // Parse the incoming message (little-endian format)
        CmToCcuOverUsbFirmwarePacketRequest_t msg = new CmToCcuOverUsbFirmwarePacketRequest_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        CcuLog.d(TAG, "[UPDATE] CM asks for packet " + msg.sequenceNumber.get() + " of "
                + (mUpdateLengthSq / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE));

        // Process the request
        sendPacketSequence(msg.lwMeshAddress.get(), msg.sequenceNumber.get());
        // Update diagnostic points
        OtaStatusDiagPoint.Companion.updateCcuToCmSeqProgress(
                (mUpdateLengthSq / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE),
                msg.sequenceNumber.get(),
                msg.lwMeshAddress.get(), mServerIdPcn);
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
            moveUpdateToNextNode();
        }
    }

    /**
     * Extracts and processes metadata from a sequence file and manages the OTA update cache.
     * <p>
     * This function:
     * 1. Locates the metadata file for the specified sequence file
     * 2. Extracts metadata information from the file
     * 3. Cleans up the current OTA request from the cache
     *
     * @param filename              The base filename (without extension) of the sequence file to process
     * @param mCurrentLwMeshAddress
     * @param seqVersion
     */
    void extractFileSequenceMeta(String filename, int mCurrentLwMeshAddress, Integer seqVersion) {
        SeqCache cache = new SeqCache();
        File metadata = findFile(OTAUpdateService.DOWNLOAD_SEQ_DIR, filename, METADATA_FILE_FORMAT);

        ConnectNodeUtil.Companion.connectNodeEquip(mCurrentLwMeshAddress)
                .getSequenceVersion().writeHisVal(seqVersion);

        extractSequenceMetadata(metadata);
        cache.removeRequest(currentOtaRequest);
        otaRequestsQueue.removeIf(intent -> Objects.equals(intent.getStringExtra(MESSAGE_ID), currentOtaRequest));

    }

    /**
     * Extracts and processes an OTA (Over-The-Air) firmware binary file for sequence-based updates.
     *
     * This function:
     * 1. Locates the firmware binary file in the designated download directory
     * 2. Splits the binary into packets of the specified size for OTA transmission
     * 3. Stores the resulting packets in the class-level 'packets' variable
     *
     * @param filename The base filename (without extension) of the firmware binary to process
     *
     * Preconditions:
     * - DOWNLOAD_DIR must be accessible and valid
     * - MPY_FILE_FORMAT must match the actual file extension
     * - FIRMWARE_UPDATE_PACKET_SIZE must be > 0
     *
     */
    void extractFileSequenceOta(String filename) {
        File binary = findFile(OTAUpdateService.DOWNLOAD_DIR, filename, MPY_FILE_FORMAT);
        packets = importFile(binary, MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE);
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
            short currentBand = L.ccu().getAddressBand();
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

    /**
     * Handles progress updates for the OTA (Over-The-Air) sequence transfer from CM (Control Module) to device
     * over modbus interface.
     *
     * This function:
     * 1. Parses the incoming progress message (little-endian format)
     * 2. Updates the last OTA activity timestamp
     * 3. Logs the current progress state
     * 4. Handles different OTA states with appropriate actions:
     *    - Successful sequence reception
     *    - Transfer progress updates
     *    - Completion handling
     *    - Timeout scenarios
     *
     * @param eventBytes The raw byte array containing the progress message
     *
     * Message Handling:
     * - Uses CmToCcuOtaStatus_t structure (little-endian format)
     * - Contains:
     *   - currentState: The OTA progress state (SequenceOtaState/OtaState enum ordinal)
     *   - data: Additional progress data (typically packet sequence number)
     *
     * State Handling:
     * - SEQUENCE_RECEIVED/SEQUENCE_COPY_AVAILABLE:
     *   - Updates diagnostic points
     *   - Checks address band conditions
     *   - Marks update as waiting to complete
     * - CM_DEVICE_IN_PROGRESS:
     *   - Updates progress diagnostics
     * - SEQUENCE_UPDATE_COMPLETE:
     *   - Triggers completion handler
     * - CM_DEVICE_TIMEOUT:
     *   - Broadcasts timeout notification
     */
    private void handleCmToDeviceSequenceProgress(byte[] eventBytes){
        LSerial.fromBytes(eventBytes, CmToCcuOtaStatus_t.class);
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        CmToCcuOtaStatus_t msg = new CmToCcuOtaStatus_t();
        msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

        CcuLog.i(L.TAG_CCU_OTA_PROCESS, " CM to Device process : State : "+ SequenceOtaState.values()[msg.currentState.get()].name() + " Data : "+msg.data.get());
        ConnectNodeUtil.Companion.updateOtaSequenceState(msg.currentState.get(), mCurrentLwMeshAddress);
        if ( msg.currentState.get() == SequenceOtaState.SEQUENCE_COPY_AVAILABLE.ordinal()) {
            ConnectNodeDevice connectNodeEquip = getConnectDevice();
            // Check if the device we are updating is a SN PCN or connect node device
            if(mServerIdPcn == DeviceConstants.PCN_ADDRESS) {
                PCNDevice pcnDevice = PCNUtil.Companion.getPcnNodeDevice(mCurrentLwMeshAddress);
                pcnDevice.updateDeliveryTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
            } else {
                connectNodeEquip.updateDeliveryTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
            }
        }
        if ( msg.currentState.get() == SequenceOtaState.SEQUENCE_RECEIVED.ordinal()
                || msg.currentState.get() == SequenceOtaState.SEQUENCE_COPY_AVAILABLE.ordinal()) {

            updateOtaSequenceStatus(SequenceOtaStatus.SEQ_CCU_TO_CM_FIRMWARE_RECEIVED, mCurrentLwMeshAddress);
            mUpdateWaitingToComplete = true;
        } else if ( msg.currentState.get() == SequenceOtaState.CM_DEVICE_IN_PROGRESS.ordinal()) {
            OtaStatusDiagPoint.Companion.updateCmToDeviceSeqProgress(
                    mUpdateLengthSq / MessageConstants.FIRMWARE_UPDATE_PACKET_SIZE,
                    msg.data.get(), mCurrentLwMeshAddress, mServerIdPcn
            );
        }
        else if ( msg.currentState.get() == SequenceOtaState.CM_DEVICE_TIMEOUT.ordinal()) {
            sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_TIMED_OUT));
        }
    }

    /**
     * Handles the successful completion of a Sequence OTA (Over-The-Air) update.
     *
     * This function:
     * 1. Logs the successful completion
     * 2. Updates the OTA status point to SUCCEEDED
     * 3. Records the confirmation timestamp
     * 4. Initiates the update process for the next node in sequence
     */
    private void handleSequenceComplete() {
        ConnectNodeDevice connectNodeEquip = null;
        connectNodeEquip = getConnectDevice();

        HashMap<Object, Object> deviceMap = CCUHsApi.getInstance().readMapById(connectNodeEquip.getId());
        if(deviceMap.containsKey("roomRef")){
            String roomId = deviceMap.get("roomRef").toString();
            CustomScheduleManager.Companion.getInstance().getReconfiguredRooms().add(roomId);
            CcuLog.i(TAG, "Reconfigured room: "+roomId+" for device: "+connectNodeEquip.getId());
        }
        CcuLog.d(TAG, "Sequence OTA - ReconfiguredRooms:  "+
                CustomScheduleManager.Companion.getInstance().getReconfiguredRooms());
        updateOtaSequenceStatus(SequenceOtaStatus.SEQ_SUCCEEDED, mCurrentLwMeshAddress);
        // Update the delivery time
        if(mServerIdPcn == DeviceConstants.PCN_ADDRESS) {
            // If it is done for PCN device then, get the pcnEquip first, and then update the confirmed time
            PCNDevice pcnDevice = PCNUtil.Companion.getPcnNodeDevice(mCurrentLwMeshAddress);
            pcnDevice.updateConfirmedTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
        } else {
            connectNodeEquip.updateConfirmedTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
        }
        moveUpdateToNextNode();
    }

    /**
     * Retrieves the ConnectNodeDevice instance
     *
     * This function gets the connect device depending on whether the current upgrade is on PCN connect node or normal Connect Node.
     *
     * @return The ConnectNodeDevice instance associated with the current Mesh address.
     */
    private static @NonNull ConnectNodeDevice getConnectDevice() {
        ConnectNodeDevice connectNodeEquip;
        if(mServerIdPcn != -1 && mServerIdPcn < DeviceConstants.PCN_ADDRESS) {
            HashMap<Object, Object> pcnEquip = PCNUtil.Companion.getPcnByNodeAddress(String.valueOf(mCurrentLwMeshAddress), CCUHsApi.getInstance());
            connectNodeEquip = PCNUtil.Companion.getConnectNodeEquip(pcnEquip, mServerIdPcn);
        } else {
            connectNodeEquip = ConnectNodeUtil.Companion.connectNodeEquip(mCurrentLwMeshAddress);
        }
        return connectNodeEquip;
    }

    /**
     * Handles the failure of a Sequence OTA (Over-The-Air) update.
     *
     * This function:
     * 1. Logs the failure
     * 2. Updates the OTA sequence status to FAILED
     * 3. Initiates the update process for the next node in sequence
     */
    private void handleSequenceOtaFail() {
        CcuLog.d(TAG, "Sequence OTA update failed");
        updateOtaSequenceStatus(SequenceOtaStatus.SEQ_UPDATE_FAILED, mCurrentLwMeshAddress);
        moveUpdateToNextNode();
    }

    
    /**
     * @brief Handles the reboot indication message for a PCN (Primary Control Node) device.
     *
     * This function processes the reboot indication message received from a PCN device after an OTA (Over-The-Air) update attempt.
     * 
     *   Parses the given event bytes to populate a {@link PcnRebootIndicationMessage_t} message.
     *   If the device type is null, returns immediately after logging.
     *   Retrieves the associated room using the node address and adds it to the reconfigured rooms
     *       if available, also updating connect node details.
     *   If the device type is not CONTROL_MOTE and the message's node address doesn't match the current node being updated,
     *       it logs and returns.
     *   If the node is being updated and a reboot occurs:
     *     Sends a broadcast about the reboot event.
     *     If this was a sequence OTA, determines if the outcome was a success or failure based on OTA flags and calls the respective handlers.
     *     Otherwise, parses firmware version and reports the result (success or failure) by logging and updating diagnostic status points.
     *     Updates OTA status point according to the actual node status after reboot if available.
     *     Calls {@code moveUpdateToNextNode()} to continue the OTA process for other nodes if necessary.
     *   Catches and logs any exceptions to ensure the handler does not crash due to malformed messages.
     *
     * @param eventBytes The raw byte array containing the reboot indication message from a PCN device.
     */
    private void handlePcnNodeReboot(byte[] eventBytes) {
        try {
            PcnRebootIndicationMessage_t msg = new PcnRebootIndicationMessage_t();
            msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

            if (msg.smartNodeDeviceType == null) {
                CcuLog.d(TAG, " SmartNode device type is null");
                return;
            }
            CcuLog.d(TAG, "Node: " + msg.smartNodeAddress.get() + " Device :" + msg.smartNodeDeviceType.get() + " Device Rebooted");
            HashMap<Object, Object> map = ConnectNodeUtil.Companion.getConnectNodeByNodeAddress(
                    String.valueOf(msg.smartNodeAddress.get()), CCUHsApi.getInstance());

            if(!map.isEmpty()) {
                String roomId = map.get("roomRef").toString();
                CustomScheduleManager.Companion.getInstance().getReconfiguredRooms().add(roomId);
                ConnectNodeUtil.Companion.rewriteConnectNodeDetails(roomId);
            }
            if (msg.smartNodeDeviceType.get() != FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE
                    && mCurrentLwMeshAddress != msg.smartNodeAddress.get()) {
                CcuLog.d(TAG, mCurrentLwMeshAddress + " != " + msg.smartNodeAddress.get());
                return;
            }
            CcuLog.d(TAG, "mUpdateInProgress : " + mUpdateInProgress);
            if ((msg.smartNodeDeviceType.get() == FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE ||
                    (msg.smartNodeAddress.get() == mCurrentLwMeshAddress)) && mUpdateInProgress) {
                sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT));
                // Check if the reboot message is after sequence update. This is to ensure that we dont check for any equip ref since this device dont have the equip ref.
                // We will be returning here for success and failure of sequence update.
                if ((msg.nodeStatus.get() & 0x07) == SEQ_TYPE) {
                    // Extract bits 3-7 (last 5 bits) using bitmask 0xF8 (11111000 in binary)
                    int last5Bits = msg.nodeStatus.get() & NODE_STATUS_FW_OTA_VALUE_MASK;  // Mask keeps only bits 3-7
                    if (last5Bits == NODE_STATUS_VALUE_SEQ_OTA_SUCCESS) {
                        CcuLog.d(TAG, "Sequence OTA update completed for Node: " + mCurrentLwMeshAddress);
                        handleSequenceComplete();
                    } else {
                        CcuLog.d(TAG, "Sequence OTA update failed for Node: " + mCurrentLwMeshAddress);
                        handleSequenceOtaFail();
                    }
                    return;
                }

                short versionMajor = msg.smartNodeMajorFirmwareVersion.get();
                short versionMinor = msg.smartNodeMinorFirmwareVersion.get();
                String ccuName = Domain.ccuDevice.getCcuDisName();
                AlertGenerateHandler.handleDeviceMessage(FIRMWARE_OTA_UPDATE_ENDED, "Firmware OTA update for" + " " + ccuName + " " +
                        "ended for " + mFirmwareDeviceType.getUpdateFileName() + " " + mCurrentLwMeshAddress + " " + "with version" + " " + versionMajor +
                        // changed Smart node to Smart Device as it is indicating the general name (US:9387)
                        "." + versionMinor, getDeviceId(String.valueOf(mFirmwareDeviceType), mCurrentLwMeshAddress));
                CcuLog.d(TAG, mUpdateWaitingToComplete + " - " + versionMajor + " - " + versionMinor);

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
        } catch (Exception e) {
            //CCU should not crash if an older CM sends incorrectly formatted message.
            CcuLog.e(TAG, "Error parsing node reboot message: " + e.getMessage());
        }
    }


    private void handleNodeReboot(byte[] eventBytes) {
        try {
            SnRebootIndicationMessage_t msg = new SnRebootIndicationMessage_t();
            msg.setByteBuffer(ByteBuffer.wrap(eventBytes).order(ByteOrder.LITTLE_ENDIAN), 0);

            if (msg.smartNodeDeviceType == null) {
                CcuLog.d(TAG, " SmartNode device type is null");
                return;
            }
            CcuLog.d(TAG, "Node: " + msg.smartNodeAddress.get() + " Device :" + msg.smartNodeDeviceType.get() + " Device Rebooted");
            HashMap<Object, Object> map = ConnectNodeUtil.Companion.getConnectNodeByNodeAddress(
                    String.valueOf(msg.smartNodeAddress.get()), CCUHsApi.getInstance());

            if(!map.isEmpty()) {
                String roomId = map.get("roomRef").toString();
                CustomScheduleManager.Companion.getInstance().getReconfiguredRooms().add(roomId);
                ConnectNodeUtil.Companion.rewriteConnectNodeDetails(roomId);
            }
            if (msg.smartNodeDeviceType.get() != FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE
                    && mCurrentLwMeshAddress != msg.smartNodeAddress.get()) {
                CcuLog.d(TAG, mCurrentLwMeshAddress + " != " + msg.smartNodeAddress.get());
                return;
            }
            CcuLog.d(TAG, "mUpdateInProgress : " + mUpdateInProgress);
            if ((msg.smartNodeDeviceType.get() == FirmwareDeviceType_t.FIRMWARE_DEVICE_CONTROL_MOTE ||
                    (msg.smartNodeAddress.get() == mCurrentLwMeshAddress)) && mUpdateInProgress) {
                sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_NODE_REBOOT));
                // Check if the reboot message is after sequence update. This is to ensure that we dont check for any equip ref since this device dont have the equip ref.
                // We will be returning here for success and failure of sequence update.
                if ((msg.nodeStatus.get() & 0x07) == SEQ_TYPE) {
                    // Extract bits 3-7 (last 5 bits) using bitmask 0xF8 (11111000 in binary)
                    int last5Bits = msg.nodeStatus.get() & NODE_STATUS_FW_OTA_VALUE_MASK;  // Mask keeps only bits 3-7
                    if (last5Bits == NODE_STATUS_VALUE_SEQ_OTA_SUCCESS) {
                        CcuLog.d(TAG, "Sequence OTA update completed for Node: " + mCurrentLwMeshAddress);
                        handleSequenceComplete();
                    } else {
                        CcuLog.d(TAG, "Sequence OTA update failed for Node: " + mCurrentLwMeshAddress);
                        handleSequenceOtaFail();
                    }
                    return;
                }

                short versionMajor = msg.smartNodeMajorFirmwareVersion.get();
                short versionMinor = msg.smartNodeMinorFirmwareVersion.get();
                String ccuName = Domain.ccuDevice.getCcuDisName();
                AlertGenerateHandler.handleDeviceMessage(FIRMWARE_OTA_UPDATE_ENDED, "Firmware OTA update for" + " " + ccuName + " " +
                        "ended for " + mFirmwareDeviceType.getUpdateFileName() + " " + mCurrentLwMeshAddress + " " + "with version" + " " + versionMajor +
                        // changed Smart node to Smart Device as it is indicating the general name (US:9387)
                        "." + versionMinor, getDeviceId(String.valueOf(mFirmwareDeviceType), mCurrentLwMeshAddress));
                CcuLog.d(TAG, mUpdateWaitingToComplete + " - " + versionMajor + " - " + versionMinor);

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
        } catch (Exception e) {
            //CCU should not crash if an older CM sends incorrectly formatted message.
            CcuLog.e(TAG, "Error parsing node reboot message: " + e.getMessage());
        }
    }

    private String getDeviceId(String deviceType, int deviceMeshAddress) {
        if (deviceType.equals(FirmwareComponentType_t.CONTROL_MOTE_DEVICE_TYPE.toString())) {
            return CCUHsApi.getInstance().readId("(device and domainName == \"cmBoardDevice\" ) or device and cm ");
        }
        // connect module  we can use for hyperstatsplit also ,checking the system ,zone,module  level OTA means i am not sending the connect module ID
        else if (deviceType.equals(FirmwareComponentType_t.CONNECT_MODULE_DEVICE_TYPE.toString()) && !ZoneAndModuleLevelUpdate) {
            return CCUHsApi.getInstance().readId("device and (domainName == \"connectModuleDevice\" or domainName == \"connectNodeDevice\")");
        }
        // For terminal devices
        return CCUHsApi.getInstance().readId("device and addr==\"" + deviceMeshAddress + "\"");
    }

    /**
     * Takes an Intent and parses its extra data to determine what type of device is being
     * updated, and to what version
     *
     * @param intent The Intent which started this OTA update
     */
    private void handleOtaUpdateStartRequest(Intent intent) {
        otaRequestProcessInProgress = true;
        ZoneAndModuleLevelUpdate = false;
        OtaStatusDiagPoint.Companion.setConnectModuleUpdateInZoneOrModuleLevel(false);
        String cmdType = intent.getStringExtra(CMD_TYPE);
        CcuLog.i(TAG, "handleOtaUpdateStartRequest: called started a request");

        // If the sequence update is not requested, delete the files in the download directory
        // Since for the sequence download, the files are already downloaded in the directory
        if(cmdType == null || !cmdType.equals("sequence")) {
            deleteFilesByDeviceType(DOWNLOAD_DIR);
        }
        String id = intent.getStringExtra(ID);
        String firmwareVersion = intent.getStringExtra(FIRMWARE_VERSION);
        String cmdLevel = intent.getStringExtra(CMD_LEVEL);
        currentOtaRequest = intent.getStringExtra(MESSAGE_ID);
        currentRunningRequestType = intent.getStringExtra(CMD_TYPE);

        // If the sequence update is requested, we need to extract the metadata and binary file information and start sequence update
        if (cmdType != null && cmdType.equals("sequence")) {
            boolean eraseSequence = Objects.equals(intent.getStringExtra(ERASE_SEQUENCE), "true");
            mCurrentLwMeshAddress = Integer.parseInt(Objects.requireNonNull(intent.getStringExtra(NODE_ADDRESS)));
            // Extract the metadata and binary file information from the files which are already present in the download directory
            mSequenceMetaFileName = intent.getStringExtra(META_NAME);
            mServerIdPcn = Short.parseShort(intent.getStringExtra(SEQ_SERVER_ID));
            // Send metadata only if the erase sequence is not requested

            if(!eraseSequence) {
                extractFileSequenceMeta(mSequenceMetaFileName, mCurrentLwMeshAddress,
                        Integer.parseInt(Objects.requireNonNull(intent.getStringExtra(SEQ_VERSION))));
                mSequenceSeqFileName = intent.getStringExtra(FIRMWARE_NAME);
                extractFileSequenceOta(mSequenceSeqFileName);
                mSequenceEmptyRequest = false;
            } else {
                SeqCache cache = new SeqCache();
                cache.removeRequest(currentOtaRequest);
                otaRequestsQueue.removeIf(intent1 -> Objects.equals(intent1.getStringExtra(MESSAGE_ID), currentOtaRequest));
                mSequenceEmptyRequest = true;
            }

            startSequenceUpdate(id, String.valueOf(mCurrentLwMeshAddress), eraseSequence);
            return;
        }

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
        } else if (firmwareVersion.startsWith("MyStat_")){
            mFirmwareDeviceType = FirmwareComponentType_t.MY_STAT_DEVICE_TYPE;
            startUpdate(id, cmdLevel, mVersionMajor, mVersionMinor, mFirmwareDeviceType,  currentRunningRequestType, currentOtaRequest);
        } else{
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
                // Following query gets connect module device connected to ADV AHU only and not others(lowCode will ensure the connect module is running in ADV AHU mode)
                HashMap<Object, Object> deviceListConnect= CCUHsApi.getInstance().readEntity("device and connectModule and not hyperstatsplit and not lowCode");
                HashMap equipment =  CCUHsApi.getInstance().readEntity("equip and oao and not hyperstatsplit");
                //OTA is not supported in system level for Connect Module
                if (deviceListConnect != null && !deviceListConnect.isEmpty() && deviceType.equals(FirmwareComponentType_t.CONNECT_MODULE_DEVICE_TYPE)) {
                    CcuLog.i(TAG, "[VALIDATION] Adding Connect Module device " + deviceListConnect.get(Tags.ADDR).toString() + " to update");
                    mLwMeshAddresses.add(Integer.parseInt(deviceListConnect.get(Tags.ADDR).toString()));
                }

                if(!equipment.isEmpty()){
                    Device OAOdevice = HSUtil.getDevice(Short.parseShort(equipment.get(Tags.GROUP).toString()));
                    if(OAOdevice.getMarkers().contains( deviceType.getHsMarkerName() )) {
                        CcuLog.d(TAG, "[VALIDATION] Adding OAO device " + OAOdevice.getAddr() + " to update");
                        mLwMeshAddresses.add(Integer.parseInt(OAOdevice.getAddr()));
                    }
                }


                if(deviceList.containsKey( deviceType.getHsMarkerName())){
                        mLwMeshAddresses.add( 99 + L.ccu().getAddressBand());
                    }

                for(Floor floor : HSUtil.getFloors()) {
                    for(Zone zone : HSUtil.getZones(floor.getId())) {
                        for(Device device : HSUtil.getDevices(zone.getId())) {
                            isConnectModuleUpdate(device, deviceType);
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
                    isConnectModuleUpdate(device, deviceType);
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
                isConnectModuleUpdate(device, deviceType);
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
            OtaCache cache = new OtaCache();
            cache.removeRequest(currentRequest); // Before moving to next request, remove the current request from cache
            CcuLog.d(TAG, "[RESET] OTA Resetting the Update Variables & moving for next request");
            processOtaRequest();
            return;
        }
        OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_REQUEST_RECEIVED,mLwMeshAddresses);

        moveUpdateToNextNode();

        sendBroadcast(new Intent(Globals.IntentActions.OTA_UPDATE_START));
    }

    /**
     * Updates the OTA (Over-The-Air) sequence status based on the device type and addressing scheme.
     *
     * This function routes the status update to the appropriate handler based on the device configuration:
     * - Direct PCN device updates
     * - Connect modules connected to PCN devices
     * - Standard connect modules not associated with PCN
     *
     * @param status The OTA sequence status to update (SUCCESS, FAILED, IN_PROGRESS, etc.)
     * @param address The node address or device identifier for the target device
     *
     * Note: PCN_ADDRESS is a constant that defines the address range reserved for PCN devices.
     *       Devices with addresses below this value are considered connect modules.
     *
     */
    void updateOtaSequenceStatus(SequenceOtaStatus status, int address) {
        if(mServerIdPcn == DeviceConstants.PCN_ADDRESS) {
            // Update the PCN device status based on node address of SN PCN
            PCNUtil.Companion.updateOtaSequenceStatus(status, address);
        } else if (mServerIdPcn != -1 && mServerIdPcn < DeviceConstants.PCN_ADDRESS) {
            // Update the status of connect module OTA status that is connect to PCN device
            PCNUtil.Companion.updateOtaSequenceStatus(status, mServerIdPcn);
        } else {
            // Update the status of normal sequence OTA that is not related to PCN device
            ConnectNodeUtil.Companion.updateOtaSequenceStatus(status, address);
        }
    }

    /**
     * Initiates a sequence OTA (Over-The-Air) update for a specific device.
     *
     * @param id The identifier of the device
     * @param currentRequest The current OTA request identifier for cache management
     *
     * This function:
     * 1. Validates no update is already in progress
     * 2. Initializes/resets the target address list
     * 3. Looks up and validates the device address
     * 4. Updates the OTA status to REQUEST_RECEIVED
     * 5. Handles failure cases with proper cleanup
     * 6. Initiates the metadata transfer if validation passes
     *
     * State Management:
     * - Sets mUpdateInProgress flag to prevent concurrent updates
     * - Maintains mLwMeshSeqAddresses list for target devices
     * - Manages otaRequestProcessInProgress flag
     *
     * Error Handling:
     * - Validates device address availability
     * - Performs complete cleanup on failure
     * - Removes failed requests from cache
     *
     * Broadcasts:
     * - OTA_UPDATE_START when update begins successfully
     */
    private void   startSequenceUpdate(String id, String currentRequest, boolean eraseSequence) {
        // 1. Check for existing update in progress
        if (mUpdateInProgress) {
            CcuLog.d(TAG, "[VALIDATION] Sequence Update already in progress");
            return;
        }

        // 2. Initialize target address list
        if(mLwMeshSeqAddresses == null) {
            mLwMeshSeqAddresses = new ArrayList<>();
        }
        mLwMeshSeqAddresses.clear();

        String address = ConnectNodeUtil.Companion.getAddressById(id, CCUHsApi.getInstance());
        if(mServerIdPcn != -1) {
            address = String.valueOf(currentRequest);
        }
        mLwMeshSeqAddresses.add(Integer.parseInt(address));
        updateOtaSequenceStatus(SequenceOtaStatus.SEQ_REQUEST_RECEIVED, Integer.parseInt(address));

        if(mLwMeshSeqAddresses == null || mLwMeshSeqAddresses.isEmpty()) {
            CcuLog.d(TAG, "[VALIDATION] Could not find device " + id + " at level " + "sequence");

            resetUpdateVariables();
            otaRequestProcessInProgress = false;
            OtaCache cache = new OtaCache();
            cache.removeRequest(currentRequest); // Before moving to next request, remove the current request from cache
            CcuLog.d(TAG, "[RESET] Sequence OTA Resetting the Update Variables & moving for next request");
            return;
        }

        mUpdateInProgress = true;
        // update the device type to be used later during reboot message
        mFirmwareDeviceType = FirmwareComponentType_t.CONNECT_MODULE_SEQUENCE_TYPE;
        if(eraseSequence) {
            sendPacketEraseSequence(mLwMeshSeqAddresses.get(0));
            mLwMeshSeqAddresses.remove(0);
        } else {
            sendSequenceMetadata(FirmwareComponentType_t.CONNECT_MODULE_SEQUENCE_TYPE);
        }

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
        } else if (deviceType.getHsMarkerName().equals(Tags.HYPERSTATSPLIT) && device.getMarkers().contains(Tags.CONNECTMODULE)) {
            // Connect Module is a special case, since it can be used for HyperStat Split, advanced AHU and connect module as a zone
            return true;
        } else if (deviceType.getHsMarkerName().equals(Tags.SMART_NODE) && device.getMarkers().contains(Tags.PCN)) {
            // PCN is nothing but a Smart Node with additional features, so it should be included in Smart Node updates
            mServerIdPcn = 100; // PCN devices have fixed server ID 100
            return true;
        } else if (deviceType.getHsMarkerName().equals(Tags.CONNECTMODULE) && device.getMarkers().contains(Tags.PCN)) {
            mServerIdPcn = (short) Integer.parseInt(device.getAddr());
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
        String ccuName = Domain.ccuDevice.getCcuDisName();
        AlertGenerateHandler.handleDeviceMessage(FIRMWARE_OTA_UPDATE_STARTED, "Firmware OTA update for"+" "+ccuName+" "+
                "started for "+deviceType.getUpdateFileName()+" "+mCurrentLwMeshAddress+" "+"with version"+" "+versionMajor + "." + versionMinor , getDeviceId(String.valueOf(deviceType), mCurrentLwMeshAddress));
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
            for(File file : Objects.requireNonNull(dir.listFiles())) {
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
            moveUpdateToNextNode();
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
     * Parses sequence metadata from a JSON file and updates relevant class properties.
     *
     * @param json The file containing the sequence metadata in JSON format
     * @return true if parsing was successful, false if any error occurred
     *
     * This function:
     * 1. Reads and parses the JSON metadata file
     * 2. Extracts and validates the following fields:
     *    - deviceType: Converts to FirmwareComponentType_t enum
     *    - versionMinor: Stores as short value
     *    - updateLength: Stores sequence length
     *    - firmwareSignature: Converts hex string to byte array
     *    - sequenceName: Stores if non-empty
     * 3. Performs automatic file cleanup on failure
     * 4. Provides detailed debug logging when in debug mode
     */
    private boolean extractSequenceMetadata(File json) {
        try {
            JsonReader reader = new JsonReader(new FileReader(json));

            reader.beginObject();

            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "deviceType":
                        // This is currently only used for HyperStat and HyperStat Split
                        mSequenceDeviceTypeFromMeta = FirmwareComponentType_t.values()[reader.nextInt()];
                        break;
                    case "versionMinor":
                        mSequenceVersion = (short) reader.nextInt();
                        break;
                    case "updateLength":
                        mUpdateLengthSq = reader.nextInt();
                        break;
                    case "firmwareSignature":
                        String firmwareUpdateString = reader.nextString();
                        mSequenceSignature = ByteArrayUtils.hexStringToByteArray(firmwareUpdateString);
                        break;
                    case "sequenceName":
                        String sequenceName = reader.nextString();
                        if (sequenceName != null && !sequenceName.isEmpty()) {
                            mSequenceName = sequenceName;
                        }
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
     * Sends the pcn firmware metadata to the CM
     *
     * @param firmware The type of device being updated
     * @param serverId The server ID for the PCN update
     */
    private void sendFirmwareMetadataPcn(FirmwareComponentType_t firmware, Short serverId) {
        updateOtaStatusToOtaStarted();
        CcuToCmOverUsbFirmwarePcnMetadataMessage_t message = new CcuToCmOverUsbFirmwarePcnMetadataMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_FIRMWARE_METADATA);
        message.lwMeshAddress.set(mCurrentLwMeshAddress);

        FirmwareComponentType_t componentType = getFirmwareComponentType(firmware);
        CcuLog.d(TAG, "index: " + componentType.ordinal() +
                ", filename: " + componentType.getUpdateFileName() +
                ", directory: " + componentType.getUpdateUrlDirectory() +
                ", marker: " + componentType.getHsMarkerName());

        message.metadata.deviceType.set(componentType);

        message.metadata.majorVersion.set(mVersionMajor);
        message.metadata.minorVersion.set(mVersionMinor);
        message.metadata.lengthInBytes.set(mUpdateLength);

        message.metadata.setSignature(mFirmwareSignature);
        message.serverId.set(serverId);
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
     * Sends the firmware metadata to the CM
     *
     * @param firmware The type of device being updated
     */
    private void sendFirmwareMetadata(FirmwareComponentType_t firmware) {
        updateOtaStatusToOtaStarted();
        // Check if this is a PCN update which has a serverId associated with it
        if(mServerIdPcn != -1) {
            sendFirmwareMetadataPcn(firmware, mServerIdPcn);
            return;
        }
        CcuToCmOverUsbFirmwareMetadataMessage_t message = new CcuToCmOverUsbFirmwareMetadataMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_FIRMWARE_METADATA);
        message.lwMeshAddress.set(mCurrentLwMeshAddress);

        FirmwareComponentType_t componentType = getFirmwareComponentType(firmware);
        CcuLog.d(TAG, "index: " + componentType.ordinal() +
                ", filename: " + componentType.getUpdateFileName() +
                ", directory: " + componentType.getUpdateUrlDirectory() +
                ", marker: " + componentType.getHsMarkerName());

        message.metadata.deviceType.set(componentType);

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
     * Sends the firmware metadata to the CM
     *
     * @param firmware The type of device being updated
     */
    private void sendSequenceMetadata(FirmwareComponentType_t firmware) {
        if(mServerIdPcn != -1) {
            sendPcnSequenceMetadata(firmware);
            return;
        }
        // Since we are sending the metadata to CM, we need to remove the current mLwMeshAddresses
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        mCurrentLwMeshAddress = mLwMeshSeqAddresses.get(0);
        ConnectNodeDevice connectNodeEquip = ConnectNodeUtil.Companion.connectNodeEquip(mCurrentLwMeshAddress);
        connectNodeEquip.getSequenceStatus().writePointValue(SequenceOtaStatus.SEQ_UPDATE_STARTED.ordinal());

        mLwMeshSeqAddresses.remove(0);

       // updateOtaStatusToOtaStarted();
        CcuToCmOverUsbSequenceMetadataMessage_t message = new CcuToCmOverUsbSequenceMetadataMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SEQUENCE_METADATA);
        message.lwMeshAddress.set(mCurrentLwMeshAddress);

//        FirmwareComponentType_t componentType = getFirmwareComponentType(firmware);
//        CcuLog.d(TAG, "index: " + componentType.ordinal() +
//                ", filename: " + componentType.getUpdateFileName() +
//                ", directory: " + componentType.getUpdateUrlDirectory() +
//                ", marker: " + componentType.getHsMarkerName());

        message.metadata.lengthInBytes.set(mUpdateLengthSq);

        message.metadata.setSignature(mSequenceSignature);
        message.metadata.setName(mSequenceName);
        message.metadata.sequenceId.set(mSequenceVersion);
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
     * Sends the pcn sequence metadata to the CM
     *
     * @param firmware The type of device being updated
     */
    private void sendPcnSequenceMetadata(FirmwareComponentType_t firmware) {

        // Since we are sending the metadata to CM, we need to remove the current mLwMeshAddresses
        OTAUpdateHandlerService.lastOTAUpdateTime = System.currentTimeMillis();
        mCurrentLwMeshAddress = mLwMeshSeqAddresses.get(0);
        ConnectNodeDevice connectNodeEquip = ConnectNodeUtil.Companion.connectNodeEquip(mCurrentLwMeshAddress);
        connectNodeEquip.getSequenceStatus().writePointValue(SequenceOtaStatus.SEQ_UPDATE_STARTED.ordinal());

        mLwMeshSeqAddresses.remove(0);

        // updateOtaStatusToOtaStarted();
        CcuToCmOverUsbPcnSequenceMetaMessage_t message = new CcuToCmOverUsbPcnSequenceMetaMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SEQUENCE_METADATA);
        message.lwMeshAddress.set(mCurrentLwMeshAddress);

//        FirmwareComponentType_t componentType = getFirmwareComponentType(firmware);
//        CcuLog.d(TAG, "index: " + componentType.ordinal() +
//                ", filename: " + componentType.getUpdateFileName() +
//                ", directory: " + componentType.getUpdateUrlDirectory() +
//                ", marker: " + componentType.getHsMarkerName());

        message.metadata.lengthInBytes.set(mUpdateLengthSq);

        message.metadata.setSignature(mSequenceSignature);
        message.metadata.setName(mSequenceName);
        message.metadata.sequenceId.set(mSequenceVersion);
        message.metadata.slaveId.set(mServerIdPcn); // PCN sequence update is always for slave 0
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
     * Sends a PCN firmware packet to the CM (Central Manager) over USB.
     *
     * <p>This method validates the packet request parameters (such as node address
     * and packet number), checks update completion conditions, constructs the
     * PCN firmware packet message, and transmits it to the CM.
     *
     * <p>Key behavior:</p>
     * <ul>
     *   <li>Rejects packet requests from invalid node addresses or out-of-range packet numbers.</li>
     *   <li>Flags the update as waiting-to-complete upon sending the final packet.</li>
     *   <li>Logs diagnostic details such as node address, packet size, sequence number, and data.</li>
     *   <li>Attempts to send the packet to the CM using {@code MeshUtil.sendStructToCM}.</li>
     * </ul>
     *
     * @param lwMeshAddress The mesh address of the node requesting the packet.
     * @param packetNumber  The index of the packet to be sent.
     */
    private void sendPacketPcn(int lwMeshAddress, int packetNumber) {

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

        CcuToCmOverUsbPcnFirmwarePacketMessage_t message = new CcuToCmOverUsbPcnFirmwarePacketMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_FIRMWARE_PACKET);
        message.lwMeshAddress.set(lwMeshAddress);
        message.sequenceNumber.set(packetNumber);
        message.serverId.set(mServerIdPcn); // PCN update can be for any slave, set during request

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

    public void sendPcnPacketEraseSequence(int lwMeshAddress) {
        CcuToCmOverUsbPcnFirmwareEraseMessage_t message = new CcuToCmOverUsbPcnFirmwareEraseMessage_t();
        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SEQUENCE_ERASE);
        CcuLog.d(TAG, "[UPDATE] Erase sequence request to address " + lwMeshAddress);
        message.lwMeshAddress.set(lwMeshAddress);
        message.serverId.set(mServerIdPcn); // PCN update can be for any slave, set during request
        try {
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            CcuLog.e(TAG, "[UPDATE] [Erase sequence Node Address:" + lwMeshAddress + "] [FAILED]");
        }
    }

    private void sendPacketEraseSequence(int lwMeshAddress) {
        if(mServerIdPcn != -1) {
            sendPcnPacketEraseSequence(lwMeshAddress);
            return;
        }
        CcuToCmOverUsbFirmwarePacketMessage_t message = new CcuToCmOverUsbFirmwarePacketMessage_t();
        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SEQUENCE_ERASE);
        CcuLog.d(TAG, "[UPDATE] Erase sequence request to address " + lwMeshAddress);
        message.lwMeshAddress.set(lwMeshAddress);
        try {
            MeshUtil.sendStructToCM(message);
        } catch (Exception e) {
            CcuLog.e(TAG, "[UPDATE] [Erase sequence Node Address:" + lwMeshAddress + "] [FAILED]");
        }
    }

    private void sendPacketPcnSequence(int lwMeshAddress, int packetNumber) {

        if (lwMeshAddress != mCurrentLwMeshAddress) {
            CcuLog.d(TAG, "[UPDATE] Received pcn packet request from address " + lwMeshAddress + ", instead of " + mCurrentLwMeshAddress);
            return;
        }
        if (mUpdateWaitingToComplete && packetNumber == packets.size() /*- 2*/) {
            CcuLog.d(TAG, "[UPDATE] Received pcn packet request while waiting for the update to complete");
            return;
        }
        if (packetNumber == 0) {
            if(mServerIdPcn == DeviceConstants.PCN_ADDRESS) {
                PCNDevice pcnDevice = PCNUtil.Companion.getPcnNodeDevice(mCurrentLwMeshAddress);
                pcnDevice.updateDeliveryTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
            } else {
                HashMap<Object, Object> pcnEquip = PCNUtil.Companion.getPcnByNodeAddress(String.valueOf(mCurrentLwMeshAddress), CCUHsApi.getInstance());
                ConnectNodeDevice connectNodeEquip = PCNUtil.Companion.getConnectNodeEquip(pcnEquip, mServerIdPcn);
                connectNodeEquip.updateDeliveryTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
            }
        }
        if (packetNumber < 0 || packetNumber > packets.size()) {
            CcuLog.d(TAG, "[UPDATE] Received pcn request for invalid packet: " + packetNumber);
            return;
        }
        if (!mUpdateWaitingToComplete && (packetNumber == (packets.size() - 1))) {
            CcuLog.d(TAG, "[UPDATE] Received pcn request for final packet");
            mUpdateWaitingToComplete = true;
        }

        CcuToCmOverUsbPcnFirmwarePacketMessage_t message = new CcuToCmOverUsbPcnFirmwarePacketMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SEQUENCE_PACKET);
        message.lwMeshAddress.set(lwMeshAddress);
        message.sequenceNumber.set(packetNumber);

        message.setPacket(packets.get(packetNumber));
        message.serverId.set(mServerIdPcn); // PCN update can be for any slave, set during request

        if (packetNumber > mLastSentPacket) {
            mLastSentPacket = packetNumber;
            if (BuildConfig.DEBUG) {
                if (packetNumber % 100 == 0) {
                    CcuLog.d(TAG,
                            "[UPDATE] [PCN] [Node Address:" + lwMeshAddress + "]" + "PS:"+packets.size()+","+message.packet.length+","+message.sequenceNumber.get()+" [PN:" + mLastSentPacket
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

    private void sendPacketSequence(int lwMeshAddress, int packetNumber) {
        if(mServerIdPcn != -1) {
            sendPacketPcnSequence(lwMeshAddress, packetNumber);
            return;
        }

        if (lwMeshAddress != mCurrentLwMeshAddress) {
            CcuLog.d(TAG, "[UPDATE] Received packet request from address " + lwMeshAddress + ", instead of " + mCurrentLwMeshAddress);
            return;
        }
        if (mUpdateWaitingToComplete && packetNumber == packets.size() /*- 2*/) {
            CcuLog.d(TAG, "[UPDATE] Received packet request while waiting for the update to complete");
            return;
        }
        if(packetNumber == 0) {
            ConnectNodeDevice connectNodeEquip = ConnectNodeUtil.Companion.connectNodeEquip(mCurrentLwMeshAddress);
            connectNodeEquip.updateDeliveryTime(HDateTime.make(System.currentTimeMillis(), HTimeZone.make("UTC")));
        }
        if (packetNumber < 0 || packetNumber > packets.size()) {
            CcuLog.d(TAG, "[UPDATE] Received request for invalid packet: " + packetNumber);
            return;
        }
        if (!mUpdateWaitingToComplete && (packetNumber == (packets.size() - 1))) {
            CcuLog.d(TAG, "[UPDATE] Received request for final packet");
            mUpdateWaitingToComplete = true;
//            otaRequestProcessInProgress = false;
//            completeUpdate();
//            processOtaRequest();
//            moveUpdateToNextNode();
        }

        CcuToCmOverUsbFirmwarePacketMessage_t message = new CcuToCmOverUsbFirmwarePacketMessage_t();

        message.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SEQUENCE_PACKET);
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
    private boolean isConnectDeviceToCm(int nodeAddress) {
        HashMap<Object, Object> connectNodeEquip = ConnectNodeUtil.Companion.getConnectNodeByNodeAddress(String.valueOf(nodeAddress), CCUHsApi.getInstance());
        // Ensure that the connectNodeEquip exists and is not already linked to a device. Otherwise it is connected to PCN
        boolean connectNodeEquipAvailable = !connectNodeEquip.isEmpty() && connectNodeEquip.containsKey(Tags.DEVICE_REF);

        return nodeAddress == ( L.ccu().getAddressBand() + 98 ) || connectNodeEquipAvailable;
    }

    private Integer checkSequenceCacheForUpdate() {
        SeqCache seqCache = new SeqCache();
        LinkedTreeMap<String, LinkedTreeMap<String,String>> seqRequests = seqCache.getRequestMap();

        if (seqRequests.isEmpty()) {
            return null;
        }
        LinkedTreeMap<String, String> currentRequest = seqRequests.entrySet().iterator().next().getValue();
        if(mLwMeshSeqAddresses == null) {
            mLwMeshSeqAddresses = new ArrayList<>();
        }
        mLwMeshSeqAddresses.clear();
        mLwMeshSeqAddresses.add(Integer.valueOf(Objects.requireNonNull(currentRequest.get(NODE_ADDRESS))));
        mCurrentLwMeshAddress = Integer.parseInt(Objects.requireNonNull(currentRequest.get(NODE_ADDRESS)));
//        mFirmwareDeviceType = FirmwareComponentType_t.values()[Integer.parseInt(Objects.requireNonNull(DEVICE_TYPE))];
        mSequenceMetaFileName = Objects.requireNonNull(currentRequest.get(META_NAME));
        mSequenceSeqFileName = Objects.requireNonNull(currentRequest.get(FIRMWARE_NAME));
        currentOtaRequest = Objects.requireNonNull(currentRequest.get(MESSAGE_ID));
        mServerIdPcn = Short.parseShort(currentRequest.get(SEQ_SERVER_ID));
        //
        String eraseSequenceStr = currentRequest.getOrDefault(ERASE_SEQUENCE, "false");
        mSequenceEmptyRequest = eraseSequenceStr.equals("true");
        CcuLog.d(TAG, "[VALIDATION] Found sequence update in cache for device type: " + mFirmwareDeviceType + ", addresses: " + mLwMeshSeqAddresses);

        return Integer.valueOf(Objects.requireNonNull(currentRequest.get(SEQ_VERSION)));
    }

    private void moveUpdateToNextNode() {
        // Add support for the sequence update
        resetUpdateVariables();
        Integer seqVersion = checkSequenceCacheForUpdate();
        if((mLwMeshAddresses == null || mLwMeshAddresses.isEmpty()) && (mLwMeshSeqAddresses == null || mLwMeshSeqAddresses.isEmpty())) {
            completeUpdate();
            deleteFilesByDeviceType(DOWNLOAD_DIR);
            return;
        }
        if(mLwMeshAddresses != null && !mLwMeshAddresses.isEmpty() && !isConnectDeviceToCm(mLwMeshAddresses.get(0)) && !HeartBeatUtil.isModuleAlive(mLwMeshAddresses.get(0).toString()) && !isCMDevice(mLwMeshAddresses.get(0)) ) {
            CcuLog.d(TAG, "[UPDATE] [FAILED] [Node Address:" + mLwMeshAddresses.get(0) + "] Skipping update due to RF Signal Dead ");
            OtaStatusDiagPoint.Companion.updateOtaStatusPoint(OtaStatus.OTA_CCU_TO_CM_FAILED, mLwMeshAddresses.get(0));
            mLwMeshAddresses.remove(0);
            moveUpdateToNextNode();
            return;
        }

        // Give first preference to sequence update since they are smaller in size
        if(mLwMeshSeqAddresses != null && !mLwMeshSeqAddresses.isEmpty()) {
            ConnectNodeDevice connectNodeEquip = ConnectNodeUtil.Companion.connectNodeEquip(mCurrentLwMeshAddress);
            connectNodeEquip.getSequenceStatus().writePointValue(SequenceOtaStatus.SEQ_UPDATE_STARTED.ordinal());
            // update the device type to be used later during reboot message
            mFirmwareDeviceType = FirmwareComponentType_t.CONNECT_MODULE_SEQUENCE_TYPE;
            if(!mSequenceEmptyRequest) {
                extractFileSequenceMeta(mSequenceMetaFileName, mCurrentLwMeshAddress, seqVersion);
                extractFileSequenceOta(mSequenceSeqFileName);
                sendSequenceMetadata(FirmwareComponentType_t.CONNECT_MODULE_SEQUENCE_TYPE);
            } else {
                SeqCache cache = new SeqCache();
                cache.removeRequest(currentOtaRequest);
                otaRequestsQueue.removeIf(intent1 -> Objects.equals(intent1.getStringExtra(MESSAGE_ID), currentOtaRequest));
                sendPacketEraseSequence(mLwMeshSeqAddresses.get(0));
                mLwMeshSeqAddresses.remove(0);
            }
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
        mServerIdPcn = -1;

        mMetadataDownloadId = -1;
        mBinaryDownloadId = -1;

        mUpdateLengthSq = -1;
        mSequenceVersion = -1;
        mSequenceName = "";
        mSequenceEmptyRequest = false;

        mUpdateWaitingToComplete = false;
        CcuLog.d(TAG, "[RESET] Reset OTA update status");

    }

    /**
     *
     */
    private void completeUpdate() {
        deleteFilesByDeviceType(DOWNLOAD_DIR, mFirmwareDeviceType);
        new OtaCache().saveRunningDeviceDetails(-1,-1,-1,-1, new ArrayList<>());
        new SeqCache().saveRunningDeviceDetails(-1,-1,"","", new ArrayList<>());
        mVersionMajor = -1;
        mVersionMinor = -1;

        mUpdateLength = -1;
        mServerIdPcn = -1;

        mMetadataDownloadId = -1;
        mBinaryDownloadId = -1;

        mUpdateLengthSq = -1;
        mSequenceVersion = -1;
        mSequenceName = null;
        mSequenceEmptyRequest = false;

        mUpdateInProgress = false;
        Intent completeIntent = new Intent(Globals.IntentActions.OTA_UPDATE_COMPLETE);
        sendBroadcast(completeIntent);
        otaRequestProcessInProgress = false;
        CcuLog.d(TAG, "[RESET] OTA Request process completed moving for next request");
        processOtaRequest();

    }

    /**
     * Check if the request is already present in the queue by comparing the fields of the request queue with
     * the new request
     */
    boolean isSequenceRequestPresent(Intent newOtaRequest) {

        if (otaRequestsQueue.isEmpty()){
            return false;
        }

        for (Intent request : otaRequestsQueue) {
            if (request == null) continue;
            if(request.getStringExtra(ID).equals(newOtaRequest.getStringExtra(ID)) &&
                    request.getStringExtra(SEQ_VERSION).equals(newOtaRequest.getStringExtra(SEQ_VERSION)) &&
                    request.getStringExtra(NODE_ADDRESS).equals(newOtaRequest.getStringExtra(NODE_ADDRESS)) ){
                return true;
            }
        }
        return false;
    }

    private void sequenceRequest(Intent seqRequest) {
        CcuLog.i(TAG, "sequenceRequest: " + seqRequest);

        // Check if we received the addition request and add to queue
        handleDeviceList(seqRequest, seqRequest.getStringArrayListExtra(DEVICE_LIST), false);

        // Check if we received the deletion request and add to queue
        handleDeviceList(seqRequest, seqRequest.getStringArrayListExtra(REMOVE_LIST), true);
    }

    private void handleDeviceList(Intent seqRequest, ArrayList<String> deviceList, boolean isRemove) {
        if (deviceList == null || deviceList.isEmpty()) return;

        for (String device : deviceList) {
            Intent deviceIntent = new Intent(seqRequest);
            HashMap<Object, Object> deviceMap = CCUHsApi.getInstance().readMapById(device);
            boolean isPCN = deviceMap.containsKey("domainName")
                    && ModelNames.pcnDevice.equals(deviceMap.get("domainName"));
            boolean isCNInPcn = PCNUtil.Companion.isConnectNodeInPCN(deviceMap);

            deviceIntent.putExtra(ID, device);
            String nodeAddress = ConnectNodeUtil.Companion.getAddressById(device, CCUHsApi.getInstance());
//            deviceIntent.putExtra(NODE_ADDRESS, nodeAddress);

            // Common fields
//            deviceIntent.putExtra(MESSAGE_ID, nodeAddress);
            deviceIntent.putExtra(SEQ_NAME, seqRequest.getStringExtra(SEQ_NAME));
            deviceIntent.putExtra(CMD_LEVEL, seqRequest.getStringExtra(CMD_LEVEL));
            deviceIntent.putExtra(CMD_TYPE, seqRequest.getStringExtra(CMD_TYPE));
            deviceIntent.putExtra(SEQ_VERSION, seqRequest.getStringExtra(SEQ_VERSION));

            if (!isRemove) {
                // Add request
                deviceIntent.putExtra(META_NAME, seqRequest.getStringExtra(META_NAME));
                deviceIntent.putExtra(FIRMWARE_NAME, seqRequest.getStringExtra(FIRMWARE_NAME));
                deviceIntent.putExtra(ERASE_SEQUENCE, ERASE_SEQUENCE_FALSE);
            } else {
                // Remove request
                deviceIntent.putExtra(META_NAME, seqRequest.getStringExtra(EMPTY_META_NAME));
                deviceIntent.putExtra(FIRMWARE_NAME, seqRequest.getStringExtra(EMPTY_FIRMWARE_NAME));
                deviceIntent.putExtra(ERASE_SEQUENCE, ERASE_SEQUENCE_TRUE);
            }

            // Server ID logic
            if (isPCN) {
                String slaveAddress = "100"; // PCN sequence update is always for slave 100
                deviceIntent.putExtra(NODE_ADDRESS, nodeAddress);
                // In order to maintain the unique message ID for sequence request, we are combining the mesh address and slave address
                deviceIntent.putExtra(MESSAGE_ID, nodeAddress+slaveAddress);
                deviceIntent.putExtra(SEQ_SERVER_ID, "100");
            } else if (isCNInPcn) {
                String deviceRef = (String) CCUHsApi.getInstance().readMapById(device).get("deviceRef");
                String meshAddress = CCUHsApi.getInstance().readMapById(deviceRef).get("addr").toString();
                String slaveAddress = Objects.requireNonNull(deviceMap.get(Tags.ADDR)).toString();
                deviceIntent.putExtra(NODE_ADDRESS, meshAddress);
                // In order to maintain the unique message ID for sequence request, we are combining the mesh address and slave address
                deviceIntent.putExtra(MESSAGE_ID, meshAddress+slaveAddress);
                deviceIntent.putExtra(SEQ_SERVER_ID, slaveAddress);
            } else {
                deviceIntent.putExtra(MESSAGE_ID, nodeAddress);
                deviceIntent.putExtra(SEQ_SERVER_ID, "-1");
                deviceIntent.putExtra(NODE_ADDRESS, nodeAddress);
            }

            addSequenceRequest(deviceIntent);
        }
    }


    void addSequenceRequest(Intent seqRequest) {
        CcuLog.i(TAG, "addSequenceRequest: "+ otaRequestsQueue);

        if(!isSequenceRequestPresentInCache(seqRequest)) {
            otaRequestsQueue.add(seqRequest);
            SeqCache seqCache = new SeqCache();
            seqCache.saveRequest(seqRequest);
            CcuLog.i(TAG, "Sequence request added to queue: "+ seqRequest);
        } else {
            CcuLog.i(TAG, "Sequence request already present in queue. Skipping the request");
        }
        CcuLog.i(TAG, "Current Sequence size : "+otaRequestsQueue.size());
        processOtaRequest();

    }

    /**
     * Push OTA request to Queue
     * 1. Check if the request is already present in the queue
     * 2. If not present, add the request to the queue
     * 3. Start the retry handler
     * 4. Process the request
     */
    void addRequest(Intent otaRequest){

        CcuLog.i(TAG, "addRequest: "+ otaRequestsQueue);
        if (otaRequest != null) {
            if (!isOtaRequestPresentInCache(otaRequest)) {
                otaRequestsQueue.add(otaRequest);
                OtaCache cache = new OtaCache();
                cache.saveRequest(otaRequest);
            }
            else {
                CcuLog.i(TAG, "Request already present in cache. Skipping the request");
            }
             startRetryHandler();
            CcuLog.i(TAG, "Current Requests size : "+otaRequestsQueue.size());
            processOtaRequest();

        }
    }

    /* * This method is used to handle the OTA and sequence update start request
     * It checks if the request is already present in the queue and if not, it adds the request to the queue
     * and starts the retry handler
     */
    void processOtaRequest(){
        CcuLog.i(TAG,"processOtaRequest Called " + otaRequestsQueue.size() + " otaRequestProcessInProgress"+otaRequestProcessInProgress );
        try {
            CcuLog.d(TAG,"CM Connection Status -> "+LSerial.getInstance().isConnected());
            if (!otaRequestProcessInProgress && LSerial.getInstance().isConnected()){
                if((mLwMeshAddresses != null && !mLwMeshAddresses.isEmpty()) || (mLwMeshSeqAddresses != null && !mLwMeshSeqAddresses.isEmpty())) {
                    moveUpdateToNextNode();
                } else if (!otaRequestsQueue.isEmpty()) {
                    handleOtaUpdateStartRequest(Objects.requireNonNull(otaRequestsQueue.poll()));
                }
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
        return  (mCurrentLwMeshAddress ==  (L.ccu().getAddressBand() + 99));
    }

    public static void resetOtaRequestProcessInProgress() {
        otaRequestProcessInProgress = false;
        mUpdateInProgress = false;
        new OTAUpdateService().resetUpdateVariables();
    }

    //This method is used to check if the request is already present in cache and avoid duplicate request
    private boolean isOtaRequestPresentInCache(Intent newRequest){
        OtaCache cache = new OtaCache();
        LinkedTreeMap<String, LinkedTreeMap<String,String>> otaRequests = cache.getRequestMap();

//        otaRequests.clear(); // Delete this!
        if (otaRequests.isEmpty()){
            return false;
        }
        for (Map.Entry<String, LinkedTreeMap<String, String>> entry : otaRequests.entrySet()) {
            LinkedTreeMap<String, String> request = entry.getValue();
            if(request.get(ID).equals(newRequest.getStringExtra(ID)) &&
                     request.get(CMD_TYPE).equals(newRequest.getStringExtra(CMD_TYPE)) &&
                        request.get(FIRMWARE_VERSION).equals(newRequest.getStringExtra(FIRMWARE_VERSION)) ){
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the request is already present in the queue by comparing the fields of the request queue with
     * the new request
     */
    boolean isSequenceRequestPresentInCache(Intent newOtaRequest) {
        SeqCache cache = new SeqCache();
        LinkedTreeMap<String, LinkedTreeMap<String,String>> seqRequests = cache.getRequestMap();

//        otaRequests.clear(); // Delete this!
//        cache.clear();
        if (seqRequests.isEmpty()){
            return false;
        }
        for (Map.Entry<String, LinkedTreeMap<String, String>> entry : seqRequests.entrySet()) {
            LinkedTreeMap<String, String> request = entry.getValue();
            if(Objects.equals(request.get(ID), newOtaRequest.getStringExtra(ID)) &&
                    Objects.equals(request.get(SEQ_VERSION), newOtaRequest.getStringExtra(SEQ_VERSION))){
                if (!otaRequestsQueue.contains(newOtaRequest)) {
                    otaRequestsQueue.add(newOtaRequest);
                }
                return true;
            }
        }
        return false;
    }

    private void isConnectModuleUpdate(Device device, FirmwareComponentType_t deviceType) {
        if(deviceType.equals(FirmwareComponentType_t.CONNECT_MODULE_DEVICE_TYPE) && device.getMarkers().contains(Tags.HYPERSTATSPLIT)) {
            OtaStatusDiagPoint.Companion.setConnectModuleUpdateInZoneOrModuleLevel(true);
            ZoneAndModuleLevelUpdate = true;
            CcuLog.i(TAG, "Connect Module device found  in zone or Module level ");
        }
    }
    private boolean isCMDevice(int nodeAddress) {
        return nodeAddress == ( L.ccu().getAddressBand() + 99 );
    }

    private FirmwareComponentType_t getFirmwareComponentType(FirmwareComponentType_t componentType) {
        if ((componentType.equals(FirmwareComponentType_t.HYPER_STAT_DEVICE_TYPE) &&
                mFirmwareDeviceTypeFromMeta.equals(FirmwareComponentType_t.HYPERSTAT_SPLIT_DEVICE_TYPE))
                || (componentType.equals(FirmwareComponentType_t.SMART_NODE_DEVICE_TYPE) &&
                mFirmwareDeviceTypeFromMeta.equals(FirmwareComponentType_t.SMART_NODE2_DEVICE_TYPE))) {
            return mFirmwareDeviceTypeFromMeta;
        }
        return componentType;
    }
}
