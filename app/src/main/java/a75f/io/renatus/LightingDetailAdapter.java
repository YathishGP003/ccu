package a75f.io.renatus;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import a75f.io.api.haystack.MockTime;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Circuit;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Output;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.definitions.OverrideType;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.lights.LightProfile;

import static a75f.io.logic.L.ccu;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_ONE;
import static a75f.io.logic.bo.building.definitions.Port.ANALOG_OUT_TWO;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_ONE;
import static a75f.io.logic.bo.building.definitions.Port.RELAY_TWO;

public class LightingDetailAdapter extends BaseAdapter
{
    
    public static final String TAG = "Lighting";
    
    View              row;
    LayoutInflater    inflater;
    AppCompatActivity c;
    ViewHolder        viewHolder;
    ArrayList<Output> snOutPortList;
    ListView          thiSList;
    private ArrayAdapter<CharSequence> aaOccupancyMode;
    private Boolean                    lcmdab;
    private RelativeLayout             schedule;
    private LightProfile               profile;
    private Floor                      floor;
    private Zone                       zone;
    
    
    public LightingDetailAdapter(Context c, ListView thiSList, LightProfile p, Floor floor,
                                 Zone zone, Boolean lcmdab)
    {
        this.c = (AppCompatActivity) c;
        this.floor = floor;
        this.zone = zone;
        notifyDataSetChanged();
        this.snOutPortList = new ArrayList<>();
        this.thiSList = thiSList;
        this.lcmdab = lcmdab;
        this.profile = p;
        for (Short address : this.profile.getNodeAddresses())
        {
            this.snOutPortList
                    .addAll(zone.findProfile(ProfileType.LIGHT).getProfileConfiguration(address)
                                .getOutputs());
        }
        
        
        aaOccupancyMode = ArrayAdapter
                                  .createFromResource(c.getApplicationContext(), R.array.schedulePORT, R.layout.spinner_item);
        aaOccupancyMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
        Collections.sort(snOutPortList, new Comparator<Output>()
        {
            @Override
            public int compare(Output lhs, Output rhs)
            {
                return lhs.getCircuitName().compareToIgnoreCase(rhs.getCircuitName());
            }
        });
    }
    
    
    @Override
    public int getCount()
    {
        return snOutPortList.size();
    }
    
    
    @Override
    public Object getItem(int position)
    {
        return position;
    }
    
    
    @Override
    public long getItemId(int position)
    {
        return position;
    }
    
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        row = convertView;
        viewHolder = null;
        if (row == null)
        {
            inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.lighting_detail_row, parent, false);
            viewHolder = new ViewHolder(row);
            row.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder) row.getTag();
        }
        row.setBackgroundColor(
                (position % 2 == 0) ? Color.parseColor("#ececec") : Color.TRANSPARENT);

            final Output snOutput = snOutPortList.get(position);
            viewHolder.LogicalName.setText(snOutput.getCircuitName());
            viewHolder.spinnerSchedule.setOnItemSelectedListener(null);
            viewHolder.spinnerSchedule.setTag(position);
            viewHolder.spinnerSchedule.setAdapter(aaOccupancyMode);
            float logicalVal = L.resolveZoneProfileLogicalValue(profile, snOutput);
            Log.i(TAG, "Position: " + position + " LogicalValue: " + logicalVal);
                    
            String brightnessVal = L.resolveZoneProfileLogicalValue(profile, snOutput) + "";

            viewHolder.spinnerSchedule.setSelection(snOutput.getScheduleMode().ordinal());
            viewHolder.statusDetail.setText("Brightness Val: " + brightnessVal);
            if (schedule != null)
            {
                schedule.setVisibility(View.VISIBLE);
            }
            switch (snOutput.getOutputType())
            {
                case Relay:
                        viewHolder.OnOffLight.setChecked(logicalVal != 0);
                    break;
                case Analog:
                    viewHolder.brightness.setMax(100);
                    viewHolder.brightness
                            .setProgress((int)logicalVal);
                    viewHolder.brightnessVal
                            .setText(logicalVal + "");
                    break;
            }
