package a75f.io.renatus.bacnet;

import static a75f.io.api.haystack.HayStackConstants.FORCE_OVERRIDE_LEVEL;
import static a75f.io.logic.bo.building.bacnet.BacnetEquip.TAG_BACNET;

import static a75f.io.logic.bo.util.CustomScheduleUtilKt.isPointFollowingScheduleOrEvent;
import static a75f.io.logic.util.bacnet.BacnetUtilKt.isValidMstpMacAddress;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tooltip.Tooltip;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem;
import a75f.io.logic.bo.building.system.BacnetServicesUtils;
import a75f.io.logic.bo.building.system.BacnetWriteRequest;
import a75f.io.logic.bo.building.system.DestinationMultiRead;
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet;
import a75f.io.logic.bo.building.system.PropertyValueBacNet;
import a75f.io.logic.bo.building.system.WriteRequest;
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface;
import a75f.io.logic.util.bacnet.BacnetConfigConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.interfaces.ModbusDataInterface;
import a75f.io.logic.util.bacnet.ObjectType;
import a75f.io.messaging.handler.UpdatePointHandler;
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants;
import a75f.io.renatus.R;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;
import a75f.io.renatus.views.userintent.UserIntentDialog;

public class ZoneRecyclerBacnetParamAdapter extends RecyclerView.Adapter<ZoneRecyclerBacnetParamAdapter.ViewHolder> implements ModbusDataInterface {

    private static final String DEVICE_ID = "deviceId";
    private static final String DESTINATION_IP = "destinationIp";
    private static final String DESTINATION_PORT = "destinationPort";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String DEVICE_NETWORK = "deviceNetwork";

    private static final String TAG = ZoneRecyclerBacnetParamAdapter.class.getSimpleName();
    Context context;
    List<BacnetZoneViewItem> param;
    private boolean[] isFirstSelectionArray;
    private String serverIpAddress;

    private RemotePointUpdateInterface remotePointUpdateInterface;

