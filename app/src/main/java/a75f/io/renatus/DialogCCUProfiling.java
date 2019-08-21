package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import a75f.io.api.haystack.HSUtil;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.lights.LightProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by ryant on 9/27/2017.
 */

public class DialogCCUProfiling extends BaseDialogFragment
{
    
    public static final String ID = DialogCCUProfiling.class.getSimpleName();
    
    Zone         mZone;
    LightProfile mLightProfile;
    
    short        mNodeAddress;
    
    String mRoomName;
    String mFloorName;
    
    @BindView(R.id.default_text_view)
    TextView    defaultTextView;

    @Nullable
    @BindView(R.id.rl_tempinf)
    RelativeLayout rlTempInf;


    @Nullable
    @BindView(R.id.imageViewArrow)
    ImageView imageViewArrow;

    @Nullable
    @BindView(R.id.imageGoback)
    ImageView imageGoback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        defaultTextView.setText(HSUtil.getDis(mRoomName));
    }

    @Optional
    @OnClick(R.id.imageGoback)
    void onGoBackButtonClick()
    {
        removeDialogFragment(ID);
    }


    @Optional
    @OnClick(R.id.rl_tempinf)
    void onTempInfluenceOnClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.TEMP_INFLUENCE, NodeType.CONTROL_MOTE), FragmentBLEInstructionScreen.ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.ccu_module_selection, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        //mZone = L.findZoneByName(mFloorName, mRoomName);
        //mLightProfile = (LightProfile) mZone.findProfile(ProfileType.LIGHT);
        
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
        //setTitle();
    }
    
    public static DialogCCUProfiling newInstance(short meshAddress, String roomName, String floorName)
    {
        DialogCCUProfiling f = new DialogCCUProfiling();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress);
        bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName);
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName);
        f.setArguments(bundle);
        return f;
    }
    @Override
    public String getIdString()
    {
        return ID;
    }
}
