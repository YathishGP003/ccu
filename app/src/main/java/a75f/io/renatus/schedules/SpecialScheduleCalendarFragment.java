package a75f.io.renatus.schedules;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.joda.time.DateTime;
import org.projecthaystack.HDict;
import org.threeten.bp.LocalDate;

import java.util.List;

import a75f.io.api.haystack.Tags;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.renatus.R;

public class SpecialScheduleCalendarFragment extends DialogFragment{
    private static final int SPECIAL_SCHEDULE_DAYS_LIMIT = 7;
    private static final int DAYS_IN_YEAR = 365;
    private MaterialCalendarView calendarView;
    private HDict specialScheduleHDict;
    private List<CalendarDay> selectedDates;

    public interface SpecialScheduleDateListener{
        void getSpecialScheduleDate(List<CalendarDay> selectedDates);
    }

    private SpecialScheduleDateListener specialScheduleDateListener;

    public SpecialScheduleCalendarFragment(HDict specialScheduleHDict,
                                           SpecialScheduleDateListener specialScheduleDateListener,
                                           List<CalendarDay> selectedDates){
        this.specialScheduleDateListener = specialScheduleDateListener;
        this.specialScheduleHDict = specialScheduleHDict;
        this.selectedDates = selectedDates;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_special_schedule_calendar, null);
        calendarView = view.findViewById(R.id.calendarView);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);

        calendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();
        calendarView.state().edit().setMaximumDate(LocalDate.now().plusDays(DAYS_IN_YEAR- 1L)).commit();
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if(selected) {
                LocalDate localDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
                if(localDate.plusDays(SPECIAL_SCHEDULE_DAYS_LIMIT - 1L).isAfter(LocalDate.now().plusDays(DAYS_IN_YEAR- 1L))){
                    calendarView.state().edit().setMaximumDate(LocalDate.now().plusDays(DAYS_IN_YEAR- 1L)).commit();
                    calendarView.setCurrentDate(date, true);
                }
                else {
                    calendarView.state().edit().setMaximumDate(localDate.plusDays(SPECIAL_SCHEDULE_DAYS_LIMIT - 1L)).commit();
                    calendarView.setCurrentDate(date, true);
                }
            }else {
                calendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();
                calendarView.state().edit().setMaximumDate(LocalDate.now().plusDays(DAYS_IN_YEAR- 1L)).commit();
                calendarView.setCurrentDate(date, true);
            }
        });
        buttonSave.setOnClickListener(saveClick -> processCalendar());
        buttonCancel.setOnClickListener(cancelClick -> dismiss());

        displaySelectedDates();
        return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    private void displaySelectedDates(){
        if(specialScheduleHDict == null && selectedDates.isEmpty()){
            return;
        }
        DateTime startDate = null;
        DateTime endDate = null;
        if(!selectedDates.isEmpty()){
            startDate = new DateTime().withDate(selectedDates.get(0).getYear(), selectedDates.get(0).getMonth(),
                    selectedDates.get(0).getDay());
            endDate =  new DateTime().withDate(selectedDates.get(selectedDates.size() - 1).getYear(),
                    selectedDates.get(selectedDates.size() - 1).getMonth(),
                    selectedDates.get(selectedDates.size() - 1).getDay());

        }
        else if(specialScheduleHDict != null){
            HDict range = (HDict) specialScheduleHDict.get(Tags.RANGE);
            startDate = SpecialSchedule. SS_DATE_TIME_FORMATTER.parseDateTime(range.get(Tags.STDT).toString());
            endDate = SpecialSchedule. SS_DATE_TIME_FORMATTER.parseDateTime(range.get(Tags.ETDT).toString());
        }
        if(startDate == null || endDate == null){
            return;
        }


        CalendarDay startDay = CalendarDay.from(startDate.getYear(), startDate.getMonthOfYear(), startDate.getDayOfMonth());
        CalendarDay endDay = CalendarDay.from(endDate.getYear(), endDate.getMonthOfYear(), endDate.getDayOfMonth());
        calendarView.selectRange(startDay, endDay);
        LocalDate localDate = LocalDate.of(startDate.getYear(), startDate.getMonthOfYear(),
                startDate.getDayOfMonth());
        if(localDate.plusDays(SPECIAL_SCHEDULE_DAYS_LIMIT - 1L).isAfter(LocalDate.now().plusDays(DAYS_IN_YEAR- 1L))){
            calendarView.state().edit().setMaximumDate(LocalDate.now().plusDays(DAYS_IN_YEAR- 1L)).commit();
        }
        else {
            calendarView.state().edit().setMaximumDate(localDate.plusDays(SPECIAL_SCHEDULE_DAYS_LIMIT - 1L)).commit();
        }
        calendarView.setCurrentDate(startDay, true);
    }

    private void processCalendar(){
        List<CalendarDay>  selectedDates = calendarView.getSelectedDates();
        if(selectedDates.isEmpty()){
            Toast.makeText(SpecialScheduleCalendarFragment.this.getContext(), "Please select date for Special " +
                            "Schedule.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(selectedDates.size() > SPECIAL_SCHEDULE_DAYS_LIMIT){
            Toast.makeText(SpecialScheduleCalendarFragment.this.getContext(), "Special schedule should not be " +
                            "greater than 7 days.", Toast.LENGTH_LONG).show();
            return;
        }
        specialScheduleDateListener.getSpecialScheduleDate(selectedDates);
        dismiss();
    }

}
