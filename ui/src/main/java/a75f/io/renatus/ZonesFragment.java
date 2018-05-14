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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import a75f.io.bo.building.HmpProfile;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SingleStageProfile;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.kinveybo.AlgoTuningParameters;
import a75f.io.logic.L;
import a75f.io.renatus.views.SeekArc;
import a75f.io.renatus.views.ZoneImageWidget;

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
    SeekArc rbSelectedRoom            = null;
    int                        nSelectedRoomIndex        = -1;
    ArrayList<SeekArc> seekArcList = null;
    ArrayList<ZoneImageWidget> zoneWidgetList            = null;
    boolean                    showCCUDial               = false;
    boolean                    bDetailsShowing           = false;
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
    
    private LinearLayout zoneDetailsRow;
    private TextView zoneStatusView;
    
    private DataArrayAdapter<Floor> floorDataAdapter;
    private SeekArc.OnSeekArcChangeListener seekArcChangeListener =
            new SeekArc.OnSeekArcChangeListener()
            {
                @Override
                public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser)
                {
                }


                @Override
                public void onStartTrackingTouch(SeekArc seekArc)
                {
                    mDesiredTempScrolling = true;
                }


                @Override
                public void onStopTrackingTouch(final SeekArc seekArc)
                {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        public void run()
                        {
                            L.forceOverride(seekArc.getZone(), seekArc.getZoneProfile(), (float) seekArc.getDesireTemp());
                            //seekArc.getRoomData().updateRawDesiredTemp((int) CCUUtils.roundTo2Decimal(seekArc.getDesireTemp() * 2), UPDATESRC.CCU);
                            //MARK: need to update  room detials widget when this occurs
                            //updateRoomDetailsWidget(seekArc.getRoomData());
                            mDesiredTempScrolling = false;
                        }
                    }, 200);
                }
            };
    //#Mark
    //    private void updateRoomDetailsWidget(RoomData roomData) {
    //        if (roomData == null && roomData.getFSVData().size() <= 0)
    //            return;
    //
    //        ivSignal.setImageDrawable(getSignalDrawable(roomData.getSignalStatus()));
    //        ivOccupied.setVisibility(roomData.isOccupiedSlotActive() || roomData.isForcedOccupied() ? View.VISIBLE : View.INVISIBLE);
    //        //tvDesiredLabel.setText(getCurrentModeText(roomData));
    //        if (roomData.isForcedOccupied())
    //            ivOccupied.setImageResource(R.drawable.forced_occupied);
    //        else if (roomData.isAutoAway())
    //            ivOccupied.setImageResource(R.drawable.occupied_away_blue);
    //        else if (roomData.isVacationSlotActive())
    //            ivOccupied.setVisibility(View.INVISIBLE);
    //        else
    //            ivOccupied.setImageResource(R.drawable.occupied);
    //
    //        updateStandaloneParams(roomData);
    //
    //        spinSelectedSchedule.setSelection(roomData.getSchedulingMode().ordinal());
    //
    //
    //        updateVacationString(roomData);
    //        updateCurrentStatus(roomData);
    //        spinSelectedSchedule.setEnabled(true);
    //
    //        roomData.refreshRoomDataInterface();
    //    }

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
    private ZoneImageWidget.OnClickListener zoneWidgetListener =
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
                        } else if (w.getProfile() instanceof  HmpProfile) {
                            roomButtonGrid.addView(zoneDetailsRow, nDetailsLoc);
                            updateHmpWidget((HmpProfile) w.getProfile());
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
                            }else if (w.getProfile() instanceof  HmpProfile) {
                                roomButtonGrid.addView(zoneDetailsRow, nDetailsLoc);
                                updateHmpWidget((HmpProfile) w.getProfile());
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
    private RelativeLayout                  mRoomDetailsWidget = null;
    private int                             mod                = 3;

    private ImageView                  ivSignal;
    private ImageView                  ivOccupied;
    private Spinner                    spinSelectedSchedule;
    private ArrayAdapter<CharSequence> aaOccupancyMode;
    private TextView                   tvManualToday;
    private TextView                   tvManualStatusNow;
    private ImageView                  ivScheduleEdit;
    private ImageView                  ivVacationEdit;
    private TextView                   tvVacationFromTo;
    private Animation                     in                   = null;
    private SeekArc.OnClickListener seekArcClickListener = new SeekArc.OnClickListener()
    {
        @Override
        public void onClick(SeekArc r)
        {
            if (showCCUDial && (r.getIndex() == 1))
            {
                if (bDetailsShowing)
                {
                    rbSelectedRoom.SetSelected(false);
                    roomButtonGrid.removeViewAt(nRoomDetailsLocationIndex);
                    bDetailsShowing = false;
                    nSelectedRoomIndex = 1;
                    nRoomDetailsLocationIndex = -1;
                    rbSelectedRoom = null;
                    //totalSaUnitsInZone = 0;
                }
            }
            else
            {
                int index = r.getIndex();
                if (r.getZoneProfile().getProfileConfiguration().size() <= 0)
                {
                    if (bDetailsShowing)
                    {
                        if (rbSelectedRoom != null)
                        {
                            rbSelectedRoom.SetSelected(false);
                        }
                        roomButtonGrid.removeViewAt(nRoomDetailsLocationIndex);
                        //showWeather();
                        bDetailsShowing = false;
                        nSelectedRoomIndex = index;
                        nRoomDetailsLocationIndex = -1;
                        rbSelectedRoom = null;
                        // totalSaUnitsInZone = 0;
                    }
                    return;
                }
                int nDetailsLoc = ((index - 1) / mod) + 1;
                if (!bDetailsShowing)
                {
                    r.SetSelected(true);
                    //                        if (r.mDeviceType == SeekArc.DEVICE_TYPE.LCM_DAB) {
                    //                            hideWeather();
                    //                            roomButtonGrid.addView(mLightingRow, nDetailsLoc);
                    //                            UpdateRoomLightingWidget(r.getlcmdabfsv(), r.getRoomData(), true);
                    //                            mLightingRow.startAnimation(in);
                    //
                    //                        } else if (r.mDeviceType == SeekArc.DEVICE_TYPE.IFTT_DAB) {
                    //                            hideWeather();
                    //                            roomButtonGrid.addView(mIfttRow, nDetailsLoc);
                    //                            UpdateRoomIfttWidget(r.getifttdabfsv(), r.getRoomData(), true);
                    //                            mIfttRow.startAnimation(in);
                    //
                    //                        }
                    //else if (r.mDeviceType == SeekArc.DEVICE_TYPE.PURE_DAB) {
                    //  hideWeather();
                    roomButtonGrid.addView(mRoomDetailsWidget, nDetailsLoc);
                    //initStandaloneWidgets(r.getRoomData(), mRoomDetailsWidget);
                    updateRoomDetailsWidget(r);
                    mRoomDetailsWidget.startAnimation(in);
                    // }
                    bDetailsShowing = true;
                    nSelectedRoomIndex = index;
                    nRoomDetailsLocationIndex = nDetailsLoc;
                    rbSelectedRoom = r;
                    selectedDevice = null;
                    //hmpSelected = null;
                    if (nDetailsLoc < 2)
                    {
                        scrollView.fullScroll(ScrollView.FOCUS_UP);
                    }
                    else
                    {
                        scrollView.scrollTo(0, mRoomDetailsWidget.getHeight());
                    }
                }
                else
                {
                    if (rbSelectedRoom != null)
                    {
                        rbSelectedRoom.SetSelected(false);
                    }
                    //                    if (selectedDevice != null)
                    //                    {
                    //                        selectedDevice.SetSelected(false);
                    //                    }
                    //if(hmpSelected != null) hmpSelected.SetSelected(false);
                    roomButtonGrid.removeViewAt(nRoomDetailsLocationIndex);
                    if (nSelectedRoomIndex == index)
                    {
                        //  showWeather();
                        bDetailsShowing = false;
                        nSelectedRoomIndex = index;
                        nRoomDetailsLocationIndex = -1;
                        rbSelectedRoom = null;
                    }
                    else
                    {
                        //                        r.SetSelected(true);
                        //                        if (r.getlcmdabfsv() != null && r.getlcmdabfsv().size() != 0)
                        //                        {
                        //                            hideWeather();
                        //                            roomButtonGrid.addView(mLightingRow, nDetailsLoc);
                        //                            UpdateRoomLightingWidget(r.getlcmdabfsv(), r.getRoomData(), true);
                        //                            mLightingRow.startAnimation(in);
                        //                        }
                        //                        else if (r.getifttdabfsv() != null && r.getifttdabfsv().size() != 0)
                        //                        {
                        //                            hideWeather();
                        //                            roomButtonGrid.addView(mIfttRow, nDetailsLoc);
                        //                            UpdateRoomIfttWidget(r.getifttdabfsv(), r.getRoomData(), true);
                        //                            mIfttRow.startAnimation(in);
                        //                        }
                        //                        else
                        //                        {
                        //                            initStandaloneWidgets(r.getRoomData(), mRoomDetailsWidget);
                        //                            hideWeather();
                        roomButtonGrid.addView(mRoomDetailsWidget, nDetailsLoc);
                        updateRoomDetailsWidget(r);
                        mRoomDetailsWidget.startAnimation(in);
                        //}
                        bDetailsShowing = true;
                        nSelectedRoomIndex = index;
                        nRoomDetailsLocationIndex = nDetailsLoc;
                        rbSelectedRoom = r;
                        selectedDevice = null;
                        //hmpSelected = null;
                        if (nDetailsLoc < 2)
                        {
                            scrollView.fullScroll(ScrollView.FOCUS_UP);
                        }
                        else
                        {
                            scrollView.scrollTo(0, mRoomDetailsWidget.getHeight());
                        }
                    }
                }
            }
        }
    };


    public static ZonesFragment newInstance()
    {
        return new ZonesFragment();
    }


    private void updateRoomDetailsWidget(SeekArc roomData)
    {
        //TODO: signal
        //ivSignal.setImageDrawable(getSignalDrawable(roomData.getSignalStatus()));
        boolean isOccupied = L.isOccupied(roomData.getZone(), roomData.getZoneProfile());
        ivOccupied.setVisibility(isOccupied ? View.VISIBLE : View.INVISIBLE);
        //tvDesiredLabel.setText(getCurrentModeText(roomData));
        //        if (roomData.isForcedOccupied())
        //        {
        //            ivOccupied.setImageResource(R.drawable.forced_occupied);
        //        }
        //        else if (roomData.isAutoAway())
        //        {
        //            ivOccupied.setImageResource(R.drawable.occupied_away_blue);
        //        }
        //        else if (roomData.isVacationSlotActive())
        //        {
        //            ivOccupied.setVisibility(View.INVISIBLE);
        //        }
        //        else
        //        {
        ivOccupied.setImageResource(R.drawable.occupied);
        //        }
        //updateStandaloneParams(roomData);
        //spinSelectedSchedule.setSelection(roomData.getSchedulingMode().ordinal());
        //updateVacationString("vacation string");
        //updateCurrentStatus("status");
        spinSelectedSchedule.setEnabled(true);
        roomData.getZoneProfile().refreshRoomDataInterface();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View inflate = inflater.inflate(R.layout.fragment_zones, container, false);
        this.inflater = inflater;
        createDetailsWidget(inflater);
        return inflate;
    }


    @Override
    public void onStart()
    {
        super.onStart();
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
            ArrayList<Zone> zoneList = floor.mRoomList;
            View viewToAdd = null;
            int index = 0;
            //TODO - refactor
            for (Zone z : zoneList)
            {
                for (ZoneProfile zoneProfile : z.mZoneProfiles)
                {
                    if (zoneProfile.getProfileType() == ProfileType.LIGHT)
                    {
                        ZoneImageWidget zWidget = new ZoneImageWidget(getActivity()
                                                                              .getApplicationContext(),z.roomName, z.findProfile(ProfileType.LIGHT), index);
                        zWidget.setLayoutParams(new LinearLayout.LayoutParams(room_width, room_height));
                        zWidget.setZoneImage(R.drawable.light_orange);
                        zWidget.setOnClickChangeListener(zoneWidgetListener);
                        zoneWidgetList.add(zWidget);
                        viewToAdd = zWidget;
                    }else if (zoneProfile.getProfileType() == ProfileType.HMP)
                    {
                        ZoneImageWidget zWidget = new ZoneImageWidget(getActivity()
                                                                              .getApplicationContext(), z.roomName, z.findProfile(ProfileType.HMP), index);
                        zWidget.setLayoutParams(new LinearLayout.LayoutParams(room_width, room_height));
                        zWidget.setZoneImage(R.drawable.hotwater_mixture_orange);
                        zWidget.setOnClickChangeListener(zoneWidgetListener);
                        HmpProfile profile = (HmpProfile) zoneProfile;
                        zWidget.setZoneTemp(profile.getHwTemperature()+"/"+profile.getSetTemperature());
                        zoneWidgetList.add(zWidget);
                        
                        viewToAdd = zWidget;
                    }
                    else if (zoneProfile.getProfileType() == ProfileType.SSE)
                    {
                        SeekArc seekArc =
                                AddNewArc(z, (SingleStageProfile) zoneProfile, new LinearLayout.LayoutParams(room_width, room_height), index);
                        seekArcList.add(seekArc);
                        viewToAdd = seekArc;
                    }
                    if (index % mod == 0)
                    {
                        roomRow = new LinearLayout(getActivity());
                        LinearLayout.LayoutParams lp =
                                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                        roomRow.setOrientation(LinearLayout.HORIZONTAL);
                        roomRow.setLayoutParams(lp);
                        roomButtonGrid.addView(roomRow);
                    }
                    roomRow.addView(viewToAdd);
                    index++;
                }
            }
        }
    }


    public SeekArc AddNewArc(Zone zone, SingleStageProfile zoneProfile,
                             LinearLayout.LayoutParams lp, int index)
    {
        SeekArc rmSeekArc =
                new SeekArc(getActivity().getApplicationContext(), attributeSet, null);
        rmSeekArc.setCMDataToSeekArc(zoneProfile, zone.roomName, index);
        rmSeekArc.setLayoutParams(lp);
        rmSeekArc.setmPathStartAngle(120);
        rmSeekArc
                .setmBuildingLimitStartAngle((int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_BUILDING_MIN_TEMP));
        rmSeekArc
                .setmBuildingLimitEndAngle((int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_BUILDING_MAX_TEMP));
        rmSeekArc.prepareAngle();
        rmSeekArc
                .setLimitStartAngle((int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_USER_MIN_TEMP));
        rmSeekArc
                .setLimitEndAngle((int) L.resolveTuningParameter(zone, AlgoTuningParameters.SSETuners.SSE_USER_MAX_TEMP));
        rmSeekArc.setTouchInSide(true);
        rmSeekArc.setTouchOutSide(false);
        rmSeekArc.setCurrentTemp(zoneProfile.getDisplayCurrentTemp());
        rmSeekArc.setDesireTemp(L.resolveZoneProfileLogicalValue(zoneProfile));
        rmSeekArc.setZone(zone);
        rmSeekArc.invalidate();
        rmSeekArc.setOnSeekArcChangeListener(seekArcChangeListener);
        rmSeekArc.setOnClickChangeListener(seekArcClickListener);
        //rmSeekArc.refreshView();
        return rmSeekArc;
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
                if (parser.getName().equals("a75f.io.renatus.views.SeekArc"))
                {
                    Log.i("WHYMEGOD", "Found seekarc widget");
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
        seekArcList = new ArrayList<>();
        zoneWidgetList = new ArrayList<>();
        createLighting(inflater);
        createSSE(inflater);
        createHmp(inflater);
    }


    private void createLighting(LayoutInflater inflater)
    {
        mLightingRow = (LinearLayout) inflater.inflate(R.layout.zone_detail_list, null);
        mLightsDetailsView = (ListView) mLightingRow.findViewById(R.id.lighting_detail_list);
    }
    
    private void createSSE(LayoutInflater inflater)
    {
        mRoomDetailsWidget = (RelativeLayout) inflater.inflate(R.layout.roomdetails_inline, null);
        in = AnimationUtils.makeInAnimation(getActivity(), false);
        in.setDuration(250);
        mRoomDetailsWidget.setAnimation(in);
        ivSignal = (ImageView) mRoomDetailsWidget.findViewById(R.id.imageSignal);
        ivOccupied = (ImageView) mRoomDetailsWidget.findViewById(R.id.imageOccupied);
        spinSelectedSchedule = (Spinner) mRoomDetailsWidget.findViewById(R.id.spinnerSchedule);
        aaOccupancyMode = ArrayAdapter.createFromResource(getActivity()
                                                                  .getApplicationContext(), R.array.schedule, R.layout.spinner_item);
        aaOccupancyMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinSelectedSchedule.setAdapter(aaOccupancyMode);
        if (rbSelectedRoom != null)
        {
            spinSelectedSchedule.setSelection(0); //MARK
        }
        spinSelectedSchedule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
        tvManualToday = (TextView) mRoomDetailsWidget.findViewById(R.id.manualToday);
        tvManualStatusNow = (TextView) mRoomDetailsWidget.findViewById(R.id.manualStatusNow);
        tvManualToday.setVisibility(View.GONE);
        ivScheduleEdit = (ImageView) mRoomDetailsWidget.findViewById(R.id.scheduleEdit);
        ivScheduleEdit.setClickable(true);
        ivScheduleEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            }
        });
        ivVacationEdit = (ImageView) mRoomDetailsWidget.findViewById(R.id.vacationEdit);
        ivVacationEdit.setClickable(true);
        ivVacationEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            }
        });
        tvVacationFromTo = (TextView) mRoomDetailsWidget.findViewById(R.id.vacationFromTo);
    }
    
    private void createHmp(LayoutInflater inflater)
    {
        zoneDetailsRow = (LinearLayout) inflater.inflate(R.layout.zone_details_hmp, null);
        zoneStatusView = (TextView) zoneDetailsRow.findViewById(R.id.statusMsg);
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
                    showLCMLightScheduleFragment(getFloorForLightProfile(roomData), getZoneForLightProfile(roomData), roomData);
                }
            });
            Spinner spinnerSchedule = (Spinner) header.findViewById(R.id.spinnerSchedule);
            ArrayAdapter<CharSequence> aaOccupancyMode = ArrayAdapter
                                                                 .createFromResource(getActivity()
                                                                                             .getApplicationContext(), R.array.scheduleLCM, R.layout.spinner_item);
            aaOccupancyMode.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerSchedule.setAdapter(aaOccupancyMode);
            spinnerSchedule.setOnItemSelectedListener(null);
            spinnerSchedule.setSelection(roomData.getScheduleMode() == ZoneSchedule ? 0 : 1);
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
                        roomData.setScheduleMode(ZoneSchedule);
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

    private void updateHmpWidget(HmpProfile p) {
        zoneStatusView.setText("Current Hot Water Signal is "+(short)Math.round(p.getHmpValvePosition())+"%" );
    }

    private void showLCMLightScheduleFragment(Floor floor, Zone zone, ZoneProfile zoneProfile)
    {
        showDialogFragment(LightScheduleFragment
                                   .newZoneProfileInstance(floor, zone, zoneProfile), LightScheduleFragment.ID);
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
