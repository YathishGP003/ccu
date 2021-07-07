package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.BLE.FragmentDeviceScan;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by spoorthidev On 23/06/2021
 */

public class HyperStatSensePairScreen  extends BaseDialogFragment {


    public static final String ID = HyperStatSensePairScreen.class.getSimpleName();
    short        mNodeAddress;
    String       mRoomName;
    String       mFloorName;
    ProfileType       mProfileName;


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
        if (mProfileName == ProfileType.HYPERSTAT_SENSE) {
            if (L.isSimulation()) {
                showDialogFragment(
                        HyperStatSenseFragment.newInstance( mNodeAddress,
                                mRoomName,
                                mFloorName,
                                ProfileType.HYPERSTAT_SENSE),
                        HyperStatSenseFragment.ID
                );
            } else {
                FragmentDeviceScan fragmentDeviceScan = FragmentDeviceScan
                        .getInstance(mNodeAddress, mRoomName, mFloorName, NodeType.HYPER_STAT, ProfileType.HYPERSTAT_SENSE);
                showDialogFragment(fragmentDeviceScan, FragmentDeviceScan.ID);
            }
        }
    }


    @Override
    public String getIdString() {
        return ID;
    }

    public static HyperStatSensePairScreen newInstance(short meshAddress, String roomName, String floorName, ProfileType profileType){
        HyperStatSensePairScreen f = new HyperStatSensePairScreen();
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
        View view = inflater.inflate(R.layout.hyperstatsensepairinginstscr, container, false);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        mProfileName = ProfileType.valueOf(getArguments().getString(FragmentCommonBundleArgs.PROFILE_TYPE));
        ButterKnife.bind(this, view);
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
