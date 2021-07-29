package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.ProfileType;

import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;


public class FragmentBPOSTypeSelection extends BaseDialogFragment {

    public static final String ID = FragmentBPOSTypeSelection.class.getSimpleName();

   @BindView(R.id.rl_bpostempinf)
    View tempinfo;
    Zone mZone;

    short        mNodeAddress;

    String       mRoomName;
    String       mFloorName;
    Boolean      misPaired;


    @Override
    public String getIdString() {
        return ID;
    }


    public static FragmentBPOSTypeSelection newInstance(short meshAddress, String roomName, String floorName,boolean isPaired){
        FragmentBPOSTypeSelection f = new FragmentBPOSTypeSelection();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        bundle.putBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED, isPaired);
        f.setArguments(bundle);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bpos_typeselection, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        misPaired = getArguments().getBoolean(FragmentCommonBundleArgs.ALREADY_PAIRED);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.rl_bpostempinf)
    public void Onclicktempinf() {
        showDialogFragment(FragmentBPOSTempInfConfiguration.newInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.BPOS),
                FragmentBPOSTempInfConfiguration.ID);
    }

    @Optional
    @OnClick(R.id.imageGoback)
    void onGoBackButtonClick()
    {
        removeDialogFragment(ID);
    }

    @Override
    public void onStart() {
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
