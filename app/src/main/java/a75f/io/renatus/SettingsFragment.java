package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import a75f.io.renatus.registartion.CreateNewSite;
import a75f.io.renatus.registartion.InstallerOptions;
import a75f.io.renatus.registartion.Security;
import a75f.io.renatus.registartion.WifiFragment;
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
        transaction.addToBackStack(null);
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

                        if (isTransactionSafe) {
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
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 1: {

                        if (isTransactionSafe) {
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
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 2: {
                        if (isTransactionSafe) {
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
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 3: {
                        if (isTransactionSafe) {
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
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 4: {
                        if (isTransactionSafe) {
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
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 5: {
                        if (isTransactionSafe) {
                            fragmentClass = TempOverrideFragment.class;
                            try {
                                fragment = (Fragment) fragmentClass.newInstance();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            FragmentTransaction transaction = fragmentManager.beginTransaction();
                            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
                            transaction.replace(R.id.flContent, fragment);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        } else {
                            isTransactionPending = true;
                        }
                        break;
                    }
                    case 6:
                        if (isTransactionSafe) {
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
                            transaction.addToBackStack(null);
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
}
