package a75f.io.renatus;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;

import static a75f.io.renatus.ZoneFragmentTemp.getPointVal;

/**
 * Created by samjithsadasivan on 1/31/19.
 */
public class EquipTempExpandableListAdapter extends BaseExpandableListAdapter
{
    private Context                       context;
    private List<String>                  expandableListTitle;
    private HashMap<String, List<String>> expandableListDetail;
    private HashMap<String, String>       idMap;
    
    public EquipTempExpandableListAdapter(Context context, List<String> expandableListTitle,
                                      HashMap<String, List<String>> expandableListDetail, HashMap idmap) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.idMap = idmap;
    }
    
    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                                        .get(expandedListPosition);
    }
    
    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final String expandedListText = (String) getChild(listPosition, expandedListPosition);


        if(!expandedListText.equals("schedule")) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.tuner_list_item, null);


            TextView expandedListTextView = (TextView) convertView
                    .findViewById(R.id.expandedListItemName);
            TextView expandedListTextVal = (TextView) convertView
                    .findViewById(R.id.expandedListItemVal);


            expandedListTextView.setText(expandedListText);
            expandedListTextVal.setText(""+getPointVal(idMap.get(expandedListText)));
        }
        else
        {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String equipId = idMap.get(expandedListText);
            convertView = layoutInflater.inflate(R.layout.temp_schedule, null);
            TextView scheduleStatus = convertView.findViewById(R.id.schedule_status_tv);
            scheduleStatus.setText("Status & " + equipId + " :: " + expandedListText);

            Schedule schedule = Schedule.getScheduleByEquipId(equipId);


            CCUHsApi.getInstance().readHDictById(equipId);


            Spinner scheduleSpinner = convertView.findViewById(R.id.schedule_spinner);


            ImageButton scheduleImageButton = convertView.findViewById(R.id.schedule_edit_button);
            TextView vacationStatus = convertView.findViewById(R.id.vacation_status);
            ImageButton vacationEditButton = convertView.findViewById(R.id.vacation_edit_button);


        }

        return convertView;
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                                        .size();
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
}
