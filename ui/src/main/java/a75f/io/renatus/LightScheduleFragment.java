package a75f.io.renatus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;

import org.javolution.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.logic.L;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan on 9/8/17.
 */

public class LightScheduleFragment extends BaseDialogFragment
{
    
    public static final String ID = LightScheduleFragment.class.getSimpleName();
    
    //TODO: most of the UI for this...
    UUID   mCurrentPortId = null;
    Output mCurrentPort   = null;
    
    Schedule mSchedule = null;
    
    Floor floor;
    Zone  zone;
    
    @BindView(R.id.timePickerSt)
    TimePicker startTimePicker;
    
    @BindView(R.id.timePickerEt)
    TimePicker endTimePicker;
    
    @BindView(R.id.scheduleCancel)
    Button cancelBtn;
    
    @BindView(R.id.scheduleSave)
    Button saveBtn;
    
    @BindViews({R.id.checkBoxMon, R.id.checkBoxTue, R.id.checkBoxWed, R.id.checkBoxThu,
                       R.id.checkBoxFri, R.id.checkBoxSat, R.id.checkBoxSun})
    List<CheckBox> daysList;
    
    
    public LightScheduleFragment()
    {
    }
    
    
    public static LightScheduleFragment newInstance(Floor floor, Zone zone, Output port)
    {
        LightScheduleFragment fragment = new LightScheduleFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(FragmentCommonBundleArgs.SNOUTPUT_UUID, port.getUuid());
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floor.mFloorName);
        bundle.putString(FragmentCommonBundleArgs.ZONE_NAME, zone.roomName);
        fragment.setArguments(bundle);
        return fragment;
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_light_schedule, container, false);
        mCurrentPortId =
                (UUID) getArguments().getSerializable(FragmentCommonBundleArgs.SNOUTPUT_UUID);
        String zoneName = getArguments().getString(FragmentCommonBundleArgs.ZONE_NAME);
        String floorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        zone = L.findZoneByName(floorName, zoneName);
        ButterKnife.bind(this, rootView);
        //int titleTextId = getResources().getIdentifier("title", "id", "android");
        setTitle("Lighting Schedule");
        return rootView;
    }
    
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        for (Short addresses : zone.findProfile(ProfileType.LIGHT).getNodeAddresses())
        {
            for (Output output : zone.findProfile(ProfileType.LIGHT)
                                     .getProfileConfiguration(addresses).getOutputs())
            {
                if (output.getUuid().equals(mCurrentPortId))
                {
                    mCurrentPort = output;
                }
            }
        }
        // else {
        //			mSchedule = mCurrentPort.mSchedules.get(0);
        //			fillScheduleData();
        //		}
    }
    
    
    public void fillScheduleData()
    {
        for (int i = 0; i < 7; i++)
        {
            //if ((CheckBox)daysList.get(i).)
        }
    }
    
    
    @OnClick(R.id.scheduleCancel)
    public void dismissDialog()
    {
        dismiss();
    }
    
    
    @OnClick(R.id.scheduleSave)
    public void saveSchedule()
    {
        if (mSchedule == null)
        {
            mSchedule = new Schedule();
        }
        ArrayList<Integer> days = new ArrayList<Integer>();
        for (int i = 0; i < 7; i++)
        {
            if (((CheckBox) daysList.get(i)).isChecked())
            {
                days.add(i);
            }
        }
        mSchedule.setDays(days);
        mSchedule.setSt(startTimePicker.getCurrentHour(), startTimePicker.getCurrentMinute());
        mSchedule.setEt(endTimePicker.getCurrentHour(), endTimePicker.getCurrentMinute());
        mSchedule.setVal((short) 69);
        Log.i("Schedule", "mSchedule to add: " + mSchedule.toString());
        mCurrentPort.addSchedule(mSchedule);
        L.saveCCUState();
    }
    
    
    @Override
    public String getIdString()
    {
        return ID;
    }
}
