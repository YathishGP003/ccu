package a75f.io.renatus.ENGG;

import static android.app.DownloadManager.STATUS_RUNNING;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.diag.otastatus.OtaStatus;
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint;
import a75f.io.renatus.AboutFragment;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.registration.UpdateCCUFragment;

public class AppInstaller
{

    static final String CCU_APK_FILE_NAME = "Renatus_new.apk";

    public static final String DOWNLOAD_BASE_URL = "https://updates.75f.io/";
    static final String CCU_DOWNLOAD_FILE = "Renatus_Prod_Rv.apk";

    static final int CCUAPP_INSTALL_CODE = 100;
    static final int HOMEAPP_INSTALL_CODE = 200;
    static final int HOMEAPP_AND_CCUAPP_INSTALL_CODE = 300;
    static AppInstaller mSelf = null;
    private long mCCUAppDownloadId = -1;
    private long mHomeAppDownloadId = -1;


    public static synchronized AppInstaller getHandle()
    {
        if (mSelf == null)
        {
            mSelf = new AppInstaller();
        }
        return mSelf;
    }



    public long getCCUAppDownloadId()
    {
        return mCCUAppDownloadId;
    }


    public void setCCUAppDownloadId(long mCCUAppDownloadId) {
        this.mCCUAppDownloadId = mCCUAppDownloadId;
    }


    public long getHomeAppDownloadId()
    {
        return mHomeAppDownloadId;
    }


    public void setHomeAppDownloadId(long mHomeAppDownloadId) {
        this.mHomeAppDownloadId = mHomeAppDownloadId;
    }


    public void downloadInstalls() {
        reset();
        setCCUAppDownloadId(downloadFile(DOWNLOAD_BASE_URL+CCU_DOWNLOAD_FILE, CCU_APK_FILE_NAME, null, null));
        //setHomeAppDownloadId(downloadFile(DOWNLOAD_BASE_URL+HOME_DOWNLOAD_FILE, HOME_APK_FILE_NAME));
    }


    private void reset() {
        mCCUAppDownloadId = -1;
        mHomeAppDownloadId = -1;
    }

