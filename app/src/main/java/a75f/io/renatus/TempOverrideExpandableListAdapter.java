/*Created by Aniket on 07/07/2021*/
package a75f.io.renatus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.dab.DabProfile;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.definitions.ReheatType;
import a75f.io.logic.bo.building.dualduct.DualDuctProfile;
import a75f.io.logic.bo.building.dualduct.DualDuctProfileConfiguration;
import a75f.io.logic.bo.building.hyperstat.cpu.HyperStatCpuConfiguration;
import a75f.io.logic.bo.building.hyperstat.cpu.HyperStatCpuProfile;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseConfiguration;
import a75f.io.logic.bo.building.hyperstatsense.HyperStatSenseProfile;
import a75f.io.logic.bo.building.plc.PlcProfile;
import a75f.io.logic.bo.building.plc.PlcProfileConfiguration;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitConfiguration;
import a75f.io.logic.bo.building.ss2pfcu.TwoPipeFanCoilUnitProfile;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitConfiguration;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitProfile;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitConfiguration;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitProfile;
import a75f.io.logic.bo.building.sse.SingleStageConfig;
import a75f.io.logic.bo.building.sse.SingleStageProfile;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitConfiguration;
import a75f.io.logic.bo.building.sshpu.HeatPumpUnitProfile;
import a75f.io.logic.bo.building.system.dab.DabFullyModulatingRtu;
import a75f.io.logic.bo.building.system.dab.DabStagedRtu;
import a75f.io.logic.bo.building.system.vav.VavFullyModulatingRtu;
import a75f.io.logic.bo.building.system.vav.VavStagedRtu;
import a75f.io.logic.bo.building.vav.VavProfile;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.renatus.util.CCUUiUtil;
import a75f.io.renatus.util.CCUUtils;

import static a75f.io.device.mesh.LSmartNode.PULSE;
import static a75f.io.renatus.TempOverrideFragment.getPointVal;

public class TempOverrideExpandableListAdapter extends BaseExpandableListAdapter {
    private Fragment mFragment;
    private List<String> expandableListTitle;
    private TreeMap<String, List<String>> expandableListDetail;
    private TreeMap<String, String>       idMap;
    String siteName;
    private ArrayList<String> equipsRef;
    String thOver = null;
    String thEquipDetails = null;

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
        ArrayList<String> analogOut1Val = new ArrayList<String>();
        ArrayList<String> analogOut2Val = new ArrayList<String>();
        ArrayList<String> analogOut3Val = new ArrayList<String>();

        targetVal.add("- - - -");
        analogOut1Val.add("- - - -");
        analogOut2Val.add("- - - -");
        analogOut3Val.add("- - - -");

