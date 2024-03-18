package a75f.io.renatus;

import static a75f.io.renatus.UtilityApplication.context;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.oao.OAOEquip;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.modbus.ModbusConfigView;
import a75f.io.renatus.modbus.util.ModbusLevel;
import a75f.io.renatus.profiles.vav.BypassConfigFragment;
import a75f.io.renatus.registration.CreateNewSite;
import a75f.io.renatus.registration.InstallerOptions;
import a75f.io.renatus.registration.Security;
import a75f.io.renatus.registration.WifiFragment;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mahesh isOn 17/7/19.
 */
public class SystemConfigFragment extends Fragment {

    //
    @BindView(R.id.sysConfigFlContent)
    FrameLayout flSysContent;

    public static SlidingPaneLayout slidingSysPane;
    public static final String ACTION_SYSCONFIG_SCREEN =
            "a75f.io.renatus.SystemConfigFragment";
    //
    Fragment fragment = null;
    Class fragmentClass;
    public static Handler SystemConfigFragmentHandler;
    private boolean isTransactionSafe;
    private boolean isTransactionPending;

    public SystemConfigFragment() {

    }

    public static SystemConfigFragment newInstance() {
        return new SystemConfigFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_system_config, container, false);
        ButterKnife.bind(this, rootView);

        return rootView;
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        slidingSysPane = view.findViewById(R.id.sysConfig_sliding_pane);
        slidingSysPane.closePane();
        slidingSysPane.setSliderFadeColor(getResources().getColor(android.R.color.transparent));

