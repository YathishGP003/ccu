package a75f.io.renatus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;

import org.javolution.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import a75f.io.logic.bo.building.Circuit;
import a75f.io.logic.bo.building.Day;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Schedule;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ScheduleMode;
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
    
    public static final  String ID                = LightScheduleFragment.class.getSimpleName();
    private static final String ScheduableTypeKey = "ScheduableType";
    //TODO: most of the UI for this...
    UUID                mCurrentPortId = null;
    ArrayList<Schedule> mSchedules     = null;
    ZoneProfile mZoneProfile;
    Circuit     mCircuit;
    Floor       floor;
    Zone        zone;
    @BindView(R.id.timePickerSt)
    TimePicker     startTimePicker;
    @BindView(R.id.timePickerEt)
    TimePicker     endTimePicker;
    @BindView(R.id.scheduleCancel)
    Button         cancelBtn;
    @BindView(R.id.scheduleSave)
    Button         saveBtn;
    @BindViews({R.id.checkBoxMon, R.id.checkBoxTue, R.id.checkBoxWed, R.id.checkBoxThu,
                       R.id.checkBoxFri, R.id.checkBoxSat, R.id.checkBoxSun})
    List<CheckBox> daysList;
    
    @BindView(R.id.temperatureEditText)
    EditText mValueEditText;
    
    private SchedulableType mSchedulableType;
    
    
    public LightScheduleFragment()
    {
    }
    
    
    public static LightScheduleFragment newZoneProfileInstance(Floor floor, Zone zone,
                                                               ZoneProfile zoneProfile)
    {
        LightScheduleFragment fragment = new LightScheduleFragment();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentCommonBundleArgs.PROFILE_TYPE, zoneProfile.getProfileType()
                                                                           .name());
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floor.mFloorName);
        bundle.putString(FragmentCommonBundleArgs.ZONE_NAME, zone.roomName);
        bundle.putString(ScheduableTypeKey, SchedulableType.Zone.name());
        fragment.setArguments(bundle);
        return fragment;
    }
    
    
    public static LightScheduleFragment newPortInstance(Floor floor, Zone zone,
                                                        ZoneProfile zoneProfile, Circuit port)
    {
        LightScheduleFragment fragment = new LightScheduleFragment();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floor.mFloorName);
        bundle.putString(FragmentCommonBundleArgs.ZONE_NAME, zone.roomName);
        bundle.putString(FragmentCommonBundleArgs.PROFILE_TYPE, zoneProfile.getProfileType()
                                                                           .name());
        bundle.putString(FragmentCommonBundleArgs.PORT, port.getPort().name());
        bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, port.getAddress());
        bundle.putString(ScheduableTypeKey, SchedulableType.Output.name());
        fragment.setArguments(bundle);
        return fragment;
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_light_schedule, container, false);
        String scheduableType = getArguments().getString(ScheduableTypeKey);
        mSchedulableType = SchedulableType.valueOf(scheduableType);
        String zoneName = getArguments().getString(FragmentCommonBundleArgs.ZONE_NAME);
        String floorName = getArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME);
        ProfileType profileType = ProfileType.valueOf(getArguments()
                                                              .getString(FragmentCommonBundleArgs.PROFILE_TYPE));
        zone = L.findZoneByName(floorName, zoneName);
        mZoneProfile = zone.findProfile(profileType);
        if (mSchedulableType == SchedulableType.Output)
        {
            Port port = Port.valueOf(getArguments().getString(FragmentCommonBundleArgs.PORT));
            short pairingAddress =
                    getArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR);
            mCircuit = mZoneProfile.getProfileConfiguration().get(pairingAddress).findPort(port);
        }
        ButterKnife.bind(this, rootView);
        setTitle("Lighting Schedule");
        return rootView;
    }
    
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        fillScheduleData();
    }
    
    
    public void fillScheduleData()
    {
        //If mScheduableType == SchedualbeType.Output && it's not a zone schedule
        // resolve schedule will resolve the circuits named schedule or zone schedule.
        if (mSchedulableType == SchedulableType.Output &&
            mCircuit.getScheduleMode() != ScheduleMode.ZoneSchedule)
        {
            mSchedules = L.resolveSchedules(mCircuit);
        }
        else
        {
            mSchedules = L.resolveSchedules(mZoneProfile);
        }
        //TODO: what's a multi schedule?
        if (mSchedules != null && mSchedules.size() > 0)
        {
            for (Day day : mSchedules.get(0).getDays())
            {
                ((CheckBox) daysList.get(day.getDay())).setChecked(true);
                //TODO: which is the start / end value .. need to get new UI for this.
                startTimePicker.setCurrentHour(day.getSthh());
                startTimePicker.setCurrentMinute(day.getStmm());
                endTimePicker.setCurrentHour(day.getEthh());
                endTimePicker.setCurrentMinute(day.getEtmm());
            }
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
        if (mValueEditText.getText() == null ||  mValueEditText.getText().length() == 0)
        {
            mValueEditText.setError("Requires a edit value");
        }
        else
        {
            if (mSchedules == null)
            {
                mSchedules = new ArrayList<Schedule>();
                mSchedules.add(new Schedule());
                if (mSchedulableType == SchedulableType.Output)
                {
                    mCircuit.addSchedules(mSchedules, ScheduleMode.CircuitSchedule);
                }
                else
                {
                    mZoneProfile.setSchedules(mSchedules);
                    mZoneProfile.setScheduleMode(ScheduleMode.ZoneSchedule);
                }
            }
            ArrayList<Integer> days = new ArrayList<Integer>();
            for (int i = 0; i < 7; i++)
            {
                if (((CheckBox) daysList.get(i)).isChecked())
                {
                    days.add(i);
                }
            }
            ArrayList<Day> daysList = new ArrayList<Day>();
            for (int dayOfWeek : days)
            {
                Day day = new Day();
                day.setDay(dayOfWeek);
                day.setSthh(startTimePicker.getCurrentHour());
                day.setStmm(startTimePicker.getCurrentMinute());
                day.setEthh(endTimePicker.getCurrentHour());
                day.setEtmm(endTimePicker.getCurrentMinute());
                //TODO: hook up day.
                day.setVal(Short.valueOf(mValueEditText.getText().toString()));
                daysList.add(day);
            }
            mSchedules.get(0).setDays(daysList);
            L.saveCCUState();
            dismiss();
        }
    }
    
    
    @Override
    public String getIdString()
    {
        return ID;
    }
    
    
    public enum SchedulableType
    {
        Output, Zone
    }
}
