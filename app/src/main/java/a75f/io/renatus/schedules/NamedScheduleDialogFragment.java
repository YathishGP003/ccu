package a75f.io.renatus.schedules;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import a75f.io.logic.schedule.ScheduleGroup;
import a75f.io.renatus.R;

import static a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus;

public class NamedScheduleDialogFragment extends DialogFragment {

    private static final String PARAM_SCHEDULE_ID = "PARAM_SCHEDULE_ID";
    private static final String PARAM_DAY = "DAY";

    TextView day;
    TextView heatingDesiredTemp;
    TextView coolingDesiredTemp;
    TextView heatinguserLimit;
    TextView coolingUserLimit;
    TextView heatingDeadband;
    TextView coolingDeadBand;

    ArrayList<String> DAYS = new ArrayList<>();

    public static NamedScheduleDialogFragment newInstance(String scheduleId, int tag){
        NamedScheduleDialogFragment namedScheduleDialogFragment = new NamedScheduleDialogFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_SCHEDULE_ID, scheduleId);
        args.putString(PARAM_DAY, String.valueOf(tag));
        namedScheduleDialogFragment.setArguments(args);
        return namedScheduleDialogFragment;
    }

    public NamedScheduleDialogFragment() {

    }


    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.named_schedule_occupied, null);

        Schedule schedule = CCUHsApi.getInstance().getScheduleById(getArguments().getString(PARAM_SCHEDULE_ID));
        Schedule.Days days = schedule.getDays().get(Integer.parseInt(getArguments().getString(PARAM_DAY)));

        DAYS.add("MONDAY");
        DAYS.add("TUESDAY");
        DAYS.add("WEDNESDAY");
        DAYS.add("THURSDAY");
        DAYS.add("FRIDAY");
        DAYS.add("SATURDAY");
        DAYS.add("SUNDAY");

        day = view.findViewById(R.id.day);
        heatingDesiredTemp = view.findViewById(R.id.heatingDesired);
        coolingDesiredTemp = view.findViewById(R.id.coolingDesired);
        heatinguserLimit = view.findViewById(R.id.heatingUserLimit);
        coolingUserLimit = view.findViewById(R.id.coolingUserLimit);
        heatingDeadband = view.findViewById(R.id.heatingDeadband);
        coolingDeadBand = view.findViewById(R.id.coolingDeadband);

        String header = ScheduleUtil.getNamedScheduleHeader(schedule.getScheduleGroup(),
                days.getDay()) + "(" + days.getSthh() +":" +days.getStmm() +
                " to "+ days.getEthh() +":" +days.getEtmm() +  ") | "   ;

        day.setText(header);
        if(isCelsiusTunerAvailableStatus()){
            heatingDesiredTemp.setText(UnitUtils.fahrenheitToCelsius(days.getHeatingVal()) + "°C");
            coolingDesiredTemp.setText(UnitUtils.fahrenheitToCelsius(days.getCoolingVal()) + "°C");
            heatinguserLimit.setText(UnitUtils.fahrenheitToCelsius(days.getHeatingUserLimitMin()) + "°C" + " / " + UnitUtils.fahrenheitToCelsius(days.getHeatingUserLimitMax()) + "°C");
            coolingUserLimit.setText(UnitUtils.fahrenheitToCelsius(days.getCoolingUserLimitMin() )+ "°C" + " / " + UnitUtils.fahrenheitToCelsius(days.getCoolingUserLimitMax()) + "°C");
            heatingDeadband.setText(UnitUtils.fahrenheitToCelsiusRelative(days.getHeatingDeadBand()) + "°C");
            coolingDeadBand.setText(UnitUtils.fahrenheitToCelsiusRelative(days.getCoolingDeadBand()) + "°C");
        }else {
            heatingDesiredTemp.setText(days.getHeatingVal().toString() + "°F");
            coolingDesiredTemp.setText(days.getCoolingVal().toString() + "°F");
            heatinguserLimit.setText(days.getHeatingUserLimitMin() + "°F" + " / " + days.getHeatingUserLimitMax() + "°F");
            coolingUserLimit.setText(days.getCoolingUserLimitMin() + "°F" + " / " + days.getCoolingUserLimitMax() + "°F");
            heatingDeadband.setText(days.getHeatingDeadBand().toString() + "°F");
            coolingDeadBand.setText(days.getCoolingDeadBand().toString() + "°F");
        }







        return new AlertDialog.Builder(requireActivity(), R.style.NewDialogStyle)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();

        if (dialog != null) {
            int width = 850;
            int height = 646;
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            dialog.getWindow().setLayout(width, height);
        }
    }
}
