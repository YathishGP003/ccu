package a75f.io.renatus;

import static a75f.io.device.modbus.ModbusModelBuilderKt.buildModbusModelByEquipRef;
import static a75f.io.domain.api.DomainName.airTempCoolingSp;
import static a75f.io.domain.api.DomainName.airTempHeatingSp;
import static a75f.io.domain.api.DomainName.dcvDamperCalculatedSetpoint;
import static a75f.io.domain.api.DomainName.dcvDamperControlEnable;
import static a75f.io.domain.api.DomainName.dualSetpointControlEnable;
import static a75f.io.domain.api.DomainName.ductStaticPressureSetpoint;
import static a75f.io.domain.api.DomainName.operatingMode;
import static a75f.io.domain.api.DomainName.supplyAirflowTemperatureSetpoint;
import static a75f.io.domain.api.DomainName.systemEnhancedVentilationEnable;
import static a75f.io.domain.api.DomainName.systemPostPurgeEnable;
import static a75f.io.domain.api.DomainName.systemPrePurgeEnable;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.system.ExternalAhuUtilKt.DISCHARGE_AIR_TEMP;
import static a75f.io.logic.bo.building.system.ExternalAhuUtilKt.DUCT_STATIC_PRESSURE_SENSOR;
import static a75f.io.logic.bo.building.system.ExternalAhuUtilKt.getConfigValue;
import static a75f.io.logic.bo.building.system.ExternalAhuUtilKt.getModbusPointValue;
import static a75f.io.logic.bo.building.system.ExternalAhuUtilKt.getOperatingMode;
import static a75f.io.logic.bo.building.system.ExternalAhuUtilKt.getSetPoint;
import static a75f.io.logic.bo.util.UnitUtils.StatusCelsiusVal;
import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tooltip.Tooltip;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.jsoup.helper.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.DAYS;
import a75f.io.api.haystack.MockTime;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.domain.util.ModelNames;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.oao.OAOEquip;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.cloudconnectivity.CloudConnectivityListener;
import a75f.io.logic.interfaces.IntrinsicScheduleListener;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.schedule.IntrinsicScheduleCreator;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.messaging.handler.UpdatePointHandler;
import a75f.io.messaging.handler.UpdateScheduleHandler;
import a75f.io.renatus.modbus.ZoneRecyclerModbusParamAdapter;
import a75f.io.renatus.modbus.util.UtilSourceKt;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.HeartBeatUtil;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.RxjavaUtil;
import a75f.io.renatus.util.SystemProfileUtil;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.renatus.views.OaoArc;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemFragment extends Fragment implements AdapterView.OnItemSelectedListener, ZoneDataInterface,
		IntrinsicScheduleListener, CloudConnectivityListener
{
	private static IntrinsicScheduleListener intrinsicScheduleListener;
	SeekBar  sbComfortValue;
	private static final long TOOLTIP_TIME = 3000;

	Spinner targetMaxInsideHumidity;
	Spinner targetMinInsideHumidity;
	ToggleButton tbCompHumidity;
	ToggleButton tbDemandResponse;
	ToggleButton tbSmartPrePurge;
	ToggleButton tbSmartPostPurge;
	ToggleButton tbEnhancedVentilation;
	LinearLayout purgeLayout,mainLayout;

	TextView energyMeterModelDetails;
	RecyclerView energyMeterParams;
	private TextView moduleStatusEmr;
	private TextView lastUpdatedEmr;

	RecyclerView btuMeterParams;
	TextView btuMeterModelDetails;
	private TextView moduleStatusBtu;
	private TextView lastUpdatedBtu;

	private TextView updatedTimeOao;
	private TextView cloudConnectivityUpdatedTime;

	boolean minHumiditySpinnerReady = false;
	boolean maxHumiditySpinnerReady = false;

	private Handler systemFragmentHandler;

	View rootView;
	TextView ccuName;
	TextView profileTitle;
	//TODO uncomment for acctuall prod releasee, commenting it out for Automation test
	//SystemNumberPicker systemModePicker;
	NumberPicker systemModePicker;
	LinearLayout lastUpdated;
	LinearLayout scheduleType;
	
	TextView occupancyStatus;
	TextView equipmentStatus;
	OaoArc oaoArc;
	
	boolean coolingAvailable = false;
	boolean heatingAvailable = false;
	
	ArrayList<String> modesAvailable = new ArrayList<>();
	ArrayAdapter<Double> humidityAdapter;
	private TextView IEGatewayOccupancyStatus;
	private TextView GUIDDetails;
	private LinearLayout IEGatewayDetail;
	Prefs prefs;

	private TextView textViewMonday;
	private TextView textViewTuesday;
	private TextView textViewWednesday;
	private TextView textViewThursday;
	private TextView textViewFriday;
	private TextView textViewSaturday;
	private TextView textViewSunday;

	View view00;
	View view02;
	View view04;
	View view06;
	View view08;
	View view10;
	View view12;
	View view14;
	View view16;
	View view18;
	View view20;
	View view22;
	View view24;
	View view01;
	View view03;
	View view05;
	View view07;
	View view09;
	View view11;
	View view13;
	View view15;
	View view17;
	View view19;
	View view21;
	View view23;

	ArrayList<View> viewTimeLines;
	ConstraintLayout constraintScheduler;
	private float mPixelsBetweenAnHour;
	private float mPixelsBetweenADay;

	private Drawable mDrawableBreakLineLeft;
	private Drawable mDrawableBreakLineRight;

	Schedule schedule;
	RecyclerView externalModbusParams;
	TextView externalModbusModelDetails;
	private TextView externalModbusStatus;
	private TextView externalModbusLastUpdated;
	private TextView external_last_updated;

	LinearLayout setPointConfig;
	LinearLayout dcv_config;
	LinearLayout dual_config;
	LinearLayout singleSatConfig;
	LinearLayout dualSatConfig;
	TextView satSetPoint;
	TextView satCurrent;
	TextView dualSatCurrent;
	TextView dspSetPoint;
	TextView dspCurrent;
	TextView opMode;
	TextView coolingSp;
	TextView heatingSp;
	TextView external_damper;

	public SystemFragment()
	{
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);

	}

	public static SystemFragment newInstance()
	{
		return new SystemFragment();
	}

	public void refreshScreen(String id, boolean isRemoteChange)
	{
		CcuLog.i("UI_PROFILING", "SystemFragment.refreshScreen");
		
		if(getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (!(L.ccu().systemProfile instanceof DefaultSystem)) {
						checkForOao();
						fetchPoints();
						if(rootView != null){
							configEnergyMeterDetails(rootView);
							configBTUMeterDetails(rootView);
						}
					}

				}
			});
		}
		CcuLog.i("UI_PROFILING", "SystemFragment.refreshScreen Done");
		
	}

	public void refreshDesiredTemp(String nodeAddress,String  coolDt, String heatDt){}
	public void refreshScreenbySchedule(String nodeAddress, String equipId, String zoneId){}
	public void updateTemperature(double currentTemp, short nodeAddress){}
	public void updateSensorValue(short nodeAddress){}
	public void refreshHeartBeatStatus(String id){}
	@Override
	public void onResume() {
		super.onResume();
		checkForOao();
		fetchPoints();

		if(getUserVisibleHint()) {
            fetchPoints();
            if (prefs.getBoolean("REGISTRATION")) {
                UpdatePointHandler.setSystemDataInterface(this);
            }
        }
		UpdateScheduleHandler.setIntrinsicScheduleListener(this);
		SystemFragment.setIntrinsicScheduleListener(this);
		UpdatePointHandler.setIntrinsicScheduleListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (prefs.getBoolean("REGISTRATION")) {
			UpdatePointHandler.setSystemDataInterface(null);
		}
		UpdateScheduleHandler.setIntrinsicScheduleListener(null);
		SystemFragment.setIntrinsicScheduleListener(null);
		UpdatePointHandler.setIntrinsicScheduleListener(null);
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if(isVisibleToUser) {
			UpdatePointHandler.setSystemDataInterface(this);
			UpdatePointHandler.setIntrinsicScheduleListener(this);
			loadIntrinsicSchedule();
		} else {
			UpdatePointHandler.setSystemDataInterface(null);
			UpdatePointHandler.setIntrinsicScheduleListener(null);
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState)
	{
	try {
		NotificationHandler.setCloudConnectivityListener(this);
		rootView = inflater.inflate(R.layout.fragment_system_setting, container, false);
		constraintScheduler = rootView.findViewById(R.id.constraintLt_Scheduler);

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
		viewTimeLines = new ArrayList<>();
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
		viewTimeLines.add(view11);
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

		mDrawableBreakLineLeft = AppCompatResources.getDrawable(getContext(), R.drawable.ic_break_line_left_svg);
		mDrawableBreakLineRight = AppCompatResources.getDrawable(getContext(), R.drawable.ic_break_line_right_svg);

		}catch (InflateException inflateException){
			Log.d(L.TAG_CCU_UI," Problem when inflating the layout fragment_system_setting "+inflateException.getMessage());
			inflateException.printStackTrace();
		}
		return rootView;
	}

	private void loadIntrinsicSchedule(){
		RxjavaUtil.executeBackgroundTask(this::buildIntrinsicSchedule, () -> { updateUI();
		});
	}

	private void buildIntrinsicSchedule(){
		schedule = new IntrinsicScheduleCreator().buildIntrinsicScheduleForCurrentWeek();
	}

	private void drawCurrentTime() {

		try {
			DateTime now = new DateTime(MockTime.getInstance().getMockTime());


			DAYS day = DAYS.values()[now.getDayOfWeek() - 1];
			Log.i("Scheduler", "DAY: " + day.toString());
			int hh = now.getHourOfDay();
			int mm = now.getMinuteOfHour();

			AppCompatImageView imageView = new AppCompatImageView(requireContext());

			imageView.setImageResource(R.drawable.ic_time_marker_svg);
			imageView.setId(View.generateViewId());
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int) mPixelsBetweenADay);
			lp.bottomToBottom = getTextViewFromDay(day).getId();
			lp.topToTop = getTextViewFromDay(day).getId();
			lp.startToStart = viewTimeLines.get(hh).getId();
			lp.leftMargin = (int) ((mm / 60.0) * mPixelsBetweenAnHour);

			constraintScheduler.addView(imageView, lp);
		}
		catch (IllegalStateException exception) {
			// if context is null we will get this exception, some rare scenario we get this.
			Log.d(L.TAG_CCU_UI,exception.getMessage());
		}
	}

	private TextView getTextViewFromDay(DAYS day) {
		switch (day) {
			case MONDAY:
				return textViewMonday;

			case TUESDAY:
				return textViewTuesday;

			case WEDNESDAY:
				return textViewWednesday;

			case THURSDAY:
				return textViewThursday;

			case FRIDAY:
				return textViewFriday;

			case SATURDAY:
				return textViewSaturday;

			case SUNDAY:
				return textViewSunday;

			default:
				return textViewSunday;
		}
	}

	private void updateUI() {
		schedule.populateIntersections();
		Runnable runnable = () -> {
			hasTextViewChildren();
			ArrayList<Schedule.Days> days = schedule.getDays();
			Collections.sort(days, (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
			Collections.sort(days, (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));

			for (int i = 0; i < days.size(); i++) {
				Schedule.Days daysElement = days.get(i);
				drawSchedule(i, daysElement.getSthh(), daysElement.getEthh(), daysElement.getStmm(), daysElement.getEtmm(),
						DAYS.values()[daysElement.getDay()], daysElement.isIntersection());
			}
		};
		systemFragmentHandler.post(runnable);
	}

	private void hasTextViewChildren() {

		for (int i = constraintScheduler.getChildCount() - 1; i >= 0; i--) {
			if (constraintScheduler.getChildAt(i).getTag() != null) {
				constraintScheduler.removeViewAt(i);
			}
		}

	}

	private void drawSchedule(int position, int startTimeHH, int endTimeHH, int startTimeMM, int endTimeMM, DAYS day, boolean intersection) {
		Typeface typeface=Typeface.DEFAULT;
		try {
			typeface = Typeface.createFromAsset(requireActivity().getAssets(), "fonts/lato_regular.ttf");
		}catch (Exception e){
			e.printStackTrace();
		}

		if (startTimeHH > endTimeHH || (startTimeHH == endTimeHH && startTimeMM > endTimeMM)) {
			drawScheduleBlock(position, typeface, startTimeHH, 24, startTimeMM, 0,
					getTextViewFromDay(day), false, true, intersection);
			drawScheduleBlock(position, typeface, 0, endTimeHH, 0, endTimeMM,
					getTextViewFromDay(day.getNextDay()), true, false, intersection);
		} else {
			drawScheduleBlock(position, typeface, startTimeHH, endTimeHH, startTimeMM,
					endTimeMM, getTextViewFromDay(day), false, false, intersection);
		}


	}

	private void drawScheduleBlock(int position, Typeface typeface, int tempStartTime, int tempEndTime,
								   int startTimeMM, int endTimeMM, TextView textView,
								   boolean leftBreak, boolean rightBreak, boolean intersection) {

		Log.i(L.TAG_CCU_UI, "position: "+position+" tempStartTime: " + tempStartTime + " tempEndTime: " + tempEndTime + " startTimeMM: " + startTimeMM + " endTimeMM " + endTimeMM);

		if(getContext()==null) return;
		AppCompatTextView textViewTemp = new AppCompatTextView(getContext());
		textViewTemp.setGravity(Gravity.CENTER_HORIZONTAL);
		if(typeface!=null)
			textViewTemp.setTypeface(typeface);
		TextViewCompat.setAutoSizeTextTypeWithDefaults(textViewTemp, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
		textViewTemp.setMaxLines(2);
		textViewTemp.setContentDescription(textView.getText().toString()+"_"+tempStartTime+":"+startTimeMM+"-"+tempEndTime+":"+endTimeMM);
		textViewTemp.setId(ViewCompat.generateViewId());
		textViewTemp.setTag(position);


		ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(0, (int) mPixelsBetweenADay);
		lp.baselineToBaseline = textView.getId();


		int leftMargin = startTimeMM > 0 ? (int) ((startTimeMM / 60.0) * mPixelsBetweenAnHour) : lp.leftMargin;
		int rightMargin = endTimeMM > 0 ? (int) (((60 - endTimeMM) / 60.0) * mPixelsBetweenAnHour) : lp.rightMargin;

		lp.leftMargin = leftMargin;
		lp.rightMargin = rightMargin;

		Drawable drawableCompat = null;

		if (leftBreak) {
			drawableCompat = getResources().getDrawable(R.drawable.temperature_background_left, null);
			if (intersection) {
				Drawable rightGreyBar = getResources().getDrawable(R.drawable.vline, null);
				textViewTemp.setCompoundDrawablesWithIntrinsicBounds(mDrawableBreakLineLeft, null, rightGreyBar, null);
			}else
				textViewTemp.setCompoundDrawablesWithIntrinsicBounds(mDrawableBreakLineLeft, null, null, null);

			Space space = new Space(getActivity());
			space.setId(View.generateViewId());
			float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());


			ConstraintLayout.LayoutParams spaceLP = new ConstraintLayout.LayoutParams((int) px, 10);
			spaceLP.rightToLeft = viewTimeLines.get(tempStartTime).getId();

			constraintScheduler.addView(space, spaceLP);


			if (endTimeMM > 0)
				tempEndTime++;

			lp.startToStart = space.getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();


		} else if (rightBreak) {
			drawableCompat = getResources().getDrawable(R.drawable.temperature_background_right, null);
			textViewTemp.setCompoundDrawablesWithIntrinsicBounds(null, null, mDrawableBreakLineRight, null);
			Space space = new Space(getActivity());
			space.setId(View.generateViewId());
			float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());


			ConstraintLayout.LayoutParams spaceLP = new ConstraintLayout.LayoutParams((int) px, 10);
			spaceLP.leftToRight = viewTimeLines.get(tempEndTime).getId();

			constraintScheduler.addView(space, spaceLP);

			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = space.getId();
		} else {


			if (intersection) {
				Drawable rightGreyBar = getResources().getDrawable(R.drawable.vline, null);
				textViewTemp.setCompoundDrawablesWithIntrinsicBounds(null, null,
						rightGreyBar, null);
			}


			drawableCompat = getResources().getDrawable(R.drawable.temperature_background, null);

			if (endTimeMM > 0)
				tempEndTime++;

			lp.startToStart = viewTimeLines.get(tempStartTime).getId();
			lp.endToEnd = viewTimeLines.get(tempEndTime).getId();
		}

		textViewTemp.setBackground(drawableCompat);
		constraintScheduler.addView(textViewTemp, lp);
		textViewTemp.setOnClickListener(v -> {
			int clickedPosition = (int)v.getTag();
			ArrayList<Schedule.Days> days = schedule.getDays();
			Collections.sort(days, (lhs, rhs) -> lhs.getSthh() - (rhs.getSthh()));
			Collections.sort(days, (lhs, rhs) -> lhs.getDay() - (rhs.getDay()));
			Schedule.Days day = schedule.getDays().get(clickedPosition);
			String toolTipValue = new StringBuffer().append("Schedule: ")
					.append(convertIntHourMinsToString(day.getSthh(), day.getStmm()))
					.append(" to ")
					.append(convertIntHourMinsToString(day.getEthh(), day.getEtmm()))
					.toString();

			Tooltip intrinsicScheduleToolTip = new Tooltip.Builder(v)
					.setBackgroundColor(Color.BLACK)
					.setTextColor(Color.WHITE)
					.setCancelable(true)
					.setDismissOnClick(true)
					.setGravity(Gravity.TOP)
					.setText(toolTipValue)
					.show();
			systemFragmentHandler.postDelayed(intrinsicScheduleToolTip::dismiss, TOOLTIP_TIME);
		});
	}
	private String convertIntHourMinsToString(int hour, int minute){
		if(hour == 23 && minute == 59){
			return "24:00";
		}
		String hr = String.valueOf(hour);
		String min = String.valueOf(minute);
		String zero = "0";
		if(hour < 10){
			hr = zero + hr;
		}
		if(minute < 10){
			min = zero + min;
		}
		return hr + ":" +min;
	}

	public void updateIntrinsicSchedule() {
		if(getActivity() != null) {
			getActivity().runOnUiThread(this::loadIntrinsicSchedule);
			fetchPoints();
		}
	}

	@SuppressLint("ClickableViewAccessibility") @Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		CcuLog.i("UI_PROFILING", "SystemFragment.onViewCreated");
		systemFragmentHandler = new Handler(Looper.getMainLooper());

		//Measure the amount of pixels between an hour after the constraintScheduler layout draws the bars for the first time.
		//After they are measured d the schedule.
		ViewTreeObserver vto = constraintScheduler.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				constraintScheduler.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				View viewHourOne = viewTimeLines.get(1);
				View viewHourTwo = viewTimeLines.get(2);

				mPixelsBetweenAnHour = viewHourTwo.getX() - viewHourOne.getX();
				mPixelsBetweenADay = constraintScheduler.getHeight() / 7f;

				//Leave 20% for padding.
				mPixelsBetweenADay = mPixelsBetweenADay - (mPixelsBetweenADay * .2f);

				loadIntrinsicSchedule();
				drawCurrentTime();

			}
		});


		prefs = new Prefs(getActivity());
		ccuName = view.findViewById(R.id.ccuName);
		HashMap<Object, Object> ccu = CCUHsApi.getInstance().readEntity("device and ccu");
		ccuName.setText(ccu.get("dis").toString());
		profileTitle = view.findViewById(R.id.profileTitle);
		oaoArc = view.findViewById(R.id.oaoArc);
		purgeLayout = view.findViewById(R.id.purgelayout);
		systemModePicker = view.findViewById(R.id.systemModePicker);
		mainLayout = view.findViewById(R.id.main_layout);
		lastUpdated = view.findViewById(R.id.lastUpdated);
		scheduleType = view.findViewById(R.id.scheduleType);
		if(prefs.getBoolean("REGISTRATION")){
			lastUpdated.setVisibility(View.VISIBLE);
			scheduleType.setVisibility(View.VISIBLE);
			constraintScheduler.setVisibility(View.VISIBLE);
		}else {
			lastUpdated.setVisibility(View.GONE);
			scheduleType.setVisibility(View.GONE);
			constraintScheduler.setVisibility(View.GONE);
		}

		if (L.ccu().systemProfile != null) {
			coolingAvailable = L.ccu().systemProfile.isCoolingAvailable();
			heatingAvailable = L.ccu().systemProfile.isHeatingAvailable();
		}

		
		modesAvailable.add(SystemMode.OFF.displayName);
		if (coolingAvailable && heatingAvailable) {
			modesAvailable.add(SystemMode.AUTO.displayName);
		}
		if (coolingAvailable) {
			modesAvailable.add(SystemMode.COOLONLY.displayName);
		}
		if (heatingAvailable) {
			modesAvailable.add(SystemMode.HEATONLY.displayName);
		}


		systemModePicker.setMinValue(0);
		systemModePicker.setMaxValue(modesAvailable.size()-1);
		
		systemModePicker.setDisplayedValues(modesAvailable.toArray(new String[modesAvailable.size()]));
		systemModePicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		//TODO we will comment out below two lines for prod release
		systemModePicker.setWrapSelectorWheel(false);

		
		
		systemModePicker.setOnScrollListener((numberPicker, scrollState) -> {
			if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
				//Adding a dealy of 100ms as instant invocation of getVal() returns old value at times.
				Runnable myRunnable = () -> {

					WeakReference<NumberPicker> numberPickerReference = new WeakReference<>(numberPicker);
					NumberPicker currentNumberPicker = numberPickerReference.get();

					if (currentNumberPicker != null && currentNumberPicker.getValue() != TunerUtil.readSystemUserIntentVal("conditioning and mode")) {
						SystemProfileUtil.setUserIntentBackground("conditioning and mode", SystemMode.getEnum(modesAvailable.get(currentNumberPicker.getValue())).ordinal());
						if (L.ccu().systemProfile != null) {
							L.ccu().systemProfile.reset();
						}
					}
				};
				systemFragmentHandler.postDelayed(myRunnable, 100);
			}
		});

		occupancyStatus = view.findViewById(R.id.occupancyStatus);
		equipmentStatus = view.findViewById(R.id.equipmentStatus);
		IEGatewayOccupancyStatus = view.findViewById(R.id.IE_Gateway_Occupancy_Status);
		GUIDDetails = view.findViewById(R.id.GUID_Details);
		IEGatewayDetail = view.findViewById(R.id.ie_gateway_details);

		sbComfortValue = view.findViewById(R.id.systemComfortValue);
		
		targetMaxInsideHumidity = view.findViewById(R.id.targetMaxInsideHumidity);
		targetMinInsideHumidity = view.findViewById(R.id.targetMinInsideHumidity);
		CCUUiUtil.setSpinnerDropDownColor(targetMaxInsideHumidity,getContext());
		CCUUiUtil.setSpinnerDropDownColor(targetMinInsideHumidity,getContext());
		targetMinInsideHumidity.setOnTouchListener((v, event) -> {
			minHumiditySpinnerReady = true;
			return false;
		});
		
		targetMaxInsideHumidity.setOnTouchListener((v, event) -> {
			maxHumiditySpinnerReady = true;
			return false;
		});
		
		tbCompHumidity = view.findViewById(R.id.tbCompHumidity);
		tbDemandResponse = view.findViewById(R.id.tbDemandResponse);
		tbSmartPrePurge = view.findViewById(R.id.tbSmartPrePurge);
		tbSmartPostPurge = view.findViewById(R.id.tbSmartPostPurge);
		tbEnhancedVentilation = view.findViewById(R.id.tbEnhancedVentilation);
		updatedTimeOao = view.findViewById(R.id.last_updated_status_oao);
		cloudConnectivityUpdatedTime = view.findViewById(R.id.last_updated_time_ccu_hb);

		tbCompHumidity.setEnabled(false);
		tbDemandResponse.setEnabled(false);

		energyMeterParams = view.findViewById(R.id.energyMeterParams);
		energyMeterModelDetails = view.findViewById(R.id.energyMeterModelDetails);
		moduleStatusEmr = view.findViewById(R.id.module_status_emr);
		lastUpdatedEmr = view.findViewById(R.id.last_updated_emr);
		configEnergyMeterDetails(view);

		/**
		 * init Modbus BTU meter  views
		 */
		btuMeterParams = view.findViewById(R.id.btuMeterParams);
		btuMeterModelDetails = view.findViewById(R.id.btuMeterModelDetails);
		moduleStatusBtu = view.findViewById(R.id.module_status_btu);
		lastUpdatedBtu = view.findViewById(R.id.last_updated_btu);
		configBTUMeterDetails(view);

		setPointConfig = view.findViewById(R.id.setpoint_config);
		satSetPoint = view.findViewById(R.id.sat_setpoint);
		dspSetPoint = view.findViewById(R.id.dsp_setpoint);
		satCurrent = view.findViewById(R.id.sat_current);
		dualSatCurrent = view.findViewById(R.id.sat_dual_cur);
		dspCurrent = view.findViewById(R.id.dsp_current);
		external_damper = view.findViewById(R.id.external_damper);
		dcv_config = view.findViewById(R.id.dcv_config);
		singleSatConfig = view.findViewById(R.id.single_sat_config);
		dualSatConfig = view.findViewById(R.id.dual_sat_config);
		dual_config = view.findViewById(R.id.dual_config);
		opMode = view.findViewById(R.id.sat_operatingmode);
		coolingSp = view.findViewById(R.id.sat_coolingsp);
		heatingSp = view.findViewById(R.id.sat_heatingsp);

		externalModbusParams = view.findViewById(R.id.external_modbus_device);
		externalModbusModelDetails = view.findViewById(R.id.external_device_details);
		externalModbusStatus = view.findViewById(R.id.external_device_status);
		externalModbusLastUpdated = view.findViewById(R.id.external_last_updated_status);
		external_last_updated = view.findViewById(R.id.external_last_updated);
		showExternalModbusDevice();



		if (L.ccu().systemProfile instanceof DefaultSystem) {
			systemModePicker.setEnabled(false);
			sbComfortValue.setEnabled(false);

			ArrayList<Double> zoroToHundred = new ArrayList<>();
			for (double val = 0;  val <= 100.0; val++)
			{
				zoroToHundred.add(val);
			}
			humidityAdapter = new CustomSpinnerDropDownAdapter(this.requireContext(),R.layout.spinner_dropdown_item, zoroToHundred);
			humidityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
			targetMinInsideHumidity.setAdapter(humidityAdapter);
			targetMinInsideHumidity.setSelection(0);
			targetMaxInsideHumidity.setAdapter(humidityAdapter);
			targetMaxInsideHumidity.setSelection(0);

			targetMaxInsideHumidity.setEnabled(false);
			targetMinInsideHumidity.setEnabled(false);
			tbCompHumidity.setEnabled(false);
			tbDemandResponse.setEnabled(false);
			tbSmartPrePurge.setEnabled(false);
			tbSmartPostPurge.setEnabled(false);
			tbEnhancedVentilation.setEnabled(false);
			purgeLayout.setVisibility(View.GONE);
			return;
		}
		
		sbComfortValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				sbComfortValue.setContentDescription(String.valueOf(seekBar.getProgress()));
				SystemProfileUtil.setUserIntentBackground("desired and ci", 5 - (double) seekBar.getProgress());
			}
		});
		
		ArrayList<Double> zoroToHundred = new ArrayList<>();
		for (double val = 0;  val <= 100.0; val++)
		{
			zoroToHundred.add(val);
		}
		
		humidityAdapter = new CustomSpinnerDropDownAdapter(this.requireContext(), R.layout.spinner_dropdown_item, zoroToHundred);
		humidityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		
		
		targetMinInsideHumidity.setAdapter(humidityAdapter);
		targetMaxInsideHumidity.setAdapter(humidityAdapter);
		
		targetMinInsideHumidity.setOnItemSelectedListener(this);
		targetMaxInsideHumidity.setOnItemSelectedListener(this);
		
		tbCompHumidity.setOnCheckedChangeListener((compoundButton, b) -> {
			if (compoundButton.isPressed()) {
				SystemProfileUtil.setUserIntentBackground("compensate and humidity", b ? 1 : 0);
			}
		});

		tbDemandResponse.setOnCheckedChangeListener((compoundButton, b) -> {
			if (compoundButton.isPressed()) {
				SystemProfileUtil.setUserIntentBackground("demand and response", b ? 1 : 0);
			}
		});
		tbSmartPrePurge.setOnCheckedChangeListener((compoundButton, b) -> {
			if (compoundButton.isPressed()) {
				if (isExternalAhu()) {
					SystemProfileUtil.setUserIntentByDomain(systemPrePurgeEnable, b ? 1 : 0);
				} else {
					SystemProfileUtil.setUserIntentBackground("prePurge and enabled", b ? 1 : 0);
				}
			}
		});
		tbSmartPostPurge.setOnCheckedChangeListener((compoundButton, b) -> {
			if (compoundButton.isPressed()) {
				if (isExternalAhu()) {
					SystemProfileUtil.setUserIntentByDomain(systemPostPurgeEnable, b ? 1 : 0);
				} else {
					SystemProfileUtil.setUserIntentBackground("postPurge and enabled", b ? 1 : 0);
				}

			}
		});
		tbEnhancedVentilation.setOnCheckedChangeListener((compoundButton, b) -> {
			if (compoundButton.isPressed()) {
				if (isExternalAhu()) {
					SystemProfileUtil.setUserIntentByDomain(systemEnhancedVentilationEnable, b ? 1 : 0);
				} else {
					SystemProfileUtil.setUserIntentBackground("enhanced and ventilation and enabled", b ? 1 : 0);
				}

			}
		});
		getActivity().registerReceiver(occupancyReceiver, new IntentFilter(ACTION_STATUS_CHANGE));
		configWatermark();
		CcuLog.i("UI_PROFILING", "SystemFragment.onViewCreated Done");
	}

	private boolean isExternalAhu(){
		return (L.ccu().systemProfile instanceof DabExternalAhu
				|| L.ccu().systemProfile instanceof VavExternalAhu);
	}



	private void checkForOao() {
		if (L.ccu().oaoProfile != null) {
			oaoArc.setVisibility(View.VISIBLE);
			purgeLayout.setVisibility(View.VISIBLE);
			if (isExternalAhu()) {
				tbSmartPrePurge.setChecked(TunerUtil.readSystemUserIntentVal("domainName == \""+systemPrePurgeEnable+"\"") > 0);
				tbSmartPostPurge.setChecked(TunerUtil.readSystemUserIntentVal("domainName == \""+systemPostPurgeEnable+"\"") > 0);
				tbEnhancedVentilation.setChecked(TunerUtil.readSystemUserIntentVal("domainName == \""+systemEnhancedVentilationEnable+"\"") > 0);
			} else {
				tbSmartPrePurge.setChecked(TunerUtil.readSystemUserIntentVal("prePurge and enabled") > 0);
				tbSmartPostPurge.setChecked(TunerUtil.readSystemUserIntentVal("postPurge and enabled") > 0);
				tbEnhancedVentilation.setChecked(TunerUtil.readSystemUserIntentVal("enhanced and ventilation") > 0);
			}
			ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip and oao");

			if (equips != null && equips.size() > 0) {
				ArrayList<OAOEquip> equipList = new ArrayList<>();
				for (HashMap m : equips) {
					String nodeAddress = m.get("group").toString();
					equipList.add(new OAOEquip(ProfileType.OAO, Short.parseShort(nodeAddress)));
					updatedTimeOao.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
					oaoArc.updateStatus(HeartBeatUtil.isModuleAlive(nodeAddress));
				}

				double returnAirCO2 = equipList.get(0).getHisVal("return and air and co2 and sensor");
				double co2Threshold = equipList.get(0).getConfigNumVal("co2 and threshold");

				int angel = (int)co2Threshold / 20;
				if (angel < 0){
					angel = 0;
				} else if (angel > 2000){
					angel = 2000;
				}

				int progress = (int) returnAirCO2 / 20;
				if (progress < 0){
					progress = 0;
				} else if (progress > 2000){
					progress = 2000;
				}

				oaoArc.setProgress(progress);
				oaoArc.setData(angel,(int)returnAirCO2);
				oaoArc.setContentDescription(String.valueOf(returnAirCO2));
			}
		} else {
			RelativeLayout.LayoutParams layoutParams =(RelativeLayout.LayoutParams)systemModePicker.getLayoutParams();
			layoutParams.setMargins(0,300,0,0);
			systemModePicker.setLayoutParams(layoutParams);
			oaoArc.setVisibility(View.GONE);
			purgeLayout.setVisibility(View.GONE);
		}
	}

	public void fetchPoints()
	{
		if(getActivity() != null && L.ccu().systemProfile != null) {
			getActivity().runOnUiThread(() -> {
				String colorHex = CCUUiUtil.getColorCode(getContext());
				String status = CCUHsApi.getInstance().readDefaultStrVal("system and status and message");
				//If the system status is not updated yet (within a minute of registering the device), generate a
				//default message.
				if (StringUtils.isEmpty(status)) {
					status = L.ccu().systemProfile.getStatusMessage();
				}

				if (L.ccu().systemProfile instanceof DefaultSystem) {
					equipmentStatus.setText(StringUtil.isBlank(status) ? "System is in gateway mode" : Html.fromHtml(status.replace("ON", "<font color='"+colorHex+"'>ON</font>")));
					occupancyStatus.setText(ScheduleManager.getInstance().getSystemStatusString());
					tbCompHumidity.setChecked(false);
					tbDemandResponse.setChecked(false);
					tbSmartPrePurge.setChecked(false);
					tbSmartPostPurge.setChecked(false);
					tbEnhancedVentilation.setChecked(false);
					sbComfortValue.setProgress(0);
					sbComfortValue.setContentDescription("0");
					targetMaxInsideHumidity.setSelection(humidityAdapter
							.getPosition(0.0), false);
					targetMinInsideHumidity.setSelection(humidityAdapter
							.getPosition(0.0), false);
					setPointConfig.setVisibility(View.GONE);

				} else {
					systemModePicker.setValue((int) TunerUtil.readSystemUserIntentVal("conditioning and mode"));

					equipmentStatus.setText(StringUtil.isBlank(status)? Html.fromHtml("<font color='"+colorHex+"'>OFF</font>") : Html.fromHtml(status.replace("ON","<font color='"+colorHex+"'>ON</font>").replace("OFF","<font color='"+colorHex+"'>OFF</font>")));
					if (isCelsiusTunerAvailableStatus()) {
						occupancyStatus.setText(StatusCelsiusVal(ScheduleManager.getInstance()
								.getSystemStatusString(), TemperatureMode.DUAL.ordinal()));
					} else {
						occupancyStatus.setText(ScheduleManager.getInstance().getSystemStatusString());
					}
					tbCompHumidity.setChecked(TunerUtil.readSystemUserIntentVal("compensate and humidity") > 0);
					tbDemandResponse.setChecked(TunerUtil.readSystemUserIntentVal("demand and response") > 0);
					if (isExternalAhu()) {
						tbSmartPrePurge.setChecked(TunerUtil.readSystemUserIntentVal("domainName == \""+systemPrePurgeEnable+"\"") > 0);
						tbSmartPostPurge.setChecked(TunerUtil.readSystemUserIntentVal("domainName == \""+systemPostPurgeEnable+"\"") > 0);
						tbEnhancedVentilation.setChecked(TunerUtil.readSystemUserIntentVal("domainName == \""+systemEnhancedVentilationEnable+"\"") > 0);
					} else {
						tbSmartPrePurge.setChecked(TunerUtil.readSystemUserIntentVal("prePurge and enabled") > 0);
						tbSmartPostPurge.setChecked(TunerUtil.readSystemUserIntentVal("postPurge and enabled") > 0);
						tbEnhancedVentilation.setChecked(TunerUtil.readSystemUserIntentVal("enhanced and ventilation") > 0);
					}
					sbComfortValue.setProgress(5 - (int) TunerUtil.readSystemUserIntentVal("desired and ci"));
					sbComfortValue.setContentDescription(String.valueOf(5 - (int) TunerUtil.readSystemUserIntentVal("desired and ci")));

					targetMaxInsideHumidity.setSelection(humidityAdapter
							.getPosition(TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")), false);
					targetMinInsideHumidity.setSelection(humidityAdapter
							.getPosition(TunerUtil.readSystemUserIntentVal("target and min and inside and humidity")), false);

					if(L.ccu().systemProfile instanceof VavIERtu) {
						IEGatewayDetail.setVisibility(View.VISIBLE);
						IEGatewayOccupancyStatus.setText(getOccStatus());
						GUIDDetails.setText(CCUHsApi.getInstance().getSiteIdRef().toString());
					} else {
						configureExternalAhu();
					}
				}
				if (L.ccu().systemProfile != null) {
					profileTitle.setText(L.ccu().systemProfile.getProfileName());
				}
			});
		}
		
	}

	private void configureExternalAhu() {
		if(L.ccu().systemProfile instanceof DabExternalAhu ||
				L.ccu().systemProfile instanceof VavExternalAhu) {
			if (L.ccu().systemProfile instanceof DabExternalAhu) {
				setCurrentAndSetPoints (
						getSetPoint(supplyAirflowTemperatureSetpoint),
						getSetPoint(ductStaticPressureSetpoint),
						getModbusPointValue(DISCHARGE_AIR_TEMP),
						getModbusPointValue(DUCT_STATIC_PRESSURE_SENSOR),
						getConfigValue(dcvDamperControlEnable, ModelNames.DAB_EXTERNAL_AHU_CONTROLLER),
						getSetPoint(dcvDamperCalculatedSetpoint),
						getConfigValue(dualSetpointControlEnable,ModelNames.DAB_EXTERNAL_AHU_CONTROLLER),
						getSetPoint(airTempCoolingSp),
						getSetPoint(airTempHeatingSp),
						getOperatingMode(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
				);
			} else {
				setCurrentAndSetPoints (
						getSetPoint(supplyAirflowTemperatureSetpoint),
						getSetPoint(ductStaticPressureSetpoint),
						getModbusPointValue(DISCHARGE_AIR_TEMP),
						getModbusPointValue(DUCT_STATIC_PRESSURE_SENSOR),
						getConfigValue(dcvDamperControlEnable,ModelNames.VAV_EXTERNAL_AHU_CONTROLLER),
						getSetPoint(dcvDamperCalculatedSetpoint),
						getConfigValue(dualSetpointControlEnable,ModelNames.VAV_EXTERNAL_AHU_CONTROLLER),
						getSetPoint(airTempCoolingSp),
						getSetPoint(airTempHeatingSp),
						getOperatingMode(ModelNames.VAV_EXTERNAL_AHU_CONTROLLER)
				);
			}
		} else {
			setPointConfig.setVisibility(View.GONE);
		}
	}

	private void setCurrentAndSetPoints(
			String satSp, String dspSp,String satCur, String dspCur, boolean dcvEnabled,
			String dcvSp, boolean dualEnabled, String coolingSpVal, String heatingSpVal,
			String operationMode
	) {
		setPointConfig.setVisibility(View.VISIBLE);
		satSetPoint.setText(satSp);
		dspSetPoint.setText(dspSp);
		satCurrent.setText(satCur);
		dualSatCurrent.setText(satCur);
		dspCurrent.setText(dspCur);
		coolingSp.setText(coolingSpVal);
		heatingSp.setText(heatingSpVal);
		opMode.setText(operationMode);

		if (dcvEnabled) {
			external_damper.setText(dcvSp);
			dcv_config.setVisibility(View.VISIBLE);
		} else {
			dcv_config.setVisibility(View.GONE);
		}

		if (dualEnabled) {
			dual_config.setVisibility(View.VISIBLE);
			dualSatConfig.setVisibility(View.VISIBLE);
			singleSatConfig.setVisibility(View.GONE);
		} else {
			dual_config.setVisibility(View.GONE);
			dualSatConfig.setVisibility(View.GONE);
			singleSatConfig.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
	                           long arg3)
	{
		double val = Double.parseDouble(arg0.getSelectedItem().toString());
		
		switch (arg0.getId())
		{
			case R.id.targetMaxInsideHumidity:
				if (maxHumiditySpinnerReady)
				{
					maxHumiditySpinnerReady = false;
					SystemProfileUtil.setUserIntentBackground("target and max and inside and humidity", val);
				}
				break;
			case R.id.targetMinInsideHumidity:
				if (minHumiditySpinnerReady)
				{
					minHumiditySpinnerReady = false;
					SystemProfileUtil.setUserIntentBackground("target and min and inside and humidity", val);
				}
				break;
		}
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDestroyView() {
		try {
			if (getActivity() != null){
				getActivity().unregisterReceiver(occupancyReceiver);
			}
			systemFragmentHandler.removeCallbacksAndMessages(null);
		}catch (Exception e){
			e.printStackTrace();
		}
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		systemFragmentHandler = null;
		super.onDestroy();
	}

	private final BroadcastReceiver occupancyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null) {
				return;
			}
			if (getActivity() != null && isAdded()) {
				if (!(L.ccu().systemProfile instanceof DefaultSystem)) {
					fetchPoints();
				}
			}
		}
	};
	private void configEnergyMeterDetails(View view){
		EquipmentDevice emDevice = getModbusEquip("emr");;
		if (emDevice != null) {
			energyMeterParams.setVisibility(View.VISIBLE);
			energyMeterModelDetails.setVisibility(View.VISIBLE);
			moduleStatusEmr.setVisibility(View.VISIBLE);
			lastUpdatedEmr.setVisibility(View.VISIBLE);
			List<Parameter> parameterList = new ArrayList<>();

			energyMeterParams.setVisibility(View.VISIBLE);
			energyMeterModelDetails.setVisibility(View.VISIBLE);
			moduleStatusEmr.setVisibility(View.VISIBLE);
			lastUpdatedEmr.setVisibility(View.VISIBLE);
			List<Parameter> allParamList = UtilSourceKt.getParametersList(emDevice);
			allParamList.forEach(parameter -> {
				if (parameter.isDisplayInUI())
					parameterList.add(parameter);
			});
			String nodeAddress = String.valueOf(emDevice.getSlaveId());
			energyMeterModelDetails.setText(emDevice.getName()+ "("+emDevice.getEquipType().toUpperCase() + nodeAddress + ")");
			GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
			energyMeterParams.setLayoutManager(gridLayoutManager);
			ZoneRecyclerModbusParamAdapter zoneRecyclerModbusParamAdapter =
					new ZoneRecyclerModbusParamAdapter(getContext(), emDevice.getDeviceEquipRef(), parameterList);
			energyMeterParams.setAdapter(zoneRecyclerModbusParamAdapter);
			TextView emrUpdatedTime = view.findViewById(R.id.last_updated_statusEM);
			emrUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
			TextView textViewModule = view.findViewById(R.id.module_status_emr);
			HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
		}

	}

	private EquipmentDevice getModbusEquip(String filter){
		HashMap<Object, Object> equipListMap = CCUHsApi.getInstance().readEntity("equip and modbus and not equipRef and "+filter+" and system");
		if (equipListMap.isEmpty())
			return null;
		return buildModbusModelByEquipRef(Objects.requireNonNull(equipListMap.get("id")).toString());
	}
	private void configBTUMeterDetails(View view){

		EquipmentDevice btuDevice = getModbusEquip("btu");
		if(btuDevice != null) {
			btuMeterParams.setVisibility(View.VISIBLE);
			btuMeterModelDetails.setVisibility(View.VISIBLE);
			moduleStatusBtu.setVisibility(View.VISIBLE);
			lastUpdatedBtu.setVisibility(View.VISIBLE);
			List<Parameter> parameterList = new ArrayList<>();

			List<Parameter> allParamList = UtilSourceKt.getParametersList(btuDevice);
			allParamList.forEach(parameter -> {
				if (parameter.isDisplayInUI())
					parameterList.add(parameter);
			});

			String nodeAddress = String.valueOf(btuDevice.getSlaveId());
			btuMeterModelDetails.setText(btuDevice.getName()+ "("+btuDevice.getEquipType().toUpperCase() + nodeAddress + ")");
			GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
			btuMeterParams.setLayoutManager(gridLayoutManager);
			ZoneRecyclerModbusParamAdapter zoneRecyclerModbusParamAdapter =
					new ZoneRecyclerModbusParamAdapter(getContext(), btuDevice.getDeviceEquipRef(), parameterList);
			btuMeterParams.setAdapter(zoneRecyclerModbusParamAdapter);
			TextView btuUpdatedTime = view.findViewById(R.id.last_updated_statusBTU);
			btuUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
			TextView textViewModule = view.findViewById(R.id.module_status_btu);
			HeartBeatUtil.moduleStatus(textViewModule, nodeAddress);
		}

	}

	private void configWatermark(){
		mainLayout.setBackgroundResource(R.drawable.bg_logoscreen);
		if(!CCUUiUtil.isDaikinEnvironment(requireContext()) || !CCUUiUtil.isCarrierThemeEnabled(requireContext())) {
			mainLayout.setBackground(null);
		}
	}
	private String getOccStatus(){
		HashMap point = CCUHsApi.getInstance().read("point and " +
				"system and ie and occStatus");
		if (!point.isEmpty()) {
			double occStatus = CCUHsApi.getInstance().readHisValById(point.get("id").toString());
			if (occStatus == 0) {
				return "Occupied";
			} else if (occStatus == 1) {
				return "Unoccupied";
			} else {
				return "Tenant Override";
			}
		}
		return "Unoccupied";
	}

	public static void setIntrinsicScheduleListener(IntrinsicScheduleListener listener) {
		intrinsicScheduleListener = listener;
	}
	@Override
	public void refreshData() {
		cloudConnectivityUpdatedTime.setText(HeartBeatUtil.getLastUpdatedTime(Tags.CLOUD));
	}
	private void showExternalModbusDevice() {
		if (L.ccu().systemProfile instanceof DabExternalAhu || L.ccu().systemProfile instanceof VavExternalAhu) {
			HashMap<Object, Object>  modbusEquip = CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu");
			if (!modbusEquip.isEmpty()) {
				externalModbusParams.setVisibility(View.VISIBLE);
				externalModbusModelDetails.setVisibility(View.VISIBLE);
				externalModbusStatus.setVisibility(View.VISIBLE);
				externalModbusLastUpdated.setVisibility(View.VISIBLE);
				external_last_updated.setVisibility(View.VISIBLE);

				EquipmentDevice externalModbusEquip = buildModbusModelByEquipRef(Objects.requireNonNull(modbusEquip.get("id")).toString());
				List<Parameter> parameterList = new ArrayList<>();

				List<Parameter> allParamList = UtilSourceKt.getParametersList(externalModbusEquip);
				allParamList.forEach(parameter -> {
					if (parameter.isDisplayInUI())
						parameterList.add(parameter);
				});

				String nodeAddress = String.valueOf(externalModbusEquip.getSlaveId());
				externalModbusModelDetails.setText(externalModbusEquip.getName()+ "("+externalModbusEquip.getEquipType().toUpperCase() + nodeAddress + ")");
				GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
				externalModbusParams.setLayoutManager(gridLayoutManager);
				ZoneRecyclerModbusParamAdapter zoneRecyclerModbusParamAdapter =
						new ZoneRecyclerModbusParamAdapter(getContext(), externalModbusEquip.getDeviceEquipRef(), parameterList);
				externalModbusParams.setAdapter(zoneRecyclerModbusParamAdapter);
				externalModbusLastUpdated.setText(HeartBeatUtil.getLastUpdatedTime(nodeAddress));
				HeartBeatUtil.moduleStatus(externalModbusStatus, nodeAddress);
			}

		}
	}
}
