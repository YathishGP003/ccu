package a75f.io.renatus.schedules;

import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.logic.bo.util.UnitUtils;
import a75f.io.renatus.R;

public class NamedScheduleUnoccupiedDailog extends DialogFragment {

    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
    private static final String PARAM_DAY = "PARAM_DAY";
    TextView unOccupied;
    TextView title;


    public static NamedScheduleUnoccupiedDailog newInstance(String scheduleId,int day){
        NamedScheduleUnoccupiedDailog namedScheduleDialogFragment = new NamedScheduleUnoccupiedDailog();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        args.putInt(PARAM_DAY, day);
        namedScheduleDialogFragment.setArguments(args);
        return namedScheduleDialogFragment;
    }

    public NamedScheduleUnoccupiedDailog() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.named_schedule_unoccupied, null);
        unOccupied = view.findViewById(R.id.unoccupied);
        title = view.findViewById(R.id.day);

        Schedule schedule = CCUHsApi.getInstance().getScheduleById(getArguments().getString(PARAM_SCHEDULE_ID));
        ArrayList<String> DAYS = new ArrayList<>();
        DAYS.add("MONDAY");
        DAYS.add("TUESDAY");
        DAYS.add("WEDNESDAY");
        DAYS.add("THURSDAY");
        DAYS.add("FRIDAY");
        DAYS.add("SATURDAY");
        DAYS.add("SUNDAY");

        title.setText(DAYS.get(getArguments().getInt(PARAM_DAY))+" | ");
        if(isCelsiusTunerAvailableStatus()){
            unOccupied.setText((int)UnitUtils.fahrenheitToCelsiusRelative(schedule.getUnoccupiedZoneSetback()) + "\u00B0C");
        }else {
            unOccupied.setText(schedule.getUnoccupiedZoneSetback().toString() + "\u00B0F");
        }

        return new AlertDialog.Builder(requireActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }
}
