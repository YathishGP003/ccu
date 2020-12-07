package a75f.io.renatus.tuners;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
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
import a75f.io.renatus.DialogOAOProfile;
import a75f.io.renatus.DialogSmartStatProfiling;
import a75f.io.renatus.R;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class TunerFragment extends BaseDialogFragment implements TunerItemClickListener
{
    public static final String ID = TunerFragment.class.getSimpleName();
    ExpandableListView            expandableListView;
    HashMap<String, List<HashMap>> expandableListDetail;

    HashMap<String, String> tunerMap = new HashMap();

    RadioGroup radioGroupTuners;
    RadioButton radioButtonSystem;
    RadioButton radioButtonZone;
    RadioButton radioButtonModule;
    TextView reasonLabel;
    RecyclerView recyclerViewTuner;
    TunerGroupItem tunerGroupOpened=null;
    public TunerFragment()
    {
    }
    
    public static TunerFragment newInstance()
    {
        return new TunerFragment();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_tuner_editor, container, false);
        //ButterKnife.bind(this, rootView);
        return rootView;
    }
    
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        recyclerViewTuner = view.findViewById(R.id.recyclerTuner);
        expandableListView = view.findViewById(R.id.expandableListView);

        expandableListDetail = new HashMap<>();
        //updateData();



        radioGroupTuners = view.findViewById(R.id.radioGrpTuner);
        radioButtonSystem = view.findViewById(R.id.radioBtnSystem);
        radioButtonZone = view.findViewById(R.id.radioBtnZone);
        radioButtonModule = view.findViewById(R.id.radioBtnModule);

        //Default Show System Tuners
        radioGroupTuners.check(R.id.radioBtnSystem);
        getSystemTuners();

        reasonLabel = view.findViewById(R.id.textReasonLabel);
        String text = "<font color=#E24301>*</font> <font color=#999999>Reason for Change</font>";
        reasonLabel.setText(Html.fromHtml(text));

        radioGroupTuners.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnSystem) {
                Log.i("TunersUI","Selected:radioBtnSystem");
                getSystemTuners();
            } else if (checkedId == R.id.radioBtnZone) {
                Log.i("TunersUI","Selected:radioBtnZone");
                updateData();
            } else if (checkedId == R.id.radioBtnModule) {
                Log.i("TunersUI","Selected:radioBtnModule");
                updateData();
            }
        });
    }


    private void updateData() {
        tunerMap.clear();
        expandableListDetail.clear();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip");
        Log.i("TunersUI","Equips:"+equips);
        for (Map m : equips) {
            ArrayList<HashMap> tuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \""+m.get("id")+"\"");
            ArrayList tunerList = new ArrayList();
        
            for (Map t : tuners) {
                tunerList.add(t.get("dis").toString());
                tunerMap.put(t.get("dis").toString(), t.get("id").toString());
            }
            
            ArrayList<HashMap> userIntents = CCUHsApi.getInstance().readAll("userIntent and equipRef == \""+m.get("id")+"\"");
            
            for (Map t : userIntents) {
                if(!t.get("dis").toString().contains("desired")) {
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
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
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
            protected Void doInBackground( final String ... params ) {
                CCUHsApi.getInstance().writePoint(id, TunerConstants.TUNER_EQUIP_VAL_LEVEL, "ccu", val, 0);
                CCUHsApi.getInstance().writeHisValById(id, val);
                return null;
            }
        
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private void getSystemTuners() {
        TunerExpandableLayoutHelper tunerExpandableLayoutHelper = new TunerExpandableLayoutHelper(getActivity(), recyclerViewTuner, this, 2);

        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("tunerGroup");

        // Group by countryName
        Map<String, List<HashMap>> groupByTuner = equips.stream().collect(Collectors.groupingBy(p -> p.get("tunerGroup").toString()));
        Map<String, List<HashMap>> sortedGroupTuner = new TreeMap<>(groupByTuner);
        System.out.println("After Sorting:");
        Set set2 = sortedGroupTuner.entrySet();
        Iterator iterator2 = set2.iterator();
        while(iterator2.hasNext()) {
            Map.Entry me2 = (Map.Entry)iterator2.next();
            Log.i("TunersUI","Sorting-"+me2.getKey());
        }
        for(String groupTitle: sortedGroupTuner.keySet()){
            tunerExpandableLayoutHelper.addSection(groupTitle, sortedGroupTuner.get(groupTitle));
            tunerExpandableLayoutHelper.notifyDataSetChanged();
        }
    }
    @Override
    public void itemClicked(HashMap item) {
        Toast.makeText(getActivity(), "TunerUI-HashMap: " + item.get("dis") + " clicked\n" +
                " minValue:"+item.get("minVal")+" maxValue:"+item.get("maxVal")+" incrementBy:"+item.get("incrementVal"), Toast.LENGTH_SHORT).show();
        DialogTunerPriorityArray tunerPriorityArray = DialogTunerPriorityArray.newInstance(item,tunerGroupOpened);
        showDialogFragment(tunerPriorityArray, DialogTunerPriorityArray.ID);
    }

    @Override
    public void itemClicked(TunerGroupItem section) {
        Toast.makeText(getActivity(), "TunerUI-Section: " + section.getName() + " clicked", Toast.LENGTH_SHORT).show();
        tunerGroupOpened = section;
    }
    @Override
    public String getIdString() {
        return ID;
    }
}
