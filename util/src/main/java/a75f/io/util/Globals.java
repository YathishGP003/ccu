package a75f.io.util;

import android.content.Context;

import java.util.ArrayList;

import a75f.io.bo.Floor;
import a75f.io.bo.SmartNode;

/**
 * Created by rmatt on 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals {


    private static Globals globals;
    
    private Context mApplicationContext;
    private SmartNode mSmartNode;
    
    private Globals() {
    }

    public static Globals getInstance() {
        if (globals == null) {
            globals = new Globals();
        }
        return globals;
    }
    
    public SmartNode getSmartNode() {
        if (mSmartNode == null) {
            mSmartNode = new SmartNode();
        }
        return mSmartNode;
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }

    public void setApplicationContext(Context mApplicationContext) {
        this.mApplicationContext = mApplicationContext;
    }


}
