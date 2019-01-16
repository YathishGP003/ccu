package a75f.io.renatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import org.projecthaystack.HRef;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.DefaultSchedules;

public class SplashActivity extends Activity {
    public static final String TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        Log.i(TAG, "Splash");
        Log.i(TAG, "Waiting 5 seconds and navigating to the registered screen");
        new Thread() {
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
                                Intent i = new Intent(SplashActivity.this, RegisterGatherDetails.class);
                                startActivity(i);
                                finish();
                            } else if (ccu.size() == 0) {
                                System.out.println("No CCU Synced navigate to Create");
                                Intent i = new Intent(SplashActivity.this, RegisterGatherCCUDetails.class);
                                startActivity(i);
                                finish();
                            }
                            else
                            {
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
        }.start();



        //DefaultSchedules.generateDefaultSchedule()
    }
}
