package a75f.io.renatus.tuners;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.R;

public class TunerExpandableGridAdapter extends RecyclerView.Adapter<TunerExpandableGridAdapter.ViewHolder> {

    //data array
    private final ArrayList<Object> mDataArrayList;

    //context
    private final Context mContext;

    //listeners
    private final TunerItemClickListener mItemClickListener;
    private final TunerUndoClickListener mUndoClickListener;
    private final TunerGroupChangeListener mSectionStateChangeListener;

    //view type
    private static final int VIEW_TYPE_SECTION = R.layout.item_tunergroup_title;
    private static final int VIEW_TYPE_ITEM = R.layout.item_tunervalue_child;

    int lastExpandedPosition;
    int childIndexPosition = 0;
    TunerGroupItem previousOpenGroup = null;
    String tunerGroupType = "Building";

    public TunerExpandableGridAdapter(Context context, ArrayList<Object> dataArrayList,
                                      final GridLayoutManager gridLayoutManager, TunerItemClickListener itemClickListener,
                                      TunerUndoClickListener undoClickListener, TunerGroupChangeListener sectionStateChangeListener, String tunerGroupType) {
        mContext = context;
        mItemClickListener = itemClickListener;
        mSectionStateChangeListener = sectionStateChangeListener;
        mDataArrayList = dataArrayList;
        this.tunerGroupType = tunerGroupType;
        this.mUndoClickListener = undoClickListener;

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return isSection(position) ? gridLayoutManager.getSpanCount() : 1;
            }
        });
    }

    private boolean isSection(int position) {
        return mDataArrayList.get(position) instanceof TunerGroupItem;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(viewType, parent, false), viewType);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        switch (holder.viewType) {
            case VIEW_TYPE_ITEM:
                childIndexPosition++;
                final HashMap tunerItem = (HashMap) mDataArrayList.get(position);
                Log.i("TunersUI", "tunerItem:" + tunerItem);
                String tunerName = tunerItem.get("dis").toString();

                holder.itemTextView.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1));
                if (tunerItem.containsKey("newValue")) {
                    if (tunerItem.get("newValue") == null){
                        holder.itemTextValueView.setText("-");
                        if (tunerItem.containsKey("reset")){
                            holder.imgBtnUndo.setVisibility(View.VISIBLE);
                        } else {
                            holder.imgBtnUndo.setVisibility(View.GONE);
                        }
                    } else {
                        holder.itemTextValueView.setText(tunerItem.get("newValue").toString());
                        if (tunerItem.containsKey("hideRefresh")){
                            holder.imgBtnUndo.setVisibility(View.GONE);
                        } else {
                            holder.imgBtnUndo.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    holder.imgBtnUndo.setVisibility(View.GONE);
                    if (getTunerValue(tunerItem.get("id").toString()) != null){
                        holder.itemTextValueView.setText(String.valueOf(getTunerValue(tunerItem.get("id").toString())));
                    } else {
                        holder.itemTextValueView.setText("-");
                    }
                }
                if (tunerItem.containsKey("unit")) {
                    holder.itemTextView.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1) + " (" + tunerItem.get("unit").toString().toUpperCase() + ")");
                } else {
                    holder.itemTextView.setText(tunerName.substring(tunerName.lastIndexOf("-") + 1));
                }

                if (childIndexPosition % 2 == 0) {
                    holder.itemDivider.setVisibility(View.GONE);
                } else {
                    holder.itemDivider.setVisibility(View.VISIBLE);
                }

                holder.imgBtnUndo.setOnClickListener(v -> {
                    holder.imgBtnUndo.setVisibility(View.GONE);
                    tunerItem.remove("newValue");
                    tunerItem.remove("newLevel");
                    if (getTunerValue(tunerItem.get("id").toString()) != null){
                        holder.itemTextValueView.setText(String.valueOf(getTunerValue(tunerItem.get("id").toString())));
                    } else {
                        holder.itemTextValueView.setText("-");
                    }
                    mUndoClickListener.onUndoClick(tunerItem);
                    notifyItemChanged(position);
                });

                holder.view.setOnClickListener(v -> mItemClickListener.itemClicked(tunerItem, position));
                if ((position / 2) % 2 == 0) {
                    holder.tunerGridBg.setBackgroundColor(Color.parseColor("#FFFFFF"));
                } else {
                    holder.tunerGridBg.setBackgroundColor(Color.parseColor("#F9F9F9"));
                }
                break;
            case VIEW_TYPE_SECTION:
                childIndexPosition = 0;
                final TunerGroupItem section = (TunerGroupItem) mDataArrayList.get(position);
                previousOpenGroup = section;
                holder.tunerGroupTitle.setText(section.getName());
                holder.tunerGroupTitle.setOnClickListener(v -> {
                    mItemClickListener.itemClicked(section);
                    if (holder.tunerGroupToggle.isChecked()) {
                        mSectionStateChangeListener.onSectionStateChanged(section, false);
                    } else {
                        mSectionStateChangeListener.onSectionStateChanged(section, true);
                        lastExpandedPosition = position;
                    }
                });
                holder.tunerGroupToggle.setOnCheckedChangeListener(null);
                holder.tunerGroupToggle.setChecked(section.isExpanded);
                if (lastExpandedPosition != -1) {
                    mSectionStateChangeListener.onSectionStateChanged(previousOpenGroup, false);
                }
                holder.tunerGroupToggle.setOnCheckedChangeListener((buttonView, isChecked)
                        -> {
                    mItemClickListener.itemClicked(section);
                    mSectionStateChangeListener.onSectionStateChanged(section, isChecked);
                });
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + holder.viewType);
        }
    }

    @Override
    public int getItemCount() {
        return mDataArrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (isSection(position))
            return VIEW_TYPE_SECTION;
        else return VIEW_TYPE_ITEM;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        //common
        View view;
        int viewType;

        //for TunerGroupTitle
        TextView tunerGroupTitle;
        ToggleButton tunerGroupToggle;

        //for TunerItem
        TextView itemTextView;
        TextView itemTextValueView;
        CardView tunerGridBg;
        ImageButton imgBtnUndo;
        View itemDivider;

        public ViewHolder(View view, int viewType) {
            super(view);
            this.viewType = viewType;
            this.view = view;
            if (viewType == VIEW_TYPE_ITEM) {
                itemTextView = view.findViewById(R.id.expandedListItemName);
                itemTextValueView = view.findViewById(R.id.expandedListItemVal);
                itemDivider = view.findViewById(R.id.tunerDivider);
                tunerGridBg = view.findViewById(R.id.tunerGridBg);
                imgBtnUndo = view.findViewById(R.id.imgBtnUndo);
            } else {
                tunerGroupTitle = view.findViewById(R.id.groupTitle);
                tunerGroupToggle = view.findViewById(R.id.toggleTunerGroup);
            }
        }
    }

    public Double getTunerValue(String id) {
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
        return null;
    }

    public void refreshData(){
        for (Object object: mDataArrayList){
            if (!(object instanceof TunerGroupItem)){
                HashMap tunerItem = (HashMap) object;
                if (tunerItem.get("reset") == null && tunerItem.containsKey("newValue")){
                    tunerItem.put("hideRefresh",true);
                }
                if (tunerItem.containsKey("reset")){
                    tunerItem.remove("reset");
                }
            }
        }
        notifyDataSetChanged();
    }
}

