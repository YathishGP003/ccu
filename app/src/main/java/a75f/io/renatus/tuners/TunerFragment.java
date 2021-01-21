package a75f.io.renatus.tuners;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class TunerFragment extends BaseDialogFragment implements TunerItemClickListener {
    public static final String ID = TunerFragment.class.getSimpleName();

    RadioGroup radioGroupTuners;
    RadioButton radioButtonSystem;
    RadioButton radioButtonZone;
    RadioButton radioButtonModule;
    RadioButton radioBtnBuilding;
    TextView reasonLabel;
    RecyclerView recyclerViewTuner;
    TunerGroupItem tunerGroupOpened = null;
    final int DIALOG_TUNER_PRIORITY = 10;
    TunerExpandableLayoutHelper tunerExpandableLayoutHelper;
    int childSelected = 0;
    ArrayList<HashMap> updatedTunerValues;
    MaterialButton saveTunerValues;
    MaterialButton cancelTunerUpdate;
    EditText editChangeReason;
    EditText editTunerSearch;
    Spinner spinnerSelection;
    ArrayList<HashMap> tuners = new ArrayList<>();

    public TunerFragment() {
    }

    public static TunerFragment newInstance() {
        return new TunerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tuner_editor, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        recyclerViewTuner = view.findViewById(R.id.recyclerTuner);
        radioGroupTuners = view.findViewById(R.id.radioGrpTuner);
        radioBtnBuilding = view.findViewById(R.id.radioBtnBuilding);
        radioButtonSystem = view.findViewById(R.id.radioBtnSystem);
        radioButtonZone = view.findViewById(R.id.radioBtnZone);
        radioButtonModule = view.findViewById(R.id.radioBtnModule);

        saveTunerValues = view.findViewById(R.id.buttonSave);
        spinnerSelection = view.findViewById(R.id.spinnerSelection);
        cancelTunerUpdate = view.findViewById(R.id.buttonCancel);
        editChangeReason = view.findViewById(R.id.editChangeReason);
        editTunerSearch = view.findViewById(R.id.editTunerSearch);
        saveTunerValues.setEnabled(false);
        //Default Show System Tuners
        radioGroupTuners.check(R.id.radioBtnBuilding);
        getBuildingTuners();

        reasonLabel = view.findViewById(R.id.textReasonLabel);
        String text = "<font color=#E24301>*</font> <font color=#999999>Reason for Change</font>";
        reasonLabel.setText(Html.fromHtml(text));

        cancelTunerUpdate.setOnClickListener(view1 -> {
            editChangeReason.setText("");
            editChangeReason.clearFocus();
        });
        radioGroupTuners.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnBuilding) {
                getBuildingTuners();
                spinnerSelection.setVisibility(View.GONE);
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
            } else if (checkedId == R.id.radioBtnSystem) {
                getSystemTuners();
                spinnerSelection.setVisibility(View.GONE);
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
            } else if (checkedId == R.id.radioBtnZone) {
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
                ArrayList<Zone> zones = new ArrayList<>();
                for (Floor f : HSUtil.getFloors()) {
                    zones.addAll(HSUtil.getZones(f.getId()));
                }
                if (zones.size()>0){
                    spinnerSelection.setVisibility(View.VISIBLE);
                } else {
                    spinnerSelection.setVisibility(View.GONE);
                    tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);
                }
                ArrayAdapter<Zone> selectionSpinner = new ArrayAdapter<>(this.getActivity(), R.layout.spinner_item_tuner, zones);
                selectionSpinner.setDropDownViewResource(R.layout.spinner_item_tuner);
                spinnerSelection.setAdapter(selectionSpinner);

            } else if (checkedId == R.id.radioBtnModule) {
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
                ArrayList<Equip> equips = new ArrayList<>();
                for (Floor f : HSUtil.getFloors()) {
                    for (Zone z : HSUtil.getZones(f.getId())) {
                        equips.addAll(HSUtil.getEquips(z.getId()));
                    }
                }

                if (equips.size()>0){
                    spinnerSelection.setVisibility(View.VISIBLE);
                } else {
                    spinnerSelection.setVisibility(View.GONE);
                    tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);
                }
                ArrayAdapter<Equip> selectionSpinner = new ArrayAdapter<>(this.getActivity(), R.layout.spinner_item_tuner, equips);
                selectionSpinner.setDropDownViewResource(R.layout.spinner_item_tuner);
                spinnerSelection.setAdapter(selectionSpinner);
            }
        });

        ArrayList<Zone> zones = new ArrayList<>();
        for (Floor f : HSUtil.getFloors()) {
            zones.addAll(HSUtil.getZones(f.getId()));
        }

        spinnerSelection.setVisibility(View.GONE);
        ArrayAdapter<Zone> selectionSpinner = new ArrayAdapter<>(this.getActivity(), R.layout.spinner_item_tuner, zones);
        selectionSpinner.setDropDownViewResource(R.layout.spinner_item_tuner);
        spinnerSelection.setAdapter(selectionSpinner);

        spinnerSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

                Object obj = adapterView.getItemAtPosition(position);
                if (obj instanceof Zone) {
                    Zone z = (Zone) obj;
                    updateZoneData(z.getId());
                } else  if (obj instanceof Equip) {
                    Equip q = (Equip) obj;
                    updateModuleData(q.getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        updatedTunerValues = new ArrayList<>();
        saveTunerValues.setOnClickListener(v -> {
            if (editChangeReason.getText().toString().length() == 0) {
                Toast.makeText(getActivity(), "Please enter reason to save!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveTunerValues.setTextColor(getActivity().getColor(R.color.tuner_group));
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_apply_tuner, null);
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setView(dialogView);

            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            ((TextView) dialogView.findViewById(R.id.textView_Building)).setText(ccu.get("dis").toString());

            AlertDialog valueDialog = dialog.show();

            valueDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            LinearLayout linearLayoutBody = dialogView.findViewById(R.id.layoutConfirmBody);
            View tunerItemViewTop = inflater.inflate(R.layout.dialog_apply_tuner_item, null);
            linearLayoutBody.addView(tunerItemViewTop);
            for (HashMap newTunerValueItem : updatedTunerValues) {
                //Toast.makeText(getActivity(), "Tuner Values Updated: " + newTunerValueItem.get("dis").toString() + " tunerValue:" + newTunerValueItem.get("newValue").toString(), Toast.LENGTH_SHORT).show();
                View tunerItemViewBody = inflater.inflate(R.layout.dialog_apply_tuner_item, null);

                LinearLayout layoutValues = tunerItemViewBody.findViewById(R.id.layoutValues);
                layoutValues.setVisibility(View.VISIBLE);
                TextView textView_SectionLabel = tunerItemViewBody.findViewById(R.id.textView_SectionLabel);
                TextView textView_Section = tunerItemViewBody.findViewById(R.id.textView_Section);
                ImageView imageViewArrow = tunerItemViewBody.findViewById(R.id.imageViewArrow);
                TextView textView_tuner = tunerItemViewBody.findViewById(R.id.textView_tuner);
                TextView textView_level = tunerItemViewBody.findViewById(R.id.textView_level);
                TextView textView_oldValue = tunerItemViewBody.findViewById(R.id.textView_oldValue);
                TextView textView_newLevel = tunerItemViewBody.findViewById(R.id.textView_newLevel);
                TextView textView_newValue = tunerItemViewBody.findViewById(R.id.textView_newValue);
                switch (newTunerValueItem.get("newLevel").toString()) {
                    case "8":
                        textView_SectionLabel.setText("Module");
                        break;
                    case "10":
                        textView_SectionLabel.setText("Zone");
                        break;
                    case "16":
                        textView_SectionLabel.setText("Building");
                        break;
                    default:
                        textView_SectionLabel.setText("System");
                }
                textView_Section.setText(":");

                String tunerName = newTunerValueItem.get("dis").toString();
                tunerName = tunerName.substring(tunerName.lastIndexOf("-") + 1);
                if (newTunerValueItem.containsKey("unit")) {
                    textView_tuner.setText(tunerName + " " + newTunerValueItem.get("unit").toString().toUpperCase() + " | ");
                } else {
                    textView_tuner.setText(tunerName + " | ");
                }
                if (newTunerValueItem.get("newLevel").toString().equals("16") && newTunerValueItem.get("tunerGroup").toString().contains("VAV") || newTunerValueItem.get("tunerGroup").toString().contains("DAB") ){
                    if ( newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(getSystemProfileType())){
                        textView_level.setText("Level " + getTunerLevelValue(newTunerValueItem.get("id").toString()) + " : ");
                        textView_newLevel.setText("Level : " + newTunerValueItem.get("newLevel").toString());
                        textView_oldValue.setText(String.valueOf(getTunerValue(newTunerValueItem.get("id").toString())));
                        textView_newValue.setText(newTunerValueItem.get("newValue").toString());
                    } else {
                        textView_level.setText("(Tuner is not applicable)");
                        textView_newLevel.setVisibility(View.GONE);
                        textView_oldValue.setVisibility(View.GONE);
                        textView_newValue.setVisibility(View.GONE);
                        imageViewArrow.setVisibility(View.GONE);
                    }
                } else {
                    textView_level.setText("Level " + getTunerLevelValue(newTunerValueItem.get("id").toString()) + " : ");
                    textView_newLevel.setText("Level : " + newTunerValueItem.get("newLevel").toString());
                    textView_oldValue.setText(String.valueOf(getTunerValue(newTunerValueItem.get("id").toString())));
                    textView_newValue.setText(newTunerValueItem.get("newValue").toString());
                }
                linearLayoutBody.addView(tunerItemViewBody);
            }
            Button buttonApplyTuners = dialogView.findViewById(R.id.buttonApplyTuner);
            Button buttonCancelTuners = dialogView.findViewById(R.id.buttonCancelTuner);
            buttonApplyTuners.setOnClickListener(dialogV -> {
                for (HashMap newTunerValueItem : updatedTunerValues) {
                    if (newTunerValueItem.get("newLevel").toString().equals("16") && newTunerValueItem.get("tunerGroup").toString().contains("VAV") || newTunerValueItem.get("tunerGroup").toString().contains("DAB")) {
                        if (!newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(getSystemProfileType())) {
                            continue;
                        }
                    }
                    setTuner(newTunerValueItem.get("id").toString(), Integer.valueOf(newTunerValueItem.get("newLevel").toString()), Double.parseDouble(newTunerValueItem.get("newValue").toString()));
                }
                Toast.makeText(getActivity(), "Tuner Values Updated Successfully", Toast.LENGTH_SHORT).show();
                updatedTunerValues.clear();
                editChangeReason.setText("");
                valueDialog.dismiss();
                saveTunerValues.setEnabled(false);
                saveTunerValues.setTextColor(getActivity().getColor(R.color.tuner_group));
            });
            buttonCancelTuners.setOnClickListener(dialogV -> {
                saveTunerValues.setEnabled(true);
                saveTunerValues.setTextColor(getActivity().getColor(R.color.orange_75f));
                valueDialog.dismiss();
            });
            linearLayoutBody.invalidate();
            dialogView.invalidate();
            valueDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        });

        editTunerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                filter(editable.toString());
            }
        });
    }


    private void updateModuleData(String equipRef) {
        tuners.clear();
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        tuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+equipRef+"\"");

        ArrayList<HashMap> moduleTuners = new ArrayList<>();
        for (HashMap m : tuners) {
            if (!m.get("roomRef").toString().equals("SYSTEM")) {
                moduleTuners.add(m);
            }
        }

        Map<String, List<HashMap>> groupByTuner = moduleTuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        for (String groupTitle : sortedGroupTuner.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    void filter(String filteredString){
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        Map<String, List<HashMap>> groupByTuner = tuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);
        List<HashMap> filteredList = new ArrayList<>();
        for (String groupTitle : sortedGroupTuner.keySet()) {
           for (HashMap tuners : sortedGroupTuner.get(groupTitle)){
               if(tuners.get("dis").toString().contains(filteredString)) {
                   filteredList.add(tuners);
               }
            }

        }

        Map<String, List<HashMap>> filteredGroupTuner = filteredList.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> filteredSortedGroup = new TreeMap<>(filteredGroupTuner);
        for (String groupTitle : filteredSortedGroup.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle,filteredSortedGroup.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    private void updateZoneData(String roomRef) {
        tuners.clear();
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        tuners = CCUHsApi.getInstance().readAll("tuner and roomRef == \""+roomRef+"\"");

        ArrayList<HashMap> zoneTuners = new ArrayList<>();
        for (HashMap m : tuners) {
            if (!m.get("roomRef").toString().equals("SYSTEM")) {
                zoneTuners.add(m);
            }
        }

        Map<String, List<HashMap>> groupByTuner = zoneTuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        for (String groupTitle : sortedGroupTuner.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    public double getTuner(String id) {
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

    public void setTuner(String id, int level, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                CCUHsApi.getInstance().writePoint(id, level, "ccu", val, 0);
                CCUHsApi.getInstance().writeHisValById(id, val);
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void getSystemTuners() {
        tuners.clear();
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        ArrayList<HashMap> systemTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and system and roomRef == \""+ "SYSTEM" +"\"");

        for (HashMap m : systemTuners) {
            if (!m.get("dis").toString().contains("Building")) {
                tuners.add(m);
            }
        }

        Map<String, List<HashMap>> groupByTuner = tuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        for (String groupTitle : sortedGroupTuner.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    private void getBuildingTuners() {
        tuners.clear();
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        ArrayList<HashMap> buildingTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and roomRef == \""+ "SYSTEM" +"\"");
        for (HashMap m : buildingTuners) {
            if (m.get("dis").toString().contains("Building")) {
                tuners.add(m);
            }
        }

        Map<String, List<HashMap>> groupByTuner = tuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        for (String groupTitle : sortedGroupTuner.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    @Override
    public void itemClicked(HashMap item, int position) {
        //Toast.makeText(getActivity(), "TunerUI-HashMap: " + item.get("dis") + " clicked\n" + " minValue:" + item.get("minVal") + " maxValue:" + item.get("maxVal") + " incrementBy:" + item.get("incrementVal"), Toast.LENGTH_SHORT).show();
        childSelected = position;
        Log.i("TunersUI", "childSelected:" + childSelected + " hashmap:" + item);
        String tunerGroupType = "Building";
        if (radioBtnBuilding.isChecked()) {
            tunerGroupType = "Building";
        } else if (radioButtonSystem.isChecked()) {
            tunerGroupType = "System";
        } else if (radioButtonZone.isChecked()) {
            tunerGroupType = "Zone";
        } else if (radioButtonModule.isChecked()) {
            tunerGroupType = "Module";
        }
        DialogTunerPriorityArray tunerPriorityArray = DialogTunerPriorityArray.newInstance(item, tunerGroupType, tunerGroupOpened);
        tunerPriorityArray.setTargetFragment(this, DIALOG_TUNER_PRIORITY);
        showDialogFragment(tunerPriorityArray, DialogTunerPriorityArray.ID);
    }

    @Override
    public void itemClicked(TunerGroupItem section) {
        //Toast.makeText(getActivity(), "TunerUI-Section: " + section.getName() + " clicked", Toast.LENGTH_SHORT).show();
        tunerGroupOpened = section;
    }

    @Override
    public String getIdString() {
        return ID;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DIALOG_TUNER_PRIORITY:
                if (resultCode == Activity.RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    HashMap tunerItemSelected = (HashMap) bundle.getSerializable("Tuner_HashMap_Selected");
                    HashMap oldTunerItemSelected = (HashMap) bundle.getSerializable("Tuner_HashMap_Selected");
                    TunerGroupItem tunerGroupSelected = (TunerGroupItem) bundle.getSerializable("Tuner_Group_Selected");
                    String tunerValue = bundle.getString("Tuner_Value_Selected");
                    String tunerLevel = bundle.getString("Tuner_Level_Selected");
                   /* Toast.makeText(getActivity(), "TunerUI-HashMap: " + tunerItemSelected.get("dis") + " clicked\n" +
                            " tunerGroupSelected:" + tunerGroupSelected.getName() + " tunerValue:" + tunerValue, Toast.LENGTH_SHORT).show();*/
                    tunerItemSelected.put("newValue", tunerValue);
                    tunerItemSelected.put("newLevel", tunerLevel);
                    tunerExpandableLayoutHelper.updateTuner(tunerGroupSelected.getName(), tunerItemSelected, oldTunerItemSelected);
                    if (!updatedTunerValues.contains(tunerItemSelected)) {
                        updatedTunerValues.add(tunerItemSelected);
                    }
                    if (updatedTunerValues.size() > 0) {
                        saveTunerValues.setEnabled(true);
                        saveTunerValues.setTextColor(getActivity().getColor(R.color.orange_75f));
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
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

    public String getTunerLevelValue(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null && valMap.get("level") != null) {
                    return valMap.get("level").toString();
                }
            }
        }
        return "14";
    }

    private String getSystemProfileType(){
        ProfileType profileType =  L.ccu().systemProfile.getProfileType();
        switch (profileType){
            case SYSTEM_DAB_ANALOG_RTU:
            case SYSTEM_DAB_HYBRID_RTU:
            case SYSTEM_DAB_STAGED_RTU:
            case SYSTEM_DAB_STAGED_VFD_RTU:
                return "DAB";
            case SYSTEM_VAV_ANALOG_RTU:
            case SYSTEM_VAV_HYBRID_RTU:
            case SYSTEM_VAV_IE_RTU:
            case SYSTEM_VAV_STAGED_RTU:
            case SYSTEM_VAV_STAGED_VFD_RTU:
                return "VAV";
        }
        return "default";
    }
}
