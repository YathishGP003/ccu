package a75f.io.renatus.tuners;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
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
                                      HashMap<String, List<HashMap>> expandableListDetail, HashMap idMap) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.idMap = idMap;
        CcuLog.i(L.TAG_CCU_TUNERS_UI, "expandableListTitle:" + expandableListTitle);
        CcuLog.i(L.TAG_CCU_TUNERS_UI, "expandableListDetail:" + expandableListDetail);
        CcuLog.i(L.TAG_CCU_TUNERS_UI, "idMap:" + idMap);
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition)).get(expandedListPosition);
    }

    public List<HashMap> getChildList(String tunerGroupTitle) {
        CcuLog.i(L.TAG_CCU_TUNERS_UI,"getChildList:"+this.expandableListDetail.get(tunerGroupTitle));
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
            CcuLog.i(L.TAG_CCU_TUNERS_UI,"tunerGroupTitle:"+this.expandableListTitle.get(listPosition));
            CcuLog.i(L.TAG_CCU_TUNERS_UI,"tunerGridAdapter:");
            tunerGrid.setAdapter(new TunerGridViewAdapter(this.context, getChildList(this.expandableListTitle.get(expandedListPosition))));
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        CcuLog.i(L.TAG_CCU_TUNERS_UI, "expandableListTitle:" + expandableListTitle.get(listPosition));
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
        if (values != null && !values.isEmpty()) {
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
            CcuLog.i(L.TAG_CCU_TUNERS_UI, "tunerValueLabel:" + expandedListText.get("dis").toString());
            CcuLog.i(L.TAG_CCU_TUNERS_UI, "expandedListText:" + expandedListText.size());
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_tunergrid, null);
            }
            TextView expandedListTextView = convertView.findViewById(R.id.expandedListItemName);
            TextView expandedListTextVal = convertView.findViewById(R.id.expandedListItemVal);
            expandedListTextView.setText(expandedListText.get("dis").toString());
            LinearLayout tunerGridBg = convertView.findViewById(R.id.tunerGridBg);
            return convertView;
        }
    }
}
