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

        Schedule schedule = CCUHsApi.getInstance().getScheduleById(getArguments().getString(PARAM_SCHEDULE_ID));

        if(isCelsiusTunerAvailableStatus()){
            unOccupied.setText((int)UnitUtils.fahrenheitToCelsiusRelative(schedule.getUnoccupiedZoneSetback()) + "°C");
        }else {
            long value= Math.round(schedule.getUnoccupiedZoneSetback());
            unOccupied.setText(value + "°F");
        }

        return new AlertDialog.Builder(requireActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }
}
