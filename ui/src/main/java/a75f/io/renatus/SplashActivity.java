package a75f.io.renatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

public class SplashActivity extends Activity
{
    public static final String TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        Log.i(TAG, "Splash");
        Log.i(TAG, "Waiting 5 seconds and navigating to the registered screen");
        new Thread()
        {
            public void run()
            {
                try {
                    Thread.sleep(5000);
                    SplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Intent i = new Intent(SplashActivity.this, RegisterGatherDetails.class);
                            startActivity(i);
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
