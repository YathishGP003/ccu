package a75f.io.renatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.registartion.FreshRegistration;
import a75f.io.renatus.util.Prefs;

public class SplashActivity extends Activity {
    public static final String TAG = SplashActivity.class.getSimpleName();
    Prefs prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        prefs = new Prefs(this);
        Log.i(TAG, "Splash");
        Log.i(TAG, "Waiting 5 seconds and navigating to the registered screen");
        
        Thread registrationThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(5000);
                    SplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            HashMap site = CCUHsApi.getInstance().read("site");
                            HashMap ccu = CCUHsApi.getInstance().read("ccu");
                            
                            if (site.size() == 0) {
                                System.out.println("No Site Synced navigate to Register");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                startActivity(i);
                                finish();
                            }/*else if (ccu.size() == 0) {
                                System.out.println("No CCU Synced navigate to Create");
                                //Intent i = new Intent(SplashActivity.this, RegisterGatherCCUDetails.class);
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", 4);
                                startActivity(i);
                                finish();
                            }*/else if(site.size() > 0 && !prefs.getBoolean("CCU_SETUP"))
                            {
                                System.out.println("CCU Setup is not completed");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", 4);
                                startActivity(i);
                                finish();
                            }else if(prefs.getBoolean("CCU_SETUP") && !prefs.getBoolean("PROFILE_SETUP"))
                            {
                                System.out.println("No Profile Synced navigate to Create Profile");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", 9);
                                startActivity(i);
                                finish();
                            }else if(prefs.getBoolean("PROFILE_SETUP") && !prefs.getBoolean("REGISTRATION"))
                            {
                                System.out.println("No Floor is Created");
                                Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", 18);
                                startActivity(i);
                                finish();
                            } else if(prefs.getBoolean("REGISTRATION"))
                            {
                               /* Intent i = new Intent(SplashActivity.this, FreshRegistration.class);
                                i.putExtra("viewpager_position", 20);
                                startActivity(i);
                                finish();*/

                                Intent i = new Intent(SplashActivity.this, RenatusLandingActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            }

                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    
        if (prefs.getString("SERVER_ENV").equals(""))
        {
            String[] envList = {"PROD", "QA", "DEV"};
            AlertDialog.Builder alertDlg = new AlertDialog.Builder(SplashActivity.this);
            alertDlg.setIcon(R.drawable.x75f_new);
            alertDlg.setTitle("Select Haystack Server");
            alertDlg.setSingleChoiceItems(envList, -1, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int item)
                {
                    prefs.setString("SERVER_ENV", envList[item]);
                    dialog.dismiss();
                    registrationThread.start();
                }
            });
            
            AlertDialog alert = alertDlg.create();
            alert.show();
        } else {
            registrationThread.start();
        }

        //DefaultSchedules.generateDefaultSchedule()
    }
}
