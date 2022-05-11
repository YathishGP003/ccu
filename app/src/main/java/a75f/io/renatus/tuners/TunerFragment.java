package a75f.io.renatus.tuners;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HVal;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Collections;
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
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;
import a75f.io.renatus.util.CCUUiUtil;




/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class TunerFragment extends BaseDialogFragment implements TunerItemClickListener,TunerUndoClickListener {
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
    private Button saveTunerValues;
    private Button cancelTunerUpdate;
    EditText editChangeReason;
    EditText editTunerSearch;
    Spinner spinnerSelection;
    ArrayList<HashMap> tuners = new ArrayList<>();
    String tunerGroupType = "Building";
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
        //TODO: revert building tuners
        radioGroupTuners.check(R.id.radioBtnBuilding);
        getBuildingTuners();
        //getSystemTuners();

        reasonLabel = view.findViewById(R.id.textReasonLabel);
        String text = "<font color=#E24301>*</font> <font color=#999999>Reason for Change</font>";
        reasonLabel.setText(Html.fromHtml(text));

        cancelTunerUpdate.setOnClickListener(view1 -> {
            editChangeReason.setText("");
            editChangeReason.clearFocus();
        });
        radioGroupTuners.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnBuilding) {
                tunerGroupType = "Building";
                getBuildingTuners();
                spinnerSelection.setVisibility(View.GONE);
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
            } else if (checkedId == R.id.radioBtnSystem) {
                tunerGroupType = "System";
                getSystemTuners();
                spinnerSelection.setVisibility(View.GONE);
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
            } else if (checkedId == R.id.radioBtnZone) {
                tunerGroupType = "Zone";
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
                ArrayList<Zone> zones = new ArrayList<>();
                for (Floor f : HSUtil.getFloors()) {
                    zones.addAll(HSUtil.getZones(f.getId()));
                }
                if (zones.size() > 0){
                    spinnerSelection.setVisibility(View.VISIBLE);
                } else {
                    spinnerSelection.setVisibility(View.GONE);
                    tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);
                }
                ArrayAdapter<Zone> selectionSpinner = new ArrayAdapter<>(this.getActivity(), R.layout.spinner_item_tuner, zones);
                selectionSpinner.setDropDownViewResource(R.layout.spinner_item_tuner);
                spinnerSelection.setAdapter(selectionSpinner);

            } else if (checkedId == R.id.radioBtnModule) {
                tunerGroupType = "Module";
                editTunerSearch.clearFocus();
                editTunerSearch.getText().clear();
                ArrayList<Equip> equips = new ArrayList<>();
                for (Floor f : HSUtil.getFloors()) {
                    for (Zone z : HSUtil.getZones(f.getId())) {
                        equips.addAll(HSUtil.getEquips(z.getId()));
                    }
                }
                ArrayList<Equip> UpdatedEquips = new ArrayList<>();
                for(Equip p: equips){
                    HashMap<Object, Object> map = CCUHsApi.getInstance().readMapById(p.getId());
                    map.put("dis", p.getDisplayName().replace(HSUtil.getDis(p.getSiteRef())+"-",HSUtil.getDis(p.getRoomRef())+"_").replace("-",""));
                    Equip.Builder eb = new Equip.Builder();
                    eb.setHashMap(map);
                    UpdatedEquips.add(eb.build());
                }
                if (equips.size()>0){
                    spinnerSelection.setVisibility(View.VISIBLE);
                } else {
                    spinnerSelection.setVisibility(View.GONE);
                    tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);
                }
                ArrayAdapter<Equip> selectionSpinner = new ArrayAdapter<>(this.getActivity(), R.layout.spinner_item_tuner, UpdatedEquips);
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
            String changeReason = editChangeReason.getText().toString();
            if (updatedTunerValues.size() <= 0){
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
                HashMap ccuinfo = CCUHsApi.getInstance().read("device and ccu");
                switch (newTunerValueItem.get("newLevel").toString()) {
                    case "8":
                        textView_SectionLabel.setText("Module");
                        textView_Section.setText(": " +HSUtil.getDis(newTunerValueItem.get("equipRef").toString()).replace(HSUtil.getDis(newTunerValueItem.get("siteRef").toString()),"").replace("-",""));
                        break;
                    case "10":
                        textView_SectionLabel.setText("Zone");
                        textView_Section.setText(": " +HSUtil.getDis(newTunerValueItem.get("roomRef").toString()));
                        break;
                    case "16":
                        textView_SectionLabel.setText("Building");
                        textView_Section.setText(": " +ccuinfo.get("dis").toString());
                        break;
                    default:
                        textView_SectionLabel.setText("System");
                        textView_Section.setText(": " +ccuinfo.get("dis").toString());
                }

                String tunerName = newTunerValueItem.get("dis").toString();
                tunerName = tunerName.substring(tunerName.lastIndexOf("-") + 1);
                if (newTunerValueItem.containsKey("unit")) {
                    textView_tuner.setText(tunerName + " " + newTunerValueItem.get("unit").toString().toUpperCase() + " | ");
                } else {
                    textView_tuner.setText(tunerName + " | ");
                }
                if (newTunerValueItem.get("newLevel").toString().equals("16") && newTunerValueItem.get("tunerGroup").toString().contains("VAV") || newTunerValueItem.get("tunerGroup").toString().contains("DAB") ){
                    if ( newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(getSystemProfileType())){
                        textView_level.setText("Level " + newTunerValueItem.get("newLevel").toString() + " : ");
                        textView_newLevel.setText("Level : " + newTunerValueItem.get("newLevel").toString());
                        textView_oldValue.setText(getTunerValue(newTunerValueItem.get("id").toString(),newTunerValueItem.get("newLevel").toString()));
                        if (newTunerValueItem.get("newValue") != null){
                            textView_newValue.setText(newTunerValueItem.get("newValue").toString());
                        } else {
                            textView_newValue.setText("-");
                        }
                    } else {
                        textView_level.setText("(Tuner is not applicable)");
                        textView_newLevel.setVisibility(View.GONE);
                        textView_oldValue.setVisibility(View.GONE);
                        textView_newValue.setVisibility(View.GONE);
                        imageViewArrow.setVisibility(View.GONE);
                    }
                } else {
                    textView_level.setText("Level " + newTunerValueItem.get("newLevel").toString() + " : ");
                    textView_newLevel.setText("Level : " + newTunerValueItem.get("newLevel").toString());
                    textView_oldValue.setText(getTunerValue(newTunerValueItem.get("id").toString(),newTunerValueItem.get("newLevel").toString()));
                   if (newTunerValueItem.get("newValue") != null){
                       textView_newValue.setText(newTunerValueItem.get("newValue").toString());
                   } else {
                       textView_newValue.setText("-");
                   }
                }
                linearLayoutBody.addView(tunerItemViewBody);
            }
            Button buttonApplyTuners = dialogView.findViewById(R.id.buttonApplyTuner);
            Button buttonCancelTuners = dialogView.findViewById(R.id.buttonCancelTuner);
            buttonApplyTuners.setOnClickListener(dialogV -> {
                for (HashMap newTunerValueItem : updatedTunerValues) {
                    if ((newTunerValueItem.get("newLevel").toString().equals("16") || newTunerValueItem.get("newLevel").toString().equals("14")) && newTunerValueItem.get("tunerGroup").toString().contains("VAV") || newTunerValueItem.get("tunerGroup").toString().contains("DAB")) {
                        if (!newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(getSystemProfileType())) {
                            continue;
                        }
                    }

                    /*
                    * when building level tuner update check all linked level tuners (System/Zone/Module) and update
                    * */

                    if (newTunerValueItem.get("newLevel").toString().equals("16")){
                        String buildingTunerDis = newTunerValueItem.get("dis").toString();

                        //update dualDuct building tuners
                        ArrayList<HashMap> dualDuctBuildingTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and dualDuct");
                        String buildingTunerShortDis = buildingTunerDis.substring(buildingTunerDis.lastIndexOf("-") + 1).trim();
                        for (HashMap hashMap : dualDuctBuildingTuners) {
                            String hashMapDis = hashMap.get("dis").toString();
                            if (!newTunerValueItem.get("id").toString().equals(hashMap.get("id").toString()) && hashMapDis.contains("Building")&& newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(hashMap.get("tunerGroup").toString()) && buildingTunerShortDis.equalsIgnoreCase(hashMapDis.substring(hashMapDis.lastIndexOf("-") + 1).trim())) {
                                setTuner(hashMap.get("id").toString(), 16, newTunerValueItem.get("newValue") == null ? null: Double.parseDouble(newTunerValueItem.get("newValue").toString()), changeReason);
                            }
                        }

                        //Update linked system tuner
                        ArrayList<HashMap> systemTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and system and roomRef == \""+ "SYSTEM" +"\"");
                        for (HashMap systemTunersMap : systemTuners) {
                            String systemTunerDis = systemTunersMap.get("dis").toString();
                             if (!newTunerValueItem.get("id").toString().equals(systemTunersMap.get("id").toString()) && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(systemTunersMap.get("tunerGroup").toString()) && buildingTunerShortDis.equalsIgnoreCase(systemTunerDis.substring(systemTunerDis.lastIndexOf("-") + 1).trim())){
                                 setTuner(systemTunersMap.get("id").toString(), 16, newTunerValueItem.get("newValue") == null ? null: Double.parseDouble(newTunerValueItem.get("newValue").toString()), changeReason);
                             }
                        }

                        //Update linked zone tuner
                        ArrayList<Zone> zoneArrayList = new ArrayList<>();
                        ArrayList<Equip> equipArrayList = new ArrayList<>();

                        for(Floor f: HSUtil.getFloors()){
                            zoneArrayList.addAll(HSUtil.getZones(f.getId()));
                        }

                        for (Zone z: zoneArrayList) {
                            equipArrayList.addAll(HSUtil.getEquips(z.getId()));
                        }

                        //Update linked module tuner
                        for (Equip e : equipArrayList){
                            ArrayList<HashMap> moduleTuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+e.getId()+"\"");
                            for (HashMap moduleTunerMap : moduleTuners) {
                                if (!moduleTunerMap.get("roomRef").toString().equals("SYSTEM")) {
                                    String moduleTunerDis = moduleTunerMap.get("dis").toString();
                                    if (!newTunerValueItem.get("id").toString().equals(moduleTunerMap.get("id").toString()) && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(moduleTunerMap.get("tunerGroup").toString()) && buildingTunerShortDis.equalsIgnoreCase(moduleTunerDis.substring(moduleTunerDis.lastIndexOf("-") + 1).trim())) {
                                        setTuner(moduleTunerMap.get("id").toString(), 16, newTunerValueItem.get("newValue") == null ? null:Double.parseDouble(newTunerValueItem.get("newValue").toString()), changeReason);
                                    }
                                }
                            }
                        }
                    }

                    /*
                     * when system level tuner update check all linked level tuners (Building/Zone/Module) and update
                     * */

                    if (newTunerValueItem.get("newLevel").toString().equals("14")){

                        //Update linked zone tuner
                        String sysTunerDis = newTunerValueItem.get("dis").toString();
                        ArrayList<Zone> zoneArrayList = new ArrayList<>();
                        ArrayList<Equip> equipArrayList = new ArrayList<>();

                        for(Floor f: HSUtil.getFloors()){
                            zoneArrayList.addAll(HSUtil.getZones(f.getId()));
                        }

                        for (Zone z: zoneArrayList) {
                            equipArrayList.addAll(HSUtil.getEquips(z.getId()));
                        }

                        //Update linked module tuner
                        for (Equip e : equipArrayList){
                            ArrayList<HashMap> moduleTuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+e.getId()+"\"");

                            for (HashMap moduleTunerMap : moduleTuners) {
                                if (!moduleTunerMap.get("roomRef").toString().equals("SYSTEM")) {
                                    String moduleTunerDis = moduleTunerMap.get("dis").toString();
                                    if (!newTunerValueItem.get("id").toString().equals(moduleTunerMap.get("id").toString()) && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(moduleTunerMap.get("tunerGroup").toString()) && sysTunerDis.substring(sysTunerDis.lastIndexOf("-") + 1).trim().equalsIgnoreCase(moduleTunerDis.substring(moduleTunerDis.lastIndexOf("-") + 1).trim())) {
                                        setTuner(moduleTunerMap.get("id").toString(), 14, newTunerValueItem.get("newValue") == null ? null:Double.parseDouble(newTunerValueItem.get("newValue").toString()), changeReason);
                                    }
                                }
                            }
                        }
                    }

                    /*
                     * when zone level tuner update check all zone level tuners and update
                     * */

                    if (newTunerValueItem.get("newLevel").toString().equals("10")){
                        String newZoneTunerDis = newTunerValueItem.get("dis").toString();
                            ArrayList<HashMap> zoneTuners = CCUHsApi.getInstance().readAll("tuner and roomRef == \"" + newTunerValueItem.get("roomRef") + "\"");

                            for (HashMap zoneTunersMap : zoneTuners) {
                                if (!zoneTunersMap.get("roomRef").toString().equals("SYSTEM")) {
                                    String zoneTunerDis = zoneTunersMap.get("dis").toString();
                                    if (!newTunerValueItem.get("id").toString().equals(zoneTunersMap.get("id").toString()) && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(zoneTunersMap.get("tunerGroup").toString()) && newZoneTunerDis.substring(newZoneTunerDis.lastIndexOf("-") + 1).trim().equalsIgnoreCase(zoneTunerDis.substring(zoneTunerDis.lastIndexOf("-") + 1).trim())) {
                                        setTuner(zoneTunersMap.get("id").toString(), 10, newTunerValueItem.get("newValue") == null ? null:Double.parseDouble(newTunerValueItem.get("newValue").toString()), changeReason);
                                    }
                                }
                            }
                    }

                    setTuner(newTunerValueItem.get("id").toString(), Integer.parseInt(newTunerValueItem.get("newLevel").toString()), newTunerValueItem.get("newValue") == null ? null:Double.parseDouble(newTunerValueItem.get("newValue").toString()), changeReason);
                }
                Toast.makeText(getActivity(), "Tuner Values Updated Successfully", Toast.LENGTH_SHORT).show();
                updatedTunerValues.clear();
                editChangeReason.setText("");
                valueDialog.dismiss();
                saveTunerValues.setEnabled(false);
                saveTunerValues.setTextColor(getActivity().getColor(R.color.tuner_group));
                tunerExpandableLayoutHelper.notifyDataSaveChanged();
                //close soft keyboard
                InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(editChangeReason.getWindowToken(), 0);
            });
            buttonCancelTuners.setOnClickListener(dialogV -> {
                saveTunerValues.setEnabled(true);
                saveTunerValues.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
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
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);

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
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);

        Map<String, List<HashMap>> groupByTuner = tuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);
        List<HashMap> filteredList = new ArrayList<>();
        for (String groupTitle : sortedGroupTuner.keySet()) {
           for (HashMap tuners : sortedGroupTuner.get(groupTitle)){
               if(StringUtils.containsIgnoreCase(tuners.get("dis").toString().substring(tuners.get("dis").toString().lastIndexOf("-") + 1),filteredString)) {
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
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);

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

    public void setTuner(String id, int level, Double val, String reason) {

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                if (val == null){
                    CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(id), (int) level, CCUHsApi.getInstance().getCCUUserName(), HNum.make(getTuner(id)), HNum.make(1));
                    HDictBuilder b = new HDictBuilder()
                            .add("id", HRef.copy(id))
                            .add("level",level)
                            .add("who",CCUHsApi.getInstance().getCCUUserName())
                            .add("duration", HNum.make(0, "ms"))
                            .add("val", (HVal) null)
                            .add("reason", reason);
                    HDict[] dictArr = {b.toDict()};
                    HttpUtil.executePost(CCUHsApi.getInstance().pointWriteTarget(), HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
                    CCUHsApi.getInstance().writeHisValById(id, HSUtil.getPriorityVal(id));
                } else {
                    CCUHsApi.getInstance().writePointForCcuUser(id, level, val, 0, reason);
                    CCUHsApi.getInstance().writeHisValById(id, val);
                }
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
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);

        ArrayList<HashMap> systemTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and system");
        ArrayList<HashMap> moduleTuners = new ArrayList<>();


        ArrayList<Equip> equipArrayList = new ArrayList<>();
        ArrayList<Zone> zoneArrayList = new ArrayList<>();

        for (Floor f : HSUtil.getFloors()) {
            zoneArrayList.addAll(HSUtil.getZones(f.getId()));
        }

        for (Zone z : zoneArrayList) {
            equipArrayList.addAll(HSUtil.getEquips(z.getId()));
        }
        for(Equip equip : equipArrayList){
            moduleTuners.addAll(CCUHsApi.getInstance().readAll("tuner and equipRef == \"" + equip.getId() + "\""));
        }

        for (HashMap m : systemTuners) {
            if (!m.get("dis").toString().contains("Building")) {
                tuners.add(m);
           }
        }

        Collections.reverse(moduleTuners);
        tuners.addAll(moduleTuners);

        Map<String, List<HashMap>> groupByTuner = tuners.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        for (String groupTitle : sortedGroupTuner.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    private void getBuildingTuners() {
        tuners.clear();
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, this,2, tunerGroupType);

        ArrayList<HashMap> buildingTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and not dualDuct");
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
                    tunerExpandableLayoutHelper.updateTuner(tunerGroupSelected, tunerItemSelected, oldTunerItemSelected);
                    if (!updatedTunerValues.contains(tunerItemSelected)) {
                        updatedTunerValues.add(tunerItemSelected);
                    }
                    if (updatedTunerValues.size() > 0) {
                        saveTunerValues.setEnabled(true);
                        saveTunerValues.setTextColor(CCUUiUtil.getPrimaryThemeColor(getContext()));
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    public String getTunerValue(String id, String level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("level") != null && level.equals(valMap.get("level").toString()) && valMap.get("val") != null) {
                    return valMap.get("val").toString();
                }
            }
        }
        return "-";
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

    @Override
    public void onUndoClick(HashMap item) {
        if (updatedTunerValues != null && updatedTunerValues.size() > 0){
            updatedTunerValues.remove(item);
        }
    }
}