//            switch (snOutput.getPort())
//            {
//                case RELAY_ONE:
//
//
//
//                    /*
//                    //viewHolder.spinnerSchedule.setSelection(port_info.relay1.schedule_mode);
//                    viewHolder.statusDetail.setText(port_info.relay1.status);
//                    viewHolder.vacationFromTo.setText(port_info.relay1.vacation_text);
//                    if (!port_info.relay1.isOccupied) {
//                        viewHolder.imageOccupied.setVisibility(View.GONE);
//                    }
//                    if(port_info.relay1.schedule_mode == 2){
//                        viewHolder.imgNameSchedule.setVisibility(View.VISIBLE);
//                    }*/
//                    break;
//                case RELAY_TWO:
//                    if (LOutput.isOn(snOutput))
//                    {
//                        viewHolder.OnOffLight.setChecked(true);
//                    }
//                    else
//                    {
//                        viewHolder.OnOffLight.setChecked(false);
//                    }
//
//                   /*
//                    //viewHolder.spinnerSchedule.setSelection(port_info.relay2.schedule_mode);
//                    viewHolder.statusDetail.setText(port_info.relay2.status);
//                    viewHolder.vacationFromTo.setText(port_info.relay2.vacation_text);
//                    if (!port_info.relay2.isOccupied) {
//                        viewHolder.imageOccupied.setVisibility(View.GONE);
//                    }
//                    if(port_info.relay2.schedule_mode == 2){
//                        viewHolder.imgNameSchedule.setVisibility(View.VISIBLE);
//                    }*/
//                    break;
//                case ANALOG_OUT_ONE:
//
//                    /*viewHolder.statusDetail.setText(port_info.analog1_out.status);
//                    viewHolder.vacationFromTo.setText(port_info.analog1_out.vacation_text);
//                    viewHolder.spinnerSchedule.setSelection(port_info.analog1_out.schedule_mode);
//                    if (!port_info.analog1_out.isOccupied) {
//                        viewHolder.imageOccupied.setVisibility(View.GONE);
//                    }
//                    if(port_info.analog1_out.schedule_mode == 2){
//                        viewHolder.imgNameSchedule.setVisibility(View.VISIBLE);
//                    }*/
//                    break;
//                case ANALOG_OUT_TWO:
//                    viewHolder.brightness.setMax(100);
//                    viewHolder.brightness
//                            .setProgress(L.resolveZoneProfileLogicalValue(profile, snOutput));
//                    viewHolder.brightnessVal
//                            .setText(L.resolveZoneProfileLogicalValue(profile, snOutput) + "");
//                    /*viewHolder.vacationFromTo.setText(port_info.analog2_out.vacation_text);
//                    viewHolder.statusDetail.setText(port_info.analog2_out.status);
//                    viewHolder.spinnerSchedule.setSelection(port_info.analog2_out.schedule_mode);
//                    if (!port_info.analog2_out.isOccupied) {
//                        viewHolder.imageOccupied.setVisibility(View.GONE);
//                    }
//                    if(port_info.analog2_out.schedule_mode == 2){
//                        viewHolder.imgNameSchedule.setVisibility(View.VISIBLE);
//                    }*/
//                    break;
//            }
            if (snOutput.getPort() == ANALOG_OUT_ONE || snOutput.getPort() == ANALOG_OUT_TWO)
            {
                viewHolder.OnOffLight.setVisibility(View.GONE);
                viewHolder.brightness.setVisibility(View.VISIBLE);
                viewHolder.brightnessVal.setVisibility(View.VISIBLE);
            }
            if (snOutput.getPort() == RELAY_ONE || snOutput.getPort() == RELAY_TWO)
            {
                viewHolder.OnOffLight.setVisibility(View.VISIBLE);
                viewHolder.brightness.setVisibility(View.GONE);
                viewHolder.brightnessVal.setVisibility(View.GONE);
            }
            final SwitchCompat onOff = (SwitchCompat) row.findViewById(R.id.OnOffLight);
            onOff.setTag(position);
            onOff.setSwitchMinWidth(40);
            /*StateListDrawable switchStates = new StateListDrawable();
            switchStates.addState(new int[]{android.R.attr.state_checked}, new ColorDrawable(c.getResources().getColor(R.color.progress_color_orange)));
            switchStates.addState(new int[]{-android.R.attr.state_enabled}, new ColorDrawable(c.getResources().getColor(R.color.grey_select)));
            switchStates.addState(new int[]{}, new ColorDrawable(c.getResources().getColor(R.color.grey_select))); // this one has to come last
            onOff.setThumbDrawable(switchStates);*/
            onOff.setOnCheckedChangeListener(onCheckedChangeListener);
            SeekBar brightnessControl = (SeekBar) row.findViewById(R.id.brightness);
        
            final TextView val = (TextView) row.findViewById(R.id.brightnessVal);
            brightnessControl.setTag(position);
            val.setTag(position);
            brightnessControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                {
                    val.setText(progress + "");
                    
                }
                
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar)
                {
                }
                
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar)
                {
                    int value = seekBar.getProgress();
                    snOutput.setOverride(MockTime.getInstance()
                                                 .getMockTime(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) value);
                }
            });
            row.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    View vw = v;
                    RelativeLayout row2 = null;
                    row2 = (RelativeLayout) vw.findViewById(R.id.schedulerow);
                    if (schedule == null)
                    {
                        row2.setVisibility(View.VISIBLE);
                        schedule = row2;
                    }
                    else
                    {
                        if (row2 != schedule)
                        {
                            schedule.setVisibility(View.GONE);
                            row2.setVisibility(View.VISIBLE);
                            schedule = row2;
                        }
                        else
                        {
                            row2.setVisibility(View.GONE);
                            schedule = null;
                        }
                    }
                    new LayoutHelper(c).setListViewParams(thiSList, LightingDetailAdapter.this,
                            schedule == null ? 0 : 1, snOutPortList.size(), lcmdab);
                }
            });
            //changing schedule
            final ImageView vacationEdit = (ImageView) row.findViewById(R.id.vacationEdit);
            vacationEdit.setTag(position);
            vacationEdit.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                   /* FragmentTransaction ftt = c.getFragmentManager().beginTransaction();
                    Fragment prev1 = c.getFragmentManager().findFragmentByTag("vacation");
                    if (prev1 != null) {
                        ftt.remove(prev1);
                    }
                    LCMVacationScheduleEditor vacationFragment = null;
                    vacationFragment = new LCMVacationScheduleEditor(LightingDetailAdapter.this, port_map.getFsvData(), port);
                    vacationFragment.show(ftt, "vacation");*/
                }
            });
            //named schedule edit
            final ImageView imgNameSchedule =
                    (ImageView) row.findViewById(R.id.lcmNamedScheduleEdit);
            imgNameSchedule.setTag(position);
            imgNameSchedule.setOnClickListener(editScheduleOnClickListener);
        
            viewHolder.spinnerSchedule
                    .setOnItemSelectedListener(onScheduleModeItemSelectedListener);
        
        return row;
    }
    
    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton
                                                                               .OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            int position = (int) buttonView.getTag();
            Circuit circuit = snOutPortList.get(position);
            circuit.setOverride(System.currentTimeMillis(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) (isChecked
                                                                                                                                     ? 100 : 0));
        }
    };
    
    View.OnClickListener editScheduleOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int position = (int) v.getTag();
            
            Circuit snOutput = snOutPortList.get(position);
            showDialogFragment(LightScheduleFragment
                                       .newPortInstance(floor, zone, profile, snOutput), LightScheduleFragment.ID);
        }
    };
    
    AdapterView.OnItemSelectedListener onScheduleModeItemSelectedListener = new AdapterView
                                                                          .OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
        long id)
        {
            int position = (int) parent.getTag();
            
           /* Circuit snOutput = snOutPortList.get(position);
            //Zone schedule selected, remove circuit or named schedule from
            // circuit, set to zone schedule
            if (pos == 0 && snOutput.getScheduleMode() != ZoneSchedule) //lcm zone schedule
            {
                snOutput.setScheduleMode(ZoneSchedule);
                snOutput.setSchedules(new ArrayList<Schedule>());
                snOutput.setNamedSchedule("");
                //L.saveCCUState();
            }
            else if (pos == 1 && snOutput.getScheduleMode() != CircuitSchedule) // circuit schedule
            {
                snOutput.setScheduleMode(CircuitSchedule);
                snOutput.setSchedules(new ArrayList<Schedule>());
                snOutput.setNamedSchedule("");
                showDialogFragment(LightScheduleFragment.newPortInstance(floor, zone, profile, snOutput), LightScheduleFragment.ID);
            }
            else if(pos == 2 && snOutput.getScheduleMode() != NamedSchedule)
            //named schedule
            {
                showLCMNamedScheduleSelector(snOutput);
            }*/
        }
        
        
        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    };
    
    private void showDialogFragment(DialogFragment dialogFragment, String id)
    {
        FragmentTransaction ft = c.getSupportFragmentManager().beginTransaction();
        Fragment prev = c.getSupportFragmentManager().findFragmentByTag(id);
        if (prev != null)
        {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        // Create and show the dialog.
        dialogFragment.show(ft, id);
    }
    
    
    private void showLCMNamedScheduleSelector(final Circuit circuit)
    {
        final ArrayList<String> strings =
                new ArrayList<String>(ccu().getLCMNamedSchedules().keySet());
        CharSequence[] charSequences = new CharSequence[strings.size()];
        for (int i = 0; i < strings.size(); i++)
        {
            charSequences[i] = strings.get(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Named Schedule")
               .setItems(charSequences, new DialogInterface.OnClickListener()
               {
                   public void onClick(DialogInterface dialog, int which)
                   {
                       circuit.setNamedSchedule(strings.get(which));
                       //L.saveCCUState();
                   }
               });
        builder.create().show();
    }
   /* @Override
    public void OnSetLCMVacationSchedule(SortedMap<Long, JSONObject> vacationData, FSVData.SMARTNODE_PORT port, FSVData fsvData) {
        SmartNodesPortData portData = fsvData.getSmartNodePort();
        switch (port) {
            case RELAY_1:
                portData.relay1.vacation_data = vacationData;
                portData.updateVacationStartAndEndDate(portData.relay1, true);
                break;
            case RELAY_2:
                portData.relay2.vacation_data = vacationData;
                portData.updateVacationStartAndEndDate(portData.relay2, true);
                break;
            case ANALOG_1_OUT:
                portData.analog1_out.vacation_data = vacationData;
                portData.updateVacationStartAndEndDate(portData.analog1_out, true);
                break;
            case ANALOG_2_OUT:
                portData.analog2_out.vacation_data = vacationData;
                portData.updateVacationStartAndEndDate(portData.analog2_out, true);
                break;
        }
        if(fsvData.updateLCMDataFromAssignedSchedule())
            fsvData.getAssignedRoom().sendSettingsToWeb("lcm schedule change");
        notifyDataSetChanged();
    }


    private void showNameScheduleSelector(final FSVData fsvData, final FSVData.SMARTNODE_PORT port) {
        final ArrayList<String> nameSchList = NamedSchedule.getHandle().getNamedScheduleList();
        if (!nameSchList.isEmpty()) {
            try {
                AlertDialog dlg;
                final ArrayAdapter<String> nameSchAdapter = new ArrayAdapter<String>(c, android.R.layout.simple_list_item_1, nameSchList);
                AlertDialog.Builder builder = new AlertDialog.Builder(c, R.style.NewDialogStyle);
                builder.setTitle("Select Named Schedule");
                builder.setCancelable(false);
                builder.setAdapter(nameSchAdapter, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nameSchId = NamedSchedule.getHandle().getNameScheduleId(nameSchList.get(which));
                        switch (port) {
                            case RELAY_1:
                                fsvData.getSmartNodePort().updatePortScheduleMode(fsvData.getSmartNodePort().relay1, SmartNodesPortData.LCM_CIRCUIT_SCHEDULE_MODE.NAMED.ordinal(),nameSchId);
                                fsvData.getSmartNodePort().relay1.named_schedule_id = nameSchId;
                                break;
                            case RELAY_2:
                                fsvData.getSmartNodePort().updatePortScheduleMode(fsvData.getSmartNodePort().relay2, SmartNodesPortData.LCM_CIRCUIT_SCHEDULE_MODE.NAMED.ordinal(),nameSchId);
                                fsvData.getSmartNodePort().relay2.named_schedule_id = nameSchId;
                                break;
                            case ANALOG_1_OUT:
                                fsvData.getSmartNodePort().updatePortScheduleMode(fsvData.getSmartNodePort().analog1_out, SmartNodesPortData.LCM_CIRCUIT_SCHEDULE_MODE.NAMED.ordinal(),nameSchId);
                                fsvData.getSmartNodePort().analog1_out.named_schedule_id = nameSchId;
                                break;
                            case ANALOG_2_OUT:
                                fsvData.getSmartNodePort().updatePortScheduleMode(fsvData.getSmartNodePort().analog2_out, SmartNodesPortData.LCM_CIRCUIT_SCHEDULE_MODE.NAMED.ordinal(),nameSchId);
                                fsvData.getSmartNodePort().analog2_out.named_schedule_id = nameSchId;
                                break;
                        }

                        fsvData.updateLCMDataFromAssignedSchedule();
                            fsvData.getAssignedRoom().sendSettingsToWeb("lcm schedule change");
                        FloorData.saveFloorData();
                        notifyDataSetChanged();


                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (port) {
                            case RELAY_1:
                                if (fsvData.getSmartNodePort().relay1.named_schedule_id.isEmpty() && (nameSchList.size() == 0))
                                    fsvData.getSmartNodePort().relay1.status = "No Named schedule associated, Click edit to assign";
                                break;
                            case RELAY_2:
                                if (fsvData.getSmartNodePort().relay2.named_schedule_id.isEmpty() && (nameSchList.size() == 0))
                                    fsvData.getSmartNodePort().relay2.status = "No Named schedule associated, Click edit to assign";
                                break;
                            case ANALOG_1_OUT:
                                if (fsvData.getSmartNodePort().analog1_out.named_schedule_id.isEmpty() && (nameSchList.size() == 0))
                                    fsvData.getSmartNodePort().analog1_out.status = "No Named schedule associated, Click edit to assign";
                                break;
                            case ANALOG_2_OUT:
                                if (fsvData.getSmartNodePort().analog2_out.named_schedule_id.isEmpty() && (nameSchList.size() == 0))
                                    fsvData.getSmartNodePort().analog2_out.status = "No Named schedule associated, Click edit to assign";
                                break;
                        }
                        //notifyDataSetChanged();
                        dialog.dismiss();

                    }
                });
                dlg = builder.create();
                dlg.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            Toast.makeText(c, "No Named Schedule list to select ", Toast.LENGTH_SHORT).show();

    }*/
    
    public class ViewHolder
    {
        public TextView       LogicalName;
        public SeekBar        brightness;
        public SwitchCompat   OnOffLight;
        public TextView       statusDetail;
        public TextView       brightnessVal;
        public Spinner        spinnerSchedule;
        public EditText       vacationFromTo;
        public ImageView      vacationEdit;
        public ImageView      imageOccupied;
        public RelativeLayout schedulerow;
        public LinearLayout   llmain;
        public ImageView      imgNameSchedule;
        
        
        public ViewHolder(View v)
        {
            LogicalName = (TextView) v.findViewById(R.id.LogicalName);
            brightness = (SeekBar) v.findViewById(R.id.brightness);
            OnOffLight = (SwitchCompat) v.findViewById(R.id.OnOffLight);
            statusDetail = (TextView) v.findViewById(R.id.statusDetail);
            brightnessVal = (TextView) v.findViewById(R.id.brightnessVal);
            spinnerSchedule = (Spinner) v.findViewById(R.id.spinnerSchedule);
            vacationEdit = (ImageView) v.findViewById(R.id.vacationEdit);
            imageOccupied = (ImageView) v.findViewById(R.id.imageOccupied);
            vacationFromTo = (EditText) v.findViewById(R.id.vacationFromTo);
            schedulerow = (RelativeLayout) v.findViewById(R.id.schedulerow);
            llmain = (LinearLayout) v.findViewById(R.id.llmain);
            imgNameSchedule = (ImageView) v.findViewById(R.id.lcmNamedScheduleEdit);
        }
    }
}
