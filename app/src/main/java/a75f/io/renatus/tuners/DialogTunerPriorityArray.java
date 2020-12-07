package a75f.io.renatus.tuners;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import butterknife.ButterKnife;


public class DialogTunerPriorityArray extends BaseDialogFragment implements PriorityItemClickListener {
    public static final String ID = DialogTunerPriorityArray.class.getSimpleName();
    public static final String TUNER_ITEM = "tunerItem";
    public static final String TUNER_GROUP_ITEM = "TunerGroupItem";
    HashMap tunerItemSelected = null;
    TunerGroupItem tunerGroupSelected = null;
    RecyclerView recyclerViewPriority;
    TextView textTunerGroupTitle;
    TextView textTunerName;
    TextView textTunerDefaultValue;
    Button buttonSaveTuner;
    Button buttonCancel;

    public DialogTunerPriorityArray() {
    }

    public static DialogTunerPriorityArray newInstance(HashMap tunerItem, TunerGroupItem tunerGroupItem) {
        DialogTunerPriorityArray priorityArrayFragment = new DialogTunerPriorityArray();
        Bundle bundle = new Bundle();
        bundle.putSerializable(TUNER_ITEM, tunerItem);
        bundle.putSerializable(TUNER_GROUP_ITEM, tunerGroupItem);
        priorityArrayFragment.setArguments(bundle);
        return priorityArrayFragment;
    }

    @Override
    public String getIdString() {
        return ID;
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = 1165;//ViewGroup.LayoutParams.WRAP_CONTENT;
            int height = 672;//ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(TUNER_ITEM)) {
            tunerItemSelected = (HashMap) getArguments().getSerializable(TUNER_ITEM);
            tunerGroupSelected = (TunerGroupItem) getArguments().getSerializable(TUNER_GROUP_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_tuner_priority, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        ButterKnife.bind(this, view);
        recyclerViewPriority = view.findViewById(R.id.recyclerPriority);
        textTunerGroupTitle = view.findViewById(R.id.textTunerGroupName);
        textTunerName = view.findViewById(R.id.textTunerName);
        textTunerDefaultValue = view.findViewById(R.id.textTunerDefaultValue);
        buttonSaveTuner = view.findViewById(R.id.buttonSaveTuner);
        buttonCancel = view.findViewById(R.id.buttonCancelTuner);

        recyclerViewPriority.setLayoutManager(new LinearLayoutManager(getActivity()));
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        String tunerName = tunerItemSelected.get("dis").toString();
        if (tunerItemSelected.containsKey("unit")) {
            textTunerName.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1) + " (" + tunerItemSelected.get("unit").toString().toUpperCase() + ")");
            textTunerDefaultValue.setText("" + getTunerValue(tunerItemSelected.get("id").toString()) + " (" + tunerItemSelected.get("unit").toString().toUpperCase() + ")");
        } else {
            textTunerName.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1));
            textTunerDefaultValue.setText("" + getTunerValue(tunerItemSelected.get("id").toString()));
        }

        textTunerGroupTitle.setText(tunerGroupSelected.getName());

        ArrayList<HashMap> priorityList = CCUHsApi.getInstance().readPoint(tunerItemSelected.get("id").toString());
        Log.i("TunersUI", "priorityList:" + priorityList);
        PriorityArrayAdapter priorityArrayAdapter = new PriorityArrayAdapter(getActivity(), priorityList, this);
        recyclerViewPriority.setAdapter(priorityArrayAdapter);

        buttonSaveTuner.setOnClickListener(v -> {
        });
        buttonCancel.setOnClickListener(v -> dismiss());


    }


    public double getTunerValue(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    @Override
    public void priorityClicked(int position) {
        if (position == 13) {
            LayoutInflater inflater = this.getLayoutInflater();

            View dialogView = inflater.inflate(R.layout.dialog_tuner_range, null);
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setView(dialogView);
            AlertDialog valueDialog = dialog.show();
            valueDialog.getWindow().setLayout(500, 380);
            valueDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            NumberPicker npTunerRange = dialogView.findViewById(R.id.npTunerValue);
            TextView textViewLevel = dialogView.findViewById(R.id.textLevelLabel);
            String text = "<font color=#999999>Level 14</font> <font color=#E24301>System</font>";
            textViewLevel.setText(Html.fromHtml(text));

            if (tunerItemSelected.containsKey("minVal") && tunerItemSelected.containsKey("maxVal")) {

                int currentValue = (int) getTunerValue(tunerItemSelected.get("id").toString());
                int minValue = (int) (Double.parseDouble(tunerItemSelected.get("minVal").toString()));
                int maxValue = (int) (Double.parseDouble(tunerItemSelected.get("maxVal").toString()));
                int incrementVal = (int) (Double.parseDouble(tunerItemSelected.get("incrementVal").toString()));

                double currentValueDb =  getTunerValue(tunerItemSelected.get("id").toString());
                double minValueDb =  (Double.parseDouble(tunerItemSelected.get("minVal").toString()));
                double maxValueDb = (Double.parseDouble(tunerItemSelected.get("maxVal").toString()));
                double incrementValDb = (Double.parseDouble(tunerItemSelected.get("incrementVal").toString()));

                Log.i("TunersUI", "currentValue:" + currentValue + " minValue:" + minValue + " maxValue:" + maxValue + " incVal:" + incrementVal);

                ArrayList<String> valueList = new ArrayList<>();
                if (incrementValDb == 0) {
                    incrementValDb = 1.0;
                }
                int k = 0;
                int currentValPos = 0;
                for (double i = minValueDb; i <= maxValueDb; i += incrementValDb) {
                    valueList.add(String.valueOf(i));
                    if (currentValue == i) {
                        currentValPos = k;
                    }
                    k++;
                }
                Log.i("TunersUI", " currentValPos:" + currentValPos + " value:" + valueList.get(currentValPos) + " valueList:" + valueList);
                npTunerRange.setMinValue(minValue);
                if (maxValue > 0) {
                    Log.i("TunersUI", "maxValue > 0:" + " maxValue:" + (maxValue / incrementVal));
                    npTunerRange.setMaxValue(maxValue / incrementVal);
                }
                Log.i("TunersUI", "valueList :" + valueList);
                npTunerRange.setDisplayedValues(valueList.toArray(new String[valueList.size()]));
                npTunerRange.setValue(currentValPos);
                if (currentValue > maxValueDb || currentValue < minValueDb) {
                    Log.i("TunersUI", "currentValue > maxValue:" + maxValueDb + " incrementVal:" + incrementValDb + " currentValue:" + currentValueDb);
                    npTunerRange.setValue(maxValue / incrementVal);
                }
                npTunerRange.setWrapSelectorWheel(false);
                npTunerRange.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            } else {

            }

            //dialog.show();
        }
    }
}
