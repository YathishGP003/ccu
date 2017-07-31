package a75f.io.util;

import android.app.Application;

/**
 * Created by rmatt on 7/19/2017.
 */

public abstract class UtilityApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();
        Globals.getInstance().setApplicationContext(this);




    }

}
