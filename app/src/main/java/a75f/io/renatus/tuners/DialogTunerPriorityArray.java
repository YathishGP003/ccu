package a75f.io.renatus.tuners;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.util.TunerNumberPicker;
import a75f.io.renatus.views.NumberPicker.SystemNumberPicker;
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
    String selectedTunerValue;

    PriorityArrayAdapter priorityArrayAdapter;
    ArrayList<HashMap> priorityList;

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
        buttonSaveTuner.setEnabled(false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        String tunerName = tunerItemSelected.get("dis").toString();
        if (tunerItemSelected.containsKey("unit")) {
            textTunerName.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1) + " (" + tunerItemSelected.get("unit").toString().toUpperCase() + ")");
            textTunerDefaultValue.setText(getTunerDefaultValue(tunerItemSelected.get("id").toString()) + " (" + tunerItemSelected.get("unit").toString().toUpperCase() + ")");
        } else {
            textTunerName.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1));
            textTunerDefaultValue.setText("" + getTunerDefaultValue(tunerItemSelected.get("id").toString()));
        }

        textTunerGroupTitle.setText(tunerGroupSelected.getName());
        priorityList = new ArrayList<>();
        priorityList = CCUHsApi.getInstance().readPoint(tunerItemSelected.get("id").toString());
        Log.i("TunersUI", "priorityList:" + priorityList);
        priorityArrayAdapter = new PriorityArrayAdapter(getActivity(), priorityList, this);
        recyclerViewPriority.setAdapter(priorityArrayAdapter);
      //  recyclerViewPriority.scrollToPosition(priorityList.size() - 1);

        buttonSaveTuner.setOnClickListener(v -> {
            Intent tunerValue = new Intent()
                    .putExtra("Tuner_HashMap_Selected", (Serializable) tunerItemSelected)
                    .putExtra("Tuner_Group_Selected", (Serializable) tunerGroupSelected)
                    .putExtra("Tuner_Value_Selected", selectedTunerValue);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, tunerValue);
            dismiss();
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

    public double getTunerDefaultValue(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("level").toString().equals("17") && valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    @Override
    public void priorityClicked(int position) {
        if (position == 13 || position == 7) {
            LayoutInflater inflater = this.getLayoutInflater();

            View dialogView = inflater.inflate(R.layout.dialog_tuner_range, null);
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setView(dialogView);
            AlertDialog valueDialog = dialog.show();
            valueDialog.getWindow().setLayout(500, 380);
            valueDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            TunerNumberPicker npTunerRange = dialogView.findViewById(R.id.npTunerValue);
            TextView textViewLevel = dialogView.findViewById(R.id.textLevelLabel);
            Button buttonSaveAlert = dialogView.findViewById(R.id.buttonSaveTuner);
            Button buttonCancelAlert = dialogView.findViewById(R.id.buttonCancelTuner);
            ImageButton buttonUndo = dialogView.findViewById(R.id.imgBtnUndo);
            buttonUndo.setVisibility(View.GONE);
            buttonSaveAlert.setEnabled(false);
            String levelName = (position == 13) ? "System" : "Equip";
            String text = "Level "+(position +1)+" "+levelName;
            text = text.replaceAll("System","<font color='#E24301'>System</font>");
            text = text.replaceAll("Equip","<font color='#E24301'>Equip</font>");
            textViewLevel.setText(Html.fromHtml(text));

            if (tunerItemSelected.containsKey("minVal") && tunerItemSelected.containsKey("maxVal")) {

                int currentValue = (int) getTunerValue(tunerItemSelected.get("id").toString());
                int minValue = (int) (Double.parseDouble(tunerItemSelected.get("minVal").toString()));
                int maxValue = (int) (Double.parseDouble(tunerItemSelected.get("maxVal").toString()));
                int incrementVal = (int) (Double.parseDouble(tunerItemSelected.get("incrementVal").toString()));

                DecimalFormat df = new DecimalFormat("##.#");

                double currentValueDb = (getTunerValue(tunerItemSelected.get("id").toString()));
                double minValueDb = (Double.parseDouble((tunerItemSelected.get("minVal").toString())));
                double maxValueDb = (Double.parseDouble((tunerItemSelected.get("maxVal").toString())));
                double incrementValDb = (Double.parseDouble(tunerItemSelected.get("incrementVal").toString()));

                Log.i("TunersUI", "currentValue:" + currentValue + " minValue:" + minValue + " maxValue:" + maxValue + " incVal:" + incrementVal);

                ArrayList<String> valueList = new ArrayList<>();
                if (incrementValDb == 0) {
                    incrementValDb = 1.0;
                    incrementVal = 1;
                }
                int currentValPos = 0;
                if (minValue < 0) {
                    for (double i = minValueDb; i <= maxValueDb; i += incrementValDb) {
                        valueList.add(String.format("%.1f", i));
                    }
                    for (String currVal : valueList){
                        if (currentValueDb == Double.parseDouble(currVal)){
                            currentValPos = valueList.indexOf(currVal) + 1;
                            break;
                        }
                    }
                } else {
                    for (double i = minValueDb; i <= maxValueDb; i += incrementValDb) {
                        valueList.add(String.format("%.1f", i));
                    }
                    for (String currVal : valueList){
                        if (currentValueDb == Double.parseDouble(currVal)){
                            currentValPos = valueList.indexOf(currVal);
                            break;
                        }
                    }
                }
                Log.i("TunersUI", " currentValPos:" + currentValPos + " value:" + valueList.get(currentValPos) + " valueList:" + valueList);
                npTunerRange.setDisplayedValues(valueList.toArray(new String[valueList.size()]));
                npTunerRange.setMinValue(minValue);
                if (maxValue > 0) {
                    try {
                        Log.i("TunersUI", "maxValue > 0:" + "incrementVal" + incrementVal + " maxValue:" + (maxValue / incrementVal));
                        npTunerRange.setMaxValue(valueList.size() -1);
                    } catch (ArithmeticException e) {
                        npTunerRange.setMaxValue(valueList.size() -1);
                        Log.i("TunersUI", "ArithmeticException :" + e.getMessage());
                        e.printStackTrace();
                    }
                } else{
                    npTunerRange.setMaxValue(maxValue);
                }


                npTunerRange.setValue(currentValPos);
                Log.i("TunersUI", "valueList :" + Arrays.toString(npTunerRange.getDisplayedValues()));

                npTunerRange.setWrapSelectorWheel(false);
                npTunerRange.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                double finalIncrementValDb = incrementValDb;
                npTunerRange.setOnValueChangedListener((numberPicker, oldValue, newValue) ->
                {
                    Toast.makeText(getActivity(), "TunersUI-oldValue:" + oldValue + " newValue:" + newValue, Toast.LENGTH_SHORT).show();
                    if (oldValue != newValue) {
                        buttonSaveAlert.setEnabled(true);
                        buttonSaveAlert.setTextColor(getActivity().getColor(R.color.orange_75f));
                        buttonUndo.setVisibility(View.VISIBLE);
                        if (newValue < 0){
                            selectedTunerValue = String.valueOf(newValue * finalIncrementValDb);
                        } else {
                            double selectedValue = Double.parseDouble(valueList.get(newValue));
                            selectedTunerValue = new DecimalFormat("##.#").format(selectedValue);
                        }

                    }
                });
                int finalCurrentValPos = currentValPos;
                buttonUndo.setOnClickListener(v -> npTunerRange.setValue(finalCurrentValPos)
                );
                buttonCancelAlert.setOnClickListener(v -> valueDialog.dismiss());
                buttonSaveAlert.setOnClickListener(v -> {
                    if (npTunerRange.getValue() < 0){
                        selectedTunerValue = String.valueOf(npTunerRange.getValue() * finalIncrementValDb);
                    } else {
                        double selectedValue = Double.parseDouble(valueList.get(npTunerRange.getValue()));
                        selectedTunerValue = new DecimalFormat("##.#").format(selectedValue);
                    }
                            //tunerItemSelected.put("newValue", selectedTunerValue);
                            HashMap newValue = (HashMap) priorityList.get(position);
                            newValue.put("newValue", selectedTunerValue);
                            priorityList.set(position, newValue);
                            priorityArrayAdapter.notifyItemChanged(position);
                            valueDialog.dismiss();
                            buttonSaveTuner.setEnabled(true);
                            buttonSaveTuner.setTextColor(getActivity().getColor(R.color.orange_75f));
                        }
                );
            } else {

            }
            //dialog.show();
        }
    }
}
