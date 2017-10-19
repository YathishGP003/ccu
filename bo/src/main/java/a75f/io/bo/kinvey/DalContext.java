package a75f.io.bo.kinvey;

import android.content.Context;
import android.provider.Settings;

import com.kinvey.android.Client;

/**
 * Created by Yinten on 9/4/2017.
 */

public class DalContext
{
	private static DalContext mDalContext = null;
	private Client mSharedClient;
	
	
	private DalContext(Client client)
	{
		mSharedClient = client;
	}
	
	
	public static Client getSharedClient()
	{
		return mDalContext.mSharedClient;
	}
	
	
	public static void instantiate(Context context)
	{
		if (mDalContext == null)
		{
			mDalContext = new DalContext(new Client.Builder(context).build());
			mDalContext.mSharedClient.setUseDeltaCache(true);
			
			
		}
	}
	
	
	public static DalContext getInstance()
	{
		return mDalContext;
	}
	
	
	public String getKinveyId() {
		return Settings.Secure
					   .getString(mSharedClient.getContext().getContentResolver(), Settings.Secure
																				 .ANDROID_ID);
	}
	
}
