package a75f.io.renatus.bacnet;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tooltip.Tooltip;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem;
import a75f.io.logic.util.bacnet.BacnetConfigConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.interfaces.ModbusDataInterface;
import a75f.io.messaging.handler.UpdatePointHandler;
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants;
import a75f.io.renatus.ENGG.bacnet.services.BacnetServicesUtils;
import a75f.io.renatus.ENGG.bacnet.services.BacnetWriteRequest;
import a75f.io.renatus.ENGG.bacnet.services.DestinationMultiRead;
import a75f.io.renatus.ENGG.bacnet.services.ObjectIdentifierBacNet;
import a75f.io.renatus.ENGG.bacnet.services.PropertyValueBacNet;
import a75f.io.renatus.ENGG.bacnet.services.WriteRequest;
import a75f.io.renatus.R;
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter;

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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int viewPosition) {
        int position = viewHolder.getAdapterPosition();
        BacnetZoneViewItem bacnetZoneViewItem = param.get(position);
        String title = bacnetZoneViewItem.getDisName() + " (" + bacnetZoneViewItem.getBacnetObj().getDefaultUnit() + ")";
        viewHolder.tvParamLabel.setText(title);
        viewHolder.tvParamValue.setText(bacnetZoneViewItem.getValue());
        if(bacnetZoneViewItem.isWritable()) {
            viewHolder.spValue.setVisibility(View.VISIBLE);
            viewHolder.tvParamValue.setVisibility(View.GONE);
            viewHolder.spValue.setAdapter(getAdapterValue((ArrayList) bacnetZoneViewItem.getSpinnerValues()));

            int itemIndex = 0;
            try {
                itemIndex = findItemPosition(bacnetZoneViewItem.getSpinnerValues(), Double.parseDouble(bacnetZoneViewItem.getValue()));
            } catch (NumberFormatException e) {
                CcuLog.d(TAG,"this is not a number");
                itemIndex = (int) Double.parseDouble(bacnetZoneViewItem.getValue());
            }

            CcuLog.d(TAG, "onBindViewHolder:: " + bacnetZoneViewItem.getSpinnerValues() + " searching for-> " + String.valueOf(Double.parseDouble(bacnetZoneViewItem.getValue())) + "<--found at index-->" + itemIndex);
            viewHolder.spValue.setSelection(itemIndex);
        } else {
            viewHolder.tvParamValue.setVisibility(View.VISIBLE);
            viewHolder.spValue.setVisibility(View.GONE);
            //viewHolder.spinnerLayout.setVisibility(View.INVISIBLE);
        }

        AtomicBoolean isUserInteraction = new AtomicBoolean(false);
        viewHolder.spValue.setOnTouchListener((v, event) -> {
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

                String value = bacnetZoneViewItem.getSpinnerValues().get(i);

                if (NumberUtils.isCreatable(value)) {
                    writeValue(bacnetZoneViewItem, value);
                } else {
                    writeValue(bacnetZoneViewItem, String.valueOf(i));
                }
                CcuLog.d(TAG, "onItemSelected: " + value + " " + bacnetZoneViewItem.getBacnetConfig());
                isUserInteraction.set(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
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

        int objectId = bacnetZoneViewItem.getBacnetObj().getProtocolData().getBacnet().getObjectId();
        BacnetServicesUtils bacnetServicesUtils = new BacnetServicesUtils();
        bacnetServicesUtils.sendWriteRequest(generateWriteObject(configMap, objectId, selectedValue,
                bacnetZoneViewItem.getObjectType(), bacnetZoneViewItem.getBacnetObj().getDefaultWriteLevel()),
                serverIpAddress, remotePointUpdateInterface, selectedValue, bacnetZoneViewItem.getBacnetObj().getId());
    }

    private BacnetWriteRequest generateWriteObject(Map<String, String> configMap, int objectId, String selectedValue, String objectType, String priority) {

        String macAddress = "";
        if(configMap.get(MAC_ADDRESS) != null) {
            macAddress = configMap.get(MAC_ADDRESS);
        }
        //OBJECT_MULTI_STATE_VALUE
        DestinationMultiRead destinationMultiRead = new DestinationMultiRead(Objects.requireNonNull(configMap.get(DESTINATION_IP)),
                Objects.requireNonNull(configMap.get(DESTINATION_PORT)), Objects.requireNonNull(configMap.get(DEVICE_ID)),
                Objects.requireNonNull(configMap.get(DEVICE_NETWORK)), macAddress);

        int dataType;
        String selectedValueAsPerType;
        if(BacNetConstants.ObjectType.valueOf(objectType).getValue() == 2){
            dataType = BacNetConstants.DataTypes.BACNET_DT_REAL.ordinal()+1;
            selectedValueAsPerType = selectedValue;
        }else if(BacNetConstants.ObjectType.valueOf(objectType).getValue() == 5){
            dataType = BacNetConstants.DataTypes.BACNET_DT_ENUM.ordinal()+1;
            selectedValueAsPerType = selectedValue; //String.valueOf(Integer.parseInt(selectedValue)+1);
        }else {
            dataType = BacNetConstants.DataTypes.BACNET_DT_UNSIGNED.ordinal()+1;
            selectedValueAsPerType = String.valueOf(Integer.parseInt(selectedValue)+1);
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
        return new CustomSpinnerDropDownAdapter(context, R.layout.spinner_item_bacnet_property, values);
    }

    private int findItemPosition(List<String> numberStrings, double targetValue) {
        int index = -1;

        for (int i = 0; i < numberStrings.size(); i++) {
            String str = numberStrings.get(i);
            double value = Double.parseDouble(str);
            if (value == targetValue) {
                index = i;
                break;
            }
        }
        return index;
    }
}
