package a75f.io.renatus;

import android.content.DialogInterface;
import android.content.res.XmlResourceParser;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import a75f.io.bo.building.Circuit;
import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.ScheduleMode;
import a75f.io.logic.L;
import a75f.io.renatus.VIEWS.SeekArcWidget;
import a75f.io.renatus.VIEWS.ZoneImageWidget;

import static a75f.io.bo.building.definitions.ScheduleMode.CircuitSchedule;
import static a75f.io.bo.building.definitions.ScheduleMode.NamedSchedule;
import static a75f.io.bo.building.definitions.ScheduleMode.ZoneSchedule;
import static a75f.io.logic.L.ccu;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class ZonesFragment extends Fragment
{
    public ListView mLightsDetailsView = null;
    LinearLayout drawer_screen;
    int                        mCurFloorIndex            = 0;
    boolean                    mDesiredTempScrolling     = false;
    boolean                    mDetailsView              = false;
    ZoneImageWidget            selectedDevice            = null;
    int                        nRoomDetailsLocationIndex = -1;
    SeekArcWidget              rbSelectedRoom            = null;
    int                        nSelectedRoomIndex        = -1;
    ArrayList<SeekArcWidget>   seekArcWidgetList         = null;
    ArrayList<ZoneImageWidget> zoneWidgetList            = null;
    private ListView       lvFloorList;
    private DrawerLayout   mDrawerLayout;
    private ScrollView     scrollView;
    private ImageView      mOpenDrawer;
    private TextView       place;
    private TextView       temperature;
    private TextView       weather_condition;
    private ImageView      weather_icon;
    private TextView       maxmintemp;
    private TextView       note;
    private LayoutInflater inflater;
    private LinearLayout   roomButtonGrid;
    private SwitchCompat   show_weather = null;
    private RelativeLayout weather_data = null;
    private AttributeSet attributeSet;
    private int          room_width;
    private int          room_height;
    private int          eSelectedMode;
    private LinearLayout roomRow;
    private LinearLayout mLightingRow   = null;
    private View         mLcmHeaderView = null;
    private DataArrayAdapter<Floor> floorDataAdapter;
    private SeekArcWidget.OnSeekArcChangeListener seekArcChangeListener =
            new SeekArcWidget.OnSeekArcChangeListener()
            {
                @Override
                public void onProgressChanged(SeekArcWidget seekArc, int progress, boolean fromUser)
                {
                }
                
                
                @Override
                public void onStartTrackingTouch(SeekArcWidget seekArc)
                {
                    mDesiredTempScrolling = true;
                }
                
                
                @Override
                public void onStopTrackingTouch(final SeekArcWidget seekArc)
                {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        public void run()
                        {
                            if (!seekArc.getIsSensorPaired())
                            {
                                //seekArc.getRoomData().updateRawDesiredTemp((int) CCUUtils.roundTo2Decimal(seekArc.getDesireTemp() * 2), UPDATESRC.CCU);
                                //updateRoomDetailsWidget(seekArc.getRoomData());
                            }
                            mDesiredTempScrolling = false;
                        }
                    }, 200);
                }
            };
    private ZoneImageWidget.OnClickListener       zoneWidgetListener    =
            new ZoneImageWidget.OnClickListener()
            {
                
                @Override
                public void onClick(ZoneImageWidget w)
                {
                    Log.e("ZONE", "onClick ZoneImageWidget");
                    int index = w.getIndex();
                    int mod = 4;
                    int nDetailsLoc = ((index - 1) / mod) + 1;
                    if (mLightsDetailsView.getHeaderViewsCount() > 0)
                    {
                        mLightsDetailsView.removeHeaderView(mLcmHeaderView);
                    }
                    if (!mDetailsView)
                    {
                        w.setSelected(true);
                        if (w.getProfile() instanceof LightProfile)
                        {
                            roomButtonGrid.addView(mLightingRow, nDetailsLoc);
                            UpdateRoomLightingWidget((LightProfile) w.getProfile(), false);
                            //mLightingRow.startAnimation(in);
                        }
                        mDetailsView = true;
                        nSelectedRoomIndex = index;
                        nRoomDetailsLocationIndex = nDetailsLoc;
                        selectedDevice = w;
                        rbSelectedRoom = null;
                        //hmpSelected = null;
                        if (nDetailsLoc < 2)
                        {
                            scrollView.fullScroll(ScrollView.FOCUS_UP);
                        }
                        else
                        {
                            scrollView.scrollTo(0, mLightingRow.getHeight());
                        }
                    }
                    else
                    {
                        if (selectedDevice != null)
                        {
                            selectedDevice.setSelected(false);
                        }
                        if (rbSelectedRoom != null)
                        {
                            rbSelectedRoom.setSelected(false);
                        }
                        roomButtonGrid.removeViewAt(nRoomDetailsLocationIndex);
                        if (nSelectedRoomIndex == index)
                        {
                            mDetailsView = false;
                            nSelectedRoomIndex = index;
                            nRoomDetailsLocationIndex = -1;
                            selectedDevice = null;
                            rbSelectedRoom = null;
                            //hmpSelected = null;
                        }
                        else
                        {
                            w.setSelected(true);
                            if (w.getProfile() instanceof LightProfile)
                            {
                                roomButtonGrid.addView(mLightingRow, nDetailsLoc);
                                UpdateRoomLightingWidget((LightProfile) w.getProfile(), false);
                                //mLightingRow.startAnimation(in);
                            }
                            mDetailsView = true;
                            nSelectedRoomIndex = index;
                            nRoomDetailsLocationIndex = nDetailsLoc;
                            selectedDevice = w;
                            rbSelectedRoom = null;
                            //hmpSelected = null;
                            if (nDetailsLoc < 2)
                            {
                                scrollView.fullScroll(ScrollView.FOCUS_UP);
                            }
                            else
                            {
                                scrollView.scrollTo(0, mLightingRow.getHeight());
                            }
                            Log.e("ZONE", "END onClick ZoneImageWidget");
                        }
                    }
                }
            };
    
    
    public ZonesFragment()
    {
        seekArcWidgetList = new ArrayList<>();
        zoneWidgetList = new ArrayList<>();
    }
    
    
    public static ZonesFragment newInstance()
    {
        return new ZonesFragment();
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View inflate = inflater.inflate(R.layout.fragment_zones, container, false);
        Log.e("ZONE", "ONCREATEVIEW");
        this.inflater = inflater;
        createDetailsWidget(inflater);
        Log.e("ZONE", "FINISHED ONCREATEVIEW");
        return inflate;
    }
    
    
    @Override
    public void onStart()
    {
        super.onStart();
        Log.e("ZONE", "ONSTART");
        floorDataAdapter =
                new DataArrayAdapter<>(getActivity(), R.layout.listviewitem, ccu().getFloors());
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        // display size in pixels
        Point size = new Point();
        display.getSize(size);
        room_width = size.x / 4;
        room_height = (size.y) / 2 - 48;
        mOpenDrawer = (ImageView) getView().findViewById(R.id.openDrawerBtn);
        mOpenDrawer.setClickable(true);
        //mOpenDrawer.setOnClickListener(this);
        mDrawerLayout = (DrawerLayout) getView().findViewById(R.id.drawer_layout);
        mDrawerLayout.setClickable(true);
        //mDrawerLayout.setOnClickListener(this);
        scrollView = (ScrollView) getView().findViewById(R.id.floorscroll);
        weather_data = (RelativeLayout) getView().findViewById(R.id.weather_data);
        place = (TextView) getView().findViewById(R.id.place);
        temperature = (TextView) getView().findViewById(R.id.temperature);
        weather_condition = (TextView) getView().findViewById(R.id.weather_condition);
        weather_icon = (ImageView) getView().findViewById(R.id.weather_icon);
        maxmintemp = (TextView) getView().findViewById(R.id.maxmintemp);
        note = (TextView) getView().findViewById(R.id.note);
        lvFloorList = (ListView) getView().findViewById(R.id.floorList);
        lvFloorList.setAdapter(floorDataAdapter);
        //lvFloorList.setOnItemClickListener(this);
        roomButtonGrid = (LinearLayout) getView().findViewById(R.id.roomButtonGrid);
        drawer_screen = (LinearLayout) getView().findViewById(R.id.drawer_screen);
        /*show_weather = (SwitchCompat) getView().findViewById(R.id.show_weather);
        show_weather.setClickable(false);
		show_weather.setOnCheckedChangeListener(this);*/
        attributeSet = getSeekbarXmlAttributes();
        Log.e("ZONE", "END ONSTART");
    }
    
    
    @Override
    public void onResume()
    {
        // TODO Auto-generated method stub
        super.onResume();
        Log.e("ZONE", "onResume");
        fillZoneData();
        floorDataAdapter.setSelectedItem(mCurFloorIndex);
        Log.e("ZONE", "END onResume");
    }
    
    
    public void fillZoneData()
    {
        ArrayList<Floor> floorList = ccu().getFloors();
        roomButtonGrid.removeAllViews();
        roomButtonGrid.setOrientation(LinearLayout.VERTICAL);
        //arrayRooms.clear();
        for (Floor floor : floorList)
        {
            roomRow = new LinearLayout(getActivity());
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            roomRow.setOrientation(LinearLayout.HORIZONTAL);
            roomRow.setLayoutParams(lp);
            roomButtonGrid.addView(roomRow);
            ArrayList<Zone> zoneList = floor.mRoomList;
            //TODO - refactor
            for (Zone z : zoneList)
            {
                if (z.findProfile(ProfileType.LIGHT) != null)
                {
                    ZoneImageWidget zWidget = new ZoneImageWidget(getActivity()
                                                                          .getApplicationContext(), z.roomName, z.findProfile(ProfileType.LIGHT));
                    zWidget.setLayoutParams(new LinearLayout.LayoutParams(room_width, room_height));
                    zWidget.setOnClickChangeListener(zoneWidgetListener);
                    roomRow.addView(zWidget);
                }
            }
        }
    }
    
    
    private AttributeSet getSeekbarXmlAttributes()
    {
        AttributeSet as = null;
        XmlResourceParser parser = getResources().getLayout(R.layout.widget_seekarc);
        int state = 0;
        do
        {
            try
            {
                state = parser.next();
            }
            catch (XmlPullParserException e1)
            {
                e1.printStackTrace();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
            if (state == XmlPullParser.START_TAG)
            {
                if (parser.getName().equals("a75f.io.renatus.VIEWS.SeekArcWidget"))
                {
                    as = Xml.asAttributeSet(parser);
                    break;
                }
            }
        }
        while (state != XmlPullParser.END_DOCUMENT);
        return as;
    }
    
    
    private void createDetailsWidget(LayoutInflater inflater)
    {
        mLightingRow = (LinearLayout) inflater.inflate(R.layout.zone_detail_list, null);
        mLightsDetailsView = (ListView) mLightingRow.findViewById(R.id.lighting_detail_list);
    }
    
    
    public void UpdateRoomLightingWidget(final LightProfile roomData, Boolean lcmdabfsv)
    {
        mLightsDetailsView.setAdapter(null);
        if (mLcmHeaderView != null)
        {
            mLightsDetailsView.removeHeaderView(mLcmHeaderView);
        }
        if (mLightsDetailsView.getHeaderViewsCount() == 0)
        {
            View header =
                    (View) getActivity().getLayoutInflater().inflate(R.layout.lcm_header_row, null);
            ImageView signal = (ImageView) header.findViewById(R.id.imageSignal);
            ImageView occupied = (ImageView) header.findViewById(R.id.imageOccupied);
            ImageView lcmHeaderNamedSchEdit =
                    (ImageView) header.findViewById(R.id.lcmHeaderNamedSchEdit);

                lcmHeaderNamedSchEdit.setVisibility(View.VISIBLE);
                lcmHeaderNamedSchEdit.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Toast.makeText(ZonesFragment.this.getContext(),
                                "onclick " + "lcmheadernamedSchEdit", Toast.LENGTH_LONG).show();
                        showLCMLightScheduleFragment(getFloorForLightProfile(roomData),
                                getZoneForLightProfile(roomData), roomData);
                    }
                });
            
            Spinner spinnerSchedule = (Spinner) header.findViewById(R.id.spinnerSchedule);
            ArrayAdapter<CharSequence> aaOccupancyMode = ArrayAdapter
                                                                 .createFromResource(getActivity()
                                                                                             .getApplicationContext(), R.array.scheduleLCM, R.layout.spinner_item);
            aaOccupancyMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerSchedule.setAdapter(aaOccupancyMode);
            spinnerSchedule.setOnItemSelectedListener(null);
            spinnerSchedule.setSelection(roomData.getScheduleMode() == ScheduleMode.ZoneSchedule
                                                 ? 0 : 1);
            spinnerSchedule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    if (position == 1 && roomData.getScheduleMode() != NamedSchedule)
                    {
                        showLCMNamedScheduleSelector(roomData);
                    }
                    else if (position == 0 && roomData.getScheduleMode() != ZoneSchedule)
                    {
                        roomData.setScheduleMode(ScheduleMode.ZoneSchedule);
                    }
                }
                
                
                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                }
            });
            mLightsDetailsView.addHeaderView(header);
            mLcmHeaderView = header;
        }
        boolean expand = roomData == null ? false : true;
        LightingDetailAdapter adapter =
                new LightingDetailAdapter(getActivity(), mLightsDetailsView, roomData, getFloorForLightProfile(roomData), getZoneForLightProfile(roomData), expand);
        mLightsDetailsView.setAdapter(adapter);
        new LayoutHelper(getActivity()).setListViewParams(mLightsDetailsView, null, 0, 0, expand);
    }
    
    
    private void showLCMNamedScheduleSelector(final ZoneProfile zoneProfile)
    {
        final ArrayList<String> strings =
                new ArrayList<String>(ccu().getLCMNamedSchedules().keySet());
        CharSequence[] charSequences = new CharSequence[strings.size()];
        for (int i = 0; i < strings.size(); i++)
        {
            charSequences[i] = strings.get(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Named Schedule")
               .setItems(charSequences, new DialogInterface.OnClickListener()
               {
                   public void onClick(DialogInterface dialog, int which)
                   {
                       zoneProfile.setNamedSchedule(strings.get(which));
                       L.saveCCUState();
                   }
               });
        builder.create().show();
    }
    
    
    private void showLCMLightScheduleFragment(Floor floor, Zone zone, ZoneProfile zoneProfile)
    {
        showDialogFragment(LightScheduleFragment
                                   .newZoneProfileInstance(floor, zone, zoneProfile),
                LightScheduleFragment.ID);
    }
    
    
    private Floor getFloorForLightProfile(ZoneProfile zoneProfile)
    {
        for (Floor f : ccu().getFloors())
        {
            for (Zone z : f.mRoomList)
            {
                if (z.findProfile(ProfileType.LIGHT).equals(zoneProfile))
                {
                    return f;
                }
            }
        }
        return null;
    }
    
    
    private Zone getZoneForLightProfile(ZoneProfile zoneProfile)
    {
        for (Floor f : ccu().getFloors())
        {
            for (Zone z : f.mRoomList)
            {
                if (z.findProfile(ProfileType.LIGHT).equals(zoneProfile))
                {
                    return z;
                }
            }
        }
        return null;
    }
    
    
    protected void showDialogFragment(DialogFragment dialogFragment, String id)
    {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(id);
        if (prev != null)
        {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        // Create and show the dialog.
        dialogFragment.show(ft, id);
    }
}
