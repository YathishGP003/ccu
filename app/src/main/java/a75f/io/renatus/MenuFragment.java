package a75f.io.renatus;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;
import butterknife.BindView;
import butterknife.ButterKnife;


public class MenuFragment extends Fragment {

    @BindView(R.id.icon_account)
    ImageView iconAccount;
    @BindView(R.id.icon_security)
    ImageView iconSecurity;
    @BindView(R.id.icon_wifi)
    ImageView iconWifi;
    @BindView(R.id.icon_installer)
    ImageView iconInstaller;
    @BindView(R.id.icon_system_profile)
    ImageView iconSystemProfile;
    @BindView(R.id.icon_modbus_config)
    ImageView iconModbusConfig;
    @BindView(R.id.icon_temporary_override)
    ImageView iconTemporaryOverride;
    @BindView(R.id.icon_about)
    ImageView iconAbout;

    @BindView(R.id.text_Account)
    TextView textAccount;
    @BindView(R.id.text_security)
    TextView textSecurity;
    @BindView(R.id.text_wifi)
    TextView textWifi;
    @BindView(R.id.text_installer)
    TextView textInstaller;
    @BindView(R.id.text_system_profile)
    TextView textSystemProfile;
    @BindView(R.id.text_modbus_config)
    TextView textModbusConfig;
    @BindView(R.id.text_temporary_override)
    TextView textTemporaryOverride;
    @BindView(R.id.text_about)
    TextView textAbout;

    @BindView(R.id.lil_account)
    LinearLayout lilAccount;
    @BindView(R.id.lil_security)
    LinearLayout lilSecurity;
    @BindView(R.id.lil_wifi)
    LinearLayout liltWifi;
    @BindView(R.id.lil_installer)
    LinearLayout lilInstaller;
    @BindView(R.id.lil_system_profile)
    LinearLayout lilSystemProfile;
    @BindView(R.id.lil_modbus_config)
    LinearLayout lilModbusConfig;
    @BindView(R.id.lil_temporary_override)
    LinearLayout lilTemporaryOverride;
    @BindView(R.id.lil_about)
    LinearLayout lilAbout;

    int listSelectorBackground;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_menu, container, false);
        ButterKnife.bind(this, rootView);


        if (SettingsFragment.SettingFragmentHandler != null) {
            SettingsFragment.SettingFragmentHandler.sendEmptyMessage(0);
        }
        listSelectorBackground = CCUUiUtil.getListSelectorBackground(getContext());
        iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.white));
        iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));
        iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                R.color.black));

        textAccount.setTextColor(getResources().getColor(R.color.white));
        textSecurity.setTextColor(getResources().getColor(R.color.black));
        textWifi.setTextColor(getResources().getColor(R.color.black));
        textInstaller.setTextColor(getResources().getColor(R.color.black));
        textSystemProfile.setTextColor(getResources().getColor(R.color.black));
        textModbusConfig.setTextColor(getResources().getColor(R.color.black));
        textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
        textAbout.setTextColor(getResources().getColor(R.color.black));

        lilAccount.setBackgroundResource(listSelectorBackground);
        lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        return rootView;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.lil_account).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(0);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.white));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.black));

            lilAccount.setBackgroundResource(listSelectorBackground);
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        });

        view.findViewById(R.id.lil_security).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(1);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.white));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.black));

            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSecurity.setBackgroundResource(listSelectorBackground);
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        });

        view.findViewById(R.id.lil_wifi).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(2);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.white));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.black));

            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundResource(listSelectorBackground);
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        });

        view.findViewById(R.id.lil_installer).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(3);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.white));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.black));

            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundResource(listSelectorBackground);
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        });


        view.findViewById(R.id.lil_system_profile).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(4);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.white));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.black));

            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundResource(listSelectorBackground);
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        });

        view.findViewById(R.id.lil_modbus_config).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(5);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.white));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.black));


            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundResource(listSelectorBackground);
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        });

        view.findViewById(R.id.lil_temporary_override).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(6);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.white));
            textAbout.setTextColor(getResources().getColor(R.color.black));

            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundResource(listSelectorBackground);
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        });

        view.findViewById(R.id.lil_about).setOnClickListener(view1 -> {
            if (SettingsFragment.SettingFragmentHandler != null) {
                SettingsFragment.SettingFragmentHandler.sendEmptyMessage(7);
            }

            iconAccount.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSecurity.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconWifi.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconInstaller.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconSystemProfile.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconModbusConfig.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconTemporaryOverride.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.black));
            iconAbout.setColorFilter(ContextCompat.getColor(getActivity(),
                    R.color.white));

            textAccount.setTextColor(getResources().getColor(R.color.black));
            textSecurity.setTextColor(getResources().getColor(R.color.black));
            textWifi.setTextColor(getResources().getColor(R.color.black));
            textInstaller.setTextColor(getResources().getColor(R.color.black));
            textSystemProfile.setTextColor(getResources().getColor(R.color.black));
            textModbusConfig.setTextColor(getResources().getColor(R.color.black));
            textTemporaryOverride.setTextColor(getResources().getColor(R.color.black));
            textAbout.setTextColor(getResources().getColor(R.color.white));


            lilAccount.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilAbout.setBackgroundResource(listSelectorBackground);
            lilSecurity.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            liltWifi.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilInstaller.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilSystemProfile.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilModbusConfig.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            lilTemporaryOverride.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        });


    }
}
