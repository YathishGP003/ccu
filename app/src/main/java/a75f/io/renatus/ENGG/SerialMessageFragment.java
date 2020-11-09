package a75f.io.renatus.ENGG;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.x75f.modbus4j.msg.ReadCoilsRequest;
import com.x75f.modbus4j.msg.ReadDiscreteInputsRequest;
import com.x75f.modbus4j.msg.ReadHoldingRegistersRequest;
import com.x75f.modbus4j.msg.WriteCoilRequest;
import com.x75f.modbus4j.msg.WriteRegisterRequest;
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
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbErrorReportMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.renatus.R;
import a75f.io.usbserial.SerialAction;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbService;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by samjithsadasivan isOn 8/17/17.
 */

public class SerialMessageFragment extends Fragment {
    List<String> messages = Arrays.asList("Select Message",
			/*"CcuToCmOverUsbDatabaseSeedSnMessage_t",
			"CcuToCmOverUsbSnSettingsMessage_t",
			"CcuToCmOverUsbSnControlsMessage_t",
			"CcuToCmOverUsbSnLightingScheduleMessage_t",
			"CcuToCmOverUsbDatabaseSeedSmartStatMessage_t",
			"CcuToCmOverUsbSmartStatSettingsMessage_t",
			"CcuToCmOverUsbSmartStatControlsMessage_t",
			"CcuToCmOverUsbCcuHeartbeatMessage_t",
			"CcuToCmOverUsbCmRelayActivationMessage_t",*/
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
    Spinner channelSpinner;

    @BindView(R.id.msgSend)
    EditText msgSend;

    @BindView(R.id.msgRcvd)
    TextView msgRcvd;

    @BindView(R.id.sendButton)
    Button sendButton;

    private int msgSelection;
    private int channelSelection = 0;

    private Class<?> msgClass = null;

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
        sendButton.setVisibility(View.GONE);
        initMessageSpinner();
        initChannelSpinner();

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
        if (!UsbService.SERVICE_CONNECTED) {
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
                Log.d("SERIAL", "onItemSelected=" + position);
                if (position == 0)
                    msgSend.setText(null);
                else
                    fillMessageView(position);
                msgSelection = position > 0 ? position : 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void initChannelSpinner() {

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_item, channels);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(dataAdapter);
        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                channelSelection = position;

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    private void fillMessageView(int position) {
        try {
            Log.d("SERIAL", "fillMessageView =" + position + "," + messages.get(position));
            //msgClass =  Class.forName("a75f.io.device.modbus."+messages.get(position));
            //Log.d("SERIAL","fillMessageView ="+position+","+messages.get(position)+","+msgClass.getName());
            //Struct msg = (Struct) msgClass.newInstance();
            ModbusRequest request;
            RtuMessageRequest rtuMessageRequest;
            switch (position) {
                case 1:
                    Log.d("SERIAL", "send msg prep11=" + position);
					/*CcuToMbOverUsbReadHoldingRegistersRequest_t mbRequestMsg = new CcuToMbOverUsbReadHoldingRegistersRequest_t();
					mbRequestMsg.slaveId.set((short)1);
					mbRequestMsg.functionCode.set((short)3);
					mbRequestMsg.startingAddressHigh.set((short)(0 >> 8));
					mbRequestMsg.startingAddressLow.set((short)(0 & 0x00ff));
					mbRequestMsg.quantityOfCoilsHigh.set((short)(0 >> 8));
					mbRequestMsg.quantityOfCoilsLow.set((short)(14 & 0x00ff));
					mbRequestMsg.errorCheckLow.set((short)(0xc4 >> 8));
					mbRequestMsg.errorCheckHigh.set((short)(0x75 & 0x00ff));*/

                    request = new ReadHoldingRegistersRequest(1, 0, 10);
                    rtuMessageRequest = new RtuMessageRequest(request);
                    usbService.modbusWrite(rtuMessageRequest.getMessageData());
                    updateSendMsg(rtuMessageRequest.getMessageData());
                    CcuLog.i("Modbus",
                             "SerialMessage: modbus readHoldingRegister " + rtuMessageRequest.getMessageData().toString());
                    //ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
                    Log.d("Modbus", "send msg prep=" + msgClass.getName() + "," + request.toString());
                    break;
                case 2:
                    request = new ReadCoilsRequest(2, 0, 1);
                    rtuMessageRequest = new RtuMessageRequest(request);
                    usbService.modbusWrite(rtuMessageRequest.getMessageData());
                    updateSendMsg(rtuMessageRequest.getMessageData());
                    CcuLog.i("EnggUI", "SerialMessage: modbus ReadCoilsRequest " + rtuMessageRequest.getMessageData());
                    break;
                case 3:
                    request = new ReadDiscreteInputsRequest(3, 0, 1);
                    rtuMessageRequest = new RtuMessageRequest(request);
                    usbService.modbusWrite(rtuMessageRequest.getMessageData());
                    updateSendMsg(rtuMessageRequest.getMessageData());
                    CcuLog.i("EnggUI", "SerialMessage: modbus ReadDiscreateInputRequest " + rtuMessageRequest.getMessageData());
                    break;
                case 4:
                    request = new WriteCoilRequest(4, 0, true);
                    rtuMessageRequest = new RtuMessageRequest(request);
                    usbService.modbusWrite(rtuMessageRequest.getMessageData());
                    updateSendMsg(rtuMessageRequest.getMessageData());
                    CcuLog.i("EnggUI", "SerialMessage: modbus WriteCoilsRequest " + rtuMessageRequest.getMessageData());
                    break;
                case 5:
                    request = new WriteRegisterRequest(5, 0, 1);
                    rtuMessageRequest = new RtuMessageRequest(request);
                    usbService.modbusWrite(rtuMessageRequest.getMessageData());
                    updateSendMsg(rtuMessageRequest.getMessageData());
                    CcuLog.i("EnggUI", "SerialMessage: modbus WriteRegisterRequest " + rtuMessageRequest.getMessageData());
                    break;

            }

				/*case 1:
					CcuToCmOverUsbDatabaseSeedSnMessage_t snSeedMsg = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
					snSeedMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
					msgSend.setText(JsonSerializer.toJson(snSeedMsg, true));
					break;
				case 2:
					CcuToCmOverUsbSnSettingsMessage_t snSettingMsg = new CcuToCmOverUsbSnSettingsMessage_t();
					snSettingMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
					msgSend.setText(JsonSerializer.toJson(snSettingMsg, true));
					break;
				case 3:
					CcuToCmOverUsbSnControlsMessage_t snControlMsg = new CcuToCmOverUsbSnControlsMessage_t();
					snControlMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
					msgSend.setText(JsonSerializer.toJson(snControlMsg, true));
					break;
				case 4:
					CcuToCmOverUsbSnLightingScheduleMessage_t snLightingMsg = new CcuToCmOverUsbSnLightingScheduleMessage_t();
					snLightingMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE);
					msgSend.setText(JsonSerializer.toJson(snLightingMsg, true));
					break;
				case 5:
					CcuToCmOverUsbDatabaseSeedSmartStatMessage_t ssSeedMsg = new CcuToCmOverUsbDatabaseSeedSmartStatMessage_t();
					ssSeedMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SMART_STAT);
					msgSend.setText(JsonSerializer.toJson(ssSeedMsg, true));
					break;
				case 6:
					CcuToCmOverUsbSmartStatSettingsMessage_t ssSettingsMsg = new CcuToCmOverUsbSmartStatSettingsMessage_t();
					ssSettingsMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_SETTINGS);
					msgSend.setText(JsonSerializer.toJson(ssSettingsMsg, true));
					break;
				case 7:
					CcuToCmOverUsbSmartStatControlsMessage_t ssControlMsg = new CcuToCmOverUsbSmartStatControlsMessage_t();
					ssControlMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SMART_STAT_CONTROLS);
					msgSend.setText(JsonSerializer.toJson(ssControlMsg, true));
					break;
				case 8:
					CcuToCmOverUsbCcuHeartbeatMessage_t ccuHbMsg = new CcuToCmOverUsbCcuHeartbeatMessage_t();
					ccuHbMsg.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
					msgSend.setText(JsonSerializer.toJson(ccuHbMsg, true));
					break;
				case 9:
					CcuToCmOverUsbCmRelayActivationMessage_t cmRelayMsg = new CcuToCmOverUsbCmRelayActivationMessage_t();
					cmRelayMsg.messageType.set(MessageType.CCU_RELAY_ACTIVATION);
					msgSend.setText(JsonSerializer.toJson(cmRelayMsg, true));
					break;*/
        } catch (Exception e) {
            CcuLog.d("SERIAL", e.getMessage());

        }

    }

    @OnClick(R.id.sendButton)
    public void sendMessage() {
        try {

            //TODO - Complete after verifying deserialization issues.
            //CcuToCmOverUsbDatabaseSeedSnMessage_t msg = (CcuToCmOverUsbDatabaseSeedSnMessage_t) JsonSerializer.fromJson(msgSend.getText().toString(), msgClass);
            //msg.smartNodeAddress.set(Integer.parseInt(channels.get(channelSelection)));
            //Log.d("SERIAL","send msg prep="+msgSend.getText().toString()+","+msgClass.getName()+","+msgSend.getText().toString().getBytes());
            //CcuToMbOverUsbReadHoldingRegistersRequest_t msg = (CcuToMbOverUsbReadHoldingRegistersRequest_t)JsonSerializer.fromJson(msgSend.getText().toString(),msgClass);
            //usbService.write(msg.getOrderedBuffer());
            //usbService.modbusWrite(msg.getOrderedBuffer());
            //CcuLog.i("EnggUI", "SerialMessage: " + JsonSerializer.toJson(msg, true));


            ModbusMaster master = getRtuMaster();
            master.setTimeout(500);
            master.setRetries(1);
            master.init();
            //master.scanForSlaveNodes();
            Log.d("SERIAL", "Modbus send =" + scanForSlaveNodes());

            //ModbusRequest request = new ReadHoldingRegistersRequest(1, 0, 2);
            //RtuMessageRequest rtuRequest = new RtuMessageRequest(request);
            //usbService.modbusWrite(rtuRequest.getMessageData());

            Toast.makeText(this.getActivity(), "Message Sent", Toast.LENGTH_SHORT).show();

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
        msgSend.setText(null);
        msgSend.setText(msgString);
    }

    public boolean testSlaveNode(int node) {
        try {
            if (node % 3 == 0) {
                ModbusRequest request = new WriteCoilRequest(node, 0, true);
                RtuMessageRequest rtuMessageRequest = new RtuMessageRequest(request);
                usbService.modbusWrite(rtuMessageRequest.getMessageData());
                updateSendMsg(rtuMessageRequest.getMessageData());
                CcuLog.i("EnggUI", "SerialMessage: modbus write coil" + rtuMessageRequest.getMessageData());
            } else if (node % 4 == 0) {
                ModbusRequest request = new ReadDiscreteInputsRequest(node, 0, 4);
                RtuMessageRequest rtuMessageRequest = new RtuMessageRequest(request);
                usbService.modbusWrite(rtuMessageRequest.getMessageData());
                updateSendMsg(rtuMessageRequest.getMessageData());
                CcuLog.i("EnggUI", "SerialMessage: modbus ReadDiscreateInputs " + rtuMessageRequest.getMessageData());
            } else/*if(node %2 == 0)*/ {
                ModbusRequest request = new ReadHoldingRegistersRequest(node, 0, 1);
                RtuMessageRequest rtuMessageRequest = new RtuMessageRequest(request);
                usbService.modbusWrite(rtuMessageRequest.getMessageData());
                updateSendMsg(rtuMessageRequest.getMessageData());
                CcuLog.i("EnggUI", "SerialMessage: modbus readHoldingRegister " + rtuMessageRequest.getMessageData());
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
            if (testSlaveNode(i))
                result.add(i);
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
    private UsbModbusService usbService;

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
        Log.d("SERIAL test", "SerialMesgFrag =" + event.getSerialAction().name());
        if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT) {
            byte[] data = (byte[]) event.getBytes();

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
            msgRcvd.setText(msgString);
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
                DLog.LogdSerial("Modbus Response ==" + msgString);
            } catch (Exception e) {
                e.printStackTrace();
                msgRcvd.setText(e.getMessage());
            }
        }
    }

    public static ModbusMaster getRtuMaster() {
        return new ModbusFactory().createRtuMaster();
    }
}
