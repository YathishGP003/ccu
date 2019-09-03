package a75f.io.renatus.ENGG;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import a75f.io.renatus.RenatusApp;

public class AppInstaller
{
    
    static final String CCU_APK_FILE_NAME = "Renatus_new.apk";
    static final String HOME_APK_FILE_NAME = "75FRenatus_Home.apk";
    
    static final String DOWNLOAD_BASE_URL = "http://updates.75fahrenheit.com/";
    static final String CCU_DOWNLOAD_FILE = "Renatus_Prod_Rv.apk";
    static final String HOME_DOWNLOAD_FILE = "75FHomeRV1.apk";
    
    static final int CCUAPP_INSTALL_CODE = 100;
    static final int HOMEAPP_INSTALL_CODE = 200;
    static final int HOMEAPP_AND_CCUAPP_INSTALL_CODE = 300;
    static final String CCU_DOWNLOAD_FILE_DIR = "/data/app/CCUV2.apk";
    static final String CCU_NEW_FILE = "/system/priv-app/";
    public static final String COM_X75_APPMOVER_PACKAGE_NAME = "com.x75.appmover";
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
    
    
    @WorkerThread
    /***
     *  silentInstallMoverApp()
     *
     *  If the mover application package doesn't exist install it.
     *
     *
     */
    public static void silentInstallMoverApp()
    {
        final PackageManager packageManager = RenatusApp.getAppContext().getPackageManager();
        ApplicationInfo applicationInfo = null;
        try
        {
            applicationInfo = packageManager.getApplicationInfo(COM_X75_APPMOVER_PACKAGE_NAME, 0);
        }
        catch (NameNotFoundException e)
        {
            e.printStackTrace();
        }
        if (applicationInfo == null)
        {
            Log.e("upgrade", "The app wasn't installed, installing now");
            final String libs = "";
            try
            {
                
                String sFilePath = moveAssetToExternalStorage("mover.apk");
    
     
                if (!sFilePath.isEmpty())
                {
                    final String[] commands = {libs+"pm install -r -d "+sFilePath};
                    RenatusApp.executeAsRoot(commands);
                }
                try
                {
                    final String[] commands = {libs+"rm "+sFilePath};
                    RenatusApp.executeAsRoot(commands);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    
    private static String moveAssetToExternalStorage(String name) throws IOException
    {
        AssetManager assetManager = RenatusApp.getAppContext().getAssets();
        InputStream in;
        OutputStream out;
        String path = Environment.getExternalStorageDirectory().getPath()+"/"+name;
        File fileToMove = new File(path);
        if (!fileToMove.exists())
        {
            in = assetManager.open(name);
            out = new FileOutputStream(fileToMove);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            return path;
        }
        else
        {
            return path;
        }
    }
    
    
    public long getCCUAppDownloadId()
    {
        return mCCUAppDownloadId;
    }
    
    
    public void setCCUAppDownloadId(long mCCUAppDownloadId)
    {
        this.mCCUAppDownloadId = mCCUAppDownloadId;
    }
    
    
    public long getHomeAppDownloadId()
    {
        return mHomeAppDownloadId;
    }
    
    
    public void setHomeAppDownloadId(long mHomeAppDownloadId)
    {
        this.mHomeAppDownloadId = mHomeAppDownloadId;
    }
    
    
    public void downloadInstalls()
    {
        reset();
        setCCUAppDownloadId(downloadFile(DOWNLOAD_BASE_URL+CCU_DOWNLOAD_FILE, CCU_APK_FILE_NAME));
        //setHomeAppDownloadId(downloadFile(DOWNLOAD_BASE_URL+HOME_DOWNLOAD_FILE, HOME_APK_FILE_NAME));
    }
    
    
    private void reset()
    {
        mCCUAppDownloadId = -1;
        mHomeAppDownloadId = -1;
    }
    
    
    private synchronized long downloadFile(String url, String apkFile)
    {
        DownloadManager manager =
                (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading software update");
        request.setTitle("Downloading Update");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), apkFile);
        if (file.exists())
        {
            file.delete();
        }
        request.setDestinationInExternalFilesDir(RenatusApp.getAppContext(), null, apkFile);
        long dowloadId = manager.enqueue(request);
        //if (RenatusApp.DEBUG)
        {
            Log.d("CCU_DOWNLOAD", "downloading file: "+dowloadId+","+url);
        }
        return dowloadId;
    }
    

    public void downloadHomeInstall(String sFileName){
        setHomeAppDownloadId(downloadFile(DOWNLOAD_BASE_URL+sFileName,HOME_APK_FILE_NAME));
    }
    public void downloadCCUInstall(String sFileName)
    {
        setCCUAppDownloadId(downloadFile(DOWNLOAD_BASE_URL+sFileName, CCU_APK_FILE_NAME));
    }
    
    
    public void install(Activity activity, boolean bInstallHomeApp, boolean bInstallCCUApp, boolean bSilent)
    {
        if (bInstallHomeApp && bInstallCCUApp)
        {
            invokeInstallerIntent(activity, mHomeAppDownloadId, HOMEAPP_AND_CCUAPP_INSTALL_CODE, bSilent);
        }
        else if (bInstallHomeApp)
        {
            invokeInstallerIntent(activity, mHomeAppDownloadId, HOMEAPP_INSTALL_CODE, bSilent);
        }
        else if (bInstallCCUApp)
        {
            Log.d("CCU_DOWNLOAD", "Install AppInstall===>>>");
            invokeInstallerIntent(activity, mCCUAppDownloadId, CCUAPP_INSTALL_CODE, bSilent);
        }
    }
    
    
    private void invokeInstallerIntent(Activity activity, long downloadId, int requestCode, boolean bSilent)
    {
        try
        {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (bSilent)
            {
                String sFilePath = RenatusApp.getAppContext().getExternalFilesDir(null).getPath()+"/"+CCU_APK_FILE_NAME ;
                if (!sFilePath.isEmpty())
                {
                    File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), CCU_APK_FILE_NAME);
                    final String[] commands = {"pm install -r -d "+file.getAbsolutePath()};

                    Log.d("CCU_DOWNLOAD", "Install AppInstall silent invokeInstallerIntent===>>>"+sFilePath);
                    RenatusApp.executeAsRoot(commands);
                }
            }
            else
            {
                Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                Uri uri = Uri.fromFile(new File(RenatusApp.getAppContext().getExternalFilesDir(null).getPath()+"/"+CCU_APK_FILE_NAME ));
                installIntent.setData(uri);
                Log.d("CCU_DOWNLOAD", "Install AppInstall invokeInstallerIntent not silently===>>>"+manager.getUriForDownloadedFile(downloadId).getPath()+","+uri.getPath());
                //installIntent.setData(manager.getUriForDownloadedFile(downloadId));
                installIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                installIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                activity.startActivityForResult(installIntent, requestCode);
            }
        }
        catch (ActivityNotFoundException e)
        {
            //if (RenatusApp.DEBUG)
            {
                Log.d("CCU_DOWNLOAD", "ActivityNotFoundException ".concat(e.getMessage()));
            }
        }catch (Exception e){

            Log.d("CCU_DOWNLOAD", "Exception ".concat(e.getMessage()));
        }
    }
    
    
    public boolean isNewHomeAppAvailable()
    {
        return checkForVersionAndNotify(mHomeAppDownloadId);
    }
    
    
    private boolean checkForVersionAndNotify(long downloadId)
    {
        try
        {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            String sFilePath = manager.getUriForDownloadedFile(downloadId).getLastPathSegment();

            PackageManager pm = RenatusApp.getAppContext().getPackageManager();
            PackageInfo pinew = pm.getPackageArchiveInfo(sFilePath, 0);
            if (pinew == null)
            {
                File file = new File(RenatusApp.getAppContext().getExternalFilesDir(null), CCU_APK_FILE_NAME);
                pinew = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            }

            if (pinew != null)
            {
                //if (RenatusApp.DEBUG)
                {
                    Log.d("CCU_DOWNLOAD", "New Version: "+sFilePath+" "+pinew.versionName+"."+
                                                  String.valueOf(pinew.versionCode));
                }
                PackageInfo pi = pm.getPackageInfo(pinew.packageName, 0);
                //if (RenatusApp.DEBUG)
                {
                    Log.d("CCU_DOWNLOAD", "Installed Version: "+pi.versionName+"."+
                                                  String.valueOf(pi.versionCode));
                }
                if (pinew.versionCode > pi.versionCode)
                {
                    //if (RenatusApp.DEBUG)
                    {
                        Log.d("CCU_DOWNLOAD", "*****New version available to install");
                    }
                    //	if (bNotify)
                    //		NotificationHandler.setNewInstallAvailable(true, pi.versionName, pinew.versionName);
                    return true;
                }
            }
        }
        catch (NameNotFoundException e)
        {
            //if (CCUApp.DEBUG)
            {
                Log.d("CCU_DOWNLOAD", "***exception Called*** ".concat(e.toString()));
            }
            return true;
        }
        catch (NullPointerException e)
        {
            //if (CCUApp.DEBUG)
            {
                Log.d("CCU_DOWNLOAD", "***exception Called*** ".concat(e.toString()));
            }
        }
        return false;
    }
    
    
    public boolean isNewCCUAppAvailable()
    {
        return checkForVersionAndNotify(mCCUAppDownloadId);
    }
    
    
    private boolean isSystemRoot()
    {
        ApplicationInfo applicationInfo = RenatusApp.getAppContext().getApplicationInfo();
        boolean isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
        return isSystemApp;
    }
    public int getHomeAppInstalledVersion() {
        PackageManager pm = RenatusApp.getAppContext().getPackageManager();
        List<ApplicationInfo> l = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        String name = "com.x75fahrenheit.home";
        for (ApplicationInfo ai : l) {
            if (ai.packageName.contains(name)){
                PackageInfo pinew = pm.getPackageArchiveInfo(ai.sourceDir,0);
                Log.d("CCU_HOME","home app info = "+ai.sourceDir+","+ai.packageName+","+pinew.versionCode+","+pinew.versionName);
                if(pinew != null) {
                    PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit().putInt("home_app_version", pinew.versionCode).commit();
                    return pinew.versionCode;
                }
            }
        }

        return -1;
    }
    
    int getDownloadedFileVersion(long downloadId)
    {
        try
        {
            DownloadManager manager =
                    (DownloadManager) RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
            String sFilePath = manager.getUriForDownloadedFile(downloadId).getPath();
            PackageManager pm = RenatusApp.getAppContext().getPackageManager();
            PackageInfo pinew = pm.getPackageArchiveInfo(sFilePath, 0);
            if (pinew != null)
            {
                return pinew.versionCode;
            }
        }
        catch (NullPointerException e)
        {
            //if (CCUApp.DEBUG)
            {
                Log.d("CCU_DOWNLOAD", "***exception Called*** ");
            }
        }
        return 1;
    }
}
