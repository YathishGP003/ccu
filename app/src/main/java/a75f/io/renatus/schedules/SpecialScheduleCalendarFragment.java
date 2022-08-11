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

    public interface SpecialScheduleDateListener{
        void getSpecialScheduleDate(List<CalendarDay> selectedDates);
    }

    private SpecialScheduleDateListener specialScheduleDateListener;

    public SpecialScheduleCalendarFragment(HDict specialScheduleHDict, SpecialScheduleDateListener specialScheduleDateListener){
        this.specialScheduleDateListener = specialScheduleDateListener;
        this.specialScheduleHDict = specialScheduleHDict;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_special_schedule_calendar, null);
        calendarView = view.findViewById(R.id.calendarView);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);

        calendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();
        calendarView.state().edit().setMaximumDate(LocalDate.now().plusDays(DAYS_IN_YEAR-1l)).commit();
        calendarView.setSelectedDate(CalendarDay.today());
        buttonSave.setOnClickListener(saveClick -> processCalendar());
        buttonCancel.setOnClickListener(cancelClick -> dismiss());

        displaySelectedDates();
        return new AlertDialog.Builder(getActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    private void displaySelectedDates(){
        if(specialScheduleHDict == null){
            return;
        }
        HDict range = (HDict) specialScheduleHDict.get(Tags.RANGE);
        DateTime beginDateTime = SpecialSchedule. SS_DATE_TIME_FORMATTER.parseDateTime(range.get(Tags.STDT).toString());
        DateTime endDateTime = SpecialSchedule. SS_DATE_TIME_FORMATTER.parseDateTime(range.get(Tags.ETDT).toString());
        if(beginDateTime.isBeforeNow()) {
            calendarView.state().edit().setMinimumDate(CalendarDay.from(beginDateTime.getYear(),
                    beginDateTime.getMonthOfYear(), beginDateTime.getDayOfMonth())).commit();
        }
        else{
            calendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();
        }
        CalendarDay startDay = CalendarDay.from(beginDateTime.getYear(), beginDateTime.getMonthOfYear(), beginDateTime.getDayOfMonth());
        CalendarDay endDay = CalendarDay.from(endDateTime.getYear(), endDateTime.getMonthOfYear(), endDateTime.getDayOfMonth());
        calendarView.selectRange(startDay, endDay);
        calendarView.state().edit().setMaximumDate(LocalDate.now().plusDays(DAYS_IN_YEAR-1l)).commit();
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
