package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.renatus.registration.FreshRegistration;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.util.ExecutorTask;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 1/9/19.
 */

public class DefaultSystemProfile extends Fragment
{
    public static DefaultSystemProfile newInstance()
    {
        return new DefaultSystemProfile();
    }

    DefaultSystem systemProfile = null;
    Prefs prefs;
    @BindView(R.id.buttonNext)
    Button mNext;
    String PROFILE = "DEFAULT";
    boolean isFromReg = false;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_profile_default, container, false);
        if(getArguments() != null) {
            isFromReg = getArguments().getBoolean("REGISTRATION_WIZARD");
        }
        return rootView;
    }

    @SuppressLint("StaticFieldLeak") @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            ButterKnife.bind(this, view);
            prefs = new Prefs(getContext().getApplicationContext());
            if (L.ccu().systemProfile instanceof DefaultSystem) {
            } else {
                ExecutorTask.executeAsync(
                        () -> ProgressDialogUtils.showProgressDialog(getActivity(),"Loading System Profile"),
                        () ->   {
                            if (systemProfile != null) {
                            systemProfile.deleteSystemEquip();
                            L.ccu().systemProfile = null;
                            }
                            L.ccu().systemProfile = new DefaultSystem().createDefaultSystemEquip();
                            CCUHsApi.getInstance().saveTagsData();
                            CCUHsApi.getInstance().syncEntityTree();
                        },
                        ProgressDialogUtils::hideProgressDialog
                    );

            }
        if(isFromReg){
            mNext.setVisibility(View.VISIBLE);
        }
        else {
            mNext.setVisibility(View.GONE);
        }

        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mNext.setEnabled(false);
                goTonext();
            }
        });

    }
    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        prefs.setBoolean("PROFILE_SETUP",true);
        prefs.setString("PROFILE",PROFILE);
        ((FreshRegistration)getActivity()).selectItem(19);
    }
}
