package a75f.io.renatus;


import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.renatus.schedules.SchedulerFragment;

import static a75f.io.renatus.ZoneFragmentTemp.getPointVal;


/**
 * Created by samjithsadasivan on 1/31/19.
 */
public class EquipTempExpandableListAdapter extends BaseExpandableListAdapter
{
    private Fragment                      mFragment;
    private List<String>                  expandableListTitle;
    private HashMap<String, List<String>> expandableListDetail;
    private HashMap<String, String>       idMap;

    public EquipTempExpandableListAdapter(Fragment fragment, List<String> expandableListTitle,
                                          HashMap<String, List<String>> expandableListDetail, HashMap idmap)
    {
        this.mFragment = fragment;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.idMap = idmap;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition)
    {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                                        .get(expandedListPosition);
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent)
    {
        final String expandedListText = (String) getChild(listPosition, expandedListPosition);
        Log.i("Scheduler", "IDE Too Slow: " + expandedListText);

        if (!expandedListText.startsWith("schedule"))
        {
            LayoutInflater layoutInflater = (LayoutInflater) this.mFragment.getContext()
                                                                           .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.tuner_list_item, parent, false);


            TextView expandedListTextView = convertView
                    .findViewById(R.id.expandedListItemName);
            TextView expandedListTextVal = convertView
                    .findViewById(R.id.expandedListItemVal);


            expandedListTextView.setText(expandedListText);
            expandedListTextVal.setText("" + getPointVal(idMap.get(expandedListText)));
        } else
        {
            LayoutInflater layoutInflater = (LayoutInflater) this.mFragment.getContext()
                                                                           .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String equipId = idMap.get(expandedListText);
            convertView = layoutInflater.inflate(R.layout.temp_schedule, parent, false);
            TextView    scheduleStatus      = convertView.findViewById(R.id.schedule_status_tv);
            Spinner     scheduleSpinner     = convertView.findViewById(R.id.schedule_spinner);
            ImageButton scheduleImageButton = convertView.findViewById(R.id.schedule_edit_button);
            TextView    vacationStatusTV    = convertView.findViewById(R.id.vacation_status);
            ImageButton vacationEditButton  = convertView.findViewById(R.id.vacation_edit_button);
            HashMap     equipHashMap        = CCUHsApi.getInstance().readMapById(equipId);


            String zoneId         = Schedule.getZoneIdByEquipId(equipId);
            String status         = ScheduleProcessJob.getZoneStatusString(zoneId);
            String vacationStatus = ScheduleProcessJob.getVacationStateString(zoneId);
            vacationStatusTV.setText(vacationStatus);
            scheduleStatus.setText(status);


            Schedule schedule = Schedule.getScheduleByEquipId(equipId);

            Schedule vacationSchedule = Schedule.getVacationByEquipId(equipId);
            scheduleImageButton.setTag(schedule.getId());
            scheduleImageButton.setOnClickListener(v ->
                                                   {
                                                       SchedulerFragment schedulerFragment    = SchedulerFragment.newInstance((String) v.getTag());
                                                       FragmentManager   childFragmentManager = mFragment.getFragmentManager();
                                                       childFragmentManager.beginTransaction()
                                                                           .add(R.id.zone_fragment_temp, schedulerFragment)
                                                                           .addToBackStack("schedule").commit();

                                                       schedulerFragment.setOnExitListener(() -> Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show());
                                                   });

            if (schedule.isZoneSchedule())
            {
                scheduleSpinner.setSelection(1);
                scheduleImageButton.setVisibility(View.VISIBLE);
            } else if (schedule.isNamedSchedule())
            {
                scheduleSpinner.setSelection(2);
                scheduleImageButton.setVisibility(View.VISIBLE);
            } else
            {
                scheduleSpinner.setSelection(0);
                scheduleImageButton.setVisibility(View.GONE);
            }


            scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    System.out.println("Item Selected Listener: " + position);

                    if (position == 0)
                    {
                        if (schedule.isZoneSchedule())
                        {
                            schedule.setDisabled(true);
                            CCUHsApi.getInstance().updateSchedule(schedule);
                        }
                        scheduleImageButton.setVisibility(View.GONE);

                    } else if (position == 1)
                    {
                        if (schedule.isZoneSchedule() && schedule.getMarkers().contains("disabled"))
                        {
                            schedule.setDisabled(false);
                            CCUHsApi.getInstance().updateZoneSchedule(schedule, zoneId);
                            scheduleImageButton.setTag(schedule.getId());
                        } else
                        {

                            Zone     zone         = Schedule.getZoneforEquipId(equipId);
                            Schedule scheduleById = null;
                            Log.d("CCU_UI"," Edit schedule for "+zone.getDisplayName()+" : "+zone.getId());
                            if (zone.hasSchedule())
                            {
                                scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                                scheduleById.setDisabled(false);
                                CCUHsApi.getInstance().updateZoneSchedule(scheduleById, zone.getId());


                            } else if (!zone.hasSchedule())
                            {
                                zone.setScheduleRef(DefaultSchedules.generateDefaultSchedule(true, zone.getId()));
                                CCUHsApi.getInstance().updateZone(zone, zone.getId());
                                scheduleById = CCUHsApi.getInstance().getScheduleById(zone.getScheduleRef());
                            }
                            scheduleImageButton.setTag(scheduleById.getId());
                            scheduleImageButton.setVisibility(View.VISIBLE);
                        }
                    } else
                    {
                        //list named schedules
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });


        }

        return convertView;
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition)
    {
        return expandedListPosition;
    }

    @Override
    public int getChildrenCount(int listPosition)
    {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                                        .size();
    }

    @Override
    public Object getGroup(int listPosition)
    {
        return this.expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount()
    {
        return this.expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition)
    {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent)
    {
        String listTitle = (String) getGroup(listPosition);
        if (convertView == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater) this.mFragment.getContext().
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
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition)
    {
        return true;
    }

    public static double getTuner(String id)
    {
        CCUHsApi  hayStack = CCUHsApi.getInstance();
        ArrayList values   = hayStack.readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null)
                {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
}
