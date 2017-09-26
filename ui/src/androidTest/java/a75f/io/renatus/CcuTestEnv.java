package a75f.io.renatus;

import android.content.Context;

/**
 * Created by samjithsadasivan on 9/21/17.
 */

public class CcuTestEnv
{
    private Context mContext;
    
    public CcuTestEnv(Context c){
        mContext = c;
    }
    
    
    public Context getContext() {
        return mContext;
    }
}
