package a75f.io.renatus;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.ArrayList;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.definitions.DamperShape;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.vav.VavParallelFanProfile;
import a75f.io.logic.bo.building.vav.VavProfile;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.logic.bo.building.vav.VavReheatProfile;
import a75f.io.logic.bo.building.vav.VavSeriesFanProfile;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan on 6/5/18.
 */

public class DialogOAOProfile extends BaseDialogFragment
{
    public static final String ID = DialogOAOProfile.class.getSimpleName();


    public DialogOAOProfile()
    {
    }
    
    public static DialogOAOProfile newInstance(short smartNodeAddress)
    {
        DialogOAOProfile f = new DialogOAOProfile();
        Bundle bundle = new Bundle();
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, smartNodeAddress);
        //bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal());
        f.setArguments(bundle);
        return f;
    }
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    
    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 672;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
        //setTitle();
    }
    
    private void setTitle() {
        Dialog dialog = getDialog();
        
        if (dialog == null) {
            return;
        }
        dialog.setTitle("VAV Configuration");
        TextView titleView = this.getDialog().findViewById(android.R.id.title);
        if(titleView != null)
        {
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(getResources().getColor(R.color.progress_color_orange));
        }
        int titleDividerId = getContext().getResources()
                                         .getIdentifier("titleDivider", "id", "android");
        
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider != null) {
            titleDivider.setBackgroundColor(getContext().getResources()
                                                        .getColor(R.color.transparent));
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_oao_config, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        ButterKnife.bind(this, view);
        return view;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
    
    }

    
}
