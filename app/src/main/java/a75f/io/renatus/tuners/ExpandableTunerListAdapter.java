package a75f.io.renatus.tuners;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.R;

/**
 * Created by samjithsadasivan on 1/17/19.
 */

public class ExpandableTunerListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> expandableListTitle;
    //private HashMap<String, List<String>> expandableListDetail;
    private HashMap<String, List<HashMap>> expandableListDetail;
    private HashMap<String, String> idMap;

    public ExpandableTunerListAdapter(Context context, List<String> expandableListTitle,
                                      HashMap<String, List<HashMap>> expandableListDetail, HashMap idmap) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.idMap = idmap;
        Log.i("TunersUI", "expandableListTitle:" + expandableListTitle);
        Log.i("TunersUI", "expandableListDetail:" + expandableListDetail);
        Log.i("TunersUI", "idMap:" + idmap);
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition)).get(expandedListPosition);
    }

    public List<HashMap> getChildList(String tunerGroupTitle) {
        Log.i("TunersUI","getChildList:"+this.expandableListDetail.get(tunerGroupTitle));
        return this.expandableListDetail.get(tunerGroupTitle);
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        //final String expandedListText = (String) getChild(listPosition, expandedListPosition);
        final HashMap expandedListText = (HashMap) getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.tuner_list_item, null);
            GridView tunerGrid = convertView.findViewById(R.id.tunerGrid);
            Log.i("TunersUI","tunerGroupTitle:"+this.expandableListTitle.get(listPosition));
            Log.i("TunersUI","tunerGridAdapter:");
            tunerGrid.setAdapter(new TunerGridViewAdapter(this.context, getChildList(this.expandableListTitle.get(listPosition))));
        }
        return convertView;
        /*final HashMap expandedListText = (HashMap) getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.tuner_list_item, null);
        }
        TextView expandedListTextView = convertView.findViewById(R.id.expandedListItemName);
        TextView expandedListTextVal =  convertView.findViewById(R.id.expandedListItemVal);
        expandedListTextView.setText(expandedListText.get("dis").toString());
        //expandedListTextVal.setText(""+getTuner(idMap.get(expandedListText)));
        return convertView;*/
    }

    @Override
    public int getChildrenCount(int listPosition) {
        //Log.i("TunersUI", "listPosition:" + listPosition);
        Log.i("TunersUI", "expandableListTitle:" + expandableListTitle.get(listPosition));
        //Log.i("TunersUI", "expandableListDetail:" + expandableListDetail.get(expandableListTitle.get(listPosition)));
        //Log.i("TunersUI", "expandableListDetailSize:" + expandableListDetail.get(expandableListTitle.get(listPosition)).size());
        if (expandableListDetail.get(expandableListTitle.get(listPosition)) != null) {
            return (expandableListDetail.get(expandableListTitle.get(listPosition))).size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getGroup(int listPosition) {
        return this.expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount() {
        return this.expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String listTitle = (String) getGroup(listPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.tuner_list_group, null);
        }
        TextView listTitleTextView = (TextView) convertView
                .findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(listTitle);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }

    public static double getTuner(String id) {
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

    class TunerGridViewAdapter extends BaseAdapter {
        private Context context;
        private List<HashMap> tunerValueList;

        public TunerGridViewAdapter(Context context, List<HashMap> tunerValueList) { //Changed
            this.context = context;
            this.tunerValueList = tunerValueList;
        }

        @Override
        public int getCount() {
            return tunerValueList.size();
        }

        @Override
        public Object getItem(int position) {
            return tunerValueList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final HashMap expandedListText = tunerValueList.get(position);
            Log.i("TunersUI", "tunerValueLabel:" + expandedListText.get("dis").toString());
            Log.i("TunersUI", "expandedListText:" + expandedListText.size());
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_tunergrid, null);
            }
            TextView expandedListTextView = convertView.findViewById(R.id.expandedListItemName);
            TextView expandedListTextVal = convertView.findViewById(R.id.expandedListItemVal);
            expandedListTextView.setText(expandedListText.get("dis").toString());

            return convertView;
        }
    }
}