        fragmentClass = SystemProfileFragment.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.sysConfigFlContent, fragment);
        transaction.commit();

        navigationHandler();
    }

    @SuppressLint("HandlerLeak")

    /*
     * handle page selection using handler
     */
    private void navigationHandler() {
        SystemConfigFragmentHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0: {
                        if (!(fragment instanceof BypassConfigFragment)) {
                            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(0);
                            }
                            if (isTransactionSafe && !(fragment instanceof SystemProfileFragment)) {
                                fragmentClass = SystemProfileFragment.class;
                                try {
                                    fragment = (Fragment) fragmentClass.newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "SysProfileConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        } else {
                            ((BypassConfigFragment) fragment).tryNavigateAway(5);
                        }

                        break;
                    }
                    case 1: {
                        if (!(fragment instanceof BypassConfigFragment)) {
                            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(1);
                            }
                            if (L.ccu().oaoProfile != null) {
                                if (isTransactionSafe && !(fragment instanceof DialogOAOProfile)) {
                                    fragmentClass = DialogOAOProfile.class;
                                    try {
                                        short meshAddress = (short)L.ccu().oaoProfile.getNodeAddress();
                                        fragment = DialogOAOProfile.newInstance(meshAddress, "SYSTEM", "SYSTEM");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "OaoConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            } else {
                                if (isTransactionSafe && !(fragment instanceof OaoFragment)) {
                                    fragmentClass = OaoFragment.class;
                                    try {
                                        fragment = (Fragment) fragmentClass.newInstance();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "OaoConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                                ProgressDialogUtils.hideProgressDialog();
                            }
                        } else {
                            ((BypassConfigFragment) fragment).tryNavigateAway(6);
                        }
                        break;
                    }
                    case 2: {
                        if (!(fragment instanceof BypassConfigFragment)) {
                            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(2);
                            }
                            if (L.ccu().bypassDamperProfile != null) {
                                Equip bypassDamperEquip = L.ccu().bypassDamperProfile.getEquip();
                                if (isTransactionSafe && !(fragment instanceof BypassConfigFragment)) {
                                    fragmentClass = BypassConfigFragment.class;
                                    try {
                                        short meshAddress = Short.parseShort(bypassDamperEquip.getGroup());
                                        fragment = BypassConfigFragment.Companion.newInstance(meshAddress, "SYSTEM", "SYSTEM", NodeType.SMART_NODE, ProfileType.BYPASS_DAMPER);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "BypassConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            } else {
                                if (isTransactionSafe && !(fragment instanceof BypassFragment)) {
                                    fragmentClass = BypassFragment.class;
                                    try {
                                        fragment = (Fragment) fragmentClass.newInstance();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "BypassConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            }
                        }
                        break;
                    }
                    case 3: {
                        if (!(fragment instanceof BypassConfigFragment)) {
                            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(3);
                            }
                            if (isModbusEMRPaired()) {
                                if (isTransactionSafe && !(fragment instanceof ModbusConfigView && fragment.getArguments().get(FragmentCommonBundleArgs.PROFILE_TYPE).equals(ProfileType.MODBUS_EMR))) {
                                    fragmentClass = ModbusConfigView.class;
                                    try {
                                        fragment = ModbusConfigView.Companion.newInstance(getModbusEMRAddress(), "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR, ModbusLevel.SYSTEM,"emr");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "EmrConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            } else {
                                if (isTransactionSafe && !(fragment instanceof EnergyMeterFragment)) {
                                    fragmentClass = EnergyMeterFragment.class;
                                    try {
                                        fragment = (Fragment) fragmentClass.newInstance();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "EmrConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            }
                        } else {
                            ((BypassConfigFragment) fragment).tryNavigateAway(8);
                        }
                        break;
                    }
                    case 4: {
                        if (!(fragment instanceof BypassConfigFragment)) {
                            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(4);
                            }
                            if (isModbusBTUPaired()) {
                                if (isTransactionSafe && !(fragment instanceof ModbusConfigView && fragment.getArguments().get(FragmentCommonBundleArgs.PROFILE_TYPE).equals(ProfileType.MODBUS_BTU))) {
                                    fragmentClass = ModbusConfigView.class;
                                    try {
                                        fragment = ModbusConfigView.Companion.newInstance(getModbusBTUAddress(), "SYSTEM", "SYSTEM", ProfileType.MODBUS_BTU, ModbusLevel.SYSTEM,"btu");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "BtuConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            } else {
                                if (isTransactionSafe && !(fragment instanceof BtuMeterFragment)) {
                                    fragmentClass = BtuMeterFragment.class;
                                    try {
                                        fragment = (Fragment) fragmentClass.newInstance();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                                    transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                    transaction.replace(R.id.sysConfigFlContent, fragment, "BtuConfig");
                                    transaction.commit();
                                } else {
                                    isTransactionPending = true;
                                }
                            }
                        } else {
                            ((BypassConfigFragment) fragment).tryNavigateAway(9);
                        }
                        break;
                    }
                    case 5: {
                        if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                            SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(0);
                        }
                        if (isTransactionSafe && !(fragment instanceof SystemProfileFragment)) {
                            fragmentClass = SystemProfileFragment.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.sysConfigFlContent, fragment, "SysProfileConfig");
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 6: {
                        if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                            SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(1);
                        }
                        if (L.ccu().oaoProfile != null) {
                            if (isTransactionSafe) {
                                fragmentClass = DialogOAOProfile.class;
                                try {
                                    short meshAddress = (short) L.ccu().oaoProfile.getNodeAddress();
                                    fragment = DialogOAOProfile.newInstance(meshAddress, "SYSTEM", "SYSTEM");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "OaoConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        } else {
                            if (isTransactionSafe && !(fragment instanceof OaoFragment)) {
                                fragmentClass = OaoFragment.class;
                                try {
                                    fragment = (Fragment) fragmentClass.newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "OaoConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        }
                        break;
                    }
                    case 7: {
                        if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                            SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(2);
                        }
                        if (L.ccu().bypassDamperProfile != null) {
                            Equip bypassDamperEquip = L.ccu().bypassDamperProfile.getEquip();
                            if (isTransactionSafe) {
                                fragmentClass = BypassConfigFragment.class;
                                try {
                                    short meshAddress = Short.parseShort(bypassDamperEquip.getGroup());
                                    fragment = BypassConfigFragment.Companion.newInstance(meshAddress, "SYSTEM", "SYSTEM", NodeType.SMART_NODE, ProfileType.BYPASS_DAMPER);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "BypassConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        } else {
                            if (isTransactionSafe && !(fragment instanceof BypassFragment)) {
                                fragmentClass = BypassFragment.class;
                                try {
                                    fragment = (Fragment) fragmentClass.newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "BypassConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        }
                        break;
                    }
                    case 8: {
                        if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                            SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(3);
                        }
                        if (isModbusEMRPaired()) {
                            if (isTransactionSafe && !(fragment instanceof ModbusConfigView && fragment.getArguments().get(FragmentCommonBundleArgs.PROFILE_TYPE).equals(ProfileType.MODBUS_EMR))) {
                                fragmentClass = ModbusConfigView.class;
                                try {
                                    fragment = ModbusConfigView.Companion.newInstance(getModbusEMRAddress(), "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR, ModbusLevel.SYSTEM,"emr");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "EmrConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        } else {
                            if (isTransactionSafe && !(fragment instanceof EnergyMeterFragment)) {
                                fragmentClass = EnergyMeterFragment.class;
                                try {
                                    fragment = (Fragment) fragmentClass.newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "EmrConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        }
                        break;
                    }
                    case 9: {
                        if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                            SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(4);
                        }
                        if (isModbusBTUPaired()) {
                            if (isTransactionSafe && !(fragment instanceof ModbusConfigView && fragment.getArguments().get(FragmentCommonBundleArgs.PROFILE_TYPE).equals(ProfileType.MODBUS_BTU))) {
                                fragmentClass = ModbusConfigView.class;
                                try {
                                    fragment = ModbusConfigView.Companion.newInstance(getModbusBTUAddress(), "SYSTEM", "SYSTEM", ProfileType.MODBUS_BTU, ModbusLevel.SYSTEM, "btu");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "BtuConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        } else {
                            if (isTransactionSafe && !(fragment instanceof BtuMeterFragment)) {
                                fragmentClass = BtuMeterFragment.class;
                                try {
                                    fragment = (Fragment) fragmentClass.newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                                transaction.replace(R.id.sysConfigFlContent, fragment, "BtuConfig");
                                transaction.commit();
                            } else {
                                isTransactionPending = true;
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        };
    }

    private boolean isModbusEMRPaired(){
        return CCUHsApi.getInstance().readAllEntities("equip and modbus and emr and roomRef ==\"SYSTEM\"").size() > 0;
    }

    private short getModbusEMRAddress() {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and modbus and emr and roomRef ==\"SYSTEM\"");
        return Short.parseShort(equipMap.get("group").toString());
    }

    private boolean isModbusBTUPaired(){
        return CCUHsApi.getInstance().readAllEntities("equip and modbus and btu and roomRef ==\"SYSTEM\"").size() > 0;
    }

    private short getModbusBTUAddress() {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and modbus and btu and roomRef ==\"SYSTEM\"");
        return Short.parseShort(equipMap.get("group").toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        isTransactionSafe = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isTransactionSafe = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (fragment != null) {
            fragment.onStop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isTransactionSafe) {
            getActivity().sendBroadcast(new Intent(ACTION_SYSCONFIG_SCREEN));
        }
    }
}
