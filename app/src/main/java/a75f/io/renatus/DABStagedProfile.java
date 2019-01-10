package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/8/18.
 */

public class DABStagedProfile extends Fragment
{
    public static DABStagedProfile newInstance()
    {
        return new DABStagedProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_dabstagedrtu, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
    
        if (!(L.ccu().systemProfile instanceof VavStagedRtu))
        {
            //L.ccu().systemProfile = new DabStagedRtu();
            //SystemEquip.getInstance().updateSystemProfile(ProfileType.SYSTEM_DAB_STAGED_RTU);
        }
    }
}
