package a75f.io.renatus;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

class PermissionHandler {
    
    public static final int CCU_PERMISSION_REQUEST_ID = 1;
    
    public boolean hasAppPermissions(Activity activityContext) {
        List<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.addAll(getExternalStoragePermissions(activityContext));
        permissionsNeeded.addAll(getLocationPermissions(activityContext));
        permissionsNeeded.addAll(getMiscPermissions(activityContext));
        if (permissionsNeeded.isEmpty()) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activityContext,
                                              permissionsNeeded.toArray(new String[permissionsNeeded.size()]),
                                              CCU_PERMISSION_REQUEST_ID);
        }
        return false;
    }
    
    private List<String> getExternalStoragePermissions(Activity activityContext) {
        List<String> listPermissionsNeeded = new ArrayList<>();
        int readStoragePermission =
            ContextCompat.checkSelfPermission(activityContext, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        int writeStoragePermission =
            ContextCompat.checkSelfPermission(activityContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return listPermissionsNeeded;
    }
    
    private List<String> getLocationPermissions(Activity activityContext) {
        List<String> listPermissionsNeeded = new ArrayList<>();
        int coarseLocationPermission = ContextCompat.checkSelfPermission(activityContext,
                                                            Manifest.permission.ACCESS_COARSE_LOCATION);
        if (coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        int fineLocationPermission =
            ContextCompat.checkSelfPermission(activityContext, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        return listPermissionsNeeded;
    }
    
    private List<String> getMiscPermissions(Activity activityContext) {
        List<String> listPermissionsNeeded = new ArrayList<>();
        int permission = ContextCompat.checkSelfPermission(activityContext, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        
        //TODO : Current field installations are 1.533 which uses "pm install" command for OTA. (AppInstaller.java)
        // It will pop up the BLOCKING permission dialog after upgrade if RECORD_AUDIO is included.
        // As of 1.540 "pm install -g" will be used for OTA , which will silently grant permissions.
        // This can be uncommented once all devices in field are upgraded beyond 1.540.
        /*permission = ContextCompat.checkSelfPermission(activityContext, Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO);
        }*/
        return listPermissionsNeeded;
    }
}
