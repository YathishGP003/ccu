package a75f.io.renatus.tuners;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TunerExpandableLayoutHelper implements TunerGroupChangeListener {

    //data list
    private LinkedHashMap<TunerGroupItem, List<HashMap>> mSectionDataMap = new LinkedHashMap<TunerGroupItem, List<HashMap>>();
    private ArrayList<Object> mDataArrayList = new ArrayList<Object>();

    //section map
    //TODO : look for a way to avoid this
    private HashMap<String, TunerGroupItem> mSectionMap = new HashMap<String, TunerGroupItem>();

    //adapter
    private TunerExpandableGridAdapter mSectionedExpandableGridAdapter;

    //recycler view
    RecyclerView mRecyclerView;

    public TunerExpandableLayoutHelper(Context context, RecyclerView recyclerView, TunerItemClickListener itemClickListener, TunerUndoClickListener undoClickListener,
                                       int gridSpanCount, String tunerGroupType) {

        //setting the recycler view
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, gridSpanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        mSectionedExpandableGridAdapter = new TunerExpandableGridAdapter(context, mDataArrayList,
                gridLayoutManager, itemClickListener, undoClickListener,this,tunerGroupType);
        recyclerView.setAdapter(mSectionedExpandableGridAdapter);
        mRecyclerView = recyclerView;
    }

    public void notifyDataSetChanged() {
        //TODO : handle this condition such that these functions won't be called if the recycler view is on scroll
        generateDataList();
        mSectionedExpandableGridAdapter.notifyDataSetChanged();
    }

    public void notifyDataSaveChanged() {
        mSectionedExpandableGridAdapter.refreshData();
    }

    public void addSection(String section, List<HashMap> items) {
        TunerGroupItem newSection;
        mSectionMap.put(section, (newSection = new TunerGroupItem(section)));
        ArrayList<HashMap> sectionList = new ArrayList<>();
        ArrayList<String> nameList = new ArrayList<>();

        for (HashMap p: items){
            String dis = p.get("dis").toString();
            if (!nameList.contains(dis.substring(dis.lastIndexOf("-") + 1))){
                nameList.add(dis.substring(dis.lastIndexOf("-") + 1));
                sectionList.add(p);
            }
        }
        mSectionDataMap.put(newSection, sectionList);
    }

    public void addItem(String section, HashMap item) {
        mSectionDataMap.get(mSectionMap.get(section)).add(item);
    }

    public void updateTuner(TunerGroupItem section, HashMap item, HashMap oldItem) {
        Log.i("TunersUI", "section:" + section + " hashmap:" + item);
        for(HashMap m: mSectionDataMap.get(section)){
            if (m.get("id").toString().equals(oldItem.get("id").toString())){
                mSectionDataMap.get(section).set(mSectionDataMap.get(section).indexOf(m), item);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void removeItem(String section, HashMap item) {
        mSectionDataMap.get(mSectionMap.get(section)).remove(item);
    }

    public void removeSection(String section) {
        mSectionDataMap.remove(mSectionMap.get(section));
        mSectionMap.remove(section);
    }

    private void generateDataList() {
        mDataArrayList.clear();
        for (Map.Entry<TunerGroupItem, List<HashMap>> entry : mSectionDataMap.entrySet()) {
            TunerGroupItem key;
            mDataArrayList.add((key = entry.getKey()));
            if (key.isExpanded)
                mDataArrayList.addAll(entry.getValue());
        }
    }

    @Override
    public void onSectionStateChanged(TunerGroupItem section, boolean isOpen) {
        if (!mRecyclerView.isComputingLayout()) {
            section.isExpanded = isOpen;
            notifyDataSetChanged();
        }
    }
}
