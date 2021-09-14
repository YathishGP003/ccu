/*Created by Aniket on 07/07/2021*/
package a75f.io.renatus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.dualduct.DualDuctProfileConfiguration;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseConfiguration;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseProfile;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
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
        String equipId = idMap.get(expandedListText);
        double value = getPointVal(equipId);
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

        String NewexpandedListText = expandedListText;
        Object unit = null;
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

            if (Globals.getInstance().gettempOverCount() < 1){
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                edit.putString(expandedListText,equipId+"-value-"+value);
                edit.commit();

                if (expandedListText.startsWith("Analog1In") || expandedListText.startsWith("Analog1Out") ||
                        expandedListText.startsWith("Analog2In") || expandedListText.startsWith("Analog2Out") || expandedListText.startsWith("relay") || expandedListText.startsWith("Th") ||
                        expandedListText.startsWith(siteName)) {
                    //double value = getPointVal(idMap.get(expandedListText));
                    unit = CCUHsApi.getInstance().readMapById(equipId).get("unit");
                    if (Objects.nonNull(unit)) {
                        if (unit.toString().equals("mV"))
                            value = value * 0.001;
                        else if (unit.toString().equals("dV"))
                            value = value * 0.1;
                    }

                    if (expandedListText.startsWith("Analog1In")) {
                        String analogIn1Mapped = getZoneMapping("Analog1In", listPosition, convertView);
                        if (!analogIn1Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1\n(" + analogIn1Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                        String strDouble = String.format("%.1f", value);
                        //expandedListTextVal.setText("" + value + " V");
                        expandedListTextVal.setText("" + strDouble + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        spinner_override_value.setAdapter(targetValAdapter);
                        spinner_override_value.setSelection(0);
                        spinner_override_value.setSelection(0,false);
                    } else if (expandedListText.startsWith("Analog1Out")) {
                        String analogOut1Mapped = getZoneMapping("Analog-out1", listPosition, convertView);
                        if (!analogOut1Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(" + analogOut1Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1");
                        expandedListTextVal.setText("" + value + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        spinner_override_value.setAdapter(targetValAdapter);
                        spinner_override_value.setSelection(0);
                        spinner_override_value.setSelection(0,false);
                    } else if (expandedListText.startsWith("Analog2In")) {
                        String analogIn2Mapped = getZoneMapping("Analog2In", listPosition, convertView);
                        if (!analogIn2Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2\n(" + analogIn2Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2");
                        String strDouble = String.format("%.1f", value);
                        //expandedListTextVal.setText("" + value + " V");
                        expandedListTextVal.setText("" + strDouble + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        spinner_override_value.setAdapter(targetValAdapter);
                        spinner_override_value.setSelection(0);
                        spinner_override_value.setSelection(0,false);
                    } else if (expandedListText.startsWith("Analog2Out")) {
                        String analogOut2Mapped = getZoneMapping("Analog-out2", listPosition, convertView);
                        if (!analogOut2Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2" + "\n(" + analogOut2Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2");

                        expandedListTextVal.setText("" + value + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        spinner_override_value.setAdapter(targetValAdapter);
                        spinner_override_value.setSelection(0);
                        spinner_override_value.setSelection(0,false);
                    } else if (expandedListText.startsWith("relay")) {
                        String relayMapped = getZoneMapping("relay" + expandedListText.substring(5, 6), listPosition, convertView);
                        if (!relayMapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6) + "\n(" + relayMapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6));
                        txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(expandedListText)), 1.0) == 0 ? "ON" : "OFF");
                        spinner_relay.setVisibility(View.VISIBLE);
                        spinner_relay.setAdapter(relayValAdapter);
                        spinner_relay.setSelection(0);
                        spinner_relay.setSelection(0,false);
                    } else if (expandedListText.startsWith("Th")) {
                        String thermistorMapped = getZoneMapping("Thermistor" + expandedListText.substring(2, 3), listPosition, convertView);
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(2, 3) + "\n(" + thermistorMapped + ")");
                        expandedListTextVal.setText("" + value + " " + CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                        spinner_thermistor.setVisibility(View.VISIBLE);
                        spinner_thermistor.setAdapter(thermistorAdapter);
                        spinner_thermistor.setSelection(0);
                        spinner_thermistor.setSelection(0,false);
                    } else if (expandedListText.startsWith(siteName)) {
                        value = value / 10;
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, expandedListText.substring(siteName.length() + 1, expandedListText.length()));
                        if (NewexpandedListText.startsWith("CM-analog1In")) {
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                            expandedListTextVal.setText("" + value + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        } else if (NewexpandedListText.startsWith("CM-analog1Out")) {
                            if (getConfigEnabled("analog1") > 0) {
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Cooling)");
                            } else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Not Used)");
                            txt_calculated_output.setText("" + value + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        }else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                            if (getConfigEnabled("analog2") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Fan Speed)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Not Used)");
                            txt_calculated_output.setText("" + value + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                            if (getConfigEnabled("analog3") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Heating)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Not Used)");
                            txt_calculated_output.setText("" + value + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        } else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                            if (getConfigEnabled("analog4") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Composite)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Not Used)");
                            txt_calculated_output.setText("" + value + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        } else if (NewexpandedListText.startsWith("relay")) {
                            String relayPos = (expandedListText.substring(siteName.length() + 6, siteName.length() + 7));
                            if (getConfigEnabled("relay" + relayPos) > 0) {
                                String relayMapped = getRelayMapping("relay" + relayPos, convertView);
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(" + relayMapped + ")");
                            } else {
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(Not Used)");
                                Object valueToDelete = getChild(listPosition, expandedListPosition);
                                expandableListDetail.remove(valueToDelete);
                            }
                            txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(expandedListText)), 1.0) == 0 ? "ON" : "OFF");
                            spinner_relay.setVisibility(View.VISIBLE);
                            spinner_relay.setAdapter(relayValAdapter);
                            spinner_relay.setSelection(0);
                            spinner_relay.setSelection(0,false);
                        }/*else if (NewexpandedListText.startsWith("CM-th")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(siteName.length()+6, siteName.length()+7));
                        expandedListTextVal.setText("" + value+" Ohm");
                        spinner_thermistor.setVisibility(View.VISIBLE);
                    }*/
                    }
                    expandedListTextView.setText(NewexpandedListText);
                    notifyDataSetChanged();
                }
            }
            else if (Globals.getInstance().gettempOverCount()>0){
                String sharedPrefData = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(expandedListText,null);
                if (sharedPrefData != null) {
                    String[] parts = sharedPrefData.split("-value-");
                    String id = parts[0];
                    String value1 = parts[1];
                    Double val = Double.valueOf(value1);
                if (expandedListText.startsWith("Analog1In") || expandedListText.startsWith("Analog1Out") ||
                        expandedListText.startsWith("Analog2In") || expandedListText.startsWith("Analog2Out") || expandedListText.startsWith("relay") || expandedListText.startsWith("Th") ||
                        expandedListText.startsWith(siteName)) {
                    //double value = getPointVal(idMap.get(expandedListText));
                    unit = CCUHsApi.getInstance().readMapById(equipId).get("unit");
                    if (Objects.nonNull(unit)) {
                        if (unit.toString().equals("mV"))
                            val = val * 0.001;
                        else if (unit.toString().equals("dV"))
                            val = val * 0.1;
                    }

                    if (expandedListText.startsWith("Analog1In")) {
                        setPointVal(id, val);
                        idMap.put(id, value1);
                        String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                        String analogIn1Mapped = getZoneMapping("Analog1In", listPosition, convertView);
                        if (!analogIn1Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1\n(" + analogIn1Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                        String strDouble = String.format("%.1f", val);
                        expandedListTextVal.setText("" + strDouble + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_override_value.setAdapter(targetValAdapter);
                        if (sharedPrefData1!= null) {
                            int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                            spinner_override_value.setSelection(spinnerPosition);
                        }
                    } else if (expandedListText.startsWith("Analog1Out")) {
                        setPointVal(id, val);
                        idMap.put(id, value1);
                        String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                        String analogOut1Mapped = getZoneMapping("Analog-out1", listPosition, convertView);
                        if (!analogOut1Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(" + analogOut1Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1");
                        txt_calculated_output.setText("" + val + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_override_value.setAdapter(targetValAdapter);
                        if (sharedPrefData1!= null) {
                            int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                            spinner_override_value.setSelection(spinnerPosition);
                        }
                    } else if (expandedListText.startsWith("Analog2In")) {
                        setPointVal(id, val);
                        idMap.put(id, value1);
                        String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                        String analogIn2Mapped = getZoneMapping("Analog2In", listPosition, convertView);
                        if (!analogIn2Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2\n(" + analogIn2Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2");
                        expandedListTextVal.setText("" + val + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_override_value.setAdapter(targetValAdapter);
                        if (sharedPrefData1!= null) {
                            int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                            spinner_override_value.setSelection(spinnerPosition);
                        }
                    } else if (expandedListText.startsWith("Analog2Out")) {
                        setPointVal(id, val);
                        idMap.put(id, value1);
                        String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                        String analogOut2Mapped = getZoneMapping("Analog-out2", listPosition, convertView);
                        if (!analogOut2Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2" + "\n(" + analogOut2Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2");

                        txt_calculated_output.setText("" + val + " V");
                        spinner_override_value.setVisibility(View.VISIBLE);
                        targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_override_value.setAdapter(targetValAdapter);
                        if (sharedPrefData1!= null) {
                            int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                            spinner_override_value.setSelection(spinnerPosition);
                        }
                    } else if (expandedListText.startsWith("relay")) {
                        setPointVal(id, val);
                        idMap.put(id, value1);
                        String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(5, 6),null);
                        String relayMapped = getZoneMapping("relay" + expandedListText.substring(5, 6), listPosition, convertView);
                        if (!relayMapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6) + "\n(" + relayMapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6));
                        txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(value1)), 1.0) == 0 ? "ON" : "OFF");
                        spinner_relay.setVisibility(View.VISIBLE);
                        relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner_relay.setAdapter(relayValAdapter);
                        if (sharedPrefData1!= null) {
                            int spinnerPosition = relayValAdapter.getPosition(sharedPrefData1);
                            spinner_relay.setSelection(spinnerPosition);
                        }
                    } else if (expandedListText.startsWith("Th")) {
                        setPointValForThermistor(id, val);
                        idMap.put(id, value1);
                        String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(2, 3),null);
                        String thermistorMapped = getZoneMapping("Thermistor" + expandedListText.substring(2, 3), listPosition, convertView);
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(2, 3) + "\n(" + thermistorMapped + ")");
                        expandedListTextVal.setText("" + val + " " + CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                        spinner_thermistor.setVisibility(View.VISIBLE);
                        spinner_thermistor.setAdapter(thermistorAdapter);
                        if (sharedPrefData1!= null) {
                            int spinnerPosition = thermistorAdapter.getPosition(sharedPrefData1);
                            spinner_thermistor.setSelection(spinnerPosition);
                        }
                    } else if (expandedListText.startsWith(siteName)) {
                        setPointVal(id, val);
                        idMap.put(id, value1);
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, expandedListText.substring(siteName.length() + 1, expandedListText.length()));
                        if (NewexpandedListText.startsWith("CM-analog1In")) {
                            String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                            expandedListTextVal.setText("" + val/10 + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_override_value.setAdapter(targetValAdapter);
                            if (sharedPrefData1!= null) {
                                int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                        } else if (NewexpandedListText.startsWith("CM-analog1Out")) {
                            String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                            if (getConfigEnabled("analog1") > 0) {
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Cooling)");
                            } else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Not Used)");
                            txt_calculated_output.setText("" + val/10 + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_override_value.setAdapter(targetValAdapter);
                            if (sharedPrefData1!= null) {
                                int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                        }else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                            String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                            if (getConfigEnabled("analog2") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Fan Speed)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Not Used)");
                            txt_calculated_output.setText("" + val/10 + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_override_value.setAdapter(targetValAdapter);
                            if (sharedPrefData1!= null) {
                                int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                        } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                            String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                            if (getConfigEnabled("analog3") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Heating)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Not Used)");
                            txt_calculated_output.setText("" + val/10 + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_override_value.setAdapter(targetValAdapter);
                            if (sharedPrefData1!= null) {
                                int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                        } else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                            String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(6),null);
                            if (getConfigEnabled("analog4") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Composite)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Not Used)");
                            txt_calculated_output.setText("" + val/10 + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_override_value.setAdapter(targetValAdapter);
                            if (sharedPrefData1!= null) {
                                int spinnerPosition = targetValAdapter.getPosition(sharedPrefData1);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                        } else if (NewexpandedListText.startsWith("relay")) {
                            String sharedPrefData1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString(equipId+expandedListText.substring(5, 6),null);
                            String relayPos = (expandedListText.substring(siteName.length() + 6, siteName.length() + 7));
                            if (getConfigEnabled("relay" + relayPos) > 0) {
                                String relayMapped = getRelayMapping("relay" + relayPos, convertView);
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(" + relayMapped + ")");
                            } else {
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(Not Used)");
                                Object valueToDelete = getChild(listPosition, expandedListPosition);
                                expandableListDetail.remove(valueToDelete);
                            }
                            txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(value1)), 1.0) == 0 ? "ON" : "OFF");
                            spinner_relay.setVisibility(View.VISIBLE);
                            spinner_relay.setVisibility(View.VISIBLE);
                            relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_relay.setAdapter(relayValAdapter);
                            if (sharedPrefData1!= null) {
                                int spinnerPosition = relayValAdapter.getPosition(sharedPrefData1);
                                spinner_relay.setSelection(spinnerPosition);
                            }
                        }/*else if (NewexpandedListText.startsWith("CM-th")) {
                        NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(siteName.length()+6, siteName.length()+7));
                        expandedListTextVal.setText("" + value+" Ohm");
                        spinner_thermistor.setVisibility(View.VISIBLE);
                    }*/
                    }
                    expandedListTextView.setText(NewexpandedListText);
                    notifyDataSetChanged();
                }
                //setPointVal(idMap.get(getChild(listPosition, expandedListPosition)), 0.0);
                }else{
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString(expandedListText,equipId+"-value-"+value);
                    edit.commit();
                }
            }

            //targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            /*spinner_override_value.setAdapter(targetValAdapter);
            spinner_override_value.invalidate();*/

            //relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            /*spinner_relay.setAdapter(relayValAdapter);
            spinner_relay.invalidate();*/

            //thermistorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            /*spinner_thermistor.setAdapter(thermistorAdapter);
            spinner_thermistor.invalidate();*/

            Object finalUnit = unit;
            //spinner_override_value.setSelection(0,false);
            spinner_override_value.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemproryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_override_value.getSelectedItem().toString();
                    int index=selectedSpinnerItem.lastIndexOf("V");
                    if (Objects.nonNull(finalUnit)){
                        if (finalUnit.toString().equals("dV")) {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1))*10;
                            setPointVal(idMap.get(tunerName), (pointValue));
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                        else if (finalUnit.toString().equals("mV")) {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1))*1000;
                            setPointVal(idMap.get(tunerName), (pointValue));
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                        else{
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1));
                            setPointVal(idMap.get(tunerName), (pointValue));
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                    }
                    else{
                        Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1))*10;
                        setPointVal(idMap.get(tunerName), (pointValue));
                        idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                    }

                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString(equipId+expandedListText.substring(6),selectedSpinnerItem);
                    edit.apply();

                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                CCUHsApi.getInstance().syncHisData();
                            } catch (Exception e) {
                                //We do understand the consequences of doing this.
                                //But the system could still continue to work in standalone mode controlling the hvac system
                                //even if there are failures in data synchronization with backend.
                                CcuLog.e(L.TAG_CCU_JOB, "His Sync Failed !", e);
                            }
                        }
                    }.start();

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });

            //spinner_relay.setSelection(0,false);
            spinner_relay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemproryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_relay.getSelectedItem().toString();
                    setPointVal(idMap.get(tunerName), Double.parseDouble(String.valueOf(spinner_relay.getSelectedItemId())));
                    idMap.put(idMap.get(tunerName), spinner_relay.getSelectedItem().toString());

                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString(equipId+expandedListText.substring(5, 6),selectedSpinnerItem);
                    edit.apply();

                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                CCUHsApi.getInstance().syncHisData();
                            } catch (Exception e) {
                                //We do understand the consequences of doing this.
                                //But the system could still continue to work in standalone mode controlling the hvac system
                                //even if there are failures in data synchronization with backend.
                                CcuLog.e(L.TAG_CCU_JOB, "His Sync Failed !", e);
                            }
                        }
                    }.start();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }

            });

            //spinner_thermistor.setSelection(0,false);
            spinner_thermistor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemproryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_thermistor.getSelectedItem().toString();
                    int index=selectedSpinnerItem.lastIndexOf("Ohm");
                    //setPointVal(idMap.get(tunerName), Double.parseDouble(selectedSpinnerItem.substring(0,index-1)));
                    setPointValForThermistor(idMap.get(tunerName), Double.parseDouble(selectedSpinnerItem.substring(0,index-1)));
                    idMap.put(idMap.get(tunerName), selectedSpinnerItem.substring(0,index-1));

                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString(equipId+expandedListText.substring(2, 3),selectedSpinnerItem);
                    edit.apply();

                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                CCUHsApi.getInstance().syncHisData();

                            } catch (Exception e) {
                                //We do understand the consequences of doing this.
                                //But the system could still continue to work in standalone mode controlling the hvac system
                                //even if there are failures in data synchronization with backend.
                                CcuLog.e(L.TAG_CCU_JOB, "His Sync Failed !", e);
                            }
                        }
                    }.start();

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
        ProfileType profile =  L.ccu().systemProfile.getProfileType();
        List<String> hvac_stage_selector = Arrays.asList(convertView.getResources().getStringArray(R.array.hvac_stage_selector));
        VavStagedRtu vavStagedRtu = new VavStagedRtu();
        DabStagedRtu dabStagedRtu = new DabStagedRtu();
        switch (profile){
            case SYSTEM_DAB_ANALOG_RTU:
                DabFullyModulatingRtu dabFullyModulatingRtu = new DabFullyModulatingRtu();
                if (relayname.equals("relay7"))
                    if ((int)dabFullyModulatingRtu.getConfigVal("humidifier and type") == 0)
                        return "Humidifier";
                    else
                        return "De-Humidifier";
                else return "Fan Enable";
                //return ((int)dabFullyModulatingRtu.getConfigVal(relayname));
            case SYSTEM_DAB_STAGED_VFD_RTU:
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_DAB_STAGED_RTU:
                //DabStagedRtu dabStagedRtu = new DabStagedRtu();
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_DAB_HYBRID_RTU:
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_VAV_STAGED_RTU:
                //VavStagedRtu vavStagedRtu = new VavStagedRtu();
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_VAV_ANALOG_RTU:
                VavFullyModulatingRtu vavFullyModulatingRtu = new VavFullyModulatingRtu();
                if (relayname.equals("relay7")) {
                    if ((int) vavFullyModulatingRtu.getConfigVal("humidifier and type") == 0)
                        return "Humidifier";
                    else
                        return "De-Humidifier";
                }else
                    return "Fan Enable";
            case SYSTEM_VAV_STAGED_VFD_RTU:
                //VavStagedRtu vavStagedRtu1 = new VavStagedRtu();
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_VAV_HYBRID_RTU:
                //VavStagedRtu vavStagedRtu1 = new VavStagedRtu();
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
        }
        return "";
    }

    private String getZoneMapping(String pointname, int listPosition, View convertView){
        String listTitle = (String) getGroup(listPosition);
        HashMap equipGroup = CCUHsApi.getInstance().read("equip and group == \"" + listTitle.substring(3) + "\"");
        String profile = equipGroup.get("profile").toString();

        switch (profile){
            case "SSE":
                int relaypos = (int)getConfigNumVal("enable and "+pointname, Integer.parseInt(listTitle.substring(3)));
                if (pointname.equals("relay1")){
                    if (relaypos == 1)
                        return "Heating";
                    else if (relaypos == 2)
                        return "Cooling";
                    else return "Not enabled";
                }
                else if (pointname.equals("relay2")){
                    List<String> sse_relay2_mode = Arrays.asList(convertView.getResources().getStringArray(R.array.sse_relay2_mode));
                    return sse_relay2_mode.get(relaypos);
                }
                else if (pointname.equals("Thermistor1")){
                    return "Airflow Sensor";
                }else if (pointname.equals("Thermistor2")){
                    return "External Sensor";
                }
                else if (pointname.startsWith("Analog"))
                    return "Not Used";
                break;
            case "VAV_SERIES_FAN":
                if (pointname.equals("Analog-out2"))
                    return "Modulating Reheat";
                else if (pointname.equals("Analog-out1"))
                    return "Modulating Damper";
                else if (pointname.equals("relay2"))
                    return "Series Fan";
                else if (pointname.equals("Thermistor1"))
                    return "Discharge Airflow";
                else if (pointname.equals("Thermistor2"))
                    return "Supply Airflow";
                break;
            case "VAV_PARALLEL_FAN":
                if (pointname.equals("Analog-out2"))
                    return "Modulating Reheat";
                else if (pointname.equals("Analog-out1"))
                    return "Modulating Damper";
                else if (pointname.equals("relay2"))
                    return "Parallel Fan";
                else if (pointname.equals("Thermistor1"))
                    return "Discharge Airflow";
                else if (pointname.equals("Thermistor2"))
                    return "Supply Airflow";
                break;
            case "VAV_REHEAT":
                if (pointname.equals("Analog-out2"))
                    return "Modulating Reheat";
                else if (pointname.equals("Analog-out1"))
                    return "Modulating Damper";
                else if (pointname.equals("relay2"))
                    return "Series Fan";
                else if (pointname.equals("Thermistor1"))
                    return "Discharge Airflow";
                else if (pointname.equals("Thermistor2"))
                    return "Supply Airflow";
                break;
            case "SMARTSTAT_CONVENTIONAL_PACK_UNIT":
                if (pointname.equals("relay1"))
                    return "Cooling Stage 1";
                else if (pointname.equals("relay2"))
                    return "Cooling Stage 2";
                else if (pointname.equals("relay3"))
                    return "Fan Low Speed";
                else if (pointname.equals("relay4"))
                    return "Heating Stage 1";
                else if (pointname.equals("relay5"))
                    return "Heating Stage 2";
                else if (pointname.equals("relay6"))
                    return "Fan High Speed";
                else if (pointname.equals("Thermistor1"))
                    return "Airflow Sensor";
                else if (pointname.startsWith("Analog"))
                    return "Not Used";
                break;
            case "PLC":
                HashMap equip = CCUHsApi.getInstance().read("equip and pid and group == \"" + listTitle.substring(3) + "\"");
                String equipRef = equip.get("id").toString();
                if (pointname.equals("Analog1In")){
                    ArrayList<String> analog1InArr = new ArrayList<>();
                    analog1InArr.add("Not Used");
                    for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
                        analog1InArr.add(r.sensorName+" "+r.engineeringUnit);
                    }
                    int analogInSelection = CCUHsApi.getInstance().readDefaultVal("point and config and analog1 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
                    return analog1InArr.get(analogInSelection);
                }
                else if (pointname.equals("Analog2In")){
                    ArrayList<String> analog2InArr = new ArrayList<>();
                    for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
                        if (!r.sensorName.contains("ION Meter")) {
                            analog2InArr.add(r.sensorName + " " + r.engineeringUnit);
                        }
                    }
                    int analogInSelection = CCUHsApi.getInstance().readDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
                    return analog2InArr.get(analogInSelection);
                }
                else if (pointname.equals("Thermistor1")) {
                    ArrayList<String> th1InArr = new ArrayList<>();
                    th1InArr.add("Not Used");
                    for (Thermistor m : Thermistor.getThermistorList()) {
                        th1InArr.add(m.sensorName+" "+m.engineeringUnit);
                    }
                    int thSelection = CCUHsApi.getInstance().readDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
                    return th1InArr.get(thSelection);
                }
                break;
            case "SMARTSTAT_HEAT_PUMP_UNIT":
                if (pointname.equals("relay1"))
                    return "Compressor Stage 1";
                else if (pointname.equals("relay2"))
                    return "Compressor Stage 2";
                else if (pointname.equals("relay3"))
                    return "Fan Low Speed";
                else if (pointname.equals("relay4"))
                    return "Aux Heating\nStage 1";
                else if (pointname.equals("relay5"))
                    return "Fan High Speed";
                else if (pointname.equals("relay6"))
                    return "Heat Pump Changeover";
                else if (pointname.equals("Thermistor1"))
                    return "Airflow Sensor";
                else if (pointname.startsWith("Analog"))
                    return "Not Used";
                break;
            case "SMARTSTAT_TWO_PIPE_FCU":
                if (pointname.equals("relay1"))
                    return "Fan medium speed";
                else if (pointname.equals("relay2"))
                    return "Fan high speed ";
                else if (pointname.equals("relay3"))
                    return "Fan Low Speed";
                else if (pointname.equals("relay4"))
                    return "Aux Heating\nStage 1";
                else if (pointname.equals("relay6"))
                    return "Water Valve";
                else if (pointname.equals("Thermistor1"))
                    return "Airflow temper\n-ature sensor";
                else if (pointname.equals("Thermistor2"))
                    return "Supply water sensor";
                break;
            case "SMARTSTAT_FOUR_PIPE_FCU":
                if (pointname.equals("relay1"))
                    return "Fan medium speed";
                else if (pointname.equals("relay2"))
                    return "Fan high speed ";
                else if (pointname.equals("relay3"))
                    return "Fan Low Speed";
                else if (pointname.equals("relay4"))
                    return "Heating Water\nValve";
                else if (pointname.equals("relay6"))
                    return "Cooling Water\nValve";
                else if (pointname.equals("Thermistor1"))
                    return "Airflow temper\n-ature sensor";
                break;
            case "DUAL_DUCT":
                DualDuctProfile mDualDuctProfile = (DualDuctProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                DualDuctProfileConfiguration mProfileConfig = (DualDuctProfileConfiguration) mDualDuctProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
                if (pointname.equals("Thermistor1"))
                    return "Discharge Airflow\n Temp";
                else if (pointname.equals("Thermistor2"))
                    if (mProfileConfig.getThermistor2Config() == 0)
                        return "Cooling Supply\nAir Temp";
                    else return "Heating Supply\nAir Temp";
                else if (pointname.equals("Analog-out1")) {
                    int analogPos = mProfileConfig.getAnalogOut1Config();
                    if (analogPos == 0)
                        return "Not Used";
                    if (analogPos == 1)
                        return "Composite Damper\nActuator";
                    if (analogPos == 2)
                        return "Cooling Damper\nActuator";
                    if (analogPos == 3)
                        return "Heating Damper\nActuator";
                }else if (pointname.equals("Analog-out2")) {
                    int analogPos = mProfileConfig.getAnalogOut2Config();
                    if (analogPos == 0)
                        return "Not Used";
                    if (analogPos == 1)
                        return "Composite Damper\nActuator";
                    if (analogPos == 2)
                        return "Cooling Damper\nActuator";
                    if (analogPos == 3)
                        return "Heating Damper\nActuator";
                }
                break;
            case "HYPERSTAT_SENSE":
                HyperStatSenseProfile mHSSenseProfile = (HyperStatSenseProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                HyperStatSenseConfiguration mHSSenseConfig = (HyperStatSenseConfiguration) mHSSenseProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
                ArrayList<String> analogArr = new ArrayList<>();
                for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
                    analogArr.add(r.sensorName + " " + r.engineeringUnit);
                }
                ArrayList<String> thArr = new ArrayList<>();
                for (Thermistor m : Thermistor.getThermistorList()) {
                    thArr.add(m.sensorName + " " + m.engineeringUnit);
                }
                if (pointname.equals("Analog1In"))
                    return analogArr.get(mHSSenseConfig.analog1Sensor);
                else if (pointname.equals("Analog2In"))
                    return analogArr.get(mHSSenseConfig.analog2Sensor);
                else if (pointname.equals("Thermistor1"))
                    return thArr.get(mHSSenseConfig.th1Sensor);
                else if (pointname.equals("Thermistor2"))
                    return thArr.get(mHSSenseConfig.th2Sensor);
                break;
        }
        return "Not Used";
    }

    public double getConfigNumVal(String tags, int nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }

    public void setPointVal(String id, double val) {
        if (val != 0.0){
            CCUHsApi hayStack = CCUHsApi.getInstance();
            hayStack.writeHisValById(id, val);
            Object logicalPoint = hayStack.readMapById(id).get("pointRef");
            if (Objects.nonNull(logicalPoint)) {
                //hayStack.writeHisValById(logicalPoint.toString(), val / 1000);
                hayStack.writeHisValById(logicalPoint.toString(), val);
            }
        }
    }

    public void setPointValForThermistor(String id, double val) {
        if (val != 0.0){
            CCUHsApi hayStack = CCUHsApi.getInstance();
            hayStack.writeHisValById(id, val*10);
            Object logicalPoint = hayStack.readMapById(id).get("pointRef");
            if (Objects.nonNull(logicalPoint)) {
                hayStack.writeHisValById(logicalPoint.toString(), ThermistorUtil.getThermistorValueToTemp(val*10));
            }
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