    private synchronized long downloadFile(String url, String apkFile, Fragment currentFragment, FragmentActivity activity) {
        DownloadManager manager =
                (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
        removeAllQueuedDownloads(manager);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading "+apkFile+" software");
        request.setTitle("Downloading "+apkFile+" app");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setAllowedOverRoaming(true)
                .setAllowedOverMetered(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkFile);
        if (file.exists())
        {
            file.delete();
        }
        request.setDestinationInExternalFilesDir(RenatusApp.getAppContext(), null, apkFile);
        long dowloadId = manager.enqueue(request);
        CcuLog.d(L.TAG_CCU_DOWNLOAD, "downloading file: "+dowloadId+","+url);
        if(currentFragment != null) {
            checkDownload(dowloadId, manager, currentFragment, activity);
        }
        return dowloadId;
    }

    private void removeAllQueuedDownloads(DownloadManager manager) {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = manager.query(query);

        if (cursor != null) {
            try {
                int idColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                int statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                while (cursor.moveToNext()) {
                    int status = cursor.getInt(statusColumnIndex);
                    if (status != STATUS_SUCCESSFUL && status != STATUS_RUNNING) {
                        long downloadId = cursor.getLong(idColumnIndex);
                        manager.remove(downloadId);
                        CcuLog.d(L.TAG_CCU_DOWNLOAD, "Removed download: " + downloadId);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    public boolean isFIleDownloaded(long downloadId) {
        if(downloadId!=-1) {
            DownloadManager dm = (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor cursor = dm.query(query);
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                CcuLog.d(L.TAG_CCU_DOWNLOAD, "File download status : "+downloadStatus+", downloadID: "+id+" downloadId arg: "+downloadId);
                if (downloadStatus == STATUS_RUNNING) {
                    CcuLog.d(L.TAG_CCU_DOWNLOAD, "File is still downloading, downloadID: "+downloadId);
                    return false;
                } else {
                    CcuLog.d(L.TAG_CCU_DOWNLOAD, "File download status : "+downloadStatus+", downloadID: "+downloadId);
                    dm.remove(downloadId);
                }
            }
        }
        return true;
    }

    public void checkDownload(long downloadId, DownloadManager downloadManager, Fragment currentFragment, FragmentActivity activity) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    CcuLog.i(L.TAG_CCU_DOWNLOAD,"columnIndex columnIndex "+columnIndex);
                    CcuLog.i(L.TAG_CCU_DOWNLOAD,"columnIndex"+cursor.getInt(columnIndex)+" m "+DownloadManager.STATUS_FAILED);
                    if (cursor.getInt(columnIndex) == DownloadManager.STATUS_FAILED) {
                        CcuLog.i("ccu_download ","failed"+cursor.getInt(columnIndex));
                    }
                    if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                        if (currentFragment instanceof UpdateCCUFragment) {
                            UpdateCCUFragment updateCCUFragment = (UpdateCCUFragment) currentFragment;
                            updateCCUFragment.setProgress(100, downloadId, cursor.getInt(columnIndex));
                        }

                        timer.cancel();
                    } else {
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                        if (currentFragment instanceof UpdateCCUFragment) {
                            UpdateCCUFragment updateCCUFragment = (UpdateCCUFragment) currentFragment;
                            updateCCUFragment.setProgress(progress, downloadId, cursor.getInt(columnIndex));
                        }
                        CcuLog.i(L.TAG_CCU_DOWNLOAD, "Downloaded: " + progress + "%");
                    }
                } else {
                    CcuLog.i(L.TAG_CCU_DOWNLOAD, "Download cancelled");
                    cursor.close();
                    timer.cancel();
                    if (currentFragment instanceof UpdateCCUFragment) {
                        UpdateCCUFragment updateCCUFragment = (UpdateCCUFragment) currentFragment;
                        updateCCUFragment.downloadCanceled();
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    public void downloadCCUInstall(String sFileName, Fragment currentFragment, FragmentActivity activity) {
        if(isFIleDownloaded(getCCUAppDownloadId())) {
            long downloadId = downloadFile(DOWNLOAD_BASE_URL+sFileName, CCU_APK_FILE_NAME, currentFragment, activity);
            CcuLog.d(L.TAG_CCU_DOWNLOAD, "Generated downloadId: "+downloadId);
            setCCUAppDownloadId(downloadId);
        }
    }
    
    
    public void install(Activity activity, boolean bInstallHomeApp, boolean bInstallCCUApp, boolean bSilent) {
        if (bInstallHomeApp && bInstallCCUApp) {
            invokeInstallerIntent(activity, mHomeAppDownloadId, HOMEAPP_AND_CCUAPP_INSTALL_CODE, bSilent);
        }
        else if (bInstallHomeApp) {
            invokeInstallerIntent(activity, mHomeAppDownloadId, HOMEAPP_INSTALL_CODE, bSilent);
        }
        else if (bInstallCCUApp) {
            CcuLog.d(L.TAG_CCU_DOWNLOAD, "Install AppInstall===>>>");
            invokeInstallerIntent(activity, mCCUAppDownloadId, CCUAPP_INSTALL_CODE, bSilent);
        }
    }
    
    
    private void invokeInstallerIntent(Activity activity, long downloadId, int requestCode, boolean bSilent) {
        try
        {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (bSilent) {
                String sFilePath = RenatusApp.getAppContext().getExternalFilesDir(null).getPath()+"/"+CCU_APK_FILE_NAME ;
                if (!sFilePath.isEmpty())
                {
                    OtaStatusDiagPoint.Companion.updateCCUOtaStatus(OtaStatus.OTA_UPDATE_STARTED);
                    try {
                        int syncResult = Runtime.getRuntime().exec("sync").waitFor();
                        if (syncResult != 0) {
                            CcuLog.e(L.TAG_CCU_DOWNLOAD, "sync failed");
                        } else {
                            CcuLog.d(L.TAG_CCU_DOWNLOAD, "sync success");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        CcuLog.e(L.TAG_CCU_DOWNLOAD, "sync failed: " + e.getMessage());
                    }
                    CcuLog.i(L.TAG_CCU_DOWNLOAD,"sleeping the thread for  second to complete the sync process ");
                    Thread.sleep(3000);
                    File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), CCU_APK_FILE_NAME);
                    final String[] commands = {"pm install -r -d -g "+file.getAbsolutePath()};

                    CcuLog.d(L.TAG_CCU_DOWNLOAD, "Install AppInstall silent invokeInstallerIntent===>>>"+sFilePath+","+file.getAbsolutePath());
                    CCUHsApi.getInstance().resetCcuReady();
                    RenatusApp.executeAsRoot(commands, null, true, false);
                    OtaStatusDiagPoint.Companion.updateCCUOtaStatus(OtaStatus.OTA_SUCCEEDED);
                    Globals.getInstance().setCcuUpdateTriggerTimeToken(0);
                }
            }
            else {
                Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                Uri uri = Uri.fromFile(new File(RenatusApp.getAppContext().getExternalFilesDir(null).getPath()+"/"+CCU_APK_FILE_NAME ));
                installIntent.setData(uri);
                CcuLog.d(L.TAG_CCU_DOWNLOAD, "Install AppInstall invokeInstallerIntent not silently===>>>"+manager.getUriForDownloadedFile(downloadId).getPath()+","+uri.getPath());
                //installIntent.setData(manager.getUriForDownloadedFile(downloadId));
                installIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                installIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                activity.startActivityForResult(installIntent, requestCode);
            }
        }
        catch (ActivityNotFoundException e) {
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "ActivityNotFoundException ".concat(e.getMessage()));

        }catch (Exception e){
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "Exception ".concat(e.getMessage()));
        }
    }


    private boolean checkForVersionAndNotify(long downloadId) {
        try {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            String sFilePath = manager.getUriForDownloadedFile(downloadId).getLastPathSegment();

            PackageManager pm = RenatusApp.getAppContext().getPackageManager();
            PackageInfo packageInfo = getPackageInfo(pm, sFilePath);
            if (packageInfo != null) {
                CcuLog.d(L.TAG_CCU_DOWNLOAD, "New Version: "+sFilePath+" "+packageInfo.versionName+"."+
                        packageInfo.versionCode);
                PackageInfo pi = pm.getPackageInfo(packageInfo.packageName, 0);{
                    CcuLog.d(L.TAG_CCU_DOWNLOAD, "Installed Version: "+pi.versionName+"."+ pi.versionCode);
                }
                if (packageInfo.versionCode > pi.versionCode) {
                    CcuLog.d(L.TAG_CCU_DOWNLOAD, "*****New version available to install");
                    return true;
                }
            }
        }
        catch (NameNotFoundException e) {
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "***exception Called*** ".concat(e.toString()));
            return true;
        }
        catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "***exception Called*** ".concat(e.toString()));
        }
        return false;
    }

    private PackageInfo getPackageInfo(PackageManager pm, String sFilePath) {
        PackageInfo pinew = pm.getPackageArchiveInfo(sFilePath, 0);
        if (pinew == null) {
            File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), CCU_APK_FILE_NAME);
            pinew = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
        }
        return pinew;
    }

    public boolean isNewCCUAppAvailable()
    {
        return checkForVersionAndNotify(mCCUAppDownloadId);
    }


    public int getDownloadedFileVersion(long downloadId) {
        try {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            String sFilePath = manager.getUriForDownloadedFile(downloadId).getPath();
            PackageManager pm = RenatusApp.getAppContext().getPackageManager();
            PackageInfo packageInfo = getPackageInfo(pm, sFilePath);
            if (packageInfo != null) {
                return packageInfo.versionCode;
            }
        }
        catch (NullPointerException e) {
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "***exception Called*** ".concat(e.toString()));
        }
        return 1;
    }
}
