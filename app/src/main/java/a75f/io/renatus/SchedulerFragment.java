package a75f.io.renatus;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import a75f.io.renatus.ManualSchedulerDialogFragment.ManualScheduleDialogListener;
import a75f.io.renatus.util.FontManager;

import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Suresh Kumar On 02/05/19.
 */

public class SchedulerFragment extends Fragment implements AdapterView.OnItemSelectedListener,ManualScheduleDialogListener {

	TextView textViewMonday;
	TextView textViewTuesday;
	TextView textViewWednesday;
	TextView textViewThursday;
	TextView textViewFriday;
	TextView textViewSaturday;
	TextView textViewSunday;
	View view00, view02, view04, view06, view08, view10, view12, view14, view16, view18, view20, view22, view24;
	View view01, view03, view05, view07, view09, view11, view13, view15, view17, view19, view21, view23;
	TextView textViewScheduletitle;
	TextView textViewaddEntry;
	TextView textViewaddEntryIcon;
	TextView textViewVacations;
	TextView textViewaddVacations;

	ConstraintLayout constraintScheduler;
	ArrayList<View> viewTimeLines;

	final int ID_DIALOG_SCHEDULE = 01;

	String colorMinTemp = "";
	String colorMaxTemp = "";
	public SchedulerFragment() {

	}


	public static SchedulerFragment newInstance() {
		return new SchedulerFragment();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_scheduler, container, false);

		Typeface iconFont = FontManager.getTypeface(getActivity(), FontManager.FONTAWESOME);

