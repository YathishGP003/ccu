package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ToggleButton;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.renatus.registartion.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 11/8/18.
 */

public class DABFullyAHUProfile extends Fragment
{

    @BindView(R.id.analog1Min) Spinner analog1Min;
    @BindView(R.id.analog1Max) Spinner analog1Max;
    @BindView(R.id.analog2Min) Spinner analog2Min;
    @BindView(R.id.analog2Max) Spinner analog2Max;
    @BindView(R.id.analog3Min) Spinner analog3Min;
    @BindView(R.id.analog3Max) Spinner analog3Max;
    @BindView(R.id.analog4Min) Spinner analog4Min;
    @BindView(R.id.analog4Max) Spinner analog4Max;

    @BindView(R.id.toggleAnalog1) ToggleButton ahuAnalog1Tb;
    @BindView(R.id.toggleAnalog2) ToggleButton ahuAnalog2Tb;
    @BindView(R.id.toogleAnalog3) ToggleButton ahuAnalog3Tb;

    @BindView(R.id.toggleRelay3) ToggleButton relay3Tb;
    @BindView(R.id.toggleRelay7) ToggleButton relay7Tb;

    @BindView(R.id.relay7Spinner) Spinner relay7Spinner;

    @BindView(R.id.analog1Spinner) Spinner ahuAnalog1Test;
    @BindView(R.id.analog2Spinner) Spinner ahuAnalog2Test;
    @BindView(R.id.analog3Spinner) Spinner ahuAnalog3Test;

    @BindView(R.id.relay3Test) ToggleButton relay3Test;
    @BindView(R.id.relay7Test) ToggleButton relay7Test;

    Prefs prefs;
    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "DAB_FULLY_MODULATING";
    boolean isFromReg = false;
    public static DABFullyAHUProfile newInstance()
    {
        return new DABFullyAHUProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_dab_fully_ahu, container, false);
        ButterKnife.bind(this, rootView);
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {

        prefs = new Prefs(getContext().getApplicationContext());
        if (!(L.ccu().systemProfile instanceof VavStagedRtu))
        {
            //L.ccu().systemProfile = new DabStagedRtu();
            //SystemEquip.getInstance().updateSystemProfile(ProfileType.SYSTEM_DAB_STAGED_RTU);
        }



        if(isFromReg){
            mNext.setVisibility(View.VISIBLE);
        }
        else {
            mNext.setVisibility(View.GONE);
        }

        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                goTonext();
            }
        });

    }

    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        prefs.setBoolean("PROFILE_SETUP",true);
        prefs.setString("PROFILE",PROFILE);
        ((FreshRegistration)getActivity()).selectItem(18);
    }
}
