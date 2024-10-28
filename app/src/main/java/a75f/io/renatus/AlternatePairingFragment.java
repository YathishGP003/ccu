package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.hyperstat.ui.HyperStatFragment;
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvFragment;
import a75f.io.renatus.profiles.acb.AcbProfileConfigFragment;
import a75f.io.renatus.profiles.dab.DabProfileConfigFragment;
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuFragment;
import a75f.io.renatus.profiles.plc.PlcProfileConfigFragment;
import a75f.io.renatus.profiles.vav.BypassConfigFragment;
import a75f.io.renatus.profiles.vav.VavProfileConfigFragment;
import a75f.io.renatus.util.CCUUiUtil;
import butterknife.BindView;
import butterknife.ButterKnife;

public class AlternatePairingFragment extends BaseDialogFragment {
    public static final String ID = AlternatePairingFragment.class.getSimpleName();

    private static NodeType mNodeType;
    private static Short mPairingAddress;
    private static String mRoomName;
    private static String mFloorName;
    private final ProfileType mProfileType;

    @BindView(R.id.imageGoback)
    ImageView imageGoback;
    @BindView(R.id.pairinginstruct)
    ImageView imageView;
    @BindView(R.id.dynamicNodeAddress)
    TextView dynamicNodeAddress;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.pair_button)
    Button pairButton;
    @BindView(R.id.manualPairingLayout)
    RelativeLayout manualPairingLayout;

    public AlternatePairingFragment(NodeType nodeType, Short pairingAddress, String roomName,
                                    String floorName, ProfileType profileType) {
        mNodeType = nodeType;
        mPairingAddress = pairingAddress;
        mRoomName = roomName;
        mFloorName = floorName;
        mProfileType = profileType;
    }

    @SuppressLint("StaticFieldLeak") @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        if (mNodeType == NodeType.SMART_NODE) {
            title.setText(getText(R.string.title_pair_sn_manual));
            if(CCUUiUtil.isDaikinEnvironment(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_smartnode_daikin);
            } else if (CCUUiUtil.isCarrierThemeEnabled(requireContext())){
                imageView.setImageResource(R.drawable.manual_pairing_smartnode_carrier);
            } else {
                imageView.setImageResource(R.drawable.manual_pairing_smartnode);
            }
        }
        else if (mNodeType == NodeType.HELIO_NODE) {
            title.setText(getText(R.string.title_pair_hn_manual));
            if(CCUUiUtil.isDaikinEnvironment(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_helionode_daikin);
            } else if(CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                {
                    imageView.setImageResource(R.drawable.manual_pairing_helionode_carrier);
                }
            } else {
                imageView.setImageResource(R.drawable.manual_pairing_helionode);
            }
        }
        else if (mNodeType == NodeType.HYPER_STAT) {
            title.setText(R.string.title_pair_hs_manual);
            if (CCUUiUtil.isDaikinEnvironment(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_hyperstat_daikin);
            } else if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.alternate_pairing_hyperstat_carrier);
            } else {
                imageView.setImageResource(R.drawable.manual_pairing_hyperstat);
            }
        }
        else if (mNodeType == NodeType.HYPERSTATSPLIT) {
            title.setText(R.string.title_pair_hss_manual);
            if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.hyperstat_split_carrier_manual);
            } else {
                imageView.setImageResource(R.drawable.hyperstat_split_pairing_steps_manual);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);
            }
        }

        if(CCUUiUtil.isCarrierThemeEnabled(requireContext()) || mNodeType == NodeType.HYPERSTATSPLIT){
            // To update pairing address in carrier themed UI we need to add some extra margin
            // For Hyperstat profile we have added some extra margins as the image res didnt fit for SN and HS
            int leftMargin;
            int topMargin;

            if (mNodeType == NodeType.HYPER_STAT) {
                leftMargin = 507;
                topMargin = 295;
            }
            else if(mNodeType == NodeType.HYPERSTATSPLIT){
                leftMargin = 490;
                topMargin = 265;
            } else{
                leftMargin = 490;
                topMargin = 275;
            }

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                    dynamicNodeAddress.getLayoutParams();
            params.leftMargin = leftMargin;
            params.topMargin = topMargin;
            dynamicNodeAddress.setLayoutParams(params);

        }
        dynamicNodeAddress.setText(mPairingAddress.toString());
        imageGoback.setOnClickListener( v -> {
            removeDialogFragment(ID);
        });
        pairButton.setOnClickListener( v -> {
            openBLEPairing();
        });
    }

    private void openBLEPairing() {
        switch (mProfileType) {
            case SSE:
                showDialogFragment(FragmentSSEConfiguration
                        .newInstance(mPairingAddress, mRoomName, mNodeType,
                                mFloorName,mProfileType), FragmentSSEConfiguration.ID);
                break;
            case VAV_REHEAT:
            case VAV_SERIES_FAN:
            case VAV_PARALLEL_FAN:
                showDialogFragment(VavProfileConfigFragment.Companion
                        .newInstance(mPairingAddress, mRoomName, mFloorName, mNodeType,
                                mProfileType), VavProfileConfigFragment.Companion.getID());
                break;
            case VAV_ACB:
                showDialogFragment(AcbProfileConfigFragment.Companion
                        .newInstance(mPairingAddress, mRoomName, mFloorName, mNodeType,
                                mProfileType), AcbProfileConfigFragment.Companion.getID());
                break;
            case DAB:
                showDialogFragment(DabProfileConfigFragment.Companion
                        .newInstance(mPairingAddress, mRoomName, mFloorName, mNodeType,
                                mProfileType), DabProfileConfigFragment.Companion.getID());
                break;
            case DUAL_DUCT:
                showDialogFragment(FragmentDABDualDuctConfiguration
                        .newInstance(mPairingAddress, mRoomName, mNodeType, mFloorName,
                                mProfileType), FragmentDABDualDuctConfiguration.ID);
                break;
            case OAO:
                    showDialogFragment(DialogOAOProfile
                            .newInstance(mPairingAddress, "SYSTEM", "SYSTEM"),
                            DialogOAOProfile.ID);
                    break;
            case EMR:
                    showDialogFragment(FragmentEMRConfiguration
                            .newInstance(mPairingAddress, mRoomName, mNodeType, mFloorName),
                            FragmentEMRConfiguration.ID);
                    break;
            case PLC:
                showDialogFragment(PlcProfileConfigFragment.Companion
                            .newInstance(mPairingAddress, mRoomName, mFloorName, mNodeType, ProfileType.PLC),
                        PlcProfileConfigFragment.Companion.getID());
                break;
            case HYPERSTAT_MONITORING:
                showDialogFragment(HyperStatMonitoringFragment.newInstance(mPairingAddress,
                                mRoomName, mFloorName, ProfileType.HYPERSTAT_MONITORING),
                        HyperStatMonitoringFragment.ID);
                break;
            case HYPERSTAT_VRV:
                showDialogFragment(HyperStatVrvFragment.newInstance(mPairingAddress, mRoomName,
                        mFloorName), HyperStatMonitoringFragment.ID);
                break;
            case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                showDialogFragment(HyperStatFragment.newInstance(mPairingAddress, mRoomName, mFloorName,
                        mNodeType, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT), HyperStatFragment.ID);
                break;
            case HYPERSTAT_HEAT_PUMP_UNIT:
                showDialogFragment(HyperStatFragment.newInstance(mPairingAddress,mRoomName,mFloorName,
                                mNodeType,ProfileType.HYPERSTAT_HEAT_PUMP_UNIT),
                        HyperStatFragment.ID);
                break;
            case HYPERSTAT_TWO_PIPE_FCU:
                showDialogFragment(HyperStatFragment.newInstance(mPairingAddress,mRoomName,mFloorName,
                                mNodeType,ProfileType.HYPERSTAT_TWO_PIPE_FCU), HyperStatFragment.ID);
                break;
            case HYPERSTATSPLIT_CPU:
                showDialogFragment(HyperStatSplitCpuFragment.Companion.newInstance(mPairingAddress,mRoomName,mFloorName,
                                mNodeType,ProfileType.HYPERSTATSPLIT_CPU), HyperStatSplitCpuFragment.Companion.getID());
                break;
            case BYPASS_DAMPER:
                showDialogFragment(BypassConfigFragment.Companion.newInstance(mPairingAddress, mRoomName,
                        mFloorName, mNodeType, mProfileType) , BypassConfigFragment.Companion.getID());
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1165;
            int height = 720;
            dialog.getWindow().setLayout(width, height);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manual_pairing_layout, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        int width = 1165;
        int height = 762;
        getDialog().getWindow().setLayout(width, height);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public String getIdString() {
        return AlternatePairingFragment.class.getSimpleName();
    }
}