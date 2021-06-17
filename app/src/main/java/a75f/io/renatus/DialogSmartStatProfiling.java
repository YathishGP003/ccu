package a75f.io.renatus;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.CCUUiUtil;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



/**
 * Created by ryant on 9/27/2017.
 */

public class DialogSmartStatProfiling extends BaseDialogFragment
{
    public static final String ID = DialogSmartStatProfiling.class.getSimpleName();
    short        mNodeAddress;

    String mRoomName;
    String mFloorName;
    /*@BindView(R.id.smartstatModules)
    RadioGroup radioGroup;
    @BindView(R.id.conPackageUnit)
    RadioButton conPackageUnit;
    @BindView(R.id.commPackageUnit)
    RadioButton commPackageUnit;
    @BindView(R.id.hpu)
    RadioButton hpu;
    @BindView(R.id.fancoil_2)
    RadioButton fancoil_2;
    @BindView(R.id.fancoil_4)
    RadioButton fancoil_4;*/

    @BindView(R.id.rl_cpu)
    RelativeLayout rlCPU;
    @BindView(R.id.rl_commercial)
    RelativeLayout rlCommerical;
    @BindView(R.id.rl_hpu)
    RelativeLayout rlHPU;
    @BindView(R.id.rl_2pipe)
    RelativeLayout rl2Pipe;
    @BindView(R.id.rl_4pipe)
    RelativeLayout rl4Pipe;

    @Nullable
    @BindView(R.id.imageGoback)
    ImageView imageGoback;

    @OnClick(R.id.imageGoback)
    void onGoBackClick()
    {
        removeDialogFragment(ID);
    }


    @OnClick(R.id.rl_cpu)
    void onCPUClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
    }
    @OnClick(R.id.rl_commercial)
    void onCommercialClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_COMMERCIAL_PACK_UNIT, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
    }
    @OnClick(R.id.rl_hpu)
    void onHPUClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_HEAT_PUMP_UNIT, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
    }
    @OnClick(R.id.rl_2pipe)
    void on2PipeClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_TWO_PIPE_FCU, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
    }
    @OnClick(R.id.rl_4pipe)
    void on4PipeClick()
    {
        showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_FOUR_PIPE_FCU, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
    }



  /*  @OnClick(R.id.cancel_button)
    void onCancelButtonClick()
    {
        removeDialogFragment(ID);
    }

    @OnClick(R.id.pair_button)
    void onPairButtonClick()
    {
        openBLEPairingInstructions();
    }*/

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.smartstat_module_selection, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mNodeAddress = getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
        mRoomName = getArguments().getString(FragmentCommonBundleArgs.ARG_NAME);
        mFloorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);

        return view;
    }

  /*  private void openBLEPairingInstructions()
    {
        if (conPackageUnit.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);

        }
        else if (commPackageUnit.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_COMMERCIAL_PACK_UNIT, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
        }
        else if (hpu.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_HEAT_PUMP_UNIT, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
        }
        else if (fancoil_2.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_TWO_PIPE_FCU, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
        }
        else if (fancoil_4.isChecked())
        {
            showDialogFragment(FragmentBLEInstructionScreen.getInstance(mNodeAddress, mRoomName, mFloorName, ProfileType.SMARTSTAT_FOUR_PIPE_FCU, NodeType.SMART_STAT), FragmentBLEInstructionScreen.ID);
        }
    }*/

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

    private void setTitle() {
        Dialog dialog = getDialog();

        if (dialog == null) {
            return;
        }
        dialog.setTitle("Select Module Type");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
        }
        int titleDividerId = getContext().getResources()
                .getIdentifier("titleDivider", "id", "android");

        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                    .getColor(R.color.transparent));
        }
        //conPackageUnit.setChecked(true);
    }
    public static DialogSmartStatProfiling newInstance(short meshAddress, String roomName, String floorName)
    {

        DialogSmartStatProfiling f = new DialogSmartStatProfiling();
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
