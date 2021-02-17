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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.util.TunerNumberPicker;
import butterknife.ButterKnife;


public class DialogTunerPriorityArray extends BaseDialogFragment implements PriorityItemClickListener,TunerUndoClickListener {
    public static final String ID = DialogTunerPriorityArray.class.getSimpleName();
    public static final String TUNER_ITEM = "tunerItem";
    public static final String TUNER_GROUP_ITEM = "TunerGroupItem";
    public static final String TUNER_GROUP_TYPE = "TunerGroupType";
    HashMap tunerItemSelected = null;
    TunerGroupItem tunerGroupSelected = null;
    RecyclerView recyclerViewPriority;
    TextView textTunerGroupTitle;
    TextView textTunerName;
    TextView textTunerDefaultValue;
    TextView textLabelBuilding;
    TextView textLabelCCU;
    Button buttonSaveTuner;
    Button buttonCancel;
    String selectedTunerValue;
    String selectedTunerLevel;
    String tunerGroupType;

    PriorityArrayAdapter priorityArrayAdapter;
    ArrayList<HashMap> priorityList;
    HashMap revertMap = new HashMap();

    public DialogTunerPriorityArray() {
    }

    public static DialogTunerPriorityArray newInstance(HashMap tunerItem, String tunerGroupType, TunerGroupItem tunerGroupItem) {
        DialogTunerPriorityArray priorityArrayFragment = new DialogTunerPriorityArray();
        Bundle bundle = new Bundle();
        bundle.putSerializable(TUNER_ITEM, tunerItem);
        bundle.putSerializable(TUNER_GROUP_ITEM, tunerGroupItem);
        bundle.putSerializable(TUNER_GROUP_TYPE, tunerGroupType);
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
            tunerGroupType = (String)getArguments().getSerializable(TUNER_GROUP_TYPE);
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
        textLabelBuilding = view.findViewById(R.id.textLabelBuilding);
        textLabelCCU = view.findViewById(R.id.textLabelCCU);

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
        HashMap site = CCUHsApi.getInstance().read("site");
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (tunerGroupType.contains("Zone")) {
            textLabelBuilding.setText(ccu.get("dis").toString());
            textLabelCCU.setText(HSUtil.getDis(tunerItemSelected.get("roomRef").toString()));
        } else if (tunerGroupType.contains("Module")) {
            textLabelBuilding.setText(ccu.get("dis").toString());
            textLabelCCU.setText(HSUtil.getDis(tunerItemSelected.get("equipRef").toString()));
        } else {
            textLabelBuilding.setText(site.get("dis").toString());
            textLabelCCU.setText(ccu.get("dis").toString());
        }
        textTunerGroupTitle.setText(tunerGroupSelected.getName());
        priorityList = new ArrayList<>();
        priorityList = CCUHsApi.getInstance().readPoint(tunerItemSelected.get("id").toString());
        Log.i("TunersUI", "priorityList:" + priorityList);

        priorityArrayAdapter = new PriorityArrayAdapter(getActivity(),tunerGroupType, priorityList, this, this, tunerItemSelected);
        recyclerViewPriority.setAdapter(priorityArrayAdapter);

        buttonSaveTuner.setOnClickListener(v -> {
            if (revertMap.containsKey("newValue")) {
                selectedTunerValue = null;
                selectedTunerLevel = revertMap.get("newLevel").toString();
            }
            Intent tunerValue = new Intent()
                    .putExtra("Tuner_HashMap_Selected", (Serializable) tunerItemSelected)
                    .putExtra("Tuner_Group_Selected", (Serializable) tunerGroupSelected)
                    .putExtra("Tuner_Value_Selected", selectedTunerValue)
                    .putExtra("Tuner_Level_Selected", selectedTunerLevel);
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

    public double getTunerValByLevel(String id) {
        int level = 17;
        if (tunerGroupType.equalsIgnoreCase("Building")){
            level = 16;
        } else if (tunerGroupType.equalsIgnoreCase("System")){
            level = 14;
        } else if (tunerGroupType.equalsIgnoreCase("Zone")){
            level = 10;
        }else if (tunerGroupType.equalsIgnoreCase("Module")){
            level = 8;
        }

        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null && valMap.get("level").toString().equals(String.valueOf(level))) {
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
        if (position == 7 || position == 9 || position == 13 ||position == 15) {
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
            String levelName = "Building";
            int level;
            if (position == 15) {
                levelName = "Building";
            } else if (position == 13) {
                levelName = "System";
            } else if (position == 9) {
                levelName = "Zone";
            } else {
                levelName = "Module";
            }
            if (tunerItemSelected.containsKey("hideRefresh")){
                tunerItemSelected.remove("hideRefresh");
            }
            if (tunerItemSelected.containsKey("reset")){
                tunerItemSelected.remove("reset");
            }
            String text = "Level "+(position +1)+" "+levelName;
            text = text.replaceAll("System","<font color='#E24301'>System</font>");
            text = text.replaceAll(getString(R.string.txt_tunersModule),"<font color='#E24301'>Module</font>");
            text = text.replaceAll("Zone","<font color='#E24301'>Zone</font>");
            text = text.replaceAll("Building","<font color='#E24301'>Building</font>");
            textViewLevel.setText(Html.fromHtml(text));

            if (tunerItemSelected.containsKey("minVal") && tunerItemSelected.containsKey("maxVal")) {
                int currentValue;
                double currentValueDb;
                if (getTunerValByLevel(tunerItemSelected.get("id").toString()) != 0){
                    currentValue = (int) getTunerValByLevel(tunerItemSelected.get("id").toString());
                } else {
                    currentValue = (int) getTunerValue(tunerItemSelected.get("id").toString());
                }
                int minValue = (int) (Double.parseDouble(tunerItemSelected.get("minVal").toString()));
                int maxValue = (int) (Double.parseDouble(tunerItemSelected.get("maxVal").toString()));
                int incrementVal = (int) (Double.parseDouble(tunerItemSelected.get("incrementVal").toString()));

                if (getTunerValByLevel(tunerItemSelected.get("id").toString()) != 0){
                    currentValueDb = getTunerValByLevel(tunerItemSelected.get("id").toString());
                } else {
                    currentValueDb = getTunerValue(tunerItemSelected.get("id").toString());
                }

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
                    for (double i = 100*minValueDb; i <= 100*maxValueDb; i += 100*incrementValDb) {
                        valueList.add(String.valueOf(i/100.0));
                    }
                    for (String currVal : valueList){
                        if (currentValueDb == Double.parseDouble(currVal)){
                            currentValPos = valueList.indexOf(currVal);
                            break;
                        }
                    }
                } else {
                    for (double i = 100*minValueDb; i <= 100*maxValueDb; i += 100*incrementValDb) {
                        valueList.add(String.valueOf(i/100.0));
                    }
                    for (String currVal : valueList){
                        if (currentValueDb == Double.parseDouble(currVal)){
                            currentValPos = valueList.indexOf(currVal);
                            break;
                        }
                    }
                }
                npTunerRange.setDisplayedValues(valueList.toArray(new String[valueList.size()]));
                npTunerRange.setMinValue(0);
                npTunerRange.setMaxValue(valueList.size() -1);
                npTunerRange.setValue(currentValPos);
                Log.i("TunersUI", "valueList :" + Arrays.toString(npTunerRange.getDisplayedValues()));

                npTunerRange.setWrapSelectorWheel(false);
                npTunerRange.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                double finalIncrementValDb = incrementValDb;
                npTunerRange.setOnValueChangedListener((numberPicker, oldValue, newValue) ->
                {
                    //Toast.makeText(getActivity(), "TunersUI-oldValue:" + oldValue + " newValue:" + newValue, Toast.LENGTH_SHORT).show();
                    if (oldValue != newValue) {
                        buttonSaveAlert.setEnabled(true);
                        buttonSaveAlert.setTextColor(getActivity().getColor(R.color.orange_75f));
                        buttonUndo.setVisibility(View.VISIBLE);
                        selectedTunerValue = valueList.get(newValue);
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
                        selectedTunerValue = valueList.get(npTunerRange.getValue());
                    }
                            //tunerItemSelected.put("newValue", selectedTunerValue);
                            HashMap newValue = (HashMap) priorityList.get(position);
                            newValue.put("newValue", selectedTunerValue);
                            selectedTunerLevel = String.valueOf(position +1);
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

    @Override
    public void onUndoClick(HashMap item) {
        if (!item.containsKey("reset")) {
            buttonSaveTuner.setEnabled(false);
            buttonSaveTuner.setTextColor(getActivity().getColor(R.color.grey_select));
        } else {
            buttonSaveTuner.setEnabled(true);
            buttonSaveTuner.setTextColor(getActivity().getColor(R.color.orange_75f));
            revertMap = item;
        }
    }
}
