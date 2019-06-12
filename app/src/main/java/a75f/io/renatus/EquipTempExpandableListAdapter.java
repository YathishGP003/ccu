package a75f.io.renatus;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.StandaloneScheduler;
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
    
    Schedule mSchedule = null;
    int mScheduleType;

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
        //Log.i("Scheduler", "IDE Too Slow: " + expandedListText);

        if (!expandedListText.startsWith("schedule") && (!expandedListText.startsWith("smartstat")))
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
            TextView vacationStatusTV = convertView.findViewById(R.id.vacation_status);
            ImageButton vacationEditButton = convertView.findViewById(R.id.vacation_edit_button);
            LinearLayout smartStatLayout = convertView.findViewById(R.id.ss_layout);
            TextView ssStatus = convertView.findViewById(R.id.ss_conditioning_status_tv);
            Spinner ssCondModeSpinner = convertView.findViewById(R.id.ss_conditioning_spinner);
            Spinner ssFanModeSpinner = convertView.findViewById(R.id.ss_fanmode_spinner);
            LinearLayout ssHumiDifier = convertView.findViewById(R.id.ss_humidity_layout);
            TextView ssHumiDifierTV = convertView.findViewById(R.id.ss_target_humidity_tv);
            Spinner ssHumiDifierSpinner = convertView.findViewById(R.id.ss_humidity_spinner);
            HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);

            String zoneId = Schedule.getZoneIdByEquipId(equipId);
            String status = ScheduleProcessJob.getZoneStatusString(zoneId, equipId);
            String vacationStatus = ScheduleProcessJob.getVacationStateString(zoneId);
            vacationStatusTV.setText(vacationStatus);
            scheduleStatus.setText(status);
            String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \""+equipId+"\"");
            mScheduleType = (int)CCUHsApi.getInstance().readPointPriorityVal(scheduleTypeId);
    
            mSchedule = Schedule.getScheduleByEquipId(equipId);

            Schedule vacationSchedule = Schedule.getVacationByEquipId(equipId);
            scheduleImageButton.setTag(mSchedule.getId());
            scheduleImageButton.setOnClickListener(v ->
                                                   {
                                                       SchedulerFragment schedulerFragment    = SchedulerFragment.newInstance((String) v.getTag());
                                                       FragmentManager   childFragmentManager = mFragment.getFragmentManager();
                                                       childFragmentManager.beginTransaction()
                                                                           .add(R.id.zone_fragment_temp, schedulerFragment)
                                                                           .addToBackStack("schedule").commit();

                                                       schedulerFragment.setOnExitListener(() -> {
                                                           Toast.makeText(v.getContext(), "Refresh View", Toast.LENGTH_LONG).show();
                                                           mSchedule = Schedule.getScheduleByEquipId(equipId);
                                                           //CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId)
                                                           ScheduleProcessJob.updateSchedules();
                                                           
                                                           
                                                       });
                                                   });
            
            scheduleSpinner.setSelection(mScheduleType);
            if (mSchedule.isZoneSchedule())
            {
                scheduleImageButton.setVisibility(View.VISIBLE);
            } else if (mSchedule.isNamedSchedule())
            {
                scheduleImageButton.setVisibility(View.VISIBLE);
            } else
            {
                scheduleImageButton.setVisibility(View.GONE);
            }


            scheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    if (position == 0)
                    {
                        if (mSchedule.isZoneSchedule())
                        {
                            mSchedule.setDisabled(true);
                            CCUHsApi.getInstance().updateSchedule(mSchedule);
                        }
                        scheduleImageButton.setVisibility(View.GONE);
                        
                        if (mScheduleType != ScheduleType.BUILDING.ordinal()) {
                            setScheduleType(scheduleTypeId, ScheduleType.BUILDING);
                            mScheduleType = ScheduleType.BUILDING.ordinal();
                        }

                    } else if (position == 1)
                    {
                        if (mSchedule.isZoneSchedule() && mSchedule.getMarkers().contains("disabled"))
                        {
                            mSchedule.setDisabled(false);
                            CCUHsApi.getInstance().updateZoneSchedule(mSchedule, zoneId);
                            scheduleImageButton.setTag(mSchedule.getId());
                        } else
                        {

                            Zone     zone         = Schedule.getZoneforEquipId(equipId);
                            Schedule scheduleById = null;
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
                                CCUHsApi.getInstance().syncEntityTree();
                            }
                            scheduleImageButton.setTag(scheduleById.getId());
                            scheduleImageButton.setVisibility(View.VISIBLE);
                        }
                        if (mScheduleType != ScheduleType.ZONE.ordinal()) {
                            setScheduleType(scheduleTypeId, ScheduleType.ZONE);
                            mScheduleType = ScheduleType.ZONE.ordinal();
                        }
                    } else
                    {
                        //list named schedules
                    }
                    mSchedule = Schedule.getScheduleByEquipId(equipId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });
            if(expandedListText.startsWith("smartstat")){
                if(smartStatLayout != null) smartStatLayout.setVisibility(View.VISIBLE);
                String profileType = expandedListText.substring(0,13);

                double ssOperatingMode = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and temp and operation and mode and his and equipRef == \"" + equipId + "\"");
                double ssFanOpMode = CCUHsApi.getInstance().readPointPriorityValByQuery("point and standalone and fan and operation and mode and his and equipRef == \"" + equipId + "\"");
                double ssFanHighHumdOption = 1.0;
                double ssTargetHumidity = 25.0;
                double ssTargetDehumidity = 45.0;
                if(profileType.equals("smartstat_hpu"))
                    ssFanHighHumdOption = CCUHsApi.getInstance().readDefaultVal("point and zone and config and relay5 and type and equipRef == \"" + equipId + "\"");
                else if(profileType.equals("smartstat_cpu"))
                    ssFanHighHumdOption = CCUHsApi.getInstance().readDefaultVal("point and zone and config and relay6 and type and equipRef == \"" + equipId + "\"");

                if(ssFanHighHumdOption > 1.0) {
                    ssHumiDifier.setVisibility(View.VISIBLE);
                    ArrayList<Integer> arrayHumdityTargetList = new ArrayList<Integer>();
                    for (int pos = 1; pos <= 100; pos++)
                        arrayHumdityTargetList.add(pos);
                    ArrayAdapter<Integer> humidityTargetAdapter = new ArrayAdapter<Integer>(mFragment.getContext(),android.R.layout.simple_spinner_dropdown_item,arrayHumdityTargetList);
                    ssHumiDifierSpinner.setAdapter(humidityTargetAdapter);
                    if(ssFanHighHumdOption == 2.0) {
                        ssTargetHumidity = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and humidity and his and equipRef == \"" + equipId + "\"");
                        ssHumiDifierSpinner.setSelection((int)ssTargetHumidity -1);
                    }else {
                        ssHumiDifierTV.setText("Target Dehumidify:");
                        ssTargetDehumidity = CCUHsApi.getInstance().readDefaultVal("point and standalone and target and dehumidifier and his and equipRef == \"" + equipId + "\"");
                        ssHumiDifierSpinner.setSelection((int)ssTargetDehumidity - 1);
                    }
                    double finalSsFanHighHumdOption = ssFanHighHumdOption;
                    ssHumiDifierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if(finalSsFanHighHumdOption == 3.0)
                                StandaloneScheduler.updateOperationalPoints(equipId,"target and dehumidifier",position+1);
                            else
                                StandaloneScheduler.updateOperationalPoints(equipId,"target and humidity",position+1);

                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }
                ssCondModeSpinner.setSelection((int)ssOperatingMode);
                ssFanModeSpinner.setSelection((int)ssFanOpMode);
                if(equipId != null ) {

                    ssStatus.setText(StandaloneScheduler.getSmartStatStatusString(equipId));
                    ssCondModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            StandaloneScheduler.updateOperationalPoints(equipId,"temp and operation and mode",position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                    ssFanModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            StandaloneScheduler.updateOperationalPoints(equipId, "fan and operation and mode", position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }
            }

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
    
    private void setScheduleType(String id, ScheduleType schedule) {
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground( final String ... params ) {
                CCUHsApi.getInstance().writeDefaultValById(id, (double)schedule.ordinal());
                ScheduleProcessJob.handleScheduleTypeUpdate(new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(id)).build());
                return null;
            }
            
            @Override
            protected void onPostExecute( final Void result ) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }
}
