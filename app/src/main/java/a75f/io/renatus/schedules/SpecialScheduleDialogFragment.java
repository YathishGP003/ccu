package a75f.io.renatus.schedules;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.projecthaystack.HDict;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.renatus.R;
import a75f.io.renatus.util.TimeUtils;
import a75f.io.renatus.views.RangeBarView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SpecialScheduleDialogFragment extends DialogFragment {
    private NumberPicker npStartTime;
    private NumberPicker npEndTime;
    private RangeBarView rangeSeekBarView;
    private EditText specialScheduleName;
    private EditText textSpecialScheduleDate;

    private static final String TAG = "SPECIAL_SCHEDULE_UI";
    private int nMinVal = 0;
    private int nMaxVal = 96;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy", Locale.ENGLISH);
    private List<CalendarDay> selectedDates;
    private String roomRef;
    private HDict specialScheduleHDict;
    private String specialScheduleId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public SpecialScheduleDialogFragment(String specialScheduleId, String roomRef,
                                         SpecialScheduleDialogListener specialScheduleDialogListener){
        selectedDates = new ArrayList<>();
        selectedDates.add(CalendarDay.today());
        this.roomRef = roomRef;
        this.specialScheduleDialogListener = specialScheduleDialogListener;
        this.specialScheduleId = specialScheduleId;
        specialScheduleHDict = null;
        if(!StringUtils.isEmpty(specialScheduleId)){
            specialScheduleHDict = CCUHsApi.getInstance().getScheduleDictById(specialScheduleId);
            selectedDates.clear();
        }
    }
    private Schedule getScheduleForSpecialSchedule(String roomRef){
        if(!StringUtils.isEmpty(roomRef)){
            return CCUHsApi.getInstance().getZoneSchedule(roomRef, false).get(0);
        }
        return CCUHsApi.getInstance().getSystemSchedule(false).get(0);
    }
    public interface SpecialScheduleDialogListener{
        void onClickSave(String scheduleName, DateTime startDate, DateTime endDate,
                         double coolVal, double heatVal);
    }

    private SpecialScheduleDialogListener specialScheduleDialogListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_special_schedule_dialog, null);
        npStartTime = view.findViewById(R.id.np1);
        npEndTime = view.findViewById(R.id.np2);
        Button calendarSpecialSchedule = view.findViewById(R.id.calendarSpecialSchedule);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        rangeSeekBarView = view.findViewById(R.id.rangeSeekBar);
        rangeSeekBarView.setZoneSchedule(getScheduleForSpecialSchedule(roomRef));
        specialScheduleName = view.findViewById(R.id.editText_specialScheduleName);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        textSpecialScheduleDate = view.findViewById(R.id.textSpecialScheduleDate);
        textSpecialScheduleDate.setEnabled(false);

        calendarSpecialSchedule.setOnClickListener(specialScheduleCal -> showSpecialScheduleCalDialog());
        buttonSave.setOnClickListener(saveClick -> saveSpecialSchedule());
        buttonCancel.setOnClickListener(cancelClick -> dismiss());

        npStartTime.setMinValue(nMinVal);
        npStartTime.setMaxValue(nMaxVal);

        npStartTime.setValue(32);
        npStartTime.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npStartTime.setVisibility(View.VISIBLE);
        npStartTime.setWrapSelectorWheel(false);
        npStartTime.setFormatter(TimeUtils::valToTime);
        npStartTime.setOnLongClickListener(view13 -> true);
        npStartTime.setOnTouchListener((view16, motionEvent) -> false);
        npStartTime.setOnClickListener(view15 -> {});

        try {
            Method method = npStartTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npStartTime, true);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Log.e(TAG, "Reflection error for start time");
        }

        npEndTime.setMinValue(nMinVal);
        npEndTime.setMaxValue(nMaxVal);
        npEndTime.setValue(70);
        npEndTime.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npEndTime.setVisibility(View.VISIBLE);
        npEndTime.setWrapSelectorWheel(false);
        npEndTime.setOnLongClickListener(view13 -> true);
        npEndTime.setOnTouchListener((view14, motionEvent) -> false);
        npEndTime.setOnClickListener(view15 -> {});
        npEndTime.setFormatter(TimeUtils::valToTime);
        Date date =Calendar.getInstance().getTime();
        String beginDateString = simpleDateFormat.format(date);
        textSpecialScheduleDate.setText(beginDateString +" to "+ beginDateString);

        try {
            Method method = npEndTime.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(npEndTime, true);
        }  catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Log.e(TAG, "Reflection error for end time");
        }

        displaySelectedSpecialSchedule();
        return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    private void displaySelectedSpecialSchedule(){
        if(specialScheduleHDict == null){
            return;
        }
        displaySelectedSpecialScheduleName();
        HDict range = (HDict) specialScheduleHDict.get(Tags.RANGE);
        displaySelectedTemperatures(range);
        displaySelectedTime(range);
        displaySelectedDate(range);
    }

    private void displaySelectedSpecialScheduleName(){
        String scheduleName = specialScheduleHDict.get(Tags.DIS).toString();
        specialScheduleName.setText(scheduleName);
        specialScheduleName.setSelection(scheduleName.length());
    }

    private void displaySelectedTemperatures(HDict range){
        new CountDownTimer(150, 150) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                rangeSeekBarView.setLowerCoolingTemp(Float.parseFloat(range.get(Tags.COOLVAL).toString()));
                rangeSeekBarView.setLowerHeatingTemp(Float.parseFloat(range.get(Tags.HEATVAL).toString()));
            }
        }.start();
    }

    private void displaySelectedTime(HDict range){
        npStartTime.setValue(SpecialSchedule.getInt(range.get(Tags.STHH).toString()) * 4 + SpecialSchedule.getInt(range.get(Tags.STMM).toString()) / 15);
        npEndTime.setValue(SpecialSchedule.getInt(range.get(Tags.ETHH).toString()) * 4 + SpecialSchedule.getInt(range.get(Tags.ETMM).toString()) / 15);
    }
    private void displaySelectedDate(HDict range){
        String beginDateString = range.get(Tags.STDT).toString();
        String endDateString = range.get(Tags.ETDT).toString();
        DateTime beginDateTime = SpecialSchedule. SS_DATE_TIME_FORMATTER.parseDateTime(beginDateString);
        DateTime endDateTime = SpecialSchedule. SS_DATE_TIME_FORMATTER.parseDateTime(endDateString);
        CalendarDay startDay = CalendarDay.from(beginDateTime.getYear(), beginDateTime.getMonthOfYear(), beginDateTime.getDayOfMonth());
        CalendarDay endDay = CalendarDay.from(endDateTime.getYear(), endDateTime.getMonthOfYear(), endDateTime.getDayOfMonth());
        selectedDates.add(startDay);
        selectedDates.add(endDay);
        try {
            textSpecialScheduleDate.setText(simpleDateFormat.format(dateFormat.parse(beginDateString)) +
                    " to "+ simpleDateFormat.format(dateFormat.parse(endDateString)));
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void displaySpecialScheduleDate(List<CalendarDay> selectedDates){
        this.selectedDates = selectedDates;
        int selectedDatesSize = selectedDates.size();
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDates.get(0).getYear(), selectedDates.get(0).getMonth()-1,
                selectedDates.get(0).getDay());
        Date date =calendar.getTime();
        String beginDateString = simpleDateFormat.format(date);
        if(selectedDates.size() == 1){
            textSpecialScheduleDate.setText(beginDateString +" to "+ beginDateString);
        }
        if(selectedDates.size() > 1){
            calendar.set(selectedDates.get(selectedDatesSize-1).getYear(),
                    selectedDates.get(selectedDatesSize-1).getMonth()-1,
                    selectedDates.get(selectedDatesSize-1).getDay());
            date =calendar.getTime();
            textSpecialScheduleDate.setText(beginDateString +" to "+ simpleDateFormat.format(date));
        }
    }
    private void showSpecialScheduleCalDialog(){
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment specialScheduleCalFragment = getChildFragmentManager().findFragmentByTag("popup");
        if(specialScheduleCalFragment != null){
            fragmentTransaction.remove(specialScheduleCalFragment);
        }
        SpecialScheduleCalendarFragment specialScheduleCalendarFragment =
                new SpecialScheduleCalendarFragment(specialScheduleHDict,
                        SpecialScheduleDialogFragment.this::displaySpecialScheduleDate);
        specialScheduleCalendarFragment.show(fragmentTransaction, "popup");
    }

    private boolean isSpecialScheduleNameModified(String scheduleName){
        if(specialScheduleHDict == null){
            return false;
        }
        return specialScheduleHDict.get(Tags.DIS).toString().equalsIgnoreCase(scheduleName);
    }
    AlertDialog alert;
    private void saveSpecialSchedule(){
        String scheduleName  = specialScheduleName.getText().toString().trim();
        if(scheduleName.isEmpty()){
            Toast.makeText(SpecialScheduleDialogFragment.this.getContext(), "Please provide Special Schedule Name.",
                    Toast.LENGTH_SHORT).show();
           return;
        }

        int startHour = (npStartTime.getValue() - (npStartTime.getValue() % 4)) / 4;
        int startMinutes = (npStartTime.getValue() % 4) * 15;

        int endHour = (npEndTime.getValue() - (npEndTime.getValue() % 4)) / 4;
        int endMinutes = (npEndTime.getValue() % 4) * 15;

        if ((endHour < startHour) || (endHour == startHour && endMinutes <= startMinutes)) {
            Toast.makeText(SpecialScheduleDialogFragment.this.getContext(), "End time should be greater than Start " +
                            "time",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if(textSpecialScheduleDate.getText().toString().isEmpty()){
            Toast.makeText(SpecialScheduleDialogFragment.this.getContext(), "Please provide date for Special Schedule",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if(endHour == 24){
            endHour = 23;
            endMinutes = 59;
        }
        DateTime startDate = new DateTime().withDate(selectedDates.get(0).getYear(), selectedDates.get(0).getMonth(),
                selectedDates.get(0).getDay()).withTime(startHour, startMinutes, 0, 0);
        DateTime endDate =  new DateTime().withDate(selectedDates.get(selectedDates.size() - 1).getYear(),
                selectedDates.get(selectedDates.size() - 1).getMonth(),
                selectedDates.get(selectedDates.size() - 1).getDay()).withTime(endHour, endMinutes, 0, 0);

        if(endDate.getMillis() < new Date().getTime()){
            Toast.makeText(SpecialScheduleDialogFragment.this.getContext(), "End time cannot be lesser than current time",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        List<HashMap<Object, Object>> specialScheduleList = CCUHsApi.getInstance().getSpecialSchedules(roomRef);
        if(!isSpecialScheduleNameModified(scheduleName) && !SpecialSchedule.isSpecialScheduleNameAvailable(scheduleName,
                specialScheduleList)){
            Toast.makeText(SpecialScheduleDialogFragment.this.getContext(), "Special Schedule name exists.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if(!StringUtils.isEmpty(specialScheduleId)){
            boolean found = false;
            int index = 0;
            for(HashMap<Object, Object> specialSchedule : specialScheduleList){
                if(specialSchedule.get(Tags.ID).toString().equals(specialScheduleId)){
                    found = true;
                    break;
                }
                index++;
            }
            if(found){
                specialScheduleList.remove(index);
            }
        }

        MultiValuedMap<String, String> overlapSchedules = SpecialSchedule.getListOfOverlapSpecialSchedules(startDate,
                endDate, specialScheduleList);
        if(!overlapSchedules.isEmpty()){
            StringBuilder overlapMessage = new StringBuilder();
            for (Map.Entry<String, String> overlapEntry: overlapSchedules.entries()) {
                overlapMessage.append(overlapEntry.getKey()+" : "+overlapEntry.getValue());
                overlapMessage.append("\n");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage("The special schedule "+scheduleName+" cannot be applied as it is overlapping with " +
                    "the below special schedules." +"\n\n"+overlapMessage.toString())
                    .setCancelable(false)
                    .setTitle("Special Schedule Overlaps")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("OK", (dialogInterface, i) -> alert.dismiss());
            alert = builder.create();
            alert.show();
            return;
        }
        specialScheduleDialogListener.onClickSave(scheduleName, startDate, endDate,
                rangeSeekBarView.getCoolValue(), rangeSeekBarView.getHeatValue());
        dismiss();
    }
}