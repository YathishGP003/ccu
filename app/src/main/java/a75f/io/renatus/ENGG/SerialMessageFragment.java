package a75f.io.renatus.ENGG;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.x75f.modbus4j.ModbusFactory;
import com.x75f.modbus4j.ModbusMaster;
import com.x75f.modbus4j.exception.ModbusTransportException;
import com.x75f.modbus4j.msg.ModbusRequest;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.msg.ReadCoilsRequest;
import com.x75f.modbus4j.msg.ReadDiscreteInputsRequest;
import com.x75f.modbus4j.msg.ReadHoldingRegistersRequest;
import com.x75f.modbus4j.msg.ReadHoldingRegistersResponse;
import com.x75f.modbus4j.msg.WriteCoilRequest;
import com.x75f.modbus4j.msg.WriteRegisterRequest;
import com.x75f.modbus4j.serial.SerialPortWrapper;
import com.x75f.modbus4j.serial.rtu.RtuMessageParser;
import com.x75f.modbus4j.serial.rtu.RtuMessageRequest;
import com.x75f.modbus4j.sero.util.queue.ByteQueue;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.javolution.io.Struct;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import a75f.io.device.json.serializers.JsonSerializer;
import a75f.io.device.mesh.DLog;
import a75f.io.device.modbus.CcuToMbOverUsbReadHoldingRegistersRequest_t;
import a75f.io.device.modbus.LModbus;
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbErrorReportMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.R;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.SerialInputStream;
import a75f.io.usbserial.SerialOutputStream;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbSerialWrapper;
import a75f.io.usbserial.UsbService;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan isOn 8/17/17.
 */

public class SerialMessageFragment extends Fragment {
    List<String> messages = Arrays.asList("Select Message",
            "ReadHoldingRegistersRequest",
            "ReadCoilRequest",
            "ReadDiscreateInputRegister",
            "WriteCoilReqest",
            "WriteRegisterRequest");

    //List<String> channels = Arrays.asList("1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000");
    List<String> channels = Arrays.asList("10", "20", "30", "4", "5", "6", "7", "8", "9");

    @BindView(R.id.msgSpinner)
    Spinner msgSpinner;

    @BindView(R.id.channelSpinner)
    Spinner slaveSpinner;
    
    @BindView(R.id.registerSpinner)
    Spinner registerSpinner;

    @BindView(R.id.msgSend)
    EditText msgSend;

    @BindView(R.id.msgRcvd)
    TextView msgRcvd;

    @BindView(R.id.sendButton)
    Button sendButton;
    
    @BindView(R.id.textVal)
    TextView textVal;
    @BindView(R.id.writeVal)
    EditText writeVal;
    
    @BindView(R.id.addrVal)
    EditText addrVal;
    
    RtuMessageRequest rtuMessageRequest = null;
    private UsbModbusService usbService;
    ModbusRequest modbusRequest = null;
    
    public static SerialMessageFragment newInstance() {
        return new SerialMessageFragment();
    }

