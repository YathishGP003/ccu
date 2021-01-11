package a75f.io.renatus.tuners;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.renatus.BASE.BaseDialogFragment;
import a75f.io.renatus.R;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class TunerFragment extends BaseDialogFragment implements TunerItemClickListener {
    public static final String ID = TunerFragment.class.getSimpleName();
    ExpandableListView expandableListView;
    HashMap<String, List<HashMap>> expandableListDetail;

    HashMap<String, String> tunerMap = new HashMap();

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

    public TunerFragment() {
    }

    public static TunerFragment newInstance() {
        return new TunerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tuner_editor, container, false);
        //ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        recyclerViewTuner = view.findViewById(R.id.recyclerTuner);
        expandableListView = view.findViewById(R.id.expandableListView);

        expandableListDetail = new HashMap<>();
        //updateData();


        radioGroupTuners = view.findViewById(R.id.radioGrpTuner);
        radioBtnBuilding = view.findViewById(R.id.radioBtnBuilding);
        radioButtonSystem = view.findViewById(R.id.radioBtnSystem);
        radioButtonZone = view.findViewById(R.id.radioBtnZone);
        radioButtonModule = view.findViewById(R.id.radioBtnModule);

        saveTunerValues = view.findViewById(R.id.buttonSave);
        cancelTunerUpdate = view.findViewById(R.id.buttonCancel);
        editChangeReason = view.findViewById(R.id.editChangeReason);
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
                Log.i("TunersUI", "Selected:radioBtnBuilding");
                getBuildingTuners();
            } else if (checkedId == R.id.radioBtnSystem) {
                Log.i("TunersUI", "Selected:radioBtnSystem");
                getSystemTuners();
            } else if (checkedId == R.id.radioBtnZone) {
                Log.i("TunersUI", "Selected:radioBtnZone");
                updateData();
            } else if (checkedId == R.id.radioBtnModule) {
                Log.i("TunersUI", "Selected:radioBtnModule");
                updateData();
            }
        });
        updatedTunerValues = new ArrayList<>();
        saveTunerValues.setOnClickListener(v -> {
            if (editChangeReason.getText().toString().length() == 0){
                Toast.makeText(getActivity(),"Please enter reason to save!",Toast.LENGTH_SHORT).show();
                return;
            }
            saveTunerValues.setTextColor(getActivity().getColor(R.color.tuner_group));
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_apply_tuner, null);
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setView(dialogView);

            HashMap ccu = CCUHsApi.getInstance().read("ccu");
            ((TextView)dialogView.findViewById(R.id.textView_Building)).setText(ccu.get("dis").toString());

            AlertDialog valueDialog = dialog.show();

            valueDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            LinearLayout linearLayoutBody = dialogView.findViewById(R.id.layoutConfirmBody);
            View tunerItemViewTop = inflater.inflate(R.layout.dialog_apply_tuner_item, null);
            linearLayoutBody.addView(tunerItemViewTop);
            for (HashMap newTunerValueItem : updatedTunerValues) {
                Toast.makeText(getActivity(), "Tuner Values Updated: " + newTunerValueItem.get("dis").toString() + " tunerValue:" + newTunerValueItem.get("newValue").toString(), Toast.LENGTH_SHORT).show();
                View tunerItemViewBody = inflater.inflate(R.layout.dialog_apply_tuner_item, null);

                LinearLayout layoutValues = tunerItemViewBody.findViewById(R.id.layoutValues);
                layoutValues.setVisibility(View.VISIBLE);
                TextView textView_SectionLabel = tunerItemViewBody.findViewById(R.id.textView_SectionLabel);
                TextView textView_Section = tunerItemViewBody.findViewById(R.id.textView_Section);
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
                textView_level.setText("Level "+ getTunerLevelValue(newTunerValueItem.get("id").toString())+" : ");
                textView_newLevel.setText("Level : "+newTunerValueItem.get("newLevel").toString());
                textView_oldValue.setText(String.valueOf(getTunerValue(newTunerValueItem.get("id").toString())));
                textView_newValue.setText(newTunerValueItem.get("newValue").toString());
                linearLayoutBody.addView(tunerItemViewBody);
            }
            Button buttonApplyTuners = dialogView.findViewById(R.id.buttonApplyTuner);
            Button buttonCancelTuners = dialogView.findViewById(R.id.buttonCancelTuner);
            buttonApplyTuners.setOnClickListener(dialogV -> {
                for (HashMap newTunerValueItem : updatedTunerValues) {
                    setTuner(newTunerValueItem.get("id").toString(), Double.parseDouble(newTunerValueItem.get("newValue").toString()));
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
    }


    private void updateData() {
        tunerMap.clear();
        expandableListDetail.clear();
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("tuner");
        ArrayList<HashMap> zoneEquips = new ArrayList<>();
        for (HashMap m : equips) {
          if (!m.get("roomRef").toString().equals("SYSTEM")){
              zoneEquips.add(m);
          }
        }

        Map<String, List<HashMap>> groupByTuner = zoneEquips.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        Set set2 = sortedGroupTuner.entrySet();
        Iterator iterator2 = set2.iterator();
        while (iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry) iterator2.next();
            Log.i("TunersUI", "Sorting-" + me2.getKey());
        }
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

    public void setTuner(String id, double val) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                CCUHsApi.getInstance().writePoint(id, TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", val, 0);
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
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        ArrayList<HashMap> systemEquips = CCUHsApi.getInstance().readAll("tunerGroup and system");

        Map<String, List<HashMap>> groupByTuner = systemEquips.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);

        Set set2 = sortedGroupTuner.entrySet();
        Iterator iterator2 = set2.iterator();
        while (iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry) iterator2.next();
            Log.i("TunersUI", "Sorting-" + me2.getKey());
        }
        for (String groupTitle : sortedGroupTuner.keySet()) {
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }

    private void getBuildingTuners() {
        tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        ArrayList<HashMap> buildingEquips = CCUHsApi.getInstance().readAll("tunerGroup and not system");

        Map<String, List<HashMap>> groupByTuner = buildingEquips.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);
        System.out.println("After Sorting:");
        Set set2 = sortedGroupTuner.entrySet();
        Iterator iterator2 = set2.iterator();
        while (iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry) iterator2.next();
            Log.i("TunersUI", "Sorting-" + me2.getKey());
        }
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
        if (radioBtnBuilding.isChecked()){
            tunerGroupType = "Building";
        } else if(radioButtonSystem.isChecked()){
            tunerGroupType = "System";
        } else if(radioButtonZone.isChecked()){
            tunerGroupType ="Zone";
        } else if(radioButtonModule.isChecked()){
            tunerGroupType = "Module";
        }
        DialogTunerPriorityArray tunerPriorityArray = DialogTunerPriorityArray.newInstance(item,tunerGroupType, tunerGroupOpened);
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
                    Toast.makeText(getActivity(), "TunerUI-HashMap: " + tunerItemSelected.get("dis") + " clicked\n" +
                            " tunerGroupSelected:" + tunerGroupSelected.getName() + " tunerValue:" + tunerValue, Toast.LENGTH_SHORT).show();
                    tunerItemSelected.put("newValue", tunerValue);
                    tunerItemSelected.put("newLevel", tunerLevel);
                    tunerExpandableLayoutHelper.updateTuner(tunerGroupSelected.getName(), tunerItemSelected, oldTunerItemSelected);
                    if (!updatedTunerValues.contains(tunerItemSelected)){
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
}
