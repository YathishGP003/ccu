package a75f.io.renatus.tuners;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    public TunerExpandableLayoutHelper(Context context, RecyclerView recyclerView, TunerItemClickListener itemClickListener,
                                       int gridSpanCount) {

        //setting the recycler view
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, gridSpanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        mSectionedExpandableGridAdapter = new TunerExpandableGridAdapter(context, mDataArrayList,
                gridLayoutManager, itemClickListener, this);
        recyclerView.setAdapter(mSectionedExpandableGridAdapter);
        mRecyclerView = recyclerView;
    }

    public void notifyDataSetChanged() {
        //TODO : handle this condition such that these functions won't be called if the recycler view is on scroll
        generateDataList();
        mSectionedExpandableGridAdapter.notifyDataSetChanged();
    }

    public void addSection(String section, List<HashMap> items) {
        TunerGroupItem newSection;
        mSectionMap.put(section, (newSection = new TunerGroupItem(section)));
        mSectionDataMap.put(newSection, items);
    }

    public void addItem(String section, HashMap item) {
        mSectionDataMap.get(mSectionMap.get(section)).add(item);
    }

    public void updateTuner(String section, HashMap item, HashMap oldItem) {
        Log.i("TunersUI", "section:" + section + " hashmap:" + item);
        mSectionDataMap.get(mSectionMap.get(section)).set(mSectionDataMap.get(mSectionMap.get(section)).indexOf(oldItem), item);
        notifyDataSetChanged();
        //mSectionedExpandableGridAdapter.notifyItemChanged(position);
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
