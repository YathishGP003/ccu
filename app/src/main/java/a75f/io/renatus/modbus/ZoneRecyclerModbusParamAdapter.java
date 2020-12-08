package a75f.io.renatus.modbus;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.text.HtmlCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.modbus.Command;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.LogicalPointTags;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.api.haystack.modbus.UserIntentPointTags;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.modbus.LModbus;
import a75f.io.logic.pubnub.ModbusDataInterface;
import a75f.io.logic.pubnub.UpdatePointHandler;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.R;

public class ZoneRecyclerModbusParamAdapter extends RecyclerView.Adapter<ZoneRecyclerModbusParamAdapter.ViewHolder> implements ModbusDataInterface {

    Context context;
    List<Parameter> modbusParam;
    String equipRef;

    public ZoneRecyclerModbusParamAdapter(Context context, String equipRef, List<Parameter> modbusParam) {
        this.context = context;
        this.modbusParam = modbusParam;
        this.equipRef = equipRef;
        UpdatePointHandler.setModbusDataInterface(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int positon) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.zone_item_modbus, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int viewPosition) {
        int position = viewHolder.getAdapterPosition();
        viewHolder.tvParamLabel.setText(modbusParam.get(position).getName());
        if (modbusParam.get(position).getParameterDefinitionType() != null) {
            switch (modbusParam.get(position).getParameterDefinitionType()) {
                case "range":
                case "float":
                case "decimal":
                case "long":
                case "binary":
                case "integer":
                case "boolean":
                case "digital":
                    if (modbusParam.get(position).getUserIntentPointTags() != null && modbusParam.get(position).getUserIntentPointTags().size() > 0) {
                        viewHolder.spValue.setVisibility(View.VISIBLE);
                        viewHolder.tvParamValue.setVisibility(View.GONE);

                        String unit = null;
                        List<Command> commands = new ArrayList<>();
                        Point p = readPoint(modbusParam.get(position));
                        if (modbusParam.get(position).getCommands() != null && modbusParam.get(position).getCommands().size() > 0) {
                            HashMap<String, List<Command>> userIntentsMap = getUserIntentsCommandMap(modbusParam.get(position));
                            for (HashMap.Entry<String, List<Command>> entry : userIntentsMap.entrySet()) {
                                unit = entry.getKey();
                                commands = entry.getValue();
                            }
                            CommandSpinnerAdapter commandAdapter = new CommandSpinnerAdapter(context, R.layout.spinner_item_orange, commands);
                            commandAdapter.setDropDownViewResource(R.layout.spinner_item_orange);
                            viewHolder.spValue.setAdapter(commandAdapter);
                            for (int i = 0; i < modbusParam.get(position).getCommands().size(); i++) {
                                if (Double.parseDouble(modbusParam.get(position).getCommands().get(i).getBitValues()) == readVal(p.getId())) {
                                    viewHolder.spValue.setSelection(i, false);
                                }
                            }
                        } else {
                            ArrayList<Double> doubleArrayList = new ArrayList<>();
                            HashMap<String, ArrayList<Double>> userIntentsMap = getUserIntentsDoubleMap(modbusParam.get(position));
                            for (HashMap.Entry<String, ArrayList<Double>> entry : userIntentsMap.entrySet()) {
                                unit = entry.getKey();
                                doubleArrayList = entry.getValue();
                            }
                            ArrayAdapter<Double> spinnerAdapter = new ArrayAdapter<>(context, R.layout.spinner_item_orange, doubleArrayList);
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_item_orange);
                            viewHolder.spValue.setAdapter(spinnerAdapter);
                            viewHolder.spValue.setSelection(doubleArrayList.indexOf(readVal(p.getId())), false);
                        }

                        viewHolder.spValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                                new Thread(() -> {
                                    if (modbusParam.get(position).getCommands() != null && modbusParam.get(position).getCommands().size() > 0) {
                                        Command command = (Command) adapterView.getSelectedItem();
                                        writePoint(p, command.getBitValues(), modbusParam.get(position));
                                    } else {
                                        writePoint(p, adapterView.getItemAtPosition(pos).toString(), modbusParam.get(position));
                                    }
                                }).start();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });

                        if (unit != null && !unit.equals("")) {
                            viewHolder.tvUnit.setVisibility(View.VISIBLE);
                            viewHolder.tvUnit.setText("(" + unit + ")");
                        } else {
                            viewHolder.tvUnit.setVisibility(View.GONE);
                        }
                    } else {
                        if (modbusParam.get(position).getLogicalPointTags() != null && modbusParam.get(position).getLogicalPointTags().size() > 0) {
                            Point p = readPoint(modbusParam.get(position));
                            String unit = p.getUnit() == null ? " " : p.getUnit();

                            if (modbusParam.get(position).getConditions() != null && modbusParam.get(position).getConditions().size() > 0) {
                                for (int i = 0; i < modbusParam.get(position).getConditions().size(); i++) {
                                    String bitValues = modbusParam.get(position).getConditions().get(i).getBitValues();
                                    if (Double.parseDouble(bitValues == null ? "0" : bitValues) == readHisVal(p.getId())) {
                                        viewHolder.tvParamValue.setText(modbusParam.get(position).getConditions().get(i).getName());
                                    }
                                }
                            } else {
                                if (modbusParam.get(position).getParameterDefinitionType().equals("binary")) {
                                    viewHolder.tvParamValue.setText(readHisVal(p.getId()) == 1 ? HtmlCompat.fromHtml("<font color='#E24301'>ON</font>", HtmlCompat.FROM_HTML_MODE_LEGACY) : HtmlCompat.fromHtml("<font color='#000000'>OFF</font>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                                } else {
                                    viewHolder.tvParamValue.setText("" + readHisVal(p.getId()));
                                }
                            }
                            if (unit != null && !unit.equals(" ")) {
                                viewHolder.tvUnit.setVisibility(View.VISIBLE);
                                viewHolder.tvUnit.setText("(" + unit + ")");
                            } else {
                                viewHolder.tvUnit.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
                default:
                    viewHolder.spValue.setVisibility(View.GONE);
                    viewHolder.tvUnit.setVisibility(View.GONE);
                    viewHolder.tvParamValue.setVisibility(View.VISIBLE);
                    viewHolder.tvParamValue.setText(modbusParam.get(position).isDisplayInUI() ? HtmlCompat.fromHtml("<font color='#E24301'>ON</font>", HtmlCompat.FROM_HTML_MODE_LEGACY) : HtmlCompat.fromHtml("<font color='#000000'>OFF</font>", HtmlCompat.FROM_HTML_MODE_LEGACY));
                    break;
            }
        } else {
            if (modbusParam.get(position).getLogicalPointTags() != null && modbusParam.get(position).getLogicalPointTags().size() > 0) {
                Point p = readPoint(modbusParam.get(position));
                String unit = p.getUnit() == null ? " " : p.getUnit();

                if (modbusParam.get(position).getConditions() != null && modbusParam.get(position).getConditions().size() > 0) {
                    for (int i = 0; i < modbusParam.get(position).getConditions().size(); i++) {
                        String bitValues = modbusParam.get(position).getConditions().get(i).getBitValues();
                        if (Double.parseDouble(bitValues == null ? "0" : bitValues) == readHisVal(p.getId())) {
                            viewHolder.tvParamValue.setText(modbusParam.get(position).getConditions().get(i).getName());
                        }
                    }
                } else {
                    viewHolder.tvParamValue.setText("" + readHisVal(p.getId()));
                }
                if (unit != null && !unit.equals(" ")) {
                    viewHolder.tvUnit.setVisibility(View.VISIBLE);
                    viewHolder.tvUnit.setText("(" + unit + ")");
                } else {
                    viewHolder.tvUnit.setVisibility(View.GONE);
                }
            }

        }
    }

    private HashMap<String, List<Command>> getUserIntentsCommandMap(Parameter parameter) {
        HashMap<String, List<Command>> userIntentsMap = new HashMap<>();
        List<UserIntentPointTags> userIntentPointTags = parameter.getUserIntentPointTags();
        String unit = "";
        for (UserIntentPointTags tags : userIntentPointTags) {
            if (tags.getTagName().equals("unit")) {
                unit = tags.getTagName();
            }
        }

        userIntentsMap.put(unit, parameter.getCommands());

        return userIntentsMap;
    }

    private HashMap<String, ArrayList<Double>> getUserIntentsDoubleMap(Parameter parameter) {
        HashMap<String, ArrayList<Double>> userIntentsMap = new HashMap<>();
        List<UserIntentPointTags> userIntentPointTags = parameter.getUserIntentPointTags();
        ArrayList<Double> doubleArrayList = new ArrayList<>();
        double minValue = 0.0, maxValue = 0.0, incValue = 0.0;
        String unit = "";

        for (int i = 0; i < userIntentPointTags.size(); i++) {
            String tagName = userIntentPointTags.get(i).getTagName();
            String tagValue = userIntentPointTags.get(i).getTagValue();
            switch (tagName) {
                case "minVal":
                    minValue = Double.parseDouble(tagValue);
                    break;
                case "maxVal":
                    maxValue = Double.parseDouble(tagValue);
                    break;
                case "incrementVal":
                    incValue = Double.parseDouble(tagValue);
                    break;
                case "unit":
                    unit = tagValue;
                    break;
            }
        }
        for (int pos = (int) (100 * minValue); pos <= (100 * maxValue); pos += (100 * incValue)) {
            doubleArrayList.add(pos / 100.0);
        }
        userIntentsMap.put(unit, doubleArrayList);

        return userIntentsMap;
    }

    @Override
    public int getItemCount() {
        return modbusParam != null ? modbusParam.size() : 0;
    }

    @Override
    public void refreshScreen(String id) {
        HashMap pointMap = CCUHsApi.getInstance().readMapById(id);
        Point point = new Point.Builder().setHashMap(pointMap).build();
        double value = readVal(id);
        //write to modbus
        EquipmentDevice modbusDevice = EquipsManager.getInstance().fetchProfileBySlaveId(Short.parseShort(point.getGroup()));
        for (Register register : modbusDevice.getRegisters()) {
            for (Parameter pam : register.getParameters()) {
                if (pam.getUserIntentPointTags() != null) {
                    if (pam.getName().equals(point.getShortDis())) {
                        if (LSerial.getInstance().isModbusConnected()) {
                            LModbus.writeRegister(Short.parseShort(point.getGroup()), register, (int) value);
                        }
                        break;
                    }
                }
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvParamLabel;
        public TextView tvParamValue;
        public TextView tvUnit;
        public Spinner spValue;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            tvParamLabel = v.findViewById(R.id.tvParamLabel);
            tvParamValue = v.findViewById(R.id.tvParamValue);
            tvUnit = v.findViewById(R.id.tvUnit);
            spValue = v.findViewById(R.id.spValue);
        }
    }

    private Point readPoint(Parameter configParams) {
        StringBuilder tags = new StringBuilder();
        for (LogicalPointTags marker : configParams.getLogicalPointTags()) {
            if (!Objects.nonNull(marker.getTagValue())) {
                tags.append(" and ").append(marker.getTagName());
            }
        }
        HashMap pointRead = CCUHsApi.getInstance().read("point and logical and modbus and zone" + tags + " and equipRef == \"" + equipRef + "\"");
        Point logicalPoint = new Point.Builder().setHashMap(pointRead).build();
        return logicalPoint;
    }

    public double readVal(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    public double readHisVal(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        return hayStack.readHisValById(id);
    }

    private void writePoint(Point point, String value, Parameter parameter) {
        CCUHsApi.getInstance().writePoint(point.getId(), Double.valueOf(value));
        if (point.getMarkers().contains("his")) {
            CCUHsApi.getInstance().writeHisValById(point.getId(), Double.valueOf(value));
        }
        EquipmentDevice modbusDevice = EquipsManager.getInstance().fetchProfileBySlaveId(Short.parseShort(point.getGroup()));
        for (Register register : modbusDevice.getRegisters()) {
            for (Parameter pam : register.getParameters()) {
                if (pam.getUserIntentPointTags() != null) {
                    if (pam.getName().equals(parameter.getName())) {
                        if (LSerial.getInstance().isModbusConnected()) {
                            LModbus.writeRegister(Short.parseShort(point.getGroup()), register, (int) Double.parseDouble(value));
                        }
                        break;
                    }
                }
            }
        }
    }
}
