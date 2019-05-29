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

public class DABStagedRtuWithVfdProfile extends Fragment
{
    @BindView(R.id.toggleRelay1) ToggleButton relay1Tb;
    @BindView(R.id.toggleRelay2) ToggleButton relay2Tb;
    @BindView(R.id.toggleRelay3) ToggleButton relay3Tb;
    @BindView(R.id.toggleRelay4) ToggleButton relay4Tb;
    @BindView(R.id.toggleRelay5) ToggleButton relay5Tb;
    @BindView(R.id.toggleRelay6) ToggleButton relay6Tb;
    @BindView(R.id.toggleRelay7) ToggleButton relay7Tb;
    @BindView(R.id.toggleAnalog2) ToggleButton analog2Tb;


    @BindView(R.id.relay1Spinner) Spinner relay1Spinner;
    @BindView(R.id.relay2Spinner)Spinner relay2Spinner;
    @BindView(R.id.relay3Spinner)Spinner relay3Spinner;
    @BindView(R.id.relay4Spinner)Spinner relay4Spinner;
    @BindView(R.id.relay5Spinner)Spinner relay5Spinner;
    @BindView(R.id.relay6Spinner)Spinner relay6Spinner;
    @BindView(R.id.relay7Spinner)Spinner relay7Spinner;
    @BindView(R.id.fanspeedSpinner)Spinner analog2TestSpinner;

    @BindView(R.id.analog2Economizer) Spinner analog2Economizer;
    @BindView(R.id.analog2Recirculate) Spinner analog2Recirculate;
    @BindView(R.id.analog2CoolStage1) Spinner analog2CoolStage1;
    @BindView(R.id.analog2CoolStage2) Spinner analog2CoolStage2;
    @BindView(R.id.analog2CoolStage3) Spinner analog2CoolStage3;
    @BindView(R.id.analog2CoolStage4) Spinner analog2CoolStage4;
    @BindView(R.id.analog2CoolStage5) Spinner analog2CoolStage5;
    @BindView(R.id.analog2HeatStage1) Spinner analog2HeatStage1;
    @BindView(R.id.analog2HeatStage2) Spinner analog2HeatStage2;
    @BindView(R.id.analog2HeatStage3) Spinner analog2HeatStage3;
    @BindView(R.id.analog2HeatStage4) Spinner analog2HeatStage4;
    @BindView(R.id.analog2HeatStage5) Spinner analog2HeatStage5;

    @BindView(R.id.relay1Test) ToggleButton relay1Test;
    @BindView(R.id.relay2Test) ToggleButton relay2Test;
    @BindView(R.id.relay3Test) ToggleButton relay3Test;
    @BindView(R.id.relay4Test) ToggleButton relay4Test;
    @BindView(R.id.relay5Test) ToggleButton relay5Test;
    @BindView(R.id.relay6Test) ToggleButton relay6Test;
    @BindView(R.id.relay7Test) ToggleButton relay7Test;


    Prefs prefs;
    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "DAB_STAGED_RTU_VFD";
    boolean isFromReg = false;

    public static DABStagedRtuWithVfdProfile newInstance()
    {
        return new DABStagedRtuWithVfdProfile();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_dabstagedvfd, container, false);
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
