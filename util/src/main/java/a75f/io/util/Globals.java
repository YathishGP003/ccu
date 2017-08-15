package a75f.io.util;

import android.content.Context;

import a75f.io.bo.building.CCUApplication;
import a75f.io.util.prefs.LocalStorage;

/**
 * Created by rmatt on 7/19/2017.
 */


/*
    This is used to keep track of global static associated with application context.
 */
public class Globals
{
	
	private static Globals globals;
	
	private Context        mApplicationContext;
	private CCUApplication mCCUApplication;
	
	
	private Globals()
	{
	}
	
	
	public static Globals getInstance()
	{
		if (globals == null)
		{
			globals = new Globals();
		}
		return globals;
	}
	
	
	public CCUApplication getCCUApplication()
	{
		if (mCCUApplication == null)
		{
			mCCUApplication = LocalStorage.getApplicationSettings();
		}
		return mCCUApplication;
	}
	
	
	public Context getApplicationContext()
	{
		return mApplicationContext;
	}
	
	
	public void setApplicationContext(Context mApplicationContext)
	{
		this.mApplicationContext = mApplicationContext;
	}
}
