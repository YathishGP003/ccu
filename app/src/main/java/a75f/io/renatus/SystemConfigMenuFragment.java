package a75f.io.renatus;

import static a75f.io.renatus.UtilityApplication.context;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.renatus.profiles.vav.BypassConfigFragment;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.ProgressDialogUtils;
import butterknife.BindView;
import butterknife.ButterKnife;


public class SystemConfigMenuFragment extends Fragment {

    @BindView(R.id.icon_system_profile)
    ImageView iconSystemProfile;
    @BindView(R.id.icon_oao)
    ImageView iconOao;
    @BindView(R.id.icon_bypass)
    ImageView iconBypass;
    @BindView(R.id.icon_energy_meter)
    ImageView iconEnergyMeter;
    @BindView(R.id.icon_btu_meter)
    ImageView iconBtuMeter;


    @BindView(R.id.text_system_profile)
    TextView textSystemProfile;
    @BindView(R.id.text_oao)
    TextView textOao;
    @BindView(R.id.text_bypass)
    TextView textBypass;
    @BindView(R.id.text_energy_meter)
    TextView textEnergyMeter;
    @BindView(R.id.text_btu_meter)
    TextView textBtuMeter;

    @BindView(R.id.lil_system_profile)
    LinearLayout lilSystemProfile;
    @BindView(R.id.lil_oao)
    LinearLayout lilOao;
    @BindView(R.id.lil_bypass)
    LinearLayout lilBypass;
    @BindView(R.id.lil_energy_meter)
    LinearLayout liltEnergyMeter;
    @BindView(R.id.lil_btu_meter)
    LinearLayout lilBtuMeter;

    public static Handler SystemConfigMenuFragmentHandler;
    public static Handler SystemConfigNavigationHandler;

