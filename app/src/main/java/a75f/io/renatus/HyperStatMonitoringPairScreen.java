package a75f.io.renatus;

import static a75f.io.logic.bo.building.NodeType.HYPER_STAT;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.BLE.FragmentDeviceScan;
import a75f.io.renatus.profiles.hyperstat.ui.HyperStatMonitoringFragment;
import a75f.io.renatus.util.CCUUiUtil;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by spoorthidev On 23/06/2021
 */

public class HyperStatMonitoringPairScreen extends BaseDialogFragment {


    public static final String ID = HyperStatMonitoringPairScreen.class.getSimpleName();
    short        mNodeAddress;
    String       mRoomName;
    String       mFloorName;
    ProfileType   mProfileName;

    @BindView(R.id.hyperStatMonitoringPairing)
    ImageView pairImage;

    @BindView(R.id.hyperstat_monitoring_pairing_main_layout)
    RelativeLayout hyperstatMonitoringPairingMainLayout;


    @BindView(R.id.imageGoback)
    ImageView imageGoback;

    @BindView(R.id.pairbutton)
    Button pairButton;

    @Optional
    @OnClick(R.id.imageGoback)
    void onGoBackButtonClick()
    {
        removeDialogFragment(ID);
    }

    @OnClick(R.id.pairbutton)
    void onPairButtonClick()
    {
        OnPair();
    }

    private void OnPair() {
        if (mProfileName == ProfileType.HYPERSTAT_MONITORING) {
            if (L.isSimulation()) {
                showDialogFragment(
                        HyperStatMonitoringFragment.Companion.newInstance( mNodeAddress,
                                mRoomName,
                                mFloorName,
                                HYPER_STAT,
                                ProfileType.HYPERSTAT_MONITORING),
                        HyperStatMonitoringFragment.Companion.getID()
                );
            } else {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                        .getInstance(mNodeAddress, mRoomName, mFloorName, HYPER_STAT, ProfileType.HYPERSTAT_MONITORING);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
    }


    @Override
    public String getIdString() {
        return ID;
    }

    public static HyperStatMonitoringPairScreen newInstance(short meshAddress, String roomName, String floorName, ProfileType profileType){
        HyperStatMonitoringPairScreen f = new HyperStatMonitoringPairScreen();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putString(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.name() );
        f.setArguments(bundle);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hyperstatmonitoringpairinginstscr, container, false);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mProfileName = ProfileType.valueOf(getArguments().getString(FragmentCommonBundleArgs.PROFILE_TYPE));
        ButterKnife.bind(this, view);
        pairImage = view.findViewById(R.id.hyperStatMonitoringPairing);
        if(CCUUiUtil.isDaikinEnvironment(requireContext())){
            pairImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.daikenhsspairscreen, null));
        } else if(CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
            pairImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pairing_steps_hs_carrier, null));
            pairImage = view.findViewById(R.id.hyperStatMonitoringPairing);
        } else if(CCUUiUtil.isDaikinEnvironment(getContext())) {
            pairImage.setImageDrawable(getResources().getDrawable(R.drawable.daikenhsspairscreen));
        } else if(CCUUiUtil.isAiroverseThemeEnabled(requireContext())) {
            pairImage.setImageDrawable(getResources().getDrawable(R.drawable.pairing_steps_hs_airoverse));
        } else{
            pairImage.setImageResource(R.drawable.pairing_steps_hs_75f);
            hyperstatMonitoringPairingMainLayout.setBackgroundResource(R.drawable.bg_logoscreen);
        }

        int topMargin;
        topMargin=20;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                    pairImage.getLayoutParams();
            params.topMargin = topMargin;
            pairImage.setLayoutParams(params);


        return view;
    }


    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
}