        String listTitle = (String) getGroup(listPosition);
        if (!listTitle.equals("CM-device")) {
            HashMap equipGroup = CCUHsApi.getInstance().read("equip and group == \"" + listTitle.substring(3) + "\"");
            String profile = equipGroup.get("profile").toString();

            if (profile.equals("VAV_REHEAT") || profile.equals("VAV_SERIES_FAN") || profile.equals("VAV_PARALLEL_FAN")) {
                ArrayAdapter<String> damperTypesAdapter;
                ArrayList<String> damperTypes = new ArrayList<>();
                for (DamperType damper : DamperType.values()) {
                    damperTypes.add(damper.displayName);
                }
                damperTypesAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, damperTypes);
                damperTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                VavProfile mVavProfile = (VavProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                VavProfileConfiguration mProfileConfig = (VavProfileConfiguration) mVavProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
                int damperPosition = damperTypesAdapter.getPosition(DamperType.values()[mProfileConfig.damperType].displayName);

                ArrayAdapter<String> reheatTypesAdapter;
                ArrayList<String> reheatTypes = new ArrayList<>();
                for (ReheatType actuator : ReheatType.values()) {
                    reheatTypes.add(actuator.displayName);
                }
                reheatTypesAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, reheatTypes);
                reheatTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                int reheatPosition = reheatTypesAdapter.getPosition(ReheatType.values()[mProfileConfig.reheatType].displayName);
                if (damperPosition == 0) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-0-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 0); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                } else if (damperPosition == 1) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-2-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 2); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                } else if (damperPosition == 2) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-10-2v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 2); pos -= (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                } else if (damperPosition == 3) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-10-0v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 0); pos -= (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                }
                if (reheatPosition == 0) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-0-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 0); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                } else if (reheatPosition == 1) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-2-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 2); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                } else if (reheatPosition == 2) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-10-2v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 2); pos -= (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                } else if (reheatPosition == 3) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-10-0v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 0); pos -= (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                }
                for (int pos = (int) (100 * 0); pos <= (100 * 10); pos += (100 * 0.1)) {
                    targetVal.add(pos / 100.0 + " V");
                }
            }
            else if (profile.equals("DAB")) {
                DabProfile mDabProfile = (DabProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                DabProfileConfiguration mProfileConfig = (DabProfileConfiguration) mDabProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
                ;
                int damper1Position = mProfileConfig.damper1Type;
                int damper2Position = mProfileConfig.damper2Type;
                if (damper1Position == 0) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-0-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 0); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                } else if (damper1Position == 1) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-2-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 2); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                } else if (damper1Position == 2) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-10-2v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 2); pos -= (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                } else if (damper1Position == 3) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog1",profile+"-type-10-0v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 0); pos -= (100 * 0.1)) {
                        analogOut1Val.add(pos / 100.0 + " V");
                    }
                }
                if (damper2Position == 0) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-0-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 0); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                } else if (damper2Position == 1) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-2-10v");
                    edit.commit();
                    for (int pos = (int) (100 * 2); pos <= (100 * 10); pos += (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                } else if (damper2Position == 2) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-10-2v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 2); pos -= (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                } else if (damper2Position == 3) {
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                    edit.putString("cat-analog2",profile+"-type-10-0v");
                    edit.commit();
                    for (int pos = (int) (100 * 10); pos >= (100 * 0); pos -= (100 * 0.1)) {
                        analogOut2Val.add(pos / 100.0 + " V");
                    }
                }
                for (int pos = (int) (100 * 0); pos <= (100 * 10); pos += (100 * 0.1)) {
                    targetVal.add(pos / 100.0 + " V");
                }
            }
            else{
                for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                    targetVal.add(pos /100.0 +" V");
                }
                for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                    analogOut1Val.add(pos /100.0 +" V");
                }
                for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                    analogOut2Val.add(pos /100.0 +" V");
                }
                for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                    analogOut3Val.add(pos /100.0 +" V");
                }
            }
        }
        else {
            for (int pos = (int)(100*0); pos <= (100*10); pos+=(100*0.1)) {
                targetVal.add(pos /100.0 +" V");
            }
        }

        ArrayAdapter<String> analogOut1Adapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, analogOut1Val){
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
        ArrayAdapter<String> analogOut2Adapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, analogOut2Val){
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
        ArrayAdapter<String> analogOut3Adapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, analogOut3Val){
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
        relyVal.add("- - - -");
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
            Spinner spinner_analog_out1 = convertView
                    .findViewById(R.id.spinner_analog_out1);
            Spinner spinner_analog_out2 = convertView
                    .findViewById(R.id.spinner_analog_out2);
            Spinner spinner_analog_out3 = convertView
                    .findViewById(R.id.spinner_analog_out3);
            Spinner spinner_relay = convertView
                    .findViewById(R.id.spinner_relay);
            TextInputEditText etThermistor = convertView
                    .findViewById(R.id.etThermistor);
            expandedListTextVal.setText("-");
            txt_calculated_output.setText("-");
            if (Globals.getInstance().gettempOverCount() < 1){
                TempOverRiddenValue.getInstance().addOriginalValues(expandedListText,equipId+"-value-"+value);
                if (TempOverrideFragment.isExpandedTextBeginsWithAnalogType(expandedListText.toLowerCase())) {
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
                    } else if (expandedListText.toLowerCase().startsWith("analog1out")) {
                        String analogOut1Mapped = getZoneMapping("Analog-out1", listPosition, convertView);
                        if (!analogOut1Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(" + analogOut1Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1");
                        txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");

                        spinner_analog_out1.setVisibility(View.VISIBLE);
                        spinner_analog_out1.setAdapter(analogOut1Adapter);
                        spinner_analog_out1.setSelection(0);
                        spinner_analog_out1.setSelection(0,false);
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
                    } else if (expandedListText.toLowerCase().startsWith("analog2out")) {
                        String analogOut2Mapped = getZoneMapping("Analog-out2", listPosition, convertView);
                        if (!analogOut2Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2" + "\n(" + analogOut2Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2");

                        txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");
                        spinner_analog_out2.setVisibility(View.VISIBLE);
                        spinner_analog_out2.setAdapter(analogOut2Adapter);
                        spinner_analog_out2.setSelection(0);
                        spinner_analog_out2.setSelection(0,false);
                    } else if (expandedListText.toLowerCase().startsWith("analog3out")) {
                        String analogOut3Mapped = getZoneMapping("Analog-out3", listPosition, convertView);
                        if (!analogOut3Mapped.equals(""))
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3" + "\n(" + analogOut3Mapped + ")");
                        else
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3");

                        txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");
                        spinner_analog_out3.setVisibility(View.VISIBLE);
                        spinner_analog_out3.setAdapter(analogOut3Adapter);
                        spinner_analog_out3.setSelection(0);
                        spinner_analog_out3.setSelection(0,false);
                    }else if (expandedListText.startsWith("relay")) {
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
                        expandedListTextVal.setText("" + value + " " + "Kilo ohms");
                        etThermistor.setVisibility(View.VISIBLE);
                        if (!Strings.isNullOrEmpty(thOver) && thEquipDetails.equals(equipId + expandedListText.substring(2, 3)))
                            etThermistor.setText(thOver);
                        else etThermistor.getText().clear();
                    } else if (expandedListText.startsWith(siteName)) {
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
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Not Enabled)");
                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        }else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                            if (getConfigEnabled("analog2") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Fan Speed)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Not Enabled)");
                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                            if (getConfigEnabled("analog3") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Heating)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Not Enabled)");
                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            spinner_override_value.setAdapter(targetValAdapter);
                            spinner_override_value.setSelection(0);
                            spinner_override_value.setSelection(0,false);
                        } else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                            if (getConfigEnabled("analog4") > 0)
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Composite)");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Not Enabled)");
                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(value) + " V");
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
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(Not Enabled)");
                                Object valueToDelete = getChild(listPosition, expandedListPosition);
                                expandableListDetail.remove(valueToDelete);
                            }
                            txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(expandedListText)), 1.0) == 0 ? "ON" : "OFF");
                            spinner_relay.setVisibility(View.VISIBLE);
                            spinner_relay.setAdapter(relayValAdapter);
                            spinner_relay.setSelection(0);
                            spinner_relay.setSelection(0,false);
                        }
                    }
                    expandedListTextView.setText(NewexpandedListText);
                    notifyDataSetChanged();
                }
            }
            else if (Globals.getInstance().gettempOverCount()>0){
                String originalData = TempOverRiddenValue.getInstance().getOriginalValues().get(expandedListText);
                if(!Strings.isNullOrEmpty(originalData)) {
                    String[] parts = originalData.split("-value-");
                    String id = parts[0];
                    String value1 = parts[1];
                    Double val = Double.valueOf(value1);
                    if (TempOverrideFragment.isExpandedTextBeginsWithAnalogType(expandedListText.toLowerCase())) {
                        //double value = getPointVal(idMap.get(expandedListText));
                        unit = CCUHsApi.getInstance().readMapById(equipId).get("unit");
                        if (Objects.nonNull(unit)) {
                            if (unit.toString().equals("mV"))
                                val = val * 0.001;
                            else if (unit.toString().equals("dV"))
                                val = val * 0.1;
                        }

                        if (expandedListText.startsWith("Analog1In")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
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
                            if (!Strings.isNullOrEmpty(overridenData)) {
                                int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                            else{
                                setPointVal(id, val);
                                idMap.put(id, value1);
                            }
                        } else if (expandedListText.toLowerCase().startsWith("analog1out")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                            String analogOut1Mapped = getZoneMapping("Analog-out1", listPosition, convertView);

                            if (!analogOut1Mapped.equals(""))
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(" + analogOut1Mapped + ")");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1");
                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                            spinner_analog_out1.setVisibility(View.VISIBLE);
                            analogOut1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_analog_out1.setAdapter(analogOut1Adapter);
                            if (!Strings.isNullOrEmpty(overridenData)) {
                                int spinnerPosition = analogOut1Adapter.getPosition(overridenData);
                                spinner_analog_out1.setSelection(spinnerPosition);
                            }
                            else{
                                setPointVal(id, val);
                                idMap.put(id, value1);
                            }
                        } else if (expandedListText.startsWith("Analog2In")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                            String analogIn2Mapped = getZoneMapping("Analog2In", listPosition, convertView);
                            if (!analogIn2Mapped.equals(""))
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2\n(" + analogIn2Mapped + ")");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in2");
                            expandedListTextVal.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                            spinner_override_value.setVisibility(View.VISIBLE);
                            targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_override_value.setAdapter(targetValAdapter);
                            if(!Strings.isNullOrEmpty(overridenData)) {
                                int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                spinner_override_value.setSelection(spinnerPosition);
                            }
                            else{
                                setPointVal(id, val);
                                idMap.put(id, value1);
                            }
                        } else if (expandedListText.toLowerCase().startsWith("analog2out")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                            String analogOut2Mapped = getZoneMapping("Analog-out2", listPosition, convertView);
                            if (!analogOut2Mapped.equals(""))
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2" + "\n(" + analogOut2Mapped + ")");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2");

                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                            spinner_analog_out2.setVisibility(View.VISIBLE);
                            analogOut2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_analog_out2.setAdapter(analogOut2Adapter);
                            if(!Strings.isNullOrEmpty(overridenData)) {
                                int spinnerPosition = analogOut2Adapter.getPosition(overridenData);
                                spinner_analog_out2.setSelection(spinnerPosition);
                            }
                            else {
                                setPointVal(id, val);
                                idMap.put(id, value1);
                            }
                        }else if (expandedListText.toLowerCase().startsWith("analog3out")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                            String analogOut3Mapped = getZoneMapping("Analog-out3", listPosition, convertView);
                            if (!analogOut3Mapped.equals(""))
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3" + "\n(" + analogOut3Mapped + ")");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3");

                            txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                            spinner_analog_out3.setVisibility(View.VISIBLE);
                            analogOut3Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_analog_out3.setAdapter(analogOut3Adapter);
                            if(!Strings.isNullOrEmpty(overridenData)) {
                                int spinnerPosition = analogOut3Adapter.getPosition(overridenData);
                                spinner_analog_out3.setSelection(spinnerPosition);
                            }
                            else {
                                setPointVal(id, val);
                                idMap.put(id, value1);
                            }
                        } else if (expandedListText.startsWith("relay")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(5, 6));
                            String relayMapped = getZoneMapping("relay" + expandedListText.substring(5, 6), listPosition, convertView);
                            if (!relayMapped.equals(""))
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6) + "\n(" + relayMapped + ")");
                            else
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + expandedListText.substring(5, 6));
                            txt_calculated_output.setText(value1.equals("1.0") ? "ON" : "OFF");
                            spinner_relay.setVisibility(View.VISIBLE);
                            relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner_relay.setAdapter(relayValAdapter);
                            if(!Strings.isNullOrEmpty(overridenData)) {
                                int spinnerPosition = relayValAdapter.getPosition(overridenData);
                                spinner_relay.setSelection(spinnerPosition);
                            }
                            else {
                                setPointVal(id, val);
                                idMap.put(id, value1);
                            }
                        } else if (expandedListText.startsWith("Th")) {
                            String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(2, 3));
                            String thermistorMapped = getZoneMapping("Thermistor" + expandedListText.substring(2, 3), listPosition, convertView);
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Thermistor " + expandedListText.substring(2, 3) + "\n(" + thermistorMapped + ")");
                            expandedListTextVal.setText("" + val + " " + CCUHsApi.getInstance().readMapById(equipId).get("unit"));
                            etThermistor.setVisibility(View.VISIBLE);
                            if(!Strings.isNullOrEmpty(overridenData)) {
                                etThermistor.setText(overridenData);
                            }
                            else{
                                setPointValForThermistor(id, val, Integer.parseInt(listTitle.substring(3)), listTitle);
                                idMap.put(id, value1);
                            }
                        } else if (expandedListText.startsWith(siteName)) {
                            setPointVal(id, val);
                            idMap.put(id, value1);
                            NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, expandedListText.substring(siteName.length() + 1, expandedListText.length()));
                            if (NewexpandedListText.startsWith("CM-analog1In")) {
                                String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                                NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-in1");
                                expandedListTextVal.setText("" + val + " V");
                                spinner_override_value.setVisibility(View.VISIBLE);
                                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner_override_value.setAdapter(targetValAdapter);
                                if (!Strings.isNullOrEmpty(overridenData)) {
                                    int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                    spinner_override_value.setSelection(spinnerPosition);
                                }
                            } else if (NewexpandedListText.startsWith("CM-analog1Out")) {
                                String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                                if (getConfigEnabled("analog1") > 0) {
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Cooling)");
                                } else
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out1\n(Not Enabled)");
                                txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                                spinner_override_value.setVisibility(View.VISIBLE);
                                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner_override_value.setAdapter(targetValAdapter);
                                if(!Strings.isNullOrEmpty(overridenData)) {
                                    int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                    spinner_override_value.setSelection(spinnerPosition);
                                }
                            }else if (NewexpandedListText.startsWith("CM-analog2Out")) {
                                String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                                if (getConfigEnabled("analog2") > 0)
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Fan Speed)");
                                else
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out2\n(Not Enabled)");
                                txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                                spinner_override_value.setVisibility(View.VISIBLE);
                                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner_override_value.setAdapter(targetValAdapter);
                                if(!Strings.isNullOrEmpty(overridenData)) {
                                    int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                    spinner_override_value.setSelection(spinnerPosition);
                                }
                            } else if (NewexpandedListText.startsWith("CM-analog3Out")) {
                                String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                                if (getConfigEnabled("analog3") > 0)
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Heating)");
                                else
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out3\n(Not Enabled)");
                                txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                                spinner_override_value.setVisibility(View.VISIBLE);
                                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner_override_value.setAdapter(targetValAdapter);
                                if(!Strings.isNullOrEmpty(overridenData)) {
                                    int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                    spinner_override_value.setSelection(spinnerPosition);
                                }
                            } else if (NewexpandedListText.startsWith("CM-analog4Out")) {
                                String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(6));
                                if (getConfigEnabled("analog4") > 0)
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Composite)");
                                else
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Analog-out4\n(Not Enabled)");
                                txt_calculated_output.setText("" + CCUUtils.roundToTwoDecimal(val) + " V");
                                spinner_override_value.setVisibility(View.VISIBLE);
                                targetValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner_override_value.setAdapter(targetValAdapter);
                                if(!Strings.isNullOrEmpty(overridenData)) {
                                    int spinnerPosition = targetValAdapter.getPosition(overridenData);
                                    spinner_override_value.setSelection(spinnerPosition);
                                }
                            } else if (NewexpandedListText.startsWith("relay")) {
                                String overridenData = TempOverRiddenValue.getInstance().getOverriddenValues().get(equipId+expandedListText.substring(5, 6));
                                String relayPos = (expandedListText.substring(siteName.length() + 6, siteName.length() + 7));
                                if (getConfigEnabled("relay" + relayPos) > 0) {
                                    String relayMapped = getRelayMapping("relay" + relayPos, convertView);
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(" + relayMapped + ")");
                                } else {
                                    NewexpandedListText = NewexpandedListText.replace(NewexpandedListText, "Relay " + relayPos + "\n(Not Enabled)");
                                    Object valueToDelete = getChild(listPosition, expandedListPosition);
                                    expandableListDetail.remove(valueToDelete);
                                }
                                //txt_calculated_output.setText(Double.compare(getPointVal(idMap.get(value1)), 1.0) == 0 ? "ON" : "OFF");
                                txt_calculated_output.setText(value1.equals("1.0") ? "ON" : "OFF");
                                spinner_relay.setVisibility(View.VISIBLE);
                                spinner_relay.setVisibility(View.VISIBLE);
                                relayValAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner_relay.setAdapter(relayValAdapter);
                                if(!Strings.isNullOrEmpty(overridenData)) {
                                    int spinnerPosition = relayValAdapter.getPosition(overridenData);
                                    spinner_relay.setSelection(spinnerPosition);
                                }
                            }
                        }
                        expandedListTextView.setText(NewexpandedListText);
                        notifyDataSetChanged();
                    }
                }else{
                    TempOverRiddenValue.getInstance().addOriginalValues(expandedListText,equipId+"-value-"+value);
                }
            }

            Object finalUnit = unit;
            spinner_override_value.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    Globals.getInstance().setTemporaryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_override_value.getSelectedItem().toString();
                    if (!selectedSpinnerItem.equals("- - - -")) {
                        int index = selectedSpinnerItem.lastIndexOf("V");
                        if (Objects.nonNull(finalUnit)) {
                            if (finalUnit.toString().equals("dV")) {
                                Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                                setPointVal(idMap.get(tunerName), (pointValue));
                                idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                            } else if (finalUnit.toString().equals("mV")) {
                                Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 1000;
                                setPointVal(idMap.get(tunerName), (pointValue));
                                idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                            } else {
                                Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1));
                                setPointVal(idMap.get(tunerName), (pointValue));
                                idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                            }
                        } else {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointVal(idMap.get(tunerName), (pointValue));
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                        TempOverRiddenValue.getInstance().addOverRiddenValues(equipId+expandedListText.substring(6), selectedSpinnerItem);
                    }

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });
            spinner_analog_out1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    short logicalValue = 0;
                    Globals.getInstance().setTemporaryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_analog_out1.getSelectedItem().toString();
                    if (!selectedSpinnerItem.equals("- - - -")) {
                        int index = selectedSpinnerItem.lastIndexOf("V");
                        double val = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1));
                        String dataToOverride_analog1 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString("cat-analog1", null);
                        if(!Strings.isNullOrEmpty(dataToOverride_analog1)){
                            String[] parts = dataToOverride_analog1.split("-type-");
                            String type = parts[1];
                            logicalValue = (short) mapAnalogOut(type, (float) val);

                        }

                        if (Objects.nonNull(finalUnit)) {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointValForAnalog(idMap.get(tunerName), (pointValue), logicalValue);
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        } else {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointValForAnalog(idMap.get(tunerName), (pointValue), logicalValue);
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                        TempOverRiddenValue.getInstance().addOverRiddenValues(equipId+expandedListText.substring(6), selectedSpinnerItem);
                    }

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });
            spinner_analog_out2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    short logicalValue = 0;
                    Globals.getInstance().setTemporaryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_analog_out2.getSelectedItem().toString();
                    if (!selectedSpinnerItem.equals("- - - -")) {
                        int index = selectedSpinnerItem.lastIndexOf("V");
                        double val = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1));
                        String dataToOverride_analog2 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString("cat-analog2", null);
                        if(!Strings.isNullOrEmpty(dataToOverride_analog2)) {
                            String[] parts = dataToOverride_analog2.split("-type-");
                            String profile = parts[0];
                            String type = parts[1];
                            logicalValue = (short) mapAnalogOut(type, (float) val);

                        }

                        if (Objects.nonNull(finalUnit)) {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointValForAnalog(idMap.get(tunerName), (pointValue), logicalValue);
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        } else {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointValForAnalog(idMap.get(tunerName), (pointValue), logicalValue);
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                        TempOverRiddenValue.getInstance().addOverRiddenValues(equipId+expandedListText.substring(6), selectedSpinnerItem);
                    }

                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });
            spinner_analog_out3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l)
                {
                    short logicalValue = 0;
                    Globals.getInstance().setTemporaryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_analog_out3.getSelectedItem().toString();
                    if (!selectedSpinnerItem.equals("- - - -")) {
                        int index = selectedSpinnerItem.lastIndexOf("V");
                        double val = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1));
                        String dataToOverride_analog3 = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getString("cat-analog2", null);
                        if(!Strings.isNullOrEmpty(dataToOverride_analog3)) {
                            String[] parts = dataToOverride_analog3.split("-type-");
                            String profile = parts[0];
                            String type = parts[1];
                            logicalValue = (short) mapAnalogOut(type, (float) val);

                        }

                        if (Objects.nonNull(finalUnit)) {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointValForAnalog(idMap.get(tunerName), (pointValue), logicalValue);
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        } else {
                            Double pointValue = Double.parseDouble(selectedSpinnerItem.substring(0, index - 1)) * 10;
                            setPointValForAnalog(idMap.get(tunerName), (pointValue), logicalValue);
                            idMap.put(idMap.get(tunerName), String.valueOf(pointValue));
                        }
                        TempOverRiddenValue.getInstance().addOverRiddenValues(equipId+expandedListText.substring(6), selectedSpinnerItem);
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
                    Globals.getInstance().setTemporaryOverrideMode(true);
                    Globals.getInstance().incrementTempOverCount();
                    String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                            expandedListPosition);
                    String selectedSpinnerItem = spinner_relay.getSelectedItem().toString();
                    if (!selectedSpinnerItem.equals("- - - -")) {
                        setPointValForRelay(idMap.get(tunerName), Double.parseDouble(String.valueOf(spinner_relay.getSelectedItemId())),
                                "relay" + expandedListText.substring(5, 6));
                        idMap.put(idMap.get(tunerName), spinner_relay.getSelectedItem().toString());
                        TempOverRiddenValue.getInstance().addOverRiddenValues(equipId+expandedListText.substring(5, 6), selectedSpinnerItem);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }

            });
            etThermistor.setOnEditorActionListener((v, actionId, event) -> {
                if(Strings.isNullOrEmpty(etThermistor.getText().toString())){
                    return true;
                }
                int enteredOhms = Integer.parseInt(etThermistor.getText().toString());
                if (enteredOhms >= 190 && enteredOhms <= 85387) {
                    InputMethodManager mgr = (InputMethodManager)mActivity
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(etThermistor.getWindowToken(), 0);
                    Toast.makeText(mActivity, "Overriding Thermistor value..." + enteredOhms, Toast.LENGTH_LONG).show();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Globals.getInstance().setTemporaryOverrideMode(true);
                            Globals.getInstance().incrementTempOverCount();
                            String tunerName = expandableListDetail.get(expandableListTitle.get(listPosition)).get(
                                    expandedListPosition);
                            String selectedSpinnerItem = etThermistor.getText().toString();
                            if (!selectedSpinnerItem.equals("- - - -")) {
                                setPointValForThermistor(idMap.get(tunerName),
                                        Double.parseDouble(selectedSpinnerItem), Integer.parseInt(listTitle.substring(3)), listTitle);
                                idMap.put(idMap.get(tunerName), selectedSpinnerItem);
                                TempOverRiddenValue.getInstance().addOverRiddenValues(equipId + expandedListText.substring(2, 3), selectedSpinnerItem);
                            }
                        }
                    }, 5000);
                } else {
                    Toast.makeText(mActivity, "Invalid Thermistor value", Toast.LENGTH_LONG).show();
                }
                return true;
            });

            etThermistor.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after)
                {
                }

                @Override
                public void afterTextChanged(Editable s)
                {
                    thOver = s.toString();
                    thEquipDetails = equipId + expandedListText.substring(2, 3);
                }
            });

            CCUUiUtil.setSpinnerDropDownColor(spinner_override_value,mActivity.getApplicationContext());
            CCUUiUtil.setSpinnerDropDownColor(spinner_analog_out1,mActivity.getApplicationContext());
            CCUUiUtil.setSpinnerDropDownColor(spinner_analog_out2,mActivity.getApplicationContext());
            CCUUiUtil.setSpinnerDropDownColor(spinner_relay,mActivity.getApplicationContext());
        }

        return convertView;
    }

    public static float mapAnalogOut(String type, float val) {
        switch (type)
        {
            case "0-10v":
                return ((val - 0) / (10 - 0)) * 100;
            case PULSE:
                return val;
            case "10-0v":
                return ((val - 10) / (0 - 10)) * 100;
            case "2-10v":
                return ((val - 2) / (10 - 2)) * 100;
            case "10-2v":
                float num = ((val - 10) / (2 - 10)) * 100;
                return num;
            default:
                String [] arrOfStr = type.split("-");
                if (arrOfStr.length == 2)
                {
                    if (arrOfStr[1].contains("v")) {
                        arrOfStr[1] = arrOfStr[1].replace("v", "");
                    }
                    int min = (int)Double.parseDouble(arrOfStr[0]);
                    int max = (int)Double.parseDouble(arrOfStr[1]);
                    if (max > min) {
                        return (short) (min * 10 + (max - min ) * 10 * val/100);
                    } else {
                        return (short) (min * 10 - (min - max ) * 10 * val/100);
                    }
                }
        }
        return (short) 0;
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
            case SYSTEM_DAB_STAGED_VFD_RTU:
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_DAB_STAGED_RTU:
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_DAB_HYBRID_RTU:
                return hvac_stage_selector.get((int)dabStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_VAV_STAGED_RTU:
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
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
            case SYSTEM_VAV_HYBRID_RTU:
                return hvac_stage_selector.get((int)vavStagedRtu.getConfigAssociation(relayname));
        }
        return "";
    }

    private String getZoneMapping(String pointname, int listPosition, View convertView){
        String listTitle = (String) getGroup(listPosition);
        String profile = getProfileName(listTitle);
        switch (profile){
            case "SSE":
                SingleStageProfile mSSEProfile = (SingleStageProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                SingleStageConfig sseProfileConfig = (SingleStageConfig) mSSEProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
                int relaypos = (int)getConfigNumVal("enable and "+pointname, Integer.parseInt(listTitle.substring(3)));
                if (pointname.equals("relay1")){
                    if (sseProfileConfig.isOpConfigured(Port.RELAY_ONE)) {
                        if (relaypos == 1)
                            return "Heating";
                        else if (relaypos == 2)
                            return "Cooling";
                        //else return "Not enabled";
                    }
                    else {
                        return "Not Enabled";
                    }
                }
                else if (pointname.equals("relay2")){
                    if (sseProfileConfig.isOpConfigured(Port.RELAY_TWO)) {
                        List<String> sse_relay2_mode = Arrays.asList(convertView.getResources().getStringArray(R.array.sse_relay2_mode));
                        return sse_relay2_mode.get(relaypos);
                    }
                    else {
                        return "Not Enabled";
                    }
                }
                else if (pointname.equals("Thermistor1")){
                    if (sseProfileConfig.enableThermistor1)
                        return "Airflow Sensor";
                    else
                        return "Not Enabled";
                }else if (pointname.equals("Thermistor2")){
                    if (sseProfileConfig.enableThermistor2)
                        return "External Sensor";
                    else
                        return "Not Enabled";
                }
                else if (pointname.startsWith("Analog"))
                    return "Not Used";
                break;
            case "VAV_SERIES_FAN":
                if (pointname.equals("Analog-out2"))
                    return "Modulating Reheat";
                else if (pointname.equals("Analog-out1"))
                    return "Modulating Damper";
                else if (pointname.equals("relay1")) {
                    return getVavRelayMapping(pointname, listTitle);
                } else if (pointname.equals("relay2"))
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
                else if (pointname.equals("relay1")) {
                    return getVavRelayMapping(pointname, listTitle);
                } else if (pointname.equals("relay2"))
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
                else if (pointname.equals("relay1") || pointname.equals("relay2")) {
                    return getVavRelayMapping(pointname, listTitle);
                } else if (pointname.equals("Thermistor1"))
                    return "Discharge Airflow";
                else if (pointname.equals("Thermistor2"))
                    return "Supply Airflow";
                break;
            case "SMARTSTAT_CONVENTIONAL_PACK_UNIT":
                ConventionalUnitProfile mCPUProfile = (ConventionalUnitProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                ConventionalUnitConfiguration cpuProfileConfig = (ConventionalUnitConfiguration) mCPUProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));

                if (pointname.equals("relay1")) {
                    if (cpuProfileConfig.isOpConfigured(Port.RELAY_ONE))
                        return "Cooling Stage 1";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("relay2")) {
                    if (cpuProfileConfig.isOpConfigured(Port.RELAY_TWO))
                        return "Cooling Stage 2";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("relay3")) {
                    if (cpuProfileConfig.isOpConfigured(Port.RELAY_THREE))
                        return "Fan Low Speed";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("relay4")) {
                    if (cpuProfileConfig.isOpConfigured(Port.RELAY_FOUR))
                        return "Heating Stage 1";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("relay5")) {
                    if (cpuProfileConfig.isOpConfigured(Port.RELAY_FIVE))
                        return "Heating Stage 2";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("relay6")) {
                    if (cpuProfileConfig.isOpConfigured(Port.RELAY_SIX))
                        return "Fan High Speed";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("Thermistor1")) {
                    if (cpuProfileConfig.enableThermistor1)
                        return "Airflow Sensor";
                    else
                        return "Not Enabled";
                }
                else if (pointname.equals("Thermistor2")) {
                    if (cpuProfileConfig.enableThermistor2)
                        return "External Sensor";
                    else
                        return "Not Enabled";
                }
                else if (pointname.startsWith("Analog"))
                    return "Not Used";
                break;
            case "PLC":
                PlcProfile mPlcProfile = (PlcProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                PlcProfileConfiguration plcProfileConfig = (PlcProfileConfiguration) mPlcProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
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
                    if (plcProfileConfig.useAnalogIn2ForSetpoint) {
                        ArrayList<String> analog2InArr = new ArrayList<>();
                        for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
                            if (!r.sensorName.contains("ION Meter")) {
                                analog2InArr.add(r.sensorName + " " + r.engineeringUnit);
                            }
                        }
                        int analogInSelection = CCUHsApi.getInstance().readDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
                        return analog2InArr.get(analogInSelection);
                    }else
                        return "Not Enabled";
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
                else if (pointname.equals("relay1")){
                    if(plcProfileConfig.relay1ConfigEnabled)
                        return "";
                    else return "Not Enabled";
                }else if (pointname.equals("relay2")){
                    if(plcProfileConfig.relay2ConfigEnabled)
                        return "";
                    else return "Not Enabled";
                }
                break;
            case "SMARTSTAT_HEAT_PUMP_UNIT":
                HeatPumpUnitProfile mHPUProfile = (HeatPumpUnitProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));;
                HeatPumpUnitConfiguration hpuProfileConfig = (HeatPumpUnitConfiguration) mHPUProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));;
                if (pointname.equals("relay1")) {
                    if (hpuProfileConfig.isOpConfigured(Port.RELAY_ONE))
                        return "Compressor Stage 1";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay2")) {
                    if (hpuProfileConfig.isOpConfigured(Port.RELAY_TWO))
                        return "Compressor Stage 2";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay3")) {
                    if (hpuProfileConfig.isOpConfigured(Port.RELAY_THREE))
                        return "Fan Low Speed";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay4")) {
                    if (hpuProfileConfig.isOpConfigured(Port.RELAY_FOUR))
                        return "Aux Heating\nStage 1";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay5")) {
                    if (hpuProfileConfig.isOpConfigured(Port.RELAY_FIVE))
                        return "Fan High Speed";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay6")) {
                    if (hpuProfileConfig.isOpConfigured(Port.RELAY_SIX))
                        return "Heat Pump Changeover";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor1")) {
                    if (hpuProfileConfig.enableThermistor1)
                        return "Airflow Sensor";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor2")){
                    if (hpuProfileConfig.enableThermistor2)
                        return "External Sensor";
                    else return "Not Enabled";
                }
                else if (pointname.startsWith("Analog"))
                    return "Not Used";
                break;
            case "SMARTSTAT_TWO_PIPE_FCU":
                TwoPipeFanCoilUnitProfile twoPfcuProfile = (TwoPipeFanCoilUnitProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));;
                TwoPipeFanCoilUnitConfiguration twopfcuProfileConfig = (TwoPipeFanCoilUnitConfiguration) twoPfcuProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));;
                if (pointname.equals("relay1")) {
                    if (twopfcuProfileConfig.isOpConfigured(Port.RELAY_ONE))
                        return "Fan medium speed";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay2")) {
                    if (twopfcuProfileConfig.isOpConfigured(Port.RELAY_TWO))
                        return "Fan high speed ";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay3")) {
                    if (twopfcuProfileConfig.isOpConfigured(Port.RELAY_THREE))
                        return "Fan Low Speed";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay4")) {
                    if (twopfcuProfileConfig.isOpConfigured(Port.RELAY_FOUR))
                        return "Aux Heating\nStage 1";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay6")) {
                    if (twopfcuProfileConfig.isOpConfigured(Port.RELAY_SIX))
                        return "Water Valve";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor1")) {
                    if (twopfcuProfileConfig.enableThermistor1)
                        return "Airflow temper\n-ature sensor";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor2")) {
                    if (twopfcuProfileConfig.enableThermistor2) return "Supply water sensor";
                    else return "Not Enabled";
                }
                break;
            case "SMARTSTAT_FOUR_PIPE_FCU":
                FourPipeFanCoilUnitProfile fourPfcuProfile = (FourPipeFanCoilUnitProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));;
                FourPipeFanCoilUnitConfiguration fourPFCUProfileConfig = (FourPipeFanCoilUnitConfiguration) fourPfcuProfile
                        .getProfileConfiguration(Short.parseShort(listTitle.substring(3)));;
                if (pointname.equals("relay1")) {
                    if (fourPFCUProfileConfig.isOpConfigured(Port.RELAY_ONE)) return "Fan medium speed";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay2")) {
                    if (fourPFCUProfileConfig.isOpConfigured(Port.RELAY_TWO)) return "Fan high speed ";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay3")) {
                    if (fourPFCUProfileConfig.isOpConfigured(Port.RELAY_THREE)) return "Fan Low Speed";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay4")) {
                    if (fourPFCUProfileConfig.isOpConfigured(Port.RELAY_FOUR)) return "Heating Water\nValve";
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay6")) {
                    if (fourPFCUProfileConfig.isOpConfigured(Port.RELAY_SIX)) return "Cooling Water\nValve";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor1")) {
                    if (fourPFCUProfileConfig.enableThermistor1) return "Airflow temper\n-ature sensor";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor2")) {
                    if (fourPFCUProfileConfig.enableThermistor2) return "External sensor";
                    else return "Not Enabled";
                }
                break;
            case "DAB":
                if (pointname.equals("Analog-out1"))
                    return "Damper1 Type";
                else if (pointname.equals("Analog-out2"))
                    return "Damper2 Type";
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
                if (pointname.equals("Analog1In")) {
                    if (mHSSenseConfig.isAnalog1Enable)
                        return analogArr.get(mHSSenseConfig.analog1Sensor);
                    else return "Not Enabled";
                }
                else if (pointname.equals("Analog2In")) {
                    if (mHSSenseConfig.isAnalog2Enable)
                        return analogArr.get(mHSSenseConfig.analog2Sensor);
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor1")) {
                    if (mHSSenseConfig.isTh1Enable)
                        return thArr.get(mHSSenseConfig.th1Sensor);
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor2")) {
                    if (mHSSenseConfig.isTh2Enable)
                        return thArr.get(mHSSenseConfig.th2Sensor);
                    else return "Not Enabled";
                }
                break;
            case "HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT":
                HyperStatCpuProfile mHSCpuProfile = (HyperStatCpuProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
                HyperStatCpuConfiguration mHSCpuConfig = (HyperStatCpuConfiguration) mHSCpuProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
                List<String> analogCpuArr = new ArrayList<>();
                for (Sensor r : SensorManager.getInstance().getExternalSensorList()) {
                    analogCpuArr.add(r.sensorName + " " + r.engineeringUnit);
                }
                List<String> thCpuArr = new ArrayList<>();
                for (Thermistor m : Thermistor.getThermistorList()) {
                    thCpuArr.add(m.sensorName + " " + m.engineeringUnit);
                }
                if (pointname.equals("Analog1In")) {
                    if (mHSCpuConfig.getAnalogIn1State().getEnabled())
                        return mHSCpuConfig.getAnalogIn1State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("Analog2In")) {
                    if (mHSCpuConfig.getAnalogIn2State().getEnabled())
                        return mHSCpuConfig.getAnalogIn2State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("Analog-out1")) {
                    if (mHSCpuConfig.getAnalogOut1State().getEnabled())
                        return mHSCpuConfig.getAnalogOut1State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("Analog-out2")) {
                    if (mHSCpuConfig.getAnalogOut2State().getEnabled())
                        return mHSCpuConfig.getAnalogOut2State().getAssociation().toString();
                    else return "Not Enabled";
                }else if (pointname.equals("Analog-out3")) {
                    if (mHSCpuConfig.getAnalogOut3State().getEnabled())
                        return mHSCpuConfig.getAnalogOut3State().getAssociation().toString();
                    else return "Not Enabled";
                }else if (pointname.equals("relay1")) {
                    if (mHSCpuConfig.getRelay1State().getEnabled())
                        return mHSCpuConfig.getRelay1State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay2")) {
                    if (mHSCpuConfig.getRelay2State().getEnabled())
                        return mHSCpuConfig.getRelay2State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay3")) {
                    if (mHSCpuConfig.getRelay3State().getEnabled())
                        return mHSCpuConfig.getRelay3State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay4")) {
                    if (mHSCpuConfig.getRelay4State().getEnabled())
                        return mHSCpuConfig.getRelay4State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay5")) {
                    if (mHSCpuConfig.getRelay5State().getEnabled())
                        return mHSCpuConfig.getRelay5State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("relay6")) {
                    if (mHSCpuConfig.getRelay6State().getEnabled())
                        return mHSCpuConfig.getRelay6State().getAssociation().toString();
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor1")) {
                    if (mHSCpuConfig.isEnableAirFlowTempSensor())
                        return "Airflow Temperature Sensor ";
                    else return "Not Enabled";
                }
                else if (pointname.equals("Thermistor2")) {
                    if (mHSCpuConfig.isEnableDoorWindowSensor())
                        return "Door/Window Sensor 1";
                    else return "Not Enabled";
                }
                break;
        }
        return "Not Used";
    }

    private String getVavRelayMapping(String pointname, String listTitle) {
        VavProfile vavProfile = (VavProfile) L.getProfile(Short.parseShort(listTitle.substring(3)));
        VavProfileConfiguration vavProfileConfig =
            (VavProfileConfiguration) vavProfile.getProfileConfiguration(Short.parseShort(listTitle.substring(3)));
    
        if (pointname.equals("relay1") && vavProfileConfig.isOpConfigured(Port.RELAY_ONE)) {
            return "Electric Reheat Stage 1";
        } else if (pointname.equals("relay2") && vavProfileConfig.isOpConfigured(Port.RELAY_TWO)) {
            return "Electric Reheat Stage 2";
        }
        return "Not Used";
    }
    
    private String getProfileName(String listTitle) {
        HashMap equipGroup = CCUHsApi.getInstance().read("equip and group == \"" + listTitle.substring(3) + "\"");
        String profile = equipGroup.get("profile").toString();
        return profile;
    }

    public double getConfigNumVal(String tags, int nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }

    public void setPointVal(String id, double val) {
        //Log.e("InsideTempOverrideExpandableListAdapter","id- "+id);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        hayStack.writeHisValById(id, val);
        Object logicalPoint = hayStack.readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            hayStack.writeHisValById(logicalPoint.toString(), val);
        }
    }

    public void setPointValForRelay(String id, double val, String pointname) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        hayStack.writeHisValById(id, val-1);
        Object logicalPoint = hayStack.readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            hayStack.writeHisValById(logicalPoint.toString(), val-1);
        }
    }

    public void setPointValForAnalog(String id, double val, short logicalValue) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        hayStack.writeHisValById(id, val);
        Object logicalPoint = hayStack.readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            hayStack.writeHisValById(logicalPoint.toString(), Double.valueOf(logicalValue));
        }
    }

    public void setPointValForThermistor(String id, double val, int nodeAddr, String listTitle) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        hayStack.writeHisValById(id, val);
        Object logicalPoint = hayStack.readMapById(id).get("pointRef");
        if (Objects.nonNull(logicalPoint)) {
            hayStack.writeHisValById(logicalPoint.toString(),
                    ThermistorUtil.getThermistorValueToTemp(val));
        }
        String profile = getProfileName(listTitle);
        HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
        boolean is2pfcu = device.containsKey("pipe2");
        if (device.containsKey("smartstat") && !is2pfcu || profile.equals("SSE")) {
            HashMap curTempPoint = hayStack.read("point and current and temp " +
                    "and group == \""+nodeAddr+"\"");
            if (!curTempPoint.isEmpty()){
                hayStack.writeHisValById(curTempPoint.get("id").toString(),
                        CCUUtils.roundToTwoDecimal(ThermistorUtil.getThermistorValueToTemp(val)));
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