    public ZoneRecyclerBacnetParamAdapter(Context context, List<BacnetZoneViewItem> paramList, RemotePointUpdateInterface remotePointUpdateInterface) {
        this.context = context;
        this.param = paramList;
        this.remotePointUpdateInterface = remotePointUpdateInterface;
        UpdatePointHandler.setModbusDataInterface(this);
        isFirstSelectionArray = new boolean[param.size()];
        for (int i = 0; i < param.size(); i++) {
            isFirstSelectionArray[i] = true;
        }

        String bacnetServerConfig = PreferenceManager.getDefaultSharedPreferences(context).getString(BacnetConfigConstants.BACNET_CONFIGURATION, null);
        if (bacnetServerConfig != null) {
            try {
                JSONObject config = new JSONObject(bacnetServerConfig);
                JSONObject networkObject = config.getJSONObject("network");
                serverIpAddress = networkObject.getString(BacnetConfigConstants.IP_ADDRESS);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int positon) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.zone_item_bacnet, viewGroup, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int viewPosition) {
        int position = viewHolder.getAdapterPosition();
        BacnetZoneViewItem bacnetZoneViewItem = param.get(position);
        String title;
        if(bacnetZoneViewItem.getBacnetObj().getDefaultUnit().isEmpty()){
            title = bacnetZoneViewItem.getDisName() + " : ";
        }else{
            title = bacnetZoneViewItem.getDisName() + " (" + bacnetZoneViewItem.getBacnetObj().getDefaultUnit() + ") : ";
        }

        CcuLog.d(TAG,"onBindViewHolder -->"+title);
        viewHolder.tvParamLabel.setText(title);

        HDict pointMap = CCUHsApi.getInstance().readHDictById(bacnetZoneViewItem.getBacnetObj().getId());
        final Point p = new Point.Builder().setHDict(pointMap).build();
        if(bacnetZoneViewItem.isWritable()) {
            viewHolder.spValue.setVisibility(View.VISIBLE);
            viewHolder.tvParamValue.setVisibility(View.GONE);
            List<String> spinnerValues = getSpinnerValues(bacnetZoneViewItem.getSpinnerValues());
            viewHolder.spValue.setAdapter(getAdapterValue((ArrayList) spinnerValues));

            String enumValues = p.getEnums();
            int itemIndex = 0;
            if(enumValues != null){
                CcuLog.d(TAG,"onBindViewHolder bacnetZoneViewItem writable point enum-->"+bacnetZoneViewItem.getBacnetObj().getId()+"--"+enumValues);
                itemIndex = searchIndexValue(enumValues, String.valueOf((int)Double.parseDouble(bacnetZoneViewItem.getValue())));
            }else{
                try {
                    itemIndex = findItemPosition(bacnetZoneViewItem.getSpinnerValues(), Double.parseDouble(bacnetZoneViewItem.getValue()));
                } catch (NumberFormatException e) {
                    CcuLog.d(TAG,"this is not a number");
                    itemIndex = (int) Double.parseDouble(bacnetZoneViewItem.getValue());
                }
            }
            CcuLog.d(TAG, "onBindViewHolder:: " + bacnetZoneViewItem.getSpinnerValues() + " searching for-> " + String.valueOf(Double.parseDouble(bacnetZoneViewItem.getValue())) + "<--found at index-->" + itemIndex);
            viewHolder.spValue.setSelection(itemIndex);
        } else {

            String enumValues = (String) CCUHsApi.getInstance().readMapById(bacnetZoneViewItem.getBacnetObj().getId()).get("enum");
            if(enumValues != null){
                CcuLog.d(TAG,"onBindViewHolder bacnetZoneViewItem-->"+bacnetZoneViewItem.getBacnetObj().getId()+"--"+enumValues);
                String key = searchKeyForValue(enumValues, String.valueOf((int)Double.parseDouble(bacnetZoneViewItem.getValue())));
                viewHolder.tvParamValue.setText(key);
            }else{
                viewHolder.tvParamValue.setText(bacnetZoneViewItem.getValue());
            }

            viewHolder.tvParamValue.setVisibility(View.VISIBLE);
            viewHolder.spValue.setVisibility(View.GONE);
        }

        AtomicBoolean isUserInteraction = new AtomicBoolean(false);
        viewHolder.spValue.setOnTouchListener((v, event) -> {
            if(isPointFollowingScheduleOrEvent(p.getId())) {
                FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
                if (!UserIntentDialog.Companion.isDialogAlreadyVisible()) {
                    UserIntentDialog userIntentDialog = new UserIntentDialog(p.getId(), v);
                    userIntentDialog.show(fragmentManager, "UserIntentDialog");
                }
                isUserInteraction.set(true);
                return true;
            }
            isUserInteraction.set(true);
            return false;
        });
        viewHolder.spValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (isFirstSelectionArray[position]) {
                    isFirstSelectionArray[position] = false;
                    return; // Skip handling the first selection
                }

                if(!isUserInteraction.get()){
                    CcuLog.d(TAG, "onItemSelected: but not from user");
                    return;
                }

                Pair<String,Integer> selectedValue = bacnetZoneViewItem.getSpinnerValues().get(i);

                if (NumberUtils.isCreatable(selectedValue.first)) {
                    writeValue(bacnetZoneViewItem, selectedValue.first);
                } else {
                    writeValue(bacnetZoneViewItem, selectedValue.second.toString());
                }
                CcuLog.d(TAG, "onItemSelected: " + selectedValue.first + " " + bacnetZoneViewItem.getBacnetConfig());
                isUserInteraction.set(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private List<String> getSpinnerValues(List<Pair<String, Integer>> spinnerValues) {
        List<String> values = new ArrayList<>();
        for (Pair<String, Integer> pair : spinnerValues) {
            values.add(pair.first);
        }
        return values;
    }

    private void writeValue(BacnetZoneViewItem bacnetZoneViewItem, String selectedValue) {
        String[] pairs = bacnetZoneViewItem.getBacnetConfig().split(",");
        Map<String, String> configMap = new HashMap<>();
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if(keyValue.length != 2) continue; // Skip invalid key value pairs (e.g. "destinationIp:
            String key = keyValue[0];
            String value = keyValue[1];
            configMap.put(key, value);
        }
        /*if(configMap.get(DESTINATION_IP) == null || configMap.get(DESTINATION_PORT) == null || configMap.get(DEVICE_ID) == null || configMap.get(MAC_ADDRESS) == null){
            CcuLog.e(TAG, "writeValue: Invalid config map");
            Toast.makeText(context, "Invalid configuration please check ip, port, deviceId and mac address", Toast.LENGTH_SHORT).show();
            return;
        }*/
        String pointId = bacnetZoneViewItem.getBacnetObj().getId();
        String level = bacnetZoneViewItem.getBacnetObj().getDefaultWriteLevel();
        if(isPointFollowingScheduleOrEvent(pointId)) {
            level = "" + FORCE_OVERRIDE_LEVEL;
        }
        int objectId = (int) Double.parseDouble(CCUHsApi.getInstance().readMapById(pointId).get(Tags.BACNET_OBJECT_ID).toString());
        String objectType = bacnetZoneViewItem.getObjectType();
        boolean isMstpEquip = isValidMstpMacAddress(Objects.requireNonNull(configMap.getOrDefault(MAC_ADDRESS, "")));
        BacnetServicesUtils bacnetServicesUtils = new BacnetServicesUtils();
        if(bacnetZoneViewItem.getBacnetObj().isSystem()){
            CcuLog.d(TAG_BACNET, "--this is a system point, objectId--"+objectId);
            bacnetServicesUtils.sendWriteRequest(generateWriteObject(configMap, objectId, selectedValue,
                            bacnetZoneViewItem.getObjectType(), level, isMstpEquip),
                    serverIpAddress, remotePointUpdateInterface, selectedValue, bacnetZoneViewItem.getBacnetObj().getId(), isMstpEquip);
        }else{
            CcuLog.d(TAG_BACNET, "--this is a normal bacnet client point with level: " + level);
            bacnetServicesUtils.sendWriteRequest(generateWriteObject(configMap, objectId, selectedValue,
                            bacnetZoneViewItem.getObjectType(), level, isMstpEquip),
                    serverIpAddress, remotePointUpdateInterface, selectedValue, bacnetZoneViewItem.getBacnetObj().getId(), isMstpEquip);
        }


    }

    private BacnetWriteRequest generateWriteObject(Map<String, String> configMap, int objectId, String selectedValue, String objectType, String priority, boolean isMstpEquip) {

        String macAddress = "";
        if(configMap.get(MAC_ADDRESS) != null) {
            macAddress = configMap.get(MAC_ADDRESS);
        }
        //OBJECT_MULTI_STATE_VALUE
        DestinationMultiRead destinationMultiRead = new DestinationMultiRead(Objects.requireNonNull(configMap.getOrDefault(DESTINATION_IP,"")),
                Objects.requireNonNull(configMap.getOrDefault(DESTINATION_PORT, "0")), Objects.requireNonNull(configMap.getOrDefault(DEVICE_ID,"0")),
                Objects.requireNonNull(configMap.getOrDefault(DEVICE_NETWORK,"0")), macAddress);

        int dataType;
        String selectedValueAsPerType;
        if(BacNetConstants.ObjectType.valueOf(objectType).getValue() == ObjectType.OBJECT_ANALOG_VALUE.getValue() ||
                BacNetConstants.ObjectType.valueOf(objectType).getValue() == ObjectType.OBJECT_ANALOG_INPUT.getValue() ||
                BacNetConstants.ObjectType.valueOf(objectType).getValue() == ObjectType.OBJECT_ANALOG_OUTPUT.getValue()) {
            dataType = BacNetConstants.DataTypes.BACNET_DT_REAL.ordinal()+1;
            selectedValueAsPerType = selectedValue;
        }else if(BacNetConstants.ObjectType.valueOf(objectType).getValue() == ObjectType.OBJECT_BINARY_VALUE.getValue() ||
                BacNetConstants.ObjectType.valueOf(objectType).getValue() == ObjectType.OBJECT_BINARY_INPUT.getValue() ||
                BacNetConstants.ObjectType.valueOf(objectType).getValue() == ObjectType.OBJECT_BINARY_OUTPUT.getValue()) {
            dataType = BacNetConstants.DataTypes.BACNET_DT_ENUM.ordinal()+1;
            selectedValueAsPerType = selectedValue; //String.valueOf(Integer.parseInt(selectedValue)+1);
        }else {
            if (isMstpEquip) {
                dataType = BacNetConstants.DataTypes.BACNET_DT_UNSIGNED32.ordinal() + 1;
            } else {
                dataType = BacNetConstants.DataTypes.BACNET_DT_UNSIGNED.ordinal() + 1;
            }
            selectedValueAsPerType = String.valueOf(Integer.parseInt(selectedValue));
        }

        ObjectIdentifierBacNet objectIdentifierBacNet = new ObjectIdentifierBacNet(BacNetConstants.ObjectType.valueOf(objectType).getValue(), String.valueOf(objectId));
        PropertyValueBacNet propertyValueBacNet = new PropertyValueBacNet(dataType, selectedValueAsPerType);
        WriteRequest writeRequest = new WriteRequest(objectIdentifierBacNet, propertyValueBacNet, priority,
                BacNetConstants.PropertyType.valueOf("PROP_PRESENT_VALUE").getValue(), null);
        BacnetWriteRequest bacnetWriteRequest = new BacnetWriteRequest(destinationMultiRead, writeRequest);
        return bacnetWriteRequest;
    }

    @Override
    public int getItemCount() {
        return param != null ? param.size() : 0;
    }

    @Override
    public void refreshScreen(String id ) {

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvParamLabel;
        public TextView tvParamValue;

        //public View spinnerLayout;
        public Spinner spValue;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            tvParamLabel = v.findViewById(R.id.tvParamLabel);
            tvParamValue = v.findViewById(R.id.tvParamValue);
            spValue = v.findViewById(R.id.spValue);
            //spinnerLayout = v.findViewById(R.id.spinner_container);
        }
    }



    public double readHisVal(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        return hayStack.readHisValById(id);
    }

    public void showToolTip(String text, View v) {
        Tooltip intrinsicScheduleToolTip = new Tooltip.Builder(v)
                .setBackgroundColor(Color.BLACK)
                .setTextColor(Color.WHITE)
                .setCancelable(true)
                .setDismissOnClick(true)
                .setGravity(Gravity.TOP)
                .setText(text)
                .setTextSize(20.0f)
                .setPadding(10f)
                .show();
        new Handler(Looper.getMainLooper()).postDelayed(intrinsicScheduleToolTip::dismiss, 3000);
    }

    private CustomSpinnerDropDownAdapter getAdapterValue(ArrayList values) {
        CcuLog.d(TAG, "---values for adapter-->"+values);
        return new CustomSpinnerDropDownAdapter(context, R.layout.spinner_item_bacnet_property, values);
    }

    private int findItemPosition(List<Pair<String,Integer>> numberStrings, double targetValue) {
        int index = -1;

        for (int i = 0; i < numberStrings.size(); i++) {
            String str = numberStrings.get(i).first;
            double value = Double.parseDouble(str);
            if (value == targetValue) {
                index = i;
                break;
            }
        }
        return index;
    }

    private String searchKeyForValue(String enumString,
                                                   String inputValue) {
        CcuLog.d(TAG, "---------searchRealValueForOperatingMode------one----" + enumString
                + "<--inputValue-->" + inputValue);
        try {
            Map<String, String> reverseMapOne = getStringReverseMap(enumString);
            return reverseMapOne.get(inputValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputValue;
    }

    @NonNull
    private static Map<String, String> getStringReverseMap(String enumString) {
        Map<String, String> mapOne = new HashMap<>();
        for (String part : enumString.split(",")) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                mapOne.put(keyValue[0], keyValue[1]);
            }
        }
        Map<String, String> reverseMapOne = new HashMap<>();
        for (Map.Entry<String, String> entry : mapOne.entrySet()) {
            reverseMapOne.put(entry.getValue(), entry.getKey());
        }
        return reverseMapOne;
    }

    private int searchIndexValue(String enumString, String inputValue) {
        CcuLog.d(TAG_BACNET, "---------searchIndexValue-------" + enumString
                + "<--inputValue-->" + inputValue);
        try {
            String[] parts = enumString.split(",");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim(); // trim leading/trailing spaces
                String[] keyValue = part.split("=");
                if (keyValue.length == 2 && keyValue[1].trim().equals(inputValue)) {
                    return i;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Not found
    }
}
