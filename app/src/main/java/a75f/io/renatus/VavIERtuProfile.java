package a75f.io.renatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/6/18.
 */

public class VavIERtuProfile extends Fragment
{
    
    
    
    public static VavAnalogRtuProfile newInstance()
    {
        return new VavAnalogRtuProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_rtuie, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        if (!(L.ccu().systemProfile instanceof VavIERtu))
        {
            L.ccu().systemProfile = new VavIERtu();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground( final Void ... params ) {
                    return null;
                }
                @Override
                protected void onPostExecute( final Void result ) {
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        }
    }
}
