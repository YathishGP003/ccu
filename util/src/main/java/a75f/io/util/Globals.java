package a75f.io.util;

import android.content.Context;

import com.kinvey.android.Client;

/**
 * Created by rmatt on 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals {

    private static Globals globals;

    private Globals() {
    }

    public static Globals getInstance() {
        if (globals == null) {
            globals = new Globals();

        }

        return globals;
    }






    private Client kinveyClient;



    private Context mApplicationContext;

    public Context getApplicationContext() {
        return mApplicationContext;
    }

    public void setApplicationContext(Context mApplicationContext) {
        this.mApplicationContext = mApplicationContext;
    }

    public void setKinveyClient() {
        this.kinveyClient = new Client.Builder(mApplicationContext).build();
    }

    public Client getKinveyClient() {
        return kinveyClient;
    }
}
