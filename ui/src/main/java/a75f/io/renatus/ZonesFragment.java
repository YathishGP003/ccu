package a75f.io.renatus;

import android.content.DialogInterface;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
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

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.OverrideType;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.ScheduleMode;
import a75f.io.logic.L;
import a75f.io.renatus.VIEWS.SeekArcWidget;
import a75f.io.renatus.VIEWS.ZoneImageWidget;

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
    SeekArcWidget                    rbSelectedRoom            = null;
    int                        nSelectedRoomIndex        = -1;
    ArrayList<SeekArcWidget>         seekArcWidgetList         = null;
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
    private SeekArcWidget.OnSeekArcChangeListener seekArcChangeListener = new SeekArcWidget.OnSeekArcChangeListener()
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
    
    //MARK
    //    private void initStandaloneWidgets(RoomData roomData, View view){
    //        if(updateRelayStatus(roomData)) {
    //            if (totalSaUnitsInZone == 1 || totalSaUnitsInZone == 2 || totalSaUnitsInZone == 3) {
    //                standaloneGrid1 = view.findViewById(R.id.standalonelayout1);
    //                saConditioningMode1 = (Spinner) standaloneGrid1.findViewById(R.id.spinnerConditioningMode);
    //                saHeader1 = (TextView) standaloneGrid1.findViewById(R.id.headerTitle);
    //                saStatus1 = (TextView) standaloneGrid1.findViewById(R.id.saStatusNow);
    //
    //                ArrayAdapter<String> saConMode = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, conditioningModeList) {
    //                    @Override
    //                    public boolean isEnabled(int position) {
    //                        if ((isNoHeating1 || isNoCooling1) && position == 1)
    //                            return false;
    //                        else if (isNoHeating1 && position == 2)
    //                            return false;
    //                        else if (isNoCooling1 && position == 3)
    //                            return false;
    //                        else
    //                            return true;
    //                    }
    //
    //                    @Override
    //                    public View getDropDownView(int position, View convertView,
    //                                                ViewGroup parent) {
    //                        View view = super.getDropDownView(position, convertView, parent);
    //                        TextView tv = (TextView) view;
    //                        if ((isNoHeating1 || isNoCooling1) && position == 1)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoHeating1 && position == 2)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoCooling1 && position == 3)
    //                            tv.setTextColor(Color.GRAY);
    //                        else
    //                            tv.setTextColor(Color.BLACK);
    //                        return view;
    //                    }
    //                };
    //                saConMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
    //                saConditioningMode1.setAdapter(saConMode);
    //                saConditioningMode1.setOnItemSelectedListener(conditioningMode1);
    //                saFanMode1 = (Spinner) standaloneGrid1.findViewById(R.id.spinnerFanMode);
    //                ArrayAdapter<String> saFanConditioningMode = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, fanModeList) {
    //                    @Override
    //                    public boolean isEnabled(int position) {
    //                        if ((isNoFanLow && isNoFanHigh) && position == 1)
    //                            return false;
    //                        else if (isNoFanLow && position == 2)
    //                            return false;
    //                        else if (isNoFanHigh && position == 3)
    //                            return false;
    //                        else
    //                            return true;
    //                    }
    //
    //                    @Override
    //                    public View getDropDownView(int position, View convertView,
    //                                                ViewGroup parent) {
    //                        View view = super.getDropDownView(position, convertView, parent);
    //                        TextView tv = (TextView) view;
    //                        if ((isNoFanLow && isNoFanHigh) && position == 1)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoFanLow && position == 2)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoFanHigh && position == 3)
    //                            tv.setTextColor(Color.GRAY);
    //                        else
    //                            tv.setTextColor(Color.BLACK);
    //                        return view;
    //                    }
    //                };
    //                saFanConditioningMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
    //                saFanMode1.setAdapter(saFanConditioningMode);
    //                saFanMode1.setOnItemSelectedListener(fanmode1);
    //            }
    //            if (totalSaUnitsInZone == 2 || totalSaUnitsInZone == 3) {
    //                standaloneGrid2 = view.findViewById(R.id.standalonelayout2);
    //                saConditioningMode2 = (Spinner) standaloneGrid2.findViewById(R.id.spinnerConditioningMode);
    //
    //                saHeader2 = (TextView) standaloneGrid2.findViewById(R.id.headerTitle);
    //                saStatus2 = (TextView) standaloneGrid2.findViewById(R.id.saStatusNow);
    //                ArrayAdapter<String> saConMode2 = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, conditioningModeList) {
    //                    @Override
    //                    public boolean isEnabled(int position) {
    //                        if ((isNoHeating2 || isNoCooling2) && position == 1)
    //                            return false;
    //                        else if (isNoHeating2 && position == 2)
    //                            return false;
    //                        else if (isNoCooling2 && position == 3)
    //                            return false;
    //                        else
    //                            return true;
    //                    }
    //
    //                    @Override
    //                    public View getDropDownView(int position, View convertView,
    //                                                ViewGroup parent) {
    //                        View view = super.getDropDownView(position, convertView, parent);
    //                        TextView tv = (TextView) view;
    //                        if ((isNoHeating2 || isNoCooling2) && position == 1)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoHeating2 && position == 2)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoCooling2 && position == 3)
    //                            tv.setTextColor(Color.GRAY);
    //                        else
    //                            tv.setTextColor(Color.BLACK);
    //                        return view;
    //                    }
    //                };
    //                saConMode2.setDropDownViewResource(R.layout.spinner_dropdown_item);
    //                saConditioningMode2.setAdapter(saConMode2);
    //                saConditioningMode2.setOnItemSelectedListener(conditioningMode2);
    //                saFanMode2 = (Spinner) standaloneGrid2.findViewById(R.id.spinnerFanMode);
    //                ArrayAdapter<String> saFanConditioningMode2 = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, fanModeList) {
    //                    @Override
    //                    public boolean isEnabled(int position) {
    //                        if ((isNoFanLow2 && isNoFanHigh2) && position == 1)
    //                            return false;
    //                        else if (isNoFanLow2 && position == 2)
    //                            return false;
    //                        else if (isNoFanHigh2 && position == 3)
    //                            return false;
    //                        else
    //                            return true;
    //                    }
    //
    //                    @Override
    //                    public View getDropDownView(int position, View convertView,
    //                                                ViewGroup parent) {
    //                        View view = super.getDropDownView(position, convertView, parent);
    //                        TextView tv = (TextView) view;
    //                        if ((isNoFanLow2 && isNoFanHigh2) && position == 1)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoFanLow2 && position == 2)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoFanHigh2 && position == 3)
    //                            tv.setTextColor(Color.GRAY);
    //                        else
    //                            tv.setTextColor(Color.BLACK);
    //                        return view;
    //                    }
    //                };
    //                saFanConditioningMode2.setDropDownViewResource(R.layout.spinner_dropdown_item);
    //                saFanMode2.setAdapter(saFanConditioningMode2);
    //                saFanMode2.setOnItemSelectedListener(fanmode2);
    //            }
    //            if (totalSaUnitsInZone == 3) {
    //                standaloneGrid3 = view.findViewById(R.id.standalonelayout3);
    //                saConditioningMode3 = (Spinner) standaloneGrid2.findViewById(R.id.spinnerConditioningMode);
    //
    //                saHeader3 = (TextView) standaloneGrid3.findViewById(R.id.headerTitle);
    //                saStatus3 = (TextView) standaloneGrid3.findViewById(R.id.saStatusNow);
    //                ArrayAdapter<String> saConMode3 = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, conditioningModeList) {
    //                    @Override
    //                    public boolean isEnabled(int position) {
    //                        if ((isNoHeating3 || isNoCooling3) && position == 1)
    //                            return false;
    //                        else if (isNoHeating3 && position == 2)
    //                            return false;
    //                        else if (isNoCooling3 && position == 3)
    //                            return false;
    //                        else
    //                            return true;
    //                    }
    //
    //                    @Override
    //                    public View getDropDownView(int position, View convertView,
    //                                                ViewGroup parent) {
    //                        View view = super.getDropDownView(position, convertView, parent);
    //                        TextView tv = (TextView) view;
    //                        if ((isNoHeating3 || isNoCooling3) && position == 1)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoHeating3 && position == 2)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoCooling3 && position == 3)
    //                            tv.setTextColor(Color.GRAY);
    //                        else
    //                            tv.setTextColor(Color.BLACK);
    //                        return view;
    //                    }
    //                };
    //                saConMode3.setDropDownViewResource(R.layout.spinner_dropdown_item);
    //                saConditioningMode3.setAdapter(saConMode3);
    //                saConditioningMode3.setOnItemSelectedListener(conditioningMode3);
    //                saFanMode3 = (Spinner) standaloneGrid3.findViewById(R.id.spinnerFanMode);
    //                ArrayAdapter<String> saFanConditioningMode3 = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, fanModeList) {
    //                    @Override
    //                    public boolean isEnabled(int position) {
    //                        if ((isNoFanLow3 && isNoFanHigh3) && position == 1)
    //                            return false;
    //                        else if (isNoFanLow3 && position == 2)
    //                            return false;
    //                        else if (isNoFanHigh3 && position == 3)
    //                            return false;
    //                        else
    //                            return true;
    //                    }
    //
    //                    @Override
    //                    public View getDropDownView(int position, View convertView,
    //                                                ViewGroup parent) {
    //                        View view = super.getDropDownView(position, convertView, parent);
    //                        TextView tv = (TextView) view;
    //                        if ((isNoFanLow3 && isNoFanHigh3) && position == 1)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoFanLow3 && position == 2)
    //                            tv.setTextColor(Color.GRAY);
    //                        else if (isNoFanHigh3 && position == 3)
    //                            tv.setTextColor(Color.GRAY);
    //                        else
    //                            tv.setTextColor(Color.BLACK);
    //                        return view;
    //                    }
    //                };
    //                saFanConditioningMode3.setDropDownViewResource(R.layout.spinner_dropdown_item);
    //                saFanMode3.setAdapter(saFanConditioningMode3);
    //                saFanMode3.setOnItemSelectedListener(fanmode3);
    //            }
    //        }
    //
    //    }
    //
    private ZoneImageWidget.OnClickListener zoneWidgetListener = new ZoneImageWidget.OnClickListener()
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
    boolean showCCUDial     = false;
    boolean bDetailsShowing = false;
    private RelativeLayout mRoomDetailsWidget = null;
    private int            mod                = 3;
    
    private SeekArcWidget.OnClickListener seekArcClickListener = new SeekArcWidget.OnClickListener()
    {
        @Override
        public void onClick(SeekArcWidget r)
        {
            
            Zone zone = r.getZone();
            SingleStageProfile singleStageProfile = (SingleStageProfile) zone.findProfile(ProfileType.SSE);
            singleStageProfile.setOverride(1000 * 4 * 60, OverrideType.RELEASE_TIME, (short) 70);
            
            
            
            //int index = r.getIndex();
//            if (r.getRoomData().mZoneProfiles.size() <= 0)
//            {
//                if (bDetailsShowing)
//                {
//                    if (rbSelectedRoom != null)
//                    {
//                        rbSelectedRoom.SetSelected(false);
//                    }
//                    //?
//                    //                    if (selectedDevice != null)
//                    //                    {
//                    //                        selectedDevice.SetSelected(false);
//                    //                    }
//                    roomButtonGrid.removeViewAt(nRoomDetailsLocationIndex);
//                    //showWeather();
//                    bDetailsShowing = false;
//                    nSelectedRoomIndex = index;
//                    nRoomDetailsLocationIndex = -1;
//                    rbSelectedRoom = null;
//                    //totalSaUnitsInZone = 0;
//                }
//                else
//                {
//                    if (rbSelectedRoom != null)
//                    {
//                        rbSelectedRoom.SetSelected(false);
//                    }
//                    //?
//                    //                    if (selectedDevice != null)
//                    //                    {
//                    //                        selectedDevice.SetSelected(false);
//                    //                    }
//                    if (roomButtonGrid.getChildCount() > 0)
//                    {
//                        roomButtonGrid.clearAnimation();
//                    }
//                    //  showWeather();
//                    bDetailsShowing = false;
//                    nSelectedRoomIndex = index;
//                    nRoomDetailsLocationIndex = -1;
//                    rbSelectedRoom = null;
//                    ///  totalSaUnitsInZone = 0;
//                }
//                return;
//            }
//            int nDetailsLoc = ((index - 1) / mod) + 1;
//            if (!bDetailsShowing)
//            {
//                r.SetSelected(true);
//                //                if (r.mDeviceType == SeekArc.DEVICE_TYPE.LCM_DAB)
//                //                {
//                //                    hideWeather();
//                //                    roomButtonGrid.addView(mLightingRow, nDetailsLoc);
//                //                    UpdateRoomLightingWidget(r.getlcmdabfsv(), r.getRoomData(), true);
//                //                    mLightingRow.startAnimation(in);
//                //                }
//                //                else if (r.mDeviceType == SeekArc.DEVICE_TYPE.IFTT_DAB)
//                //                {
//                //                    hideWeather();
//                //                    roomButtonGrid.addView(mIfttRow, nDetailsLoc);
//                //                    UpdateRoomIfttWidget(r.getifttdabfsv(), r.getRoomData(), true);
//                //                    mIfttRow.startAnimation(in);
//                //                }
//                //                else if (r.mDeviceType == SeekArc.DEVICE_TYPE.PURE_DAB)
//                //                {
//                //                    hideWeather();
//                //                    roomButtonGrid.addView(mRoomDetailsWidget, nDetailsLoc);
//                //                    initStandaloneWidgets(r.getRoomData(), mRoomDetailsWidget);
//                //                    updateRoomDetailsWidget(r.getRoomData());
//                //                    mRoomDetailsWidget.startAnimation(in);
//                //                }
//                //initStandaloneWidgets(r.getRoomData(), mRoomDetailsWidget);
//                bDetailsShowing = true;
//                nSelectedRoomIndex = index;
//                nRoomDetailsLocationIndex = nDetailsLoc;
//                rbSelectedRoom = r;
//                selectedDevice = null;
//                //hmpSelected = null;
//                if (nDetailsLoc < 2)
//                {
//                    scrollView.fullScroll(ScrollView.FOCUS_UP);
//                }
//                else
//                {
//                    scrollView.scrollTo(0, mRoomDetailsWidget.getHeight());
//                }
//            }
//            else
//            {
//                if (rbSelectedRoom != null)
//                {
//                    rbSelectedRoom.SetSelected(false);
//                }
//                //                if (selectedDevice != null)
//                //                {
//                //                    selectedDevice.SetSelected(false);
//                //                }
//                //if(hmpSelected != null) hmpSelected.SetSelected(false);
//                roomButtonGrid.removeViewAt(nRoomDetailsLocationIndex);
//                if (nSelectedRoomIndex == index)
//                {
//                    //showWeather();
//                    bDetailsShowing = false;
//                    nSelectedRoomIndex = index;
//                    nRoomDetailsLocationIndex = -1;
//                    rbSelectedRoom = null;
//                }
//                else
//                {
//                    r.SetSelected(true);
//                    //                    if (r.getlcmdabfsv() != null && r.getlcmdabfsv().size() != 0)
//                    //                    {
//                    //                        hideWeather();
//                    //                        roomButtonGrid.addView(mLightingRow, nDetailsLoc);
//                    //                        UpdateRoomLightingWidget(r.getlcmdabfsv(), r.getRoomData(), true);
//                    //                        mLightingRow.startAnimation(in);
//                    //                    }
//                    //                    else if (r.getifttdabfsv() != null && r.getifttdabfsv().size() != 0)
//                    //                    {
//                    //                        hideWeather();
//                    //                        roomButtonGrid.addView(mIfttRow, nDetailsLoc);
//                    //                        UpdateRoomIfttWidget(r.getifttdabfsv(), r.getRoomData(), true);
//                    //                        mIfttRow.startAnimation(in);
//                    //                    }
//                    //                    else
//                    //                    {
//                    //#MARK !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//                    //                        initStandaloneWidgets(r.getRoomData(), mRoomDetailsWidget);
//                    //                        hideWeather();
//                    //                        roomButtonGrid.addView(mRoomDetailsWidget, nDetailsLoc);
//                    //                        updateRoomDetailsWidget(r.getRoomData());
//                    //                        mRoomDetailsWidget.startAnimation(in);
//                    //#MARK END !!!!!!!!!!!!!!!!
//                    //                    }
//                    bDetailsShowing = true;
//                    nSelectedRoomIndex = index;
//                    nRoomDetailsLocationIndex = nDetailsLoc;
//                    rbSelectedRoom = r;
//                    selectedDevice = null;
//                    //hmpSelected = null;
//                    if (nDetailsLoc < 2)
//                    {
//                        scrollView.fullScroll(ScrollView.FOCUS_UP);
//                    }
//                    else
//                    {
//                        scrollView.scrollTo(0, mRoomDetailsWidget.getHeight());
//                    }
//                }
//            }
        }
    };
    
    public static ZonesFragment newInstance()
    {
        return new ZonesFragment();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
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
        floorDataAdapter = new DataArrayAdapter<>(getActivity(), R.layout.listviewitem, ccu().getFloors());
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
    public SeekArcWidget AddNewArc(Zone zone, SingleStageProfile zoneProfile, LinearLayout.LayoutParams lp)
    {
        SeekArcWidget rmSeekArc = new SeekArcWidget(getActivity().getApplicationContext(), attributeSet);
        
        rmSeekArc.setLayoutParams(lp);
        rmSeekArc.setmPathStartAngle(120);
        rmSeekArc.setmBuildingLimitStartAngle(65); //MARK //L.resolveTuningParameter(AlgoTuningParameters.getHandle().getBuildingAllowNoCooler());
        rmSeekArc.setmBuildingLimitEndAngle(85);    //MARK //AlgoTuningParameters.getHandle().getBuildingAllowNoHotter());
        rmSeekArc.prepareAngle();
        rmSeekArc.setLimitStartAngle(68);  //AlgoTuningParameters.getHandle().getUserAllowNoCooler());
        rmSeekArc.setLimitEndAngle(75);  //AlgoTuningParameters.getHandle().getUserAllowNoHotter());
        //rmSeekArc.setRoomData(zone);
        //rmSeekArc.setCMDataToSeekArc(zoneProfile, 0);
        
       
       /* int nPairedSensor = roomList.get(nRoomIndex - count).getFSVData().size();
        if (nPairedSensor <= 0)
        {
            rmSeekArc.setTouchInSide(true);
            rmSeekArc.setTouchOutSide(false);
            rmSeekArc.setIsSensorPaired(true);
            rmSeekArc.setCurrentTemp(0);
            rmSeekArc.setDesireTemp(0);
            rmSeekArc.invalidate();
            rmSeekArc.setOnClickChangeListener(seekArcClickListener);
            rmSeekArc.setOnSeekArcChangeListener(null);
        }
        else
        {*/
    
        //rmSeekArc.setDetailedView(false);
        rmSeekArc.setTouchInSide(true);
        rmSeekArc.setTouchOutSide(false);
       // rmSeekArc.setIsSensorPaired(false);
        //#MARK
//        if (L.resolveZoneProfileLogicalValue(zoneProfile) < 68 || //AlgoTuningParameters.getHandle().getUserAllowNoCooler() ||
//            L.resolveZoneProfileLogicalValue(zoneProfile) > 75)//SystemSettingsData.getCurrentSlotRawTemp()
//        // > AlgoTuningParameters.getHandle().getUserAllowNoHotter())
//        {
//            rmSeekArc.setIsCurrBeyondLimit(true);
//        }
//        else
//        {
//            rmSeekArc.setIsCurrBeyondLimit(false);
//        }
        rmSeekArc.setCurrentTemp(zoneProfile.getDisplayCurrentTemp());
        rmSeekArc.setDesireTemp(zoneProfile.getActualDesiredTemp());
        //TODO: ?
        //            rmSeekArc.setdablcmfsv(lcmdabfsv);
        //            rmSeekArc.setdabifttfsv(ifttdabfsv);
        rmSeekArc.invalidate();
        
        rmSeekArc.setOnSeekArcChangeListener(seekArcChangeListener);
        rmSeekArc.setOnClickChangeListener(seekArcClickListener);
        
        rmSeekArc.setZone(zone);
        //}
        seekArcWidgetList.add(rmSeekArc);
        roomRow.addView(rmSeekArc);
        
        
        //rmSeekArc.refreshView();
        //        if (!showCCUDial && (count > 1))
        //        {
        //            count = count - 1;
        //        }
        //        nRoomIndex = showCCUDial ? nRoomIndex : nRoomIndex - count;
        //        if (((nRoomIndex + imagedevices) % mod) == 0)
        //        {
        //            roomRow = new LinearLayout(getActivity());
        //          roomRow.setLayoutParams(lp);
        //        roomRow.setOrientation(LinearLayout.HORIZONTAL);
        //       roomButtonGrid.addView(roomRow);
        //     roomRow.addView(rmSeekArc);
        //}
        //else
        //{
        //  roomRow.addView(rmSeekArc);
        //}
        return rmSeekArc;
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
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            roomRow.setOrientation(LinearLayout.HORIZONTAL);
            roomRow.setLayoutParams(lp);
            roomButtonGrid.addView(roomRow);
            ArrayList<Zone> zoneList = floor.mRoomList;
            //TODO - refactor
            for (Zone z : zoneList)
            {
                for (ZoneProfile zoneProfile : z.mZoneProfiles)
                {
                    if (zoneProfile.getProfileType() == ProfileType.LIGHT)
                    {
                        ZoneImageWidget zWidget = new ZoneImageWidget(getActivity().getApplicationContext(), z.roomName, z.findProfile(ProfileType.LIGHT));
                        zWidget.setLayoutParams(new LinearLayout.LayoutParams(room_width, room_height));
                        zWidget.setOnClickChangeListener(zoneWidgetListener);
                        roomRow.addView(zWidget);
                    }
                    else if (zoneProfile.getProfileType() == ProfileType.SSE)
                    {
                        SeekArcWidget seekArcWidget = AddNewArc(z, (SingleStageProfile) zoneProfile, new LinearLayout.LayoutParams(room_width, room_height));
                        
                        
                    }
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
        seekArcWidgetList = new ArrayList<>();
        zoneWidgetList = new ArrayList<>();
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
            View header = (View) getActivity().getLayoutInflater().inflate(R.layout.lcm_header_row, null);
            ImageView signal = (ImageView) header.findViewById(R.id.imageSignal);
            ImageView occupied = (ImageView) header.findViewById(R.id.imageOccupied);
            ImageView lcmHeaderNamedSchEdit = (ImageView) header.findViewById(R.id.lcmHeaderNamedSchEdit);
            lcmHeaderNamedSchEdit.setVisibility(View.VISIBLE);
            lcmHeaderNamedSchEdit.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Toast.makeText(ZonesFragment.this.getContext(), "onclick " + "lcmheadernamedSchEdit", Toast.LENGTH_LONG).show();
                    showLCMLightScheduleFragment(getFloorForLightProfile(roomData), getZoneForLightProfile(roomData), roomData);
                }
            });
            Spinner spinnerSchedule = (Spinner) header.findViewById(R.id.spinnerSchedule);
            ArrayAdapter<CharSequence> aaOccupancyMode = ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.scheduleLCM, R.layout.spinner_item);
            aaOccupancyMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerSchedule.setAdapter(aaOccupancyMode);
            spinnerSchedule.setOnItemSelectedListener(null);
            spinnerSchedule.setSelection(roomData.getScheduleMode() == ScheduleMode.ZoneSchedule ? 0 : 1);
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
        LightingDetailAdapter adapter = new LightingDetailAdapter(getActivity(), mLightsDetailsView, roomData, getFloorForLightProfile(roomData), getZoneForLightProfile(roomData), expand);
        mLightsDetailsView.setAdapter(adapter);
        new LayoutHelper(getActivity()).setListViewParams(mLightsDetailsView, null, 0, 0, expand);
    }
    private void showLCMNamedScheduleSelector(final ZoneProfile zoneProfile)
    {
        final ArrayList<String> strings = new ArrayList<String>(ccu().getLCMNamedSchedules().keySet());
        CharSequence[] charSequences = new CharSequence[strings.size()];
        for (int i = 0; i < strings.size(); i++)
        {
            charSequences[i] = strings.get(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Named Schedule").setItems(charSequences, new DialogInterface.OnClickListener()
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
        showDialogFragment(LightScheduleFragment.newZoneProfileInstance(floor, zone, zoneProfile), LightScheduleFragment.ID);
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