    int listSelectorBackground;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_menu_system_config, container, false);
        ButterKnife.bind(this, rootView);


        if (SystemConfigFragment.SystemConfigFragmentHandler != null) {
            SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(0);
        }
        listSelectorBackground = CCUUiUtil.getListSelectorBackground(getContext());
        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.white));
        iconOao.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconBypass.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconEnergyMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconBtuMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));

        textSystemProfile.setTextColor(getResources().getColor(R.color.white));
        textOao.setTextColor(getResources().getColor(R.color.black));
        textBypass.setTextColor(getResources().getColor(R.color.black));
        textEnergyMeter.setTextColor(getResources().getColor(R.color.black));
        textBtuMeter.setTextColor(getResources().getColor(R.color.black));

        lilSystemProfile.setBackgroundResource(listSelectorBackground);
        lilOao.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilBypass.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        liltEnergyMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilBtuMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        return rootView;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.lil_system_profile).setOnClickListener(view1 -> {

            if (SystemConfigFragment.SystemConfigFragmentHandler != null) {
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(0);
            }

        });

        view.findViewById(R.id.lil_oao).setOnClickListener(view1 -> {
            if (SystemConfigFragment.SystemConfigFragmentHandler != null) {
                if (L.ccu().oaoProfile != null) {

                    FragmentManager fm = getActivity().getSupportFragmentManager();

                    DialogOAOProfile oaoFragment = (DialogOAOProfile)fm.findFragmentByTag("OaoConfig");
                    boolean oaoFragmentOpen = oaoFragment != null && oaoFragment.isVisible();
                    if (fm.findFragmentByTag("BypassConfig") instanceof BypassConfigFragment) {
                        BypassConfigFragment bypassFragment = (BypassConfigFragment)fm.findFragmentByTag("BypassConfig");
                        boolean bypassFragmentOpen = bypassFragment != null && bypassFragment.isVisible();
                        boolean bypassHasUnsavedChanges = bypassFragment != null && bypassFragment.hasUnsavedChanges();
                        if (!(oaoFragmentOpen || (bypassFragmentOpen && bypassHasUnsavedChanges))) ProgressDialogUtils.showProgressDialog(getContext(), "Loading OAO Profile");
                    } else {
                        // If Bypass Damper has been unpaired, fragment will now be a BypassFragment instead. In this case, no need to check for unsaved changes.
                        ProgressDialogUtils.showProgressDialog(getContext(), "Loading OAO Profile");
                    }

                }
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(1);
            }

        });

        view.findViewById(R.id.lil_bypass).setOnClickListener(view1 -> {
            if (SystemConfigFragment.SystemConfigFragmentHandler != null) {
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(2);
            }

        });

        view.findViewById(R.id.lil_energy_meter).setOnClickListener(view1 -> {
            if (SystemConfigFragment.SystemConfigFragmentHandler != null) {
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(3);
            }

        });

        view.findViewById(R.id.lil_btu_meter).setOnClickListener(view1 -> {
            if (SystemConfigFragment.SystemConfigFragmentHandler != null) {
                SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(4);
            }

        });

        handleSystemProfileChange(L.ccu().systemProfile instanceof DefaultSystem);
        navigationHandler();
        menuHandler();

    }

    @SuppressLint("HandlerLeak")
    private void menuHandler() {
        SystemConfigMenuFragmentHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleSystemProfileChange(!(msg.what > 0));
            }
        };
    }

    public void handleSystemProfileChange(boolean isDefault) {
        if (isDefault) {
            lilOao.setVisibility(View.GONE);
            lilBypass.setVisibility(View.GONE);
        } else {
            lilOao.setVisibility(View.VISIBLE);
            lilBypass.setVisibility(View.VISIBLE);
        }
    }

    private void navigationHandler() {
        SystemConfigNavigationHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.white));
                        iconOao.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBypass.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconEnergyMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBtuMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));

                        textSystemProfile.setTextColor(getResources().getColor(R.color.white));
                        textOao.setTextColor(getResources().getColor(R.color.black));
                        textBypass.setTextColor(getResources().getColor(R.color.black));
                        textEnergyMeter.setTextColor(getResources().getColor(R.color.black));
                        textBtuMeter.setTextColor(getResources().getColor(R.color.black));

                        lilSystemProfile.setBackgroundResource(listSelectorBackground);
                        lilOao.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBypass.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        liltEnergyMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBtuMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                        break;

                    case 1:
                        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconOao.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.white));
                        iconBypass.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconEnergyMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBtuMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));

                        textSystemProfile.setTextColor(getResources().getColor(R.color.black));
                        textOao.setTextColor(getResources().getColor(R.color.white));
                        textBypass.setTextColor(getResources().getColor(R.color.black));
                        textEnergyMeter.setTextColor(getResources().getColor(R.color.black));
                        textBtuMeter.setTextColor(getResources().getColor(R.color.black));

                        lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilOao.setBackgroundResource(listSelectorBackground);
                        lilBypass.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        liltEnergyMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBtuMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                        break;

                    case 2:
                        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconOao.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBypass.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.white));
                        iconEnergyMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBtuMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));

                        textSystemProfile.setTextColor(getResources().getColor(R.color.black));
                        textOao.setTextColor(getResources().getColor(R.color.black));
                        textBypass.setTextColor(getResources().getColor(R.color.white));
                        textEnergyMeter.setTextColor(getResources().getColor(R.color.black));
                        textBtuMeter.setTextColor(getResources().getColor(R.color.black));

                        lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilOao.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBypass.setBackgroundResource(listSelectorBackground);
                        liltEnergyMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBtuMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                        break;

                    case 3:
                        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconOao.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBypass.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconEnergyMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.white));
                        iconBtuMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));

                        textSystemProfile.setTextColor(getResources().getColor(R.color.black));
                        textOao.setTextColor(getResources().getColor(R.color.black));
                        textBypass.setTextColor(getResources().getColor(R.color.black));
                        textEnergyMeter.setTextColor(getResources().getColor(R.color.white));
                        textBtuMeter.setTextColor(getResources().getColor(R.color.black));

                        lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        liltEnergyMeter.setBackgroundResource(listSelectorBackground);
                        lilBypass.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilOao.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBtuMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                        break;

                    case 4:
                        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconOao.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBypass.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconEnergyMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.black));
                        iconBtuMeter.setColorFilter(ContextCompat.getColor(getActivity(),
                                R.color.white));

                        textSystemProfile.setTextColor(getResources().getColor(R.color.black));
                        textOao.setTextColor(getResources().getColor(R.color.black));
                        textBypass.setTextColor(getResources().getColor(R.color.black));
                        textEnergyMeter.setTextColor(getResources().getColor(R.color.black));
                        textBtuMeter.setTextColor(getResources().getColor(R.color.white));

                        lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilBtuMeter.setBackgroundResource(listSelectorBackground);
                        lilBypass.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        lilOao.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        liltEnergyMeter.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                        break;

                    default:
                        break;
                }
            }
        };
    }

}
