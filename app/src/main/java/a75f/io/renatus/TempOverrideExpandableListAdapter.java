/*Created by Aniket on 07/07/2021*/
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
import android.view.Gravity;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.Zone;
import a75f.io.logger.CcuLog;
import a75f.io.logic.DefaultSchedules;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.renatus.BASE.FragmentCommonBundleArgs;
import a75f.io.renatus.util.CCUUiUtil;

import static a75f.io.renatus.TempOverrideFragment.getPointVal;

public class TempOverrideExpandableListAdapter extends BaseExpandableListAdapter {
    private Fragment mFragment;
    private List<String> expandableListTitle;
    private TreeMap<String, List<String>> expandableListDetail;
    private TreeMap<String, String>       idMap;
    String siteName;
    private ArrayList<String> equipsRef;

    Activity mActivity = null;

    public TempOverrideExpandableListAdapter(Fragment fragment, List<String> expandableListTitle,
                                             TreeMap<String, List<String>> expandableListDetail, TreeMap idmap, Activity activity, String siteName, ArrayList<String> equipsRef)
    {
        this.mFragment = fragment;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.idMap = idmap;
        this.mActivity = activity;
        this.siteName = siteName;
        this.equipsRef = equipsRef;
        Log.e("InsideTempOverrideExpandableListAdapter", "expandableListTitle- " + expandableListTitle);
        Log.e("InsideTempOverrideExpandableListAdapter", "expandableListDetail- " + expandableListDetail);
        Log.e("InsideTempOverrideExpandableListAdapter", "idMap- " + idMap);
        Log.e("InsideTempOverrideExpandableListAdapter", "equipsRef- " + equipsRef);
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
                    String relayMapped = getRelayMapping("relay"+expandedListText.substring(5, 6), convertView);
                    //NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6)+"("+relayMapped+")");
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
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1(Cooling)");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog2In")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2");
                        expandedListTextVal.setText("" + getPointVal(idMap.get(expandedListText))+" mV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2(Fan Speed)");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3(Heating)");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    }else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4(Composite)");
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" dV");
                        spinner_override_value.setVisibility(View.VISIBLE);
                    }
                    else if (NewexpandedListText.startsWith("relay")) {
                        String relayPos = (expandedListText.substring(siteName.length()+6, siteName.length()+7));
                        if(getConfigEnabled("relay"+relayPos) > 0) {
                            String relayMapped = getRelayMapping("relay"+relayPos, convertView);
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos +"("+relayMapped+")");
                            txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(expandedListText)), 1.0) == 0 ? "ON" : "OFF");
                            spinner_relay.setVisibility(View.VISIBLE);
                        }
                        else{
                            Object valueToDelete = getChild(listPosition, expandedListPosition);
                            expandableListDetail.remove(valueToDelete);
                        }
                    }else if (NewexpandedListText.startsWith("CM-th")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(siteName.length()+6, siteName.length()+7));
                        txt_calculated_output.setText("" + getPointVal(idMap.get(expandedListText))+" Ohm");
                        spinner_thermistor.setVisibility(View.VISIBLE);
                    }
                }
                expandedListTextView.setText(NewexpandedListText);
            }

            ArrayList<String> targetVal = new ArrayList<String>();
            for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                targetVal.add(pos /100.0 +" V");
            }
            ArrayAdapter<String> targetValAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, targetVal){
                public View getView(int position, View convertView,ViewGroup parent) {

                    View v = super.getView(position, convertView, parent);

                    //((TextView) v).setTextSize(16);
                    ((TextView) v).setTextAppearance(R.style.text_appearance);
                    ((TextView) v).setGravity(Gravity.CENTER);

                    return v;

                }

                public View getDropDownView(int position, View convertView,ViewGroup parent) {

                    View v = super.getDropDownView(position, convertView,parent);

                    ((TextView) v).setGravity(Gravity.CENTER);
                    ((TextView) v).setTextAppearance(R.style.text_appearance);

                    return v;

                }
            };

            //targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_override_value.setAdapter(targetValAdapter);
            spinner_override_value.invalidate();

            ArrayList<String> relyVal = new ArrayList<String>();
            relyVal.add("OFF");
            relyVal.add("ON");
            ArrayAdapter<String> relayValAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, relyVal){
                public View getView(int position, View convertView,ViewGroup parent) {

                    View v = super.getView(position, convertView, parent);

                    //((TextView) v).setTextSize(16);
                    ((TextView) v).setTextAppearance(R.style.text_appearance);
                    ((TextView) v).setGravity(Gravity.CENTER);

                    return v;

                }

                public View getDropDownView(int position, View convertView,ViewGroup parent) {

                    View v = super.getDropDownView(position, convertView,parent);

                    ((TextView) v).setGravity(Gravity.CENTER);
                    ((TextView) v).setTextAppearance(R.style.text_appearance);

                    return v;

                }
            };
            //relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_relay.setAdapter(relayValAdapter);
            spinner_relay.invalidate();

            ArrayList<String> thermistorVal = new ArrayList<String>();
            for (int pos = (100*0); pos <= (100*100); pos+=(100)) {
                thermistorVal.add(pos+" Ohm");
            }
            ArrayAdapter<String> thermistorAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, thermistorVal){
                public View getView(int position, View convertView,ViewGroup parent) {

                    View v = super.getView(position, convertView, parent);

                    //((TextView) v).setTextSize(16);
                    ((TextView) v).setTextAppearance(R.style.text_appearance);
                    ((TextView) v).setGravity(Gravity.CENTER);

                    return v;

                }

                public View getDropDownView(int position, View convertView,ViewGroup parent) {

                    View v = super.getDropDownView(position, convertView,parent);

                    ((TextView) v).setGravity(Gravity.CENTER);
                    ((TextView) v).setTextAppearance(R.style.text_appearance);

                    return v;

                }
            };
            //thermistorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                    String selectedSpinnerItem = spinner_override_value.getSelectedItem().toString();
                    int index=selectedSpinnerItem.lastIndexOf("V");
                    setPointVal(idMap.get(tunerName), Double.parseDouble(selectedSpinnerItem.substring(0,index-1)));
                    idMap.put(idMap.get(tunerName), selectedSpinnerItem.substring(0,index-1));

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
                    String selectedSpinnerItem = spinner_thermistor.getSelectedItem().toString();
                    int index=selectedSpinnerItem.lastIndexOf("Ohm");
                    setPointVal(idMap.get(tunerName), Double.parseDouble(selectedSpinnerItem.substring(0,index-1)));
                    idMap.put(idMap.get(tunerName), selectedSpinnerItem.substring(0,index-1));

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });

            CCUUiUtil.setSpinnerDropDownColor(spinner_override_value,mActivity.getApplicationContext());
            CCUUiUtil.setSpinnerDropDownColor(spinner_relay,mActivity.getApplicationContext());
            CCUUiUtil.setSpinnerDropDownColor(spinner_thermistor,mActivity.getApplicationContext());
        }

        return convertView;
    }

    private String getRelayMapping(String relayname, View convertView){
        String profileName =  L.ccu().systemProfile.getProfileType().toString();
        List<String> hvac_stage_selector = Arrays.asList(convertView.getResources().getStringArray(R.array.hvac_stage_selector));
        VavStagedRtu vavStagedRtu = new VavStagedRtu();
        DabStagedRtu dabStagedRtu = new DabStagedRtu();
        switch (profileName){
            case "SYSTEM_DAB_ANALOG_RTU":
                DabFullyModulatingRtu dabFullyModulatingRtu = new DabFullyModulatingRtu();
                if (relayname.equals("relay7"))
                    if ((int)dabFullyModulatingRtu.getConfigVal("humidifier and type") == 0)
                        return "Humidifier";
                    else
                        return "De-Humidifier";
                else return "Fan Enable";
                //return ((int)dabFullyModulatingRtu.getConfigVal(relayname));
            case "SYSTEM_DAB_STAGED_VFD_RTU":
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case "SYSTEM_DAB_STAGED_RTU":
                //DabStagedRtu dabStagedRtu = new DabStagedRtu();
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case "SYSTEM_DAB_HYBRID_RTU":
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case "SYSTEM_VAV_STAGED_RTU":
                //VavStagedRtu vavStagedRtu = new VavStagedRtu();
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
            case "SYSTEM_VAV_ANALOG_RTU":
                VavFullyModulatingRtu vavFullyModulatingRtu = new VavFullyModulatingRtu();
                if (relayname.equals("relay7")) {
                    if ((int) vavFullyModulatingRtu.getConfigVal("humidifier and type") == 0)
                        return "Humidifier";
                    else
                        return "De-Humidifier";
                }else
                    return "Fan Enable";
            case "SYSTEM_VAV_STAGED_VFD_RTU":
                //VavStagedRtu vavStagedRtu1 = new VavStagedRtu();
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
            case "SYSTEM_VAV_HYBRID_RTU":
                //VavStagedRtu vavStagedRtu1 = new VavStagedRtu();
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
            case "Daikin IE RTU":
        }
        return "Default";
    }

    public void setPointVal(String id, double val) {
        Object logicalPoint = CCUHsApi.getInstance().readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {

            CCUHsApi hayStack = CCUHsApi.getInstance();
            hayStack.writeHisValById(id, val);
            hayStack.writeHisValById(logicalPoint.toString(), val / 1000);
        }
        else{
            Object devicelogicalPoint = CCUHsApi.getInstance().readMapById(id).get("deviceRef");

            CCUHsApi hayStack = CCUHsApi.getInstance();
            hayStack.writeHisValById(id, val);
            hayStack.writeHisValById(devicelogicalPoint.toString(), val / 1000);
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

    public String getZoneTitle(int listPosition) {
        if (listPosition != 0) {
            String listTitle = (String) getGroup(listPosition);
            HashMap roomMap = CCUHsApi.getInstance().read("equip and group == \"" + listTitle.substring(3, listTitle.length()) + "\"");
            Zone zone = HSUtil.getZone(roomMap.get("roomRef").toString(), roomMap.get("floorRef").toString());
            return zone.getDisplayName() + "-" + listTitle.substring(3, listTitle.length());
        }
        else
            return null;
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
        String zoneTitle = getZoneTitle(listPosition);
        Log.e("InsideTempOverrideExpandableListAdapter", "zoneTitle- " + zoneTitle);
        if (convertView == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater) this.mFragment.getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.temp_override_tuner_list_group, null);
        }

        /*ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zone and roomRef");
        ArrayList<String> equipsRef = new ArrayList<String>();
        for(int i = 0; i<equips.size(); i++) {
            Log.e("InsideTempOverrideExpandableListAdapter", "equips- " + equips);
            equipsRef.add(i, equips.get(i).get("roomRef").toString());
            Log.e("InsideTempOverrideExpandableListAdapter", "equipsRef- " + equipsRef);
        }*/
            /*for (int m = 0; m < roomMap.size(); m++) {
                String zoneTitle = "";
                zoneTitle = roomMap.get(m).get("dis").toString();
                Log.e("InsideTempOverrideExpandableListAdapter", "zoneTitle- " + zoneTitle);
            }*/

        /*for (int i = 0; i<L.ccu().zoneProfiles.size(); i++) {
            ArrayList<HashMap> roomMap = CCUHsApi.getInstance().readAll("room and floorRef == \"" + floorList.get(i).getId() + "\"");
            Log.e("InsideTempOverrideExpandableListAdapter", "roomMap- " + roomMap);
        }*/
        /*String zoneTitle = roomMap.get(m).get("dis").toString()
        Log.e("InsideTempOverrideExpandableListAdapter","zoneProfiles- "+L.ccu().zoneProfiles.getClass());*/
            TextView listTitleTextView = (TextView) convertView
                    .findViewById(R.id.listTitle);
            listTitleTextView.setTypeface(null, Typeface.BOLD);
            if (listTitle.equals("CM-device"))
                listTitleTextView.setText(L.ccu().systemProfile.getProfileName());
            else {
                listTitleTextView.setText(zoneTitle);
            }
            return convertView;
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
