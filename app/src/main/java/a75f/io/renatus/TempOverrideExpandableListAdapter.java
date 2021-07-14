package a75f.io.renatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ScheduleType;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.system.SystemRelayOp;

import static a75f.io.renatus.TempOverrideFragment.getPointVal;

public class TempOverrideExpandableListAdapter extends BaseExpandableListAdapter {
    private Fragment mFragment;
    private List<String> expandableListTitle;
    private TreeMap<String, List<String>> expandableListDetail;
    //private TreeMap<String, String> expandableListDetail;
    private TreeMap<String, String>       idMap;
    String siteName;

    Schedule mSchedule = null;
    int mScheduleType = -1;

    Activity mActivity = null;

    public TempOverrideExpandableListAdapter(Fragment fragment, List<String> expandableListTitle,
                                             TreeMap<String, List<String>> expandableListDetail, TreeMap idmap, Activity activity, String siteName)
    {
        this.mFragment = fragment;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.idMap = idmap;
        this.mActivity = activity;
        this.siteName = siteName;

        Log.e("InsideTempOverrideExpandableListAdapter", "expandableListTitle- " + expandableListTitle);
        Log.e("InsideTempOverrideExpandableListAdapter", "expandableListDetail- " + expandableListDetail);
        Log.e("InsideTempOverrideExpandableListAdapter", "idMap- " + idMap);
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
        String NewexpandedListText = expandedListText;
        //Log.i("Scheduler", "IDE Too Slow: " + expandedListText);
        //Log.e("Scheduler", "InsideTempOverrideExpaAdapter" + expandedListText);

        if (!expandedListText.startsWith("schedule") && (!expandedListText.startsWith("smartstat"))) {
            LayoutInflater layoutInflater = (LayoutInflater) this.mFragment.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.tempoverride_exp_list_item, parent, false);


            TextView expandedListTextView = convertView
                    .findViewById(R.id.expandedListItemName);
            TextView expandedListTextVal = convertView
                    .findViewById(R.id.expandedListItemVal);
            TextView txt_calculated_output = convertView
                    .findViewById(R.id.txt_calculated_output);
            Spinner spinner_override_value = convertView
                    .findViewById(R.id.spinner_override_value);
            Spinner spinner_relay = convertView
                    .findViewById(R.id.spinner_relay);
            Spinner spinner_thermistor = convertView
                    .findViewById(R.id.spinner_thermistor);

            if (expandedListText.startsWith("Analog1In") || expandedListText.startsWith("Analog1Out") ||
                    expandedListText.startsWith("Analog2In") || expandedListText.startsWith("Analog2Out") || expandedListText.startsWith("relay") || expandedListText.startsWith("Th") ||
                    expandedListText.startsWith(siteName)) {
                String equipId = idMap.get(expandedListText);

                if (expandedListText.startsWith("Analog1In")) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                    expandedListTextVal.setText("" + getPointVal(idMap.get(expandedListText))+" "+CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                    spinner_override_value.setVisibility(View.VISIBLE);
                } else if (expandedListText.startsWith("Analog1Out")) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1");
                    txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" "+CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                    spinner_override_value.setVisibility(View.VISIBLE);
                } else if (expandedListText.startsWith("Analog2In")) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2");
                    expandedListTextVal.setText("" + getPointVal(idMap.get(expandedListText))+" "+CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                    spinner_override_value.setVisibility(View.VISIBLE);
                } else if (expandedListText.startsWith("Analog2Out")) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2");
                    txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" "+CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                    spinner_override_value.setVisibility(View.VISIBLE);
                } else if (expandedListText.startsWith("relay")) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6));
                    txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(expandedListText)),1.0) == 0 ? "ON" : "OFF");
                    spinner_relay.setVisibility(View.VISIBLE);
                }else if (expandedListText.startsWith("Th")) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(2, 3));
                    txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" "+CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                    spinner_thermistor.setVisibility(View.VISIBLE);
                }else if (expandedListText.startsWith(siteName)) {
                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, expandedListText.substring(siteName.length()+1, expandedListText.length()));
                    if (NewexpandedListText.startsWith("CM-analog1In")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                        expandedListTextVal.setText("" + getPointVal(idMap.get(expandedListText))+" mV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog1Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog2In")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2");
                        expandedListTextVal.setText("" + getPointVal(idMap.get(expandedListText))+" mV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    }else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    }
                    else if (NewexpandedListText.startsWith("relay")) {
                        String relayPos = (expandedListText.substring(siteName.length()+6, siteName.length()+7));
                        if(getConfigEnabled("relay"+relayPos) > 0) {
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos);
                            List<String> hvac_stage_selector = Arrays.asList(convertView.getResources().getStringArray(R.array.hvac_stage_selector));
                            Log.e("InsideTempOverrideExpandableListAdapter", "relayData1 " + relayPos + CCUHsApi.getInstance().read("relay" + relayPos));
                            Log.e("InsideTempOverrideExpandableListAdapter", "hvac_stage_selector- " + hvac_stage_selector);
                            txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(expandedListText)), 1.0) == 0 ? "ON" : "OFF");
                            spinner_relay.setVisibility(View.VISIBLE);
                        }
                        else{
                            Object valueToDelete = getChild(listPosition, expandedListPosition);
                            Object titleDelete = getGroup(listPosition);
                            Log.e("InsideTempOverrideExpandableListAdapter", "getValueToDelete- " + valueToDelete);
                            Log.e("InsideTempOverrideExpandableListAdapter", "titleDelete- " + titleDelete.toString());

                            /*for (TreeMap.Entry<String, List<String>> entry : expandableListDetail.entrySet()) {
                                Log.e("InsideTempOverrideExpandableListAdapter", "entry- " + entry);
                                //Log.e("InsideTempOverrideExpandableListAdapter", "entry_key- " + entry.getKey());
                                //Log.e("InsideTempOverrideExpandableListAdapter", "entry_value- " + entry.getValue());
                                if (entry.getKey().equals(titleDelete)){
                                    if (entry.getValue().contains(valueToDelete)) {
                                        entry.getValue().remove(valueToDelete);
                                    }
                                }
                                else
                                    continue;
                                Log.e("InsideTempOverrideExpandableListAdapter", "entry_2- " + entry.getValue());
                            }*/
                            //Log.e("InsideTempOverrideExpandableListAdapter", "expandableListDetail- " + entry.getValue());
                            expandableListDetail.remove(valueToDelete);
                            Log.e("InsideTempOverrideExpandableListAdapter", "NewexpandableListDetail- " + expandableListDetail);
                        }
                    }else if (NewexpandedListText.startsWith("CM-th")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(siteName.length()+6, siteName.length()+7));
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" Ohm");
                        spinner_thermistor.setVisibility(View.VISIBLE);
                    }
                }
                //String scheduleTypeId = CCUHsApi.getInstance().readId("point and scheduleType and equipRef == \""+idMap.get(expandedListText)+"\"");
                //String unit = CCUHsApi.getInstance().readMapById(equipId);
                //Log.e("InsideTempOverrideExpandableListAdapter1","readMapById- "+unit);
                expandedListTextView.setText(NewexpandedListText);
            }

            ArrayList<Double> targetVal = new ArrayList<Double>();
            for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                targetVal.add(pos /100.0);
            }
            ArrayAdapter<Double> targetValAdapter = new ArrayAdapter<Double>(mActivity, android.R.layout.simple_spinner_item, targetVal);
            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_override_value.setAdapter(targetValAdapter);
            spinner_override_value.invalidate();

            ArrayList<String> relyVal = new ArrayList<String>();
            relyVal.add("OFF");
            relyVal.add("ON");
            ArrayAdapter<String> relayValAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, relyVal);
            relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_relay.setAdapter(relayValAdapter);
            spinner_relay.invalidate();

            ArrayList<Integer> thermistorVal = new ArrayList<Integer>();
            for (int pos = (100); pos <= (100*100); pos+=(100)) {
                thermistorVal.add(pos);
            }
            ArrayAdapter<Integer> thermistorAdapter = new ArrayAdapter<Integer>(mActivity, android.R.layout.simple_spinner_item, thermistorVal);
            thermistorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_thermistor.setAdapter(thermistorAdapter);
            spinner_thermistor.invalidate();

            spinner_override_value.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemproryOverrideMode(true);
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);

                    //String tunerVal = String.valueOf(getPointVal(idMap.get(tunerName)));
                    if (Globals.getInstance().isTemproryOverrideMode()) {
                        setPointVal(idMap.get(tunerName), Double.parseDouble(spinner_override_value.getSelectedItem().toString()));
                        //setPointVal(idMap.get(tunerName), Double.parseDouble(spinner_override_value.getSelectedItem().toString()));
                        idMap.put(idMap.get(tunerName), spinner_override_value.getSelectedItem().toString());
                    }

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });

            spinner_relay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemproryOverrideMode(true);
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    setPointVal(idMap.get(tunerName), Double.parseDouble(String.valueOf(spinner_relay.getSelectedItemId())));
                    idMap.put(idMap.get(tunerName), spinner_relay.getSelectedItem().toString());

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });

            spinner_thermistor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemproryOverrideMode(true);
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    setPointVal(idMap.get(tunerName), Double.parseDouble(spinner_thermistor.getSelectedItem().toString()));
                    idMap.put(idMap.get(tunerName), spinner_thermistor.getSelectedItem().toString());

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });
        }

        return convertView;
    }

    public void setPointVal(String id, double val) {
        SystemRelayOp op = new SystemRelayOp();
        //Log.e("InsideTempOverrideExpandableListAdapter","system_profile- "+op.getRelayAssociation());
        String logicalPointString = null;
        String deviceLogicalPointString = null;
        Object logicalPoint = CCUHsApi.getInstance().readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            logicalPointString = logicalPoint.toString();

            CCUHsApi hayStack = CCUHsApi.getInstance();
            hayStack.writeHisValById(id, val);
            hayStack.writeHisValById(logicalPointString, val / 1000);
        }
        else{
            Object devicelogicalPoint = CCUHsApi.getInstance().readMapById(id).get("deviceRef");

            deviceLogicalPointString = devicelogicalPoint.toString();

            CCUHsApi hayStack = CCUHsApi.getInstance();
            hayStack.writeHisValById(id, val);
            hayStack.writeHisValById(deviceLogicalPointString, val / 1000);
        }
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
        Log.e("InsideTempOverrideExpandableListAdapter","listTitle- "+listTitle);
        //Log.e("InsideTempOverrideExpandableListAdapter","zoneList- "+ ZoneProfile.TAG);
        //Log.e("InsideTempOverrideExpandableListAdapter","relayData1- "+CCUHsApi.getInstance().read("point and system and config and relay6"));
        if (convertView == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater) this.mFragment.getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.temp_override_tuner_list_group, null);
        }
        TextView listTitleTextView = (TextView) convertView
                .findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        if (listTitle.equals("CM-device"))
            listTitleTextView.setText(L.ccu().systemProfile.getProfileName());
        else
            listTitleTextView.setText(listTitle);
        return convertView;
    }

    public double getConfigAssociation(String config) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and association and "+config);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }

    public double getConfigEnabled(String config) {
        //Log.e("InsideTempOverrideExpandableListAdapter","config- "+config);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
        if (configPoint.isEmpty()){
            return 0.0;
        }
        else{
            //Log.e("InsideTempOverrideExpandableListAdapter","configPoint- "+configPoint);
            return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        }

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
}
