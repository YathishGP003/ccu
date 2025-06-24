package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.modbus.ModbusConfigView;
import a75f.io.renatus.modbus.util.ModbusLevel;
import a75f.io.renatus.profiles.oao.OAOProfileFragment;
import a75f.io.renatus.profiles.system.DabModulatingRtuFragment;
import a75f.io.renatus.profiles.system.DabStagedRtuFragment;
import a75f.io.renatus.profiles.system.DabStagedVfdRtuFragment;
import a75f.io.renatus.profiles.system.VavModulatingRtuFragment;
import a75f.io.renatus.profiles.system.VavStagedRtuFragment;
import a75f.io.renatus.profiles.system.VavStagedVfdRtuFragment;
import a75f.io.renatus.profiles.system.advancedahu.dab.DabAdvancedHybridAhuFragment;
import a75f.io.renatus.profiles.system.advancedahu.vav.VavAdvancedHybridAhuFragment;
import a75f.io.renatus.profiles.system.externalahu.ExternalAhuFragment;
import a75f.io.renatus.profiles.vav.BypassConfigFragment;
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
        slidingSysPane.setSliderFadeColor(getResources().getColor(android.R.color.transparent));
        slidingSysPane.setBackground(getResources().getDrawable(R.drawable.border_background_collapsed));
        slidingSysPane.setPanelSlideListener(new SlidingPanelListener(slidingSysPane));

        fragmentClass = SystemProfileFragment.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        replaceFragment("");
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
                        systemConfigPage();
                        break;
                    }
                    case 1: {
                        if (fragment instanceof SystemProfileFragment) {
                            verifyChanges(getContext(), 1); break;
                        }
                        oaoPage();
                        break;

                    }
                    case 2: {
                        if (fragment instanceof SystemProfileFragment) {
                            verifyChanges(getContext(), 2); break;
                        }
                        bypassDamperPage(); break;
                    }
                    case 3: {
                        if (fragment instanceof SystemProfileFragment) {
                            verifyChanges(getContext(), 3); break;
                        }
                        modbusEnergyMeterPage(); break;
                    }
                    case 4: {
                        if (fragment instanceof SystemProfileFragment) {
                            verifyChanges(getContext(), 4); break;
                        }
                        btuMeterPage(); break;
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
                            replaceFragment("SysProfileConfig");
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
                                fragmentClass = OAOProfileFragment.class;
                                try {
                                    short meshAddress =Short.parseShort(L.ccu().oaoProfile.getNodeAddress()+"");
                                    fragment = OAOProfileFragment.Companion.newInstance(meshAddress,
                                            "SYSTEM", "SYSTEM", NodeType.SMART_NODE,
                                            ProfileType.OAO);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                replaceFragment("OaoConfig");
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
                                replaceFragment("OaoConfig");
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
                                replaceFragment("BypassConfig");
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
                                replaceFragment("BypassConfig");
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
                                replaceFragment("EmrConfig");
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
                                replaceFragment("EmrConfig");
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
                                replaceFragment("BtuConfig");
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
                                replaceFragment("BtuConfig");
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

    private void replaceFragment(String tag) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.sysConfigFlContent, fragment, tag);
        transaction.commit();
    }

    private void btuMeterPage() {
        if (!(fragment instanceof BypassConfigFragment)) {
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
                    replaceFragment("BtuConfig");
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
                    replaceFragment("BtuConfig");
                } else {
                    isTransactionPending = true;
                }
            }
        } else {
            ((BypassConfigFragment) fragment).tryNavigateAway(9);
        }
    }

    private void modbusEnergyMeterPage() {
        if (!(fragment instanceof BypassConfigFragment)) {
            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(3);
            }
            if (isModbusEMRPaired()) {
                if (isTransactionSafe && !(fragment instanceof ModbusConfigView && fragment.getArguments().get(FragmentCommonBundleArgs.PROFILE_TYPE).equals(ProfileType.MODBUS_EMR))) {
                    fragmentClass = ModbusConfigView.class;
                    try {
                        fragment = ModbusConfigView.Companion.newInstance(getModbusEMRAddress(), "SYSTEM", "SYSTEM", ProfileType.MODBUS_EMR, ModbusLevel.SYSTEM, "emr");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    replaceFragment("EmrConfig");
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
                    replaceFragment("EmrConfig");
                } else {
                    isTransactionPending = true;
                }
            }
        } else {
            ((BypassConfigFragment) fragment).tryNavigateAway(8);
        }
    }

    private void bypassDamperPage() {
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
                    replaceFragment("BypassConfig");
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
                    replaceFragment("BypassConfig");
                } else {
                    isTransactionPending = true;
                }
            }
        }
    }

    private void oaoPage() {
        if (!(fragment instanceof BypassConfigFragment)) {
            if (SystemConfigMenuFragment.SystemConfigNavigationHandler != null) {
                SystemConfigMenuFragment.SystemConfigNavigationHandler.sendEmptyMessage(1);
            }
            if (L.ccu().oaoProfile != null) {
                if (isTransactionSafe && !(fragment instanceof OAOProfileFragment)) {
                    fragmentClass = OAOProfileFragment.class;

                    try {
                        short meshAddress = Short.parseShort(L.ccu().oaoProfile.getNodeAddress() + "");
                        fragment = OAOProfileFragment.Companion.newInstance(meshAddress, "SYSTEM", "SYSTEM", NodeType.SMART_NODE, ProfileType.OAO);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    replaceFragment("OaoConfig");
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
                    replaceFragment("OaoConfig");
                } else {
                    isTransactionPending = true;
                }
                ProgressDialogUtils.hideProgressDialog();
            }
        } else {
            ((BypassConfigFragment) fragment).tryNavigateAway(6);
        }
    }

    private void systemConfigPage() {
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
                replaceFragment("SysProfileConfig");
            } else {
                isTransactionPending = true;
            }
        } else {
            ((BypassConfigFragment) fragment).tryNavigateAway(5);
        }
    }

    private boolean verifyChanges(Context context, int page) {
        for (Fragment fragment : getActivity().getSupportFragmentManager().getFragments()) {
            if (fragment instanceof VavStagedRtuFragment) {
                if (((VavStagedRtuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }
            if (fragment instanceof VavStagedVfdRtuFragment) {
                if (((VavStagedVfdRtuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }
            if (fragment instanceof VavModulatingRtuFragment) {
                if (((VavModulatingRtuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof DabStagedRtuFragment) {
                if (((DabStagedRtuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof DabStagedVfdRtuFragment) {
                if (((DabStagedVfdRtuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof DabModulatingRtuFragment) {
                if (((DabModulatingRtuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof VavAdvancedHybridAhuFragment) {
                if (((VavAdvancedHybridAhuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof DabAdvancedHybridAhuFragment) {
                if (((DabAdvancedHybridAhuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof ExternalAhuFragment) {
                if (((ExternalAhuFragment) fragment).hasUnsavedChanged()) {
                    return showConfirmationDialog(context, page);
                } else {
                    goTo(page);
                }
            }

            if (fragment instanceof VavHybridRtuProfile) {
                goTo(page);
            }

            if (fragment instanceof DABHybridAhuProfile) {
                goTo(page);
            }

            if (fragment instanceof DefaultSystemProfile) {
                goTo(page);
            }
        }
        return true;
    }

    private void goTo(int page) {
        if (page == 1) oaoPage();
        else if (page == 2) bypassDamperPage();
        else if (page == 3) modbusEnergyMeterPage();
        else if (page == 4) btuMeterPage();
    }


    boolean state = false;

    private boolean showConfirmationDialog(Context context, int page) {
        new AlertDialog.Builder(context)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to discard them and proceed?")
                .setPositiveButton("Proceed", (dialog, which) -> {
                    if (page == 1) oaoPage();
                    else if (page == 2) bypassDamperPage();
                    else if (page == 3) modbusEnergyMeterPage();
                    else if (page == 4) btuMeterPage();
                })
                .setNegativeButton("Stay", (dialog, which) -> {
                    state = false;
                })
                .setCancelable(false)
                .show();
        return state;
    }

    private boolean isModbusEMRPaired(){
        return !CCUHsApi.getInstance().readAllEntities("equip and modbus and emr" +
                " and (roomRef ==\"SYSTEM\" or roomRef ==\"@SYSTEM\")").isEmpty();
    }

    private short getModbusEMRAddress() {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and modbus" +
                " and emr and (roomRef ==\"SYSTEM\" or roomRef ==\"@SYSTEM\")");
        return Short.parseShort(equipMap.get("group").toString());
    }

    private boolean isModbusBTUPaired(){
        return !CCUHsApi.getInstance().readAllEntities("equip and modbus and btu" +
                " and (roomRef ==\"SYSTEM\" or roomRef ==\"@SYSTEM\")").isEmpty();
    }

    private short getModbusBTUAddress() {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and modbus and btu and (roomRef ==\"SYSTEM\" or roomRef ==\"@SYSTEM\")");
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
