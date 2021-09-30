package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import a75f.io.renatus.registration.CreateNewSite;
import a75f.io.renatus.registration.InstallerOptions;
import a75f.io.renatus.registration.Security;
import a75f.io.renatus.registration.WifiFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by mahesh isOn 17/7/19.
 */
public class SettingsFragment extends Fragment {

    //
    @BindView(R.id.flContent)
    FrameLayout flContent;

    public static SlidingPaneLayout slidingPane;
    public static final String ACTION_SETTING_SCREEN =
            "a75f.io.renatus.SettingsFragment";
    //
    Fragment fragment = null;
    Class fragmentClass;
    public static Handler SettingFragmentHandler;
    private boolean isTransactionSafe;
    private boolean isTransactionPending;

    public SettingsFragment() {

    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, rootView);

        return rootView;
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        slidingPane = view.findViewById(R.id.sliding_pane);
        slidingPane.setSliderFadeColor(getResources().getColor(android.R.color.transparent));

        fragmentClass = CreateNewSite.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.replace(R.id.flContent, fragment);
        transaction.commit();

        navigationHandler();
    }

    @SuppressLint("HandlerLeak")

    /*
     * handle page selection using handler
     */
    private void navigationHandler() {
        SettingFragmentHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0: {

                        if (isTransactionSafe && !(fragment instanceof CreateNewSite)) {
                            fragmentClass = CreateNewSite.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 1: {

                        if (isTransactionSafe && !(fragment instanceof Security)) {
                            fragmentClass = Security.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 2: {
                        if (isTransactionSafe && !(fragment instanceof WifiFragment)) {
                            fragmentClass = WifiFragment.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 3: {
                        if (isTransactionSafe && !(fragment instanceof InstallerOptions)) {
                            fragmentClass = InstallerOptions.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 4: {
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
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
    
                    case 5: {
                        if (isTransactionSafe && !(fragment instanceof ModbusConfigFragment)) {
                            fragmentClass = ModbusConfigFragment.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 6: {
                        if (isTransactionSafe && !(fragment instanceof TempOverrideFragment)) {
                            //TODO:
                           /* fragmentClass = TempOverrideFragment.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();*/
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 7:
                        if (isTransactionSafe && !(fragment instanceof AboutFragment)) {
                            fragmentClass = AboutFragment.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    default:
                        break;
                }
            }
        };
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
        if (isVisibleToUser) {
            getActivity().sendBroadcast(new Intent(ACTION_SETTING_SCREEN));
        }
    }
}