    public SerialMessageFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_serial_mesages, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //sendButton.setVisibility(View.GONE);
        initMessageSpinner();
        initChannelSpinner();
        initRegisterSpinner();

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbModbusService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        getActivity().unbindService(usbConnection);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        getActivity().registerReceiver(mUsbReceiver, filter);
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbModbusService.SERVICE_CONNECTED) {
            Intent startService = new Intent(getActivity(), service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            getActivity().startService(startService);
        }
        Intent bindingIntent = new Intent(this.getActivity(), service);
        getActivity().bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private void initMessageSpinner() {
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, messages);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        msgSpinner.setAdapter(dataAdapter);
        msgSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                /*if (position == 0)
                    msgSend.setText(null);
                else
                    fillMessageView(position);*/
                
                if (position > 3) {
                    textVal.setVisibility(View.VISIBLE);
                    writeVal.setVisibility(View.VISIBLE);
                } else {
                    textVal.setVisibility(View.GONE);
                    writeVal.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void initChannelSpinner() {
    
        List<String> slaves = new ArrayList<>();

        for (int slaveCount = 1; slaveCount < 248 ; slaveCount++) {
            slaves.add(String.valueOf(slaveCount));
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_item, slaves);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        slaveSpinner.setAdapter(dataAdapter);
        slaveSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }
    
    private void initRegisterSpinner() {
        
        List<String> registers = new ArrayList<>();
        
        for (int registerCount = 1; registerCount < 126 ; registerCount++) {
            registers.add(String.valueOf(registerCount));
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
                                                                    android.R.layout.simple_spinner_item, registers);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        registerSpinner.setAdapter(dataAdapter);
        registerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
    }
    private void fillMessageView(int position) {
        try {
            Log.d(L.TAG_CCU_MODBUS,"fillMessageView =" + position + "," + messages.get(position));
            switch (position) {
                case 1:
                    Log.d(L.TAG_CCU_MODBUS, "send msg prep11=" + position);
                    
                    modbusRequest = new ReadHoldingRegistersRequest(Integer.parseInt(slaveSpinner.getSelectedItem().toString()),
                                                        Integer.parseInt(addrVal.getText().toString()),
                                                        Integer.parseInt(registerSpinner.getSelectedItem().toString()));
                    rtuMessageRequest = new RtuMessageRequest(modbusRequest);
                    CcuLog.i(L.TAG_CCU_MODBUS,
                             "SerialMessage: modbus readHoldingRegister " + rtuMessageRequest.getMessageData().toString());
                    //ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
                    Log.d(L.TAG_CCU_MODBUS, "send msg prep=" + modbusRequest.getClass() + "," + modbusRequest.toString());
                    break;
                case 2:
                    modbusRequest = new ReadCoilsRequest(Integer.parseInt(slaveSpinner.getSelectedItem().toString()),
                                                         Integer.parseInt(addrVal.getText().toString()), 1);
                    rtuMessageRequest = new RtuMessageRequest(modbusRequest);
                    CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus ReadCoilsRequest " + rtuMessageRequest.getMessageData());
                    break;
                case 3:
                    modbusRequest = new ReadDiscreteInputsRequest(Integer.parseInt(slaveSpinner.getSelectedItem().toString()),
                                                                  Integer.parseInt(addrVal.getText().toString()), 1);
                    rtuMessageRequest = new RtuMessageRequest(modbusRequest);
                    CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus ReadDiscreateInputRequest " + rtuMessageRequest.getMessageData());
                    break;
                case 4:
                    modbusRequest = new WriteCoilRequest(Integer.parseInt(slaveSpinner.getSelectedItem().toString()),
                                                         Integer.parseInt(addrVal.getText().toString()), true);
                    rtuMessageRequest = new RtuMessageRequest(modbusRequest);
                    CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus WriteCoilsRequest " + rtuMessageRequest.getMessageData());
                    break;
                case 5:
                    modbusRequest = new WriteRegisterRequest(Integer.parseInt(slaveSpinner.getSelectedItem().toString()),
                                                 Integer.parseInt(addrVal.getText().toString()),
                                                 Integer.parseInt(writeVal.getText().toString()));
                    rtuMessageRequest = new RtuMessageRequest(modbusRequest);
                    CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus WriteRegisterRequest " + rtuMessageRequest.getMessageData());
                    break;

            }
    
            updateSendMsg(rtuMessageRequest.getMessageData());
        } catch (Exception e) {
            CcuLog.d(L.TAG_CCU_MODBUS, e.getMessage());

        }

    }

    @OnClick(R.id.sendButton)
    public void sendMessage() {
        try {
            
            /*SerialInputStream is = new SerialInputStream();
            SerialOutputStream os = new SerialOutputStream(usbService);
            ModbusMaster master = getRtuMaster(new UsbSerialWrapper(os, is));
            master.setTimeout(10000);
            master.setRetries(0);
            master.init();
            usbService.setSerialInputStream(is);*/
            /*new Thread(() -> {
                if (rtuMessageRequest != null) {
                    try {
                        Log.d(L.TAG_CCU_MODBUS," Request: "+Arrays.toString(rtuMessageRequest.getMessageData()));
                        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse)master.send(modbusRequest);
                        Log.d(L.TAG_CCU_MODBUS," Response: "+Arrays.toString(response.getData()));
                    
                    } catch (ModbusTransportException e) {
                        Log.e(L.TAG_CCU_MODBUS, " ModbusTransportException "+e.getMessage());
                    }
                } else {
                    Log.d(L.TAG_CCU_MODBUS, " Invalid modbusRequest");
                }
            }).start();*/
            
            if (msgSpinner.getSelectedItemPosition() < 1) {
                Toast.makeText(this.getActivity(), "Invalid Message", Toast.LENGTH_SHORT).show();
                return;
            }
            msgRcvd.setText(null);
            fillMessageView(msgSpinner.getSelectedItemPosition());
            
            if (rtuMessageRequest != null) {
                new Thread(() -> {
                    usbService.modbusWrite(rtuMessageRequest.getMessageData());
                }).start();
                Toast.makeText(this.getActivity(), "Message Sent", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this.getActivity(), "Invalid Message", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            CcuLog.e("CCU", "Exception ", e);

        }

    }

    private void updateSendMsg(byte[] data) {
        String msgString = null;
        CcuToMbOverUsbReadHoldingRegistersRequest_t msg = new CcuToMbOverUsbReadHoldingRegistersRequest_t();
        msg.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
        try {
            msgString = JsonSerializer.toJson(msg, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(L.TAG_CCU_MODBUS, "Show Message "+msgString );
        msgSend.setText(msgString);
    }

    public boolean testSlaveNode(int node) {
        try {
            RtuMessageRequest rtuMessageRequest = null;
            if (node % 3 == 0) {
                ModbusRequest request = new WriteCoilRequest(node, 0, true);
                rtuMessageRequest = new RtuMessageRequest(request);
                CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus write coil" + rtuMessageRequest.getMessageData());
            } else if (node % 4 == 0) {
                ModbusRequest request = new ReadDiscreteInputsRequest(node, 0, 4);
                rtuMessageRequest = new RtuMessageRequest(request);
                CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus ReadDiscreateInputs " + rtuMessageRequest.getMessageData());
            } else if(node %2 == 0) {
                ModbusRequest request = new ReadHoldingRegistersRequest(node, 0, 1);
                rtuMessageRequest = new RtuMessageRequest(request);
                CcuLog.i(L.TAG_CCU_MODBUS, "SerialMessage: modbus readHoldingRegister " + rtuMessageRequest.getMessageData());
            }
            
            if (rtuMessageRequest != null) {
                usbService.modbusWrite(rtuMessageRequest.getMessageData());
                updateSendMsg(rtuMessageRequest.getMessageData());
            }
        } catch (ModbusTransportException e) {
            // If there was a transport exception, there's no node there.
            return false;
        }
        return true;
    }

    public List<Integer> scanForSlaveNodes() {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= 240; i++) {
            if (testSlaveNode(i)) {
                result.add(i);
                Log.d(L.TAG_CCU_MODBUS, " Slave Exist "+i);
            } else {
                Log.d(L.TAG_CCU_MODBUS, " Slave Does not Exist "+i);
            }
        }
        return result;
    }

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbModbusService.UsbBinder) arg1).getService();
            usbService.setHandler(null);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSerialEvent(SerialEvent event) {
        Log.d(L.TAG_CCU_MODBUS, "onSerialEvent  : " + event.getSerialAction().name());
        if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT) {
            /*byte[] data = (byte[]) event.getBytes();

            MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
            String msgString = null;
            Struct msg = null;

            Log.d("SERIAL test", "SerialMesgFrag22 =" + messageType.ordinal() + "," + messageType.name());
            switch (messageType) {
                case CM_REGULAR_UPDATE:
                    msg = new CmToCcuOverUsbCmRegularUpdateMessage_t();
                    break;
                case CM_ERROR_REPORT:
                    msg = new CmToCcuOverUsbErrorReportMessage_t();
                    break;
                case CM_TO_CCU_OVER_USB_SN_REBOOT:
                    // TODO - define struct
                    break;
                case CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE:
                    msg = new CmToCcuOverUsbSnRegularUpdateMessage_t();
                    break;
                case CM_TO_CCU_OVER_USB_SMART_STAT_REGULAR_UPDATE:
                    //TODO - define struct
                    break;
            }
            msg.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
            try {
                msgString = JsonSerializer.toJson(msg, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            msgRcvd.setText(null);
            msgRcvd.setText(msgString);*/
        } else if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_MODBUS) {
            byte[] data = (byte[]) event.getBytes();

            MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
            String msgString = null;
            Struct msg = null;
            ByteQueue queue = new ByteQueue(data);
            RtuMessageParser rtuMessageParser = new RtuMessageParser(true);
            try {
                //ModbusResponse response = ModbusResponse.createModbusResponse(queue);
                msgString = rtuMessageParser.parseMessage(queue).toString();
                //msgString = msgString +"\n SlaveId :"+response.getSlaveId()+"\n FunctionCode: "+response.getFunctionCode()+"\n ExceptionMsg:"+response.getExceptionMessage();
                msgRcvd.setText(null);
                msgRcvd.setText(msgString);
                Log.d(L.TAG_CCU_MODBUS,"Modbus Response ==" + msgString);
                //DLog.LogdSerial("Modbus Response ==" + msgString);
            } catch (Exception e) {
                e.printStackTrace();
                msgRcvd.setText(e.getMessage());
            }
        }
    }

    public static ModbusMaster getRtuMaster(SerialPortWrapper serialWrapper) {
        return new ModbusFactory().createRtuMaster(serialWrapper);
    }
}
