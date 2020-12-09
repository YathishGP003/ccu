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
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.javolution.text.Text;

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
    TextView reasonLabel;
    RecyclerView recyclerViewTuner;
    TunerGroupItem tunerGroupOpened = null;
    final int DIALOG_TUNER_PRIORITY = 10;
    TunerExpandableLayoutHelper tunerExpandableLayoutHelper;
    int childSelected = 0;
    ArrayList<HashMap> updatedTunerValues;
    MaterialButton saveTunerValues;
    MaterialButton cancelTunerUpdate;

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
        radioButtonSystem = view.findViewById(R.id.radioBtnSystem);
        radioButtonZone = view.findViewById(R.id.radioBtnZone);
        radioButtonModule = view.findViewById(R.id.radioBtnModule);

        saveTunerValues = view.findViewById(R.id.buttonSave);
        cancelTunerUpdate = view.findViewById(R.id.buttonCancel);
        saveTunerValues.setEnabled(false);
        //Default Show System Tuners
        radioGroupTuners.check(R.id.radioBtnSystem);
        getSystemTuners();

        reasonLabel = view.findViewById(R.id.textReasonLabel);
        String text = "<font color=#E24301>*</font> <font color=#999999>Reason for Change</font>";
        reasonLabel.setText(Html.fromHtml(text));

        radioGroupTuners.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnSystem) {
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
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_apply_tuner, null);
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setView(dialogView);
            AlertDialog valueDialog = dialog.show();

            valueDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            LinearLayout linearLayoutBody = dialogView.findViewById(R.id.layoutConfirmBody);
            View tunerItemViewTop = inflater.inflate(R.layout.dialog_apply_tuner_item, null);
            linearLayoutBody.addView(tunerItemViewTop);
            for (HashMap newTunerValueItem : updatedTunerValues) {
                Toast.makeText(getActivity(), "Tuner Values Updated: " + newTunerValueItem.get("dis").toString() + " tunerValue:" + newTunerValueItem.get("newValue").toString(), Toast.LENGTH_SHORT).show();
                View tunerItemViewBody = inflater.inflate(R.layout.dialog_apply_tuner_item, null);

                LinearLayout layoutValuesTop = tunerItemViewBody.findViewById(R.id.layoutBuilding);
                layoutValuesTop.setVisibility(View.GONE);

                LinearLayout layoutValues = tunerItemViewBody.findViewById(R.id.layoutValues);
                layoutValues.setVisibility(View.VISIBLE);

                TextView textView_SectionLabel = tunerItemViewBody.findViewById(R.id.textView_SectionLabel);
                TextView textView_Section = tunerItemViewBody.findViewById(R.id.textView_Section);
                TextView textView_tuner = tunerItemViewBody.findViewById(R.id.textView_tuner);
                TextView textView_level = tunerItemViewBody.findViewById(R.id.textView_level);
                TextView textView_oldValue = tunerItemViewBody.findViewById(R.id.textView_oldValue);
                TextView textView_newLevel = tunerItemViewBody.findViewById(R.id.textView_newLevel);
                TextView textView_newValue = tunerItemViewBody.findViewById(R.id.textView_newValue);
                textView_SectionLabel.setText("System");
                textView_Section.setText(":");

                String tunerName = newTunerValueItem.get("dis").toString();
                tunerName = tunerName.substring(tunerName.lastIndexOf("-") + 1);
                if (newTunerValueItem.containsKey("unit")) {
                    textView_tuner.setText(tunerName + " " + newTunerValueItem.get("unit").toString().toUpperCase() + " | ");
                } else {
                    textView_tuner.setText(tunerName + " | ");
                }
                textView_level.setText("Level 14 : ");
                textView_newLevel.setText("Level 14 : ");
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
                valueDialog.dismiss();
                saveTunerValues.setEnabled(false);
            });
            buttonCancelTuners.setOnClickListener(dialogV -> valueDialog.dismiss());
            linearLayoutBody.invalidate();
            dialogView.invalidate();
            valueDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        });
    }


    private void updateData() {
        tunerMap.clear();
        expandableListDetail.clear();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        Log.i("TunersUI", "Equips:" + equips);
        for (Map m : equips) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \"" + m.get("id") + "\"");
            ArrayList tunerList = new ArrayList();

            for (Map t : tuners) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }

            ArrayList<HashMap> userIntents = CCUHsApi.getInstance().readAll("userIntent and equipRef == \"" + m.get("id") + "\"");

            for (Map t : userIntents) {
                if (!t.get("dis").toString().contains("desired")) {
                    tunerList.add(t.get("dis").toString());
                    tunerMap.put(t.get("dis").toString(), t.get("id").toString());
                }
            }
            expandableListDetail.put(m.get("dis").toString(), tunerList);
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

        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("tunerGroup");

        // Group by countryName
        Map<String, List<HashMap>> groupByTuner = equips.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
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
        DialogTunerPriorityArray tunerPriorityArray = DialogTunerPriorityArray.newInstance(item, tunerGroupOpened);
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
                    TunerGroupItem tunerGroupSelected = (TunerGroupItem) bundle.getSerializable("Tuner_Group_Selected");
                    String tunerValue = bundle.getString("Tuner_Value_Selected");
                    Toast.makeText(getActivity(), "TunerUI-HashMap: " + tunerItemSelected.get("dis") + " clicked\n" +
                            " tunerGroupSelected:" + tunerGroupSelected.getName() + " tunerValue:" + tunerValue, Toast.LENGTH_SHORT).show();
                    tunerItemSelected.put("newValue", tunerValue);
                    tunerExpandableLayoutHelper.updateTuner(tunerGroupSelected.getName(), tunerItemSelected, childSelected);
                    updatedTunerValues.add(tunerItemSelected);
                    if (updatedTunerValues.size() > 0) {
                        saveTunerValues.setEnabled(true);
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
}