		//Scheduler Layout
		constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);

		textViewScheduletitle = rootView.findViewById(R.id.scheduleTitle);
		textViewaddEntry = rootView.findViewById(R.id.addEntry);
		textViewaddEntryIcon = rootView.findViewById(R.id.addEntryIcon);

		textViewaddEntryIcon.setTypeface(iconFont);
		textViewaddEntryIcon.setText(getString(R.string.icon_plus));


		textViewVacations = rootView.findViewById(R.id.vacationsTitle);
		textViewaddVacations = rootView.findViewById(R.id.addVacations);

		//Week Days
		textViewMonday = rootView.findViewById(R.id.textViewMonday);
		textViewTuesday = rootView.findViewById(R.id.textViewTuesday);
		textViewWednesday = rootView.findViewById(R.id.textViewWednesday);
		textViewThursday = rootView.findViewById(R.id.textViewThursday);
		textViewFriday = rootView.findViewById(R.id.textViewFriday);
		textViewSaturday = rootView.findViewById(R.id.textViewSaturday);
		textViewSunday = rootView.findViewById(R.id.textViewSunday);

		//Time lines with 2 hrs Interval 00:00 to 24:00
		view00 = rootView.findViewById(R.id.view00);
		view02 = rootView.findViewById(R.id.view02);
		view04 = rootView.findViewById(R.id.view04);
		view06 = rootView.findViewById(R.id.view06);
		view08 = rootView.findViewById(R.id.view08);
		view10 = rootView.findViewById(R.id.view10);
		view12 = rootView.findViewById(R.id.view12);
		view14 = rootView.findViewById(R.id.view14);
		view16 = rootView.findViewById(R.id.view16);
		view18 = rootView.findViewById(R.id.view18);
		view20 = rootView.findViewById(R.id.view20);
		view22 = rootView.findViewById(R.id.view22);
		view24 = rootView.findViewById(R.id.view24);

		//Time lines with 1hr Inerval 00:00 to 24:00
		view01 = rootView.findViewById(R.id.view01);
		view03 = rootView.findViewById(R.id.view03);
		view05 = rootView.findViewById(R.id.view05);
		view07 = rootView.findViewById(R.id.view07);
		view09 = rootView.findViewById(R.id.view09);
		view11 = rootView.findViewById(R.id.view11);
		view13 = rootView.findViewById(R.id.view13);
		view15 = rootView.findViewById(R.id.view15);
		view17 = rootView.findViewById(R.id.view17);
		view19 = rootView.findViewById(R.id.view19);
		view21 = rootView.findViewById(R.id.view21);
		view23 = rootView.findViewById(R.id.view23);

		//collecting each timeline to arraylist
		viewTimeLines = new ArrayList<View>();
		viewTimeLines.add(view00);
		viewTimeLines.add(view01);
		viewTimeLines.add(view02);
		viewTimeLines.add(view03);
		viewTimeLines.add(view04);
		viewTimeLines.add(view05);
		viewTimeLines.add(view06);
		viewTimeLines.add(view07);
		viewTimeLines.add(view08);
		viewTimeLines.add(view09);
		viewTimeLines.add(view10);
		viewTimeLines.add(view12);
		viewTimeLines.add(view13);
		viewTimeLines.add(view14);
		viewTimeLines.add(view15);
		viewTimeLines.add(view16);
		viewTimeLines.add(view17);
		viewTimeLines.add(view18);
		viewTimeLines.add(view19);
		viewTimeLines.add(view20);
		viewTimeLines.add(view21);
		viewTimeLines.add(view22);
		viewTimeLines.add(view23);
		viewTimeLines.add(view24);

		colorMinTemp = getResources().getString(0+R.color.min_temp);
		colorMinTemp = "#"+colorMinTemp.substring(3);
		//colorMinTemp = "#" + Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.min_temp));
		colorMaxTemp = getResources().getString(0+R.color.max_temp);
		colorMaxTemp = "#"+colorMaxTemp.substring(3);
		//colorMaxTemp = "#" + Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.max_temp));

		textViewaddEntry.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog(ID_DIALOG_SCHEDULE);
			}
		});
		textViewaddEntryIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog(ID_DIALOG_SCHEDULE);
			}
		});


		return rootView;
	}

	private void showDialog(int id) {
		switch (id) {
			case ID_DIALOG_SCHEDULE:
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				Fragment prev = getFragmentManager().findFragmentByTag("schedule");
				if (prev != null) {
					ft.remove(prev);
				}
				ManualSchedulerDialogFragment newFragment = new ManualSchedulerDialogFragment(this);
				newFragment.show(ft, "schedule");
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
							   long arg3) {
		double val = Double.parseDouble(arg0.getSelectedItem().toString());
		switch (arg0.getId()) {

		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}


	public boolean onClickSave(double minTemp, double maxTemp, int startTime, int endTime,
							   Boolean isMonday, Boolean isTuesday, Boolean isWednesday, Boolean isThursday, Boolean isFriday, Boolean isSaturday, Boolean isSunday)
	{
		String strminTemp = FontManager.getColoredSpanned(Double.toString(minTemp),colorMinTemp);
		String strmaxTemp = FontManager.getColoredSpanned(Double.toString(maxTemp),colorMaxTemp);

		Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/lato_regular.ttf");

		int tempStartTime = (startTime/4);
		int tempEndTime = (endTime/4);

		if(isMonday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);

			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewMonday.getId();
			lp.bottomToBottom = textViewMonday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		if(isTuesday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewTuesday.getId();
			lp.bottomToBottom = textViewTuesday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		if(isWednesday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewWednesday.getId();
			lp.bottomToBottom = textViewWednesday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		if(isThursday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewThursday.getId();
			lp.bottomToBottom = textViewThursday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		if(isFriday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewFriday.getId();
			lp.bottomToBottom = textViewFriday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		if(isSaturday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewSaturday.getId();
			lp.bottomToBottom = textViewSaturday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		if(isSunday)
		{
			TextView textViewTemp = new TextView(getActivity());
			textViewTemp.setGravity(Gravity.CENTER);
			textViewTemp.setText(Html.fromHtml(strminTemp+" "+strmaxTemp));
			textViewTemp.setBackground(getResources().getDrawable(R.drawable.temperature_background));
			textViewTemp.setTypeface(typeface);
			textViewTemp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
			textViewTemp.setId(View.generateViewId());

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0,40);
			lp.topToTop = textViewSunday.getId();
			lp.bottomToBottom = textViewSunday.getId();
			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
			constraintScheduler.addView(textViewTemp,lp);

		}
		return true;
	}


	public boolean onClickCancel(DialogFragment dialog) {
		return true;
	}
}
