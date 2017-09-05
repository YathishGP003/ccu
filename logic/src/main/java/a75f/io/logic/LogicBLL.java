package a75f.io.logic;

import android.content.Context;

import a75f.io.dal.DalContext;

/**
 * Created by Yinten on 9/4/2017.
 */

public class LogicBLL
{
	
	public static void initializeKinvey(Context context)
	{
		DalContext.instantiate(context);
	}
}
