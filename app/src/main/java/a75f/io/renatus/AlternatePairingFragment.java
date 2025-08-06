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
import a75f.io.renatus.hyperstat.vrv.HyperStatVrvFragment;
import a75f.io.renatus.profiles.acb.AcbProfileConfigFragment;
import a75f.io.renatus.profiles.connectnode.ConnectNodeFragment;
import a75f.io.renatus.profiles.dab.DabProfileConfigFragment;
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatV2HpuFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatV2Pipe2Fragment;
import a75f.io.renatus.profiles.mystat.ui.MyStatCpuFragment;
import a75f.io.renatus.profiles.mystat.ui.MyStatHpuFragment;
import a75f.io.renatus.profiles.mystat.ui.MyStatPipe2Fragment;
import a75f.io.renatus.profiles.plc.PlcProfileConfigFragment;
import a75f.io.renatus.profiles.vav.BypassConfigFragment;
import a75f.io.renatus.profiles.oao.OAOProfileFragment;
import a75f.io.renatus.profiles.sse.SseProfileConfigFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatV2CpuFragment;
import a75f.io.renatus.profiles.hyperstatv2.ui.HyperStatMonitoringFragment;
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
                imageView.setImageResource(R.drawable.manual_pairing_sn_carrier);
            }
            else if (CCUUiUtil.isAiroverseThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_sn_airoverse);
            }
            else {
                imageView.setImageResource(R.drawable.manual_pairing_sn_75f);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);

            }
        }
        else if (mNodeType == NodeType.HELIO_NODE) {
            title.setText(getText(R.string.title_pair_hn_manual));
            if(CCUUiUtil.isDaikinEnvironment(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_helionode_daikin);
            } else if(CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_hn_carrier);
            } else if (CCUUiUtil.isAiroverseThemeEnabled(requireContext())){
                imageView.setImageResource(R.drawable.manual_pairing_hn_airoverse);
            } else {
                imageView.setImageResource(R.drawable.manual_pairing_hn_75f);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);
            }
        }
        else if (mNodeType == NodeType.HYPER_STAT) {
            title.setText(R.string.title_pair_hs_manual);
            if (CCUUiUtil.isDaikinEnvironment(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_hyperstat_daikin);
            } else if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_hs_carrier);
            }
            else if (CCUUiUtil.isAiroverseThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_hs_airoverse);
            } else {
                imageView.setImageResource(R.drawable.manual_pairing_hs_75f);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);
            }
        }
        else if (mNodeType == NodeType.HYPERSTATSPLIT) {
            title.setText(R.string.title_pair_hss_manual);
            if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_hss_carrier);
            }
            else if(CCUUiUtil.isAiroverseThemeEnabled(requireContext())){
                imageView.setImageResource(R.drawable.manual_pairing_hss_airoverse);
            }else {
                imageView.setImageResource(R.drawable.manual_pairing_hss_75f);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);
            }
        }
        else if (mNodeType == NodeType.MYSTAT) {
            title.setText(R.string.title_pair_ms_manual);
            if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_ms_carrier);
            } else if (CCUUiUtil.isAiroverseThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_ms_airoverse);
            }
            else {
                imageView.setImageResource(R.drawable.manual_pairing_ms_75f);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);
            }
        }
        else if (mNodeType == NodeType.CONNECTNODE) {
            title.setText(R.string.title_pair_cn_manual);
            if (CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_cn_carrier);
            } else if (CCUUiUtil.isAiroverseThemeEnabled(requireContext())) {
                imageView.setImageResource(R.drawable.manual_pairing_cn_airoverse);
            } else {
                imageView.setImageResource(R.drawable.manual_pairing_cn_75f);
                manualPairingLayout.setBackgroundResource(R.drawable.bg_logoscreen);
            }
        }

        if(CCUUiUtil.isCarrierThemeEnabled(requireContext())
                || mNodeType == NodeType.HYPERSTATSPLIT || mNodeType == NodeType.MYSTAT ||
                mNodeType == NodeType.HYPER_STAT || mNodeType == NodeType.CONNECTNODE)
        {
            // To update pairing address in carrier themed UI we need to add some extra margin
            // For Hyperstat profile we have added some extra margins as the image res didnt fit for SN and HS
            int leftMargin;
            int topMargin;

            if (mNodeType == NodeType.HYPER_STAT ) {
                leftMargin = 525;
                topMargin = 250;
            } else if (mNodeType == NodeType.MYSTAT || mNodeType == NodeType.CONNECTNODE) {
                leftMargin = 490;
                topMargin = 255;
            }
            else if(mNodeType == NodeType.HYPERSTATSPLIT){
                leftMargin = 490;
                topMargin = 255;
            } else {
                leftMargin = 490;
                topMargin = 275;
            }

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                    dynamicNodeAddress.getLayoutParams();
            params.leftMargin = leftMargin;
            params.topMargin = topMargin;
            dynamicNodeAddress.setLayoutParams(params);

        }

        if(CCUUiUtil.isAiroverseThemeEnabled(requireContext()) || CCUUiUtil.isCarrierThemeEnabled(requireContext()) ||
                mNodeType == NodeType.HYPERSTATSPLIT || mNodeType == NodeType.MYSTAT ||
                mNodeType == NodeType.HYPER_STAT || mNodeType == NodeType.CONNECTNODE){

            int topMargin;
            if (mNodeType == NodeType.HYPER_STAT ) {
                topMargin = 60;
            } else if (mNodeType == NodeType.MYSTAT )  {
                topMargin = 60;
            }
            else if (mNodeType == NodeType.CONNECTNODE){
                topMargin=70;

            }
            else if(mNodeType == NodeType.HYPERSTATSPLIT){

                topMargin = 60;
            } else {
                topMargin = 75;
            }

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                    imageView.getLayoutParams();
            params.topMargin = topMargin;
            imageView.setLayoutParams(params);
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
                showDialogFragment(SseProfileConfigFragment.Companion
                        .newInstance(mPairingAddress, mRoomName, mFloorName, mNodeType,
                                mProfileType), SseProfileConfigFragment.Companion.getID());
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
                showDialogFragment(OAOProfileFragment.Companion
                        .newInstance(mPairingAddress, mRoomName, mFloorName, mNodeType,
                                mProfileType), OAOProfileFragment.Companion.getID());
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
                showDialogFragment(HyperStatMonitoringFragment.Companion.newInstance(mPairingAddress,
                                mRoomName, mFloorName,mNodeType, ProfileType.HYPERSTAT_MONITORING),
                        HyperStatMonitoringFragment.Companion.getID());
                break;
            case HYPERSTAT_VRV:
                showDialogFragment(HyperStatVrvFragment.newInstance(mPairingAddress, mRoomName,
                        mFloorName), HyperStatVrvFragment.ID);
                break;
            case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                showDialogFragment(HyperStatV2CpuFragment.newInstance(mPairingAddress, mRoomName, mFloorName,
                        mNodeType, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT), HyperStatV2CpuFragment.ID);
                break;
            case HYPERSTAT_HEAT_PUMP_UNIT:
                showDialogFragment(HyperStatV2HpuFragment.newInstance(mPairingAddress,mRoomName,mFloorName,
                                mNodeType,ProfileType.HYPERSTAT_HEAT_PUMP_UNIT),
                        HyperStatV2HpuFragment.ID);
                break;
            case HYPERSTAT_TWO_PIPE_FCU:
                showDialogFragment(HyperStatV2Pipe2Fragment.newInstance(mPairingAddress,mRoomName,mFloorName,
                        mNodeType,ProfileType.HYPERSTAT_TWO_PIPE_FCU), HyperStatV2Pipe2Fragment.ID);
                break;
            case HYPERSTATSPLIT_CPU:
                showDialogFragment(HyperStatSplitCpuFragment.Companion.newInstance(mPairingAddress,mRoomName,mFloorName,
                        mNodeType,ProfileType.HYPERSTATSPLIT_CPU), HyperStatSplitCpuFragment.Companion.getID());
                break;
            case BYPASS_DAMPER:
                showDialogFragment(BypassConfigFragment.Companion.newInstance(mPairingAddress, mRoomName,
                        mFloorName, mNodeType, mProfileType) , BypassConfigFragment.Companion.getID());
                break;
            case MYSTAT_CPU:
                showDialogFragment(MyStatCpuFragment.Companion.newInstance(mPairingAddress, mRoomName,
                        mFloorName, mNodeType, mProfileType) , BypassConfigFragment.Companion.getID());
                break;
            case MYSTAT_PIPE2:
                showDialogFragment(MyStatPipe2Fragment.Companion.newInstance(mPairingAddress, mRoomName,
                        mFloorName, mNodeType, mProfileType) , BypassConfigFragment.Companion.getID());
                break;
            case MYSTAT_HPU:
                showDialogFragment(MyStatHpuFragment.Companion.newInstance(mPairingAddress, mRoomName,
                        mFloorName, mNodeType, mProfileType) , BypassConfigFragment.Companion.getID());
                break;
            case CONNECTNODE:
                showDialogFragment(ConnectNodeFragment.Companion.newInstance(mPairingAddress, mRoomName,
                        mFloorName, mNodeType, mProfileType), ConnectNodeFragment.Companion.getIdString());
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
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
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