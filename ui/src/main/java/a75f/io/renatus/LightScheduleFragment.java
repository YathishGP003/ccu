package a75f.io.renatus;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import org.javolution.annotations.Nullable;

import java.util.List;
import java.util.UUID;


import a75f.io.bo.building.LightSmartNodeOutput;
import a75f.io.bo.building.LightingSchedule;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;

import a75f.io.util.Globals;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan on 9/8/17.
 */

public class LightScheduleFragment extends DialogFragment
{
	
	UUID mCurrentPortId = null;
	LightSmartNodeOutput mCurrentPort = null;
	
	LightingSchedule mSchedule = null;
	
	@BindView(R.id.timePickerSt)
	TimePicker startTimePicker;
	
	@BindView(R.id.timePickerEt)
	TimePicker endTimePicker;
	
	@BindView(R.id.scheduleCancel)
	Button cancelBtn;
	
	@BindView(R.id.scheduleSave)
	Button saveBtn;
	
	@BindViews({R.id.checkBoxSun, R.id.checkBoxMon, R.id.checkBoxTue, R.id.checkBoxWed, R.id.checkBoxThu, R.id.checkBoxFri, R.id.checkBoxSat})
	List<CheckBox> daysList;
	
	public static LightScheduleFragment newInstance(LightSmartNodeOutput port){
		LightScheduleFragment fragment = new LightScheduleFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(FragmentCommonBundleArgs.SNOUTPUT_UUID, port.mUniqueID);
		fragment.setArguments(bundle);
		return fragment;
	}
	
	public LightScheduleFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_light_schedule, container, false);
		mCurrentPortId = (UUID)getArguments().getSerializable(FragmentCommonBundleArgs.SNOUTPUT_UUID);
		int titleTextId = getResources().getIdentifier("title", "id", "android");
		TextView titleText = (TextView)getDialog().findViewById(titleTextId);
		titleText.setText("Lighting Schedule");
		titleText.setTextColor(ContextCompat.getColor(getActivity(), R.color.progress_color_orange));
		ButterKnife.bind(this, rootView);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		mCurrentPort = (LightSmartNodeOutput) Globals.getInstance().getCCUApplication().findSmartNodePortByUUID(mCurrentPortId);
		
		if (mCurrentPort == null) {
			dismiss();
		} else {
			mSchedule = mCurrentPort.schedule;
			fillScheduleData();
		}
		
	}
	
	public void fillScheduleData() {
		if (mSchedule == null) {
			//No schedule yet
			return;
		}
	   
		
		for (int i = 0 ;i < 7; i++) {
			 
			//if ((CheckBox)daysList.get(i).)
		}
    }
    
	@OnClick(R.id.scheduleCancel)
	public void dismissDialog() {
		dismiss();
	}
	
	@OnClick(R.id.scheduleSave)
	public void saveSchedule() {
		if (mSchedule == null) {
			mSchedule = new LightingSchedule();
		}
		
		for (int i = 0 ;i < 7; i++) {
			
			if (((CheckBox)daysList.get(i)).isChecked()) {
				//Add schedules
			}
		}
		
	}
	
	
	
}
