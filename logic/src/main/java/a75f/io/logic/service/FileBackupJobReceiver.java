package a75f.io.logic.service;
import static a75f.io.logic.L.TAG_CCU_BACKUP;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.common.base.Strings;

import org.projecthaystack.HRef;

import java.io.File;
import java.io.IOException;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.cloud.FileBackupManager;
import a75f.io.logic.util.backupfiles.FileConstants;
import a75f.io.logic.util.backupfiles.FileOperationsUtil;

public class FileBackupJobReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        performConfigFileBackup();
        performModbusSideLoadedJsonsBackup();
    }
    private static void performConfigFileBackup(){
        try {
            String ccuId = getCcuId();
            if (Strings.isNullOrEmpty(ccuId)) {
                return;
            }
            String siteId =getSiteId();
            
            if (Strings.isNullOrEmpty(siteId)) {
                return;
            }
            Log.i(TAG_CCU_BACKUP," File backup service invoked  for Config files "+ccuId);
            FileOperationsUtil.zipSingleFile(FileConstants.CCU_CONFIG_FILE_PATH, FileConstants.CCU_CONFIG_FILE_NAME,
                    ccuId);
            File file = new File(FileConstants.CCU_CONFIG_FILE_PATH + ccuId  + ".zip");
            new FileBackupManager().uploadBackupConfigFiles(file, siteId, ccuId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void performModbusSideLoadedJsonsBackup(){
        try {
            String ccuId = getCcuId();
            if (Strings.isNullOrEmpty(ccuId)) {
                return;
            }
            String siteId =getSiteId();
            Log.i(TAG_CCU_BACKUP," File backup service invoked  for side-loaded modbus json files "+ccuId);
            FileOperationsUtil.zipFolder(FileConstants.MODBUS_SIDE_LOADED_JSON_PATH, ccuId);
            File file = new File(FileConstants.MODBUS_SIDE_LOADED_JSON_PATH + ccuId  + ".zip");
            if(file.exists()) {
                new FileBackupManager().uploadModbusSideLoadedJsonsFiles(file, siteId, ccuId);
            }
            else{
                Log.i(TAG_CCU_BACKUP,"Side-loaded modbus json files are not present to backup "+ccuId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String getCcuId(){
        String ccuId = CCUHsApi.getInstance().getCcuId();
        return (!Strings.isNullOrEmpty(ccuId) && ccuId.startsWith("@")) ? ccuId.substring(1) : ccuId;
    }
    private static String getSiteId(){
        HRef siteRef = CCUHsApi.getInstance().getSiteIdRef();
        if (siteRef == null) {
            return null;
        }
        String siteId = siteRef.toString();
        return (!Strings.isNullOrEmpty(siteId) && siteId.startsWith("@")) ? siteId.substring(1) : siteId;
    }
}