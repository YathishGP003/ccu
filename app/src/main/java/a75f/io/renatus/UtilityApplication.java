package a75f.io.renatus;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatDelegate;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.apdu.APDU;
import com.renovo.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.renovo.bacnet4j.event.DeviceEventAdapter;
import com.renovo.bacnet4j.event.ReinitializeDeviceHandler;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.npdu.ip.IpNetwork;
import com.renovo.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.Service;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.renovo.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.renovo.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.renovo.bacnet4j.service.confirmed.WritePropertyRequest;
import com.renovo.bacnet4j.service.unconfirmed.IAmRequest;
import com.renovo.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.renovo.bacnet4j.transport.DefaultTransport;
import com.renovo.bacnet4j.transport.Transport;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.ClientCov;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.LogRecord;
import com.renovo.bacnet4j.type.constructed.PropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.constructed.TimeStamp;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.LoggingType;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Date;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.Time;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.bacnet.BACnetScheduler;
import a75f.io.device.bacnet.BACnetUpdateJob;
import a75f.io.device.DeviceUpdateJob;
import a75f.io.device.bacnet.BACnetUtils;
import a75f.io.device.mesh.LSerial;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.pubnub.RemoteCommandHandleInterface;
import a75f.io.logic.watchdog.Watchdog;
import a75f.io.renatus.util.Prefs;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbService;


/**
 * Created by rmatt isOn 7/19/2017.
 */

public abstract class UtilityApplication extends Application
{
    public static LocalDevice localDevice = null;
    public static IpNetwork network;
    public static DhcpInfo dhcpInfo;
    public static WifiManager wifiManager;
    public static Context context = null;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            switch (intent.getAction())
            {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED

                    NotificationHandler.setCMConnectionStatus(true);
                    LSerial.getInstance().setResetSeedMessage(true);
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    // DeviceUpdateJobOld.scheduleJob();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED

                    NotificationHandler.setCMConnectionStatus(false);
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService               usbService;

    private DeviceUpdateJob          deviceUpdateJob;
    private BACnetUpdateJob          baCnetUpdateJob;
    private static Prefs             prefs;
    private final ServiceConnection usbConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1)
        {
            try {
                Log.d("USB Permisssion", "utility Application -" + arg1.isBinderAlive() + "," + arg1.toString() + "," + arg0.getClassName() + "," + arg1.getInterfaceDescriptor());
                if (arg1.isBinderAlive()) {
                    usbService = ((UsbService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setUSBService(usbService);

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbService.setHandler(null);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            usbService = null;
        }
    };


    @Override
    public void onCreate()
    {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Globals.getInstance().setApplicationContext(this);
        AlertManager.getInstance(this).setApplicationContext(this);
        setUsbFilters();  // Start listening notifications from UsbService
        startService(new Intent(this, OTAUpdateHandlerService.class));  // Start OTA update event + timer handler service
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        EventBus.getDefault().register(this);
        //disablePush();
//        new Thread()
//        {
//            @Override
//            public void run()
//            {
//                super.run();
//                openBot();
//            }
//        }.start();

        /*try {
                //Todo Check Ethernet Connected or not
                String checkEthernetConnected = ShellExecuter("cat /sys/class/net/eth0/operstate");
                Log.i("Bacnet", "isEthernet Up:" + checkEthernetConnected);
                if(CheckEthernet()) {
                    String networkConfig = getIPConfig();
                    enableBACnet(networkConfig);
                }else{//Todo Using Wifi IP for Testing
                    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    String networkConfig = getWiFiConfig();
                    enableBACnet(networkConfig);
                    *//*wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    dhcpInfo = wifiManager.getDhcpInfo();
                    String subNetmask = Formatter.formatIpAddress(dhcpInfo.gateway);
                    Log.i("Bacnet","Wifi address :"+dhcpInfo.gateway+" dns:"+dhcpInfo.dns1);
                    IpNetworkBuilder ipNetworkBuilder = new IpNetworkBuilder();
                    ipNetworkBuilder.withSubnet(subNetmask, subNetmask.length());
                    network = ipNetworkBuilder.build();
                    Transport defaultTransport = new DefaultTransport(ipNetworkBuilder.build());
                    String ccuName = L.ccu().getCCUName();
                    HashMap ccuinfo = CCUHsApi.getInstance().read("device and ccu");
                    if (ccuinfo.size() > 0) {
                        ccuName = ccuinfo.get("dis").toString();
                    }
                    localDevice = new LocalDevice(L.ccu().getSmartNodeAddressBand() + 99, ccuName, defaultTransport);
                    Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName);

                    localDevice.writePropertyInternal(PropertyIdentifier.firmwareRevision, new Real(4.12f));


                    HashMap site = CCUHsApi.getInstance().read("site");
                    String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
                    Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName + " Serial:" + site.get("id").toString() + " GUID:" + siteGUID);
                    //tvSerialNumber.setText(siteGUID == null? site.get("id").toString() :siteGUID);
                    localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(site.get("id").toString()));
                    localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new UnsignedInteger(BuildConfig.VERSION_CODE));
                    localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, new UnsignedInteger(15));
                    localDevice.getServicesSupported();

                    localDevice.getEventHandler().addListener(new RenatusApp.Listener());
                    localDevice.initialize();*//*
                }

                    String ccuName = L.ccu().getCCUName();
                    HashMap ccuinfo = CCUHsApi.getInstance().read("device and ccu");
                    if (ccuinfo.size() > 0) {
                        ccuName = ccuinfo.get("dis").toString();
                    }
                    localDevice = new LocalDevice(L.ccu().getSmartNodeAddressBand() + 99, ccuName, defaultTransport);
                    localDevice.writePropertyInternal(PropertyIdentifier.firmwareRevision, new Real(4.12f));
                    HashMap site = CCUHsApi.getInstance().read("site");
                    localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(site.get("id").toString()));
                    localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new UnsignedInteger(BuildConfig.VERSION_CODE));
                    localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, new UnsignedInteger(15));
                    localDevice.getServicesSupported();

                    localDevice.getEventHandler().addListener(new RenatusApp.Listener());
                    localDevice.initialize();
                    Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + localDevice.getDeviceObject().getObjectName()+" IP:"+localDevice.getNetwork().getAllLocalAddresses()[0]+" IP2:"+localDevice.getNetwork().getAllLocalAddresses()[1]+" IP3:"+localDevice.getNetwork().getAllLocalAddresses()[2]);
                    Log.i("Bacnet", "Address"+localDevice.getNetwork());
                    if(localDevice.isInitialized()) {
                        localDevice.sendLocalBroadcast(new WhoIsRequest());
                    }
                }
        } catch (Exception e) {
            Log.i("Bacnet",""+e.toString());
            e.printStackTrace();
        }*/
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        deviceUpdateJob = new DeviceUpdateJob();
        deviceUpdateJob.scheduleJob("DeviceUpdateJob", 60,
                15, TimeUnit.SECONDS);
        Watchdog.getInstance().addMonitor(deviceUpdateJob);
        context = getApplicationContext();
        prefs = new Prefs(context);
    }

    private void setUsbFilters()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
    {
        if (!UsbService.SERVICE_CONNECTED)
        {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty())
            {
                Set<String> keys = extras.keySet();
                for (String key : keys)
                {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            this.startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }





    /*public void openBot()
    {
        final String slackToken = "xoxb-255909669140-FdBDuw3yE0Syn2O7hLXQsjOO";
        mWebApiClient = SlackClientFactory.createWebApiClient(slackToken);
        String webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText();
        mRtmClient = new SlackRealTimeMessagingClient(webSocketUrl);
        mRtmClient.addListener(Event.HELLO, new EventListener()
        {

            @Override
            public void onMessage(JsonNode message)
            {
                Authentication authentication = mWebApiClient.auth();
                mBotId = authentication.getUser_id();
                System.out.println("User id: " + mBotId);
                System.out.println("Team name: " + authentication.getTeam());
                System.out.println("User name: " + authentication.getUser());
            }
        });
        final String selectedFloor = "";
        mRtmClient.addListener(Event.MESSAGE, new EventListener()
        {
            Zone selectedZone = null;


            @Override
            public void onMessage(JsonNode message)
            {
                String channelId = message.findPath("channel").asText();
                String userId = message.findPath("user").asText();
                String text = message.findPath("text").asText().trim();
                text = text.replace("<@U7HSRKP44> ", "");
                if (userId != null && !userId.equals(mBotId))
                {
                    Channel channel;
                    try
                    {
                        channel = mWebApiClient.getChannelInfo(channelId);
                    }
                    catch (SlackResponseErrorException e)
                    {
                        channel = null;
                    }
                    User user = mWebApiClient.getUserInfo(userId);
                    String userName = user.getName();
                    int min = 0;
                    int max = 10;
                    mWebApiClient.meMessage(channelId, "PING:" + text);
                    Log.i("CCU", "CCU state: " + text);
                    if (text.equals("state"))
                    {
                        mWebApiClient.meMessage(channelId,
                                LocalStorage.getApplicationSettingsAsString().length() > 999
                                        ? LocalStorage.getApplicationSettingsAsString()
                                                      .substring(0, 998)
                                        : LocalStorage.getApplicationSettingsAsString());
                    }
                    else if (text.equals("Clean up your grammer"))
                    {
                        mWebApiClient.meMessage(channelId,
                                "OK, I'll be more straight forward with you {state}, {zones}, " +
                                "{[zone selected] light, dark}");
                    }
                    else if (text.equals("zones"))
                    {
                        String zonesMessage = ":::: ";
                        for (Zone z : ccu().getFloors().get(0).mRoomList)
                        {
                            zonesMessage = z.roomName + " ::::";
                        }
                        mWebApiClient.meMessage(channelId, zonesMessage);
                    }
                    else if (text.startsWith("zones"))
                    {
                        String zoneSelected = text.replace("zones ", "");
                        for (Zone z : ccu().getFloors().get(0).mRoomList)
                        {
                            if (z.roomName.equalsIgnoreCase(zoneSelected))
                            {
                                selectedZone = z;
                                mWebApiClient.meMessage(channelId,
                                        selectedZone.roomName + " wise" + " choice");
                            }
                        }
                    }
                    else if (text.equals("light"))
                    {
                        if (selectedZone == null)
                        {
                            mWebApiClient
                                    .meMessage(channelId, " nub " + " must select zone first!");
                        }
                        else
                        {
                            selectedZone.findProfile(ProfileType.LIGHT)
                                        .setOverride(System.currentTimeMillis(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) 100);
                            mWebApiClient.meMessage(channelId,
                                    " TA DA, " + selectedZone.roomName + " is now" + " LIT!" +
                                    "--- [check " + "state for verification]");
                        }
                    }
                    else if (text.equals("dark"))
                    {
                        if (selectedZone == null)
                        {
                            mWebApiClient.meMessage(channelId,
                                    selectedZone.roomName + " nub " + " must select zone first!");
                        }
                        else
                        {
                            selectedZone.findProfile(ProfileType.LIGHT)
                                        .setOverride(System.currentTimeMillis(), OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND, (short) 0);
                            mWebApiClient.meMessage(channelId,
                                    " Hey, who" + " turn out the lights --- [check " +
                                    "state for verification]");
                        }
                    } int randomNum = rand.nextInt((max - min) + 1) + min;
                    if (randomNum == 0)
                    {
                        mWebApiClient.meMessage(channelId, "Ryan's CCU is sad : (");
                    }
                    else if (randomNum == 1)
                    {
                        mWebApiClient.meMessage(channelId, "Ryan's CCU is happy : )");
                    }
                    else if (randomNum == 2)
                    {
                        mWebApiClient.meMessage(channelId, "Ryan's CCU is confused :S");
                    }
                }
            }
        }); mRtmClient.connect();
    }*/


    @Override
    public void onTerminate()
    {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        super.onTerminate();
    }


    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSerialEvent(SerialEvent event)
    {
        LSerial.handleSerialEvent(this, event);
    }

    static class Listener extends DeviceEventAdapter {
        public void listenerException(Throwable e) {
            Log.i("Bacnet", "DiscoveryTest listenerException " + e.getMessage());
        }

        public void iAmReceived(RemoteDevice d) {
            Log.i("Bacnet", "DiscoveryTest Whois Received from "+d.getName()+" Ip:"+d.getAddress());

            // TODO SEND IAM request automatically by bacnet4j
            //localDevice.send(d,new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()),new UnsignedInteger(1476), Segmentation.segmentedBoth,new UnsignedInteger(500)));

            //localDevice.sendGlobalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()),new UnsignedInteger(1476), Segmentation.segmentedBoth,new UnsignedInteger(500)));

            Log.i("Bacnet", "DiscoveryTest "+d.getName()+" Ip:"+d.getAddress());
        }
        public void requestReceived(final Address from, final Service service) {

            Log.i("Bacnet", "DiscoveryTest Service Request Recieved "+from.getMacAddress()+" Choice ID:"+service.getChoiceId()+" Service:"+service.toString()+" Service Data:"+service.getNetworkPriority());
            if(((int) service.getChoiceId()==WhoIsRequest.TYPE_ID)){
                Log.i("Bacnet","WhoIS Service from :"+from.getNetworkNumber());
                localDevice.send(from,new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()), localDevice.get(PropertyIdentifier.maxApduLengthAccepted), Segmentation.noSegmentation,localDevice.get(PropertyIdentifier.vendorIdentifier)));
            }
            if(((int)service.getChoiceId()== WritePropertyRequest.TYPE_ID)){
                WritePropertyRequest writePropertyRequest = (WritePropertyRequest)service;
                if(writePropertyRequest.getPropertyIdentifier().equals(PropertyIdentifier.utcOffset)){
                    Log.i("Bacnet","UTC Value:"+writePropertyRequest.getPropertyValue());
                    TimeZone timeZone = checkTimeZone(writePropertyRequest.getPropertyValue().toString());
                    if(timeZone == null){
                        Log.i("Bacnet","Invalid Time Zone UTC");
                    }else {
                        Log.i("Bacnet","Valid Time Zone:"+timeZone);
                    }
                }/*if(writePropertyRequest.getPropertyIdentifier().equals(PropertyIdentifier.localDate)){
                    Log.i("Bacnet","Date Value:"+writePropertyRequest.getPropertyValue());//Date [year=120, month=JANUARY, day=6, dayOfWeek=MONDAY]
                    setTime(BACnetUtils.convertDateTime((Date)writePropertyRequest.getPropertyValue(),new Time(localDevice)));


                }if(writePropertyRequest.getPropertyIdentifier().equals(PropertyIdentifier.localTime)){
                    Log.i("Bacnet","Time Formatted :"+(Time)(writePropertyRequest.getPropertyValue())+" Date"+ new Date(localDevice));
                    setTime(BACnetUtils.convertDateTime(new Date(localDevice),(Time)(writePropertyRequest.getPropertyValue())));
                    //setTime(BACnetUtils.convertDateTime((DateTime)writePropertyRequest.getPropertyValue()));
                }*/
            }
            if(((int)service.getChoiceId() == ReinitializeDeviceRequest.TYPE_ID)){
                try {
                    Log.i("Bacnet","ReInitialze Device Service:"+service);
                    ReinitializeDeviceRequest reinitializeDeviceRequest = (ReinitializeDeviceRequest)service;
                    ReinitializeDeviceRequest.ReinitializedStateOfDevice reinitializedStateOfDevice =  reinitializeDeviceRequest.getReinitializedStateOfDevice();
                    String devicePassword = reinitializeDeviceRequest.getPasswordRecieved();
                    Log.i("Bacnet","ReInitialze Device Service State:"+reinitializedStateOfDevice+" Password:"+devicePassword);
                    if (localDevice.isInitialized()) {
                        Log.i("Bacnet","ReInitialze Device Service State isIntialized:"+localDevice.isInitialized());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
        public void propertyWritten(final Address from, final BACnetObject obj, final PropertyValue pv) {
            // Override as required
            try {
                Log.i("Bacnet", "Object Type:"+obj.getObjectName()+" "+obj.readProperty(PropertyIdentifier.objectType)+" PID:"+pv.getPropertyIdentifier().toString());
                if(obj.readProperty(PropertyIdentifier.objectType) == ObjectType.schedule){
                    if(pv.getPropertyIdentifier().equals(PropertyIdentifier.weeklySchedule)){
                        BACnetUtils.updateBacnetChanges(from, obj, pv,prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)),true);
                    }
                    if(pv.getPropertyIdentifier().equals(PropertyIdentifier.exceptionSchedule)){
                        Log.i("Bacnet","Schedule Object Value:"+obj.toString()+" Effective Period:"+pv);
                        //BACnetUtils.updateBacnetChanges(from, obj, pv,prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)),false);
                        BACnetScheduler.addNewVacations(obj,pv);
                    }
                }if(obj.readProperty(PropertyIdentifier.objectType) == ObjectType.calendar){
                    if(pv.getPropertyIdentifier().equals(PropertyIdentifier.dateList)){
                        Log.i("Bacnet","Calendar Object Value:"+obj.toString()+" Value:"+pv);
                        BACnetUtils.updateBacnetChanges(from, obj, pv,prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)),false);
                    }
                }
            } catch (BACnetServiceException e) {
                e.printStackTrace();
            }
            BACnetUtils.updateBacnetChanges(from, obj, pv,prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)),false);
        }

        @Override
        public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
            Log.i("Bacnet", "Address "+from.getDescription()+" "+from.getMacAddress()+" DateTime:"+dateTime.toString()+ " UTC:"+utc+" Support Sync:"+localDevice.getServicesSupported().isTimeSynchronization()+" SyncDate:"+BACnetUtils.convertDateTime(dateTime));
            synchronized (dateTime) {
                if(utc){
                    String convertedTime = BACnetUtils.convertUTCtime(dateTime, BACnetUtils.getUtcOffset());
                    setTime(convertedTime);
                }else {
                    setTime(BACnetUtils.convertDateTime(dateTime));
                    dateTime.notify();
                }
            }
            //Todo Check UTC and upate the local device.
            // Override as required
        }

        @Override
        public void covNotificationReceived(final UnsignedInteger subscriberProcessIdentifier,
                                            final ObjectIdentifier initiatingDeviceIdentifier,
                                            final ObjectIdentifier monitoredObjectIdentifier, final UnsignedInteger timeRemaining,
                                            final SequenceOf<PropertyValue> listOfValues) {
            //LOG.debug("Received COV notification");
            /*System.out.println("Bacnet Trend log LoggingType:COV UA"+" localSubs:"+subscriberProcessIdentifier);
            System.out.println("Bacnet Trend log LoggingType:COV UA"+" initiatingDeviceIdentifier:"+initiatingDeviceIdentifier);
            System.out.println("Bacnet Trend log LoggingType:COV UA"+" monitoredObjectIdentifier:"+monitoredObjectIdentifier);
            System.out.println("Bacnet Trend log LoggingType:COV UA"+" Received COV notification");
*/
        }


        @Override
        public void eventNotificationReceived(UnsignedInteger processIdentifier, ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass, UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType, Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues) {
            Log.i("Bacnet", "processIdentifier:"+processIdentifier+" initiatingDeviceIdentifier:"+initiatingDeviceIdentifier+" eventObjectIdentifier:"+eventObjectIdentifier+" timeStamp:"+timeStamp+" notificationClass:"+notificationClass+" priority:"+priority+" eventType:"+eventType+" messageText:"+messageText+" notifyType:"+notifyType+" ackRequired:"+ackRequired+" fromState:"+fromState+" toState:"+toState+" eventValues:"+eventValues);
            super.eventNotificationReceived(processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText, notifyType, ackRequired, fromState, toState, eventValues);
        }

        /*@Override
        public void requestSent(AcknowledgementService service) {
            Log.i("Bacnet", "AckSent:"+service);
            Log.i("Bacnet", "AckSent:"+service+" TypeOfAck:"+service.getChoiceId()+" TypeOfAck:"+service.getTypeofAck());
        }*/
    }
    public static String ShellExecuter(String command) {
        Log.i("Bacnet","Shell Command:"+command);
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = output.toString();
        return response;
    }

    public static void rootCommand(String command){
        Process su = null;
        try {
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes(command+"\n");
            Log.i("Bacnet","Root Command:"+command);
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    public static String ReadIp(String command) {

        StringBuffer output = new StringBuffer();
        ArrayList<String> ipLines = new ArrayList<String>();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                ipLines.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = ipLines.get(1);
        return response;
    }

    public static boolean CheckEthernet(){
        boolean isEthernetConnected = false;
        String checkEthernetConnected = ShellExecuter("cat /sys/class/net/eth0/operstate");
        if(checkEthernetConnected.contains("up")) {
            isEthernetConnected = true;
        }else if(checkEthernetConnected.contains("down")) {
            isEthernetConnected = false;
        }
        return isEthernetConnected;
    }

    public static String getIPConfig(){

        //Todo Get Network Configuration of Ethernet and format for usage
        try {
            String networkConfig = ReadIp("ifconfig eth0");
            networkConfig = networkConfig.replaceAll("\\s",""); // Remove Space
            networkConfig = networkConfig.replaceAll("Bcast","");
            networkConfig = networkConfig.replaceAll("Mask","");
            return networkConfig;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getWiFiConfig(){

        //Todo Get Network Configuration of Wifi and format for usage
        /*String networkConfig = "";
        dhcpInfo = wifiManager.getDhcpInfo();
        String ipAddress = Formatter.formatIpAddress(dhcpInfo.ipAddress);
        String subNetmask = Formatter.formatIpAddress(dhcpInfo.netmask);
        String gateWay = Formatter.formatIpAddress(dhcpInfo.gateway);
        networkConfig = "NetConfig:"+ipAddress+":"+subNetmask+":"+gateWay;*/
        try {
            String networkConfig = ReadIp("ifconfig wlan0");
            networkConfig = networkConfig.replaceAll("\\s",""); // Remove Space
            networkConfig = networkConfig.replaceAll("Bcast","");
            networkConfig = networkConfig.replaceAll("Mask","");
            return networkConfig;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LocalDevice enableBACnet(String networkConfig){
        try {
            String[] ethConfig = networkConfig.split(":");
            Log.i("Bacnet", " IP Addr:"+ethConfig[1]+" BroadCast:"+ethConfig[2]+" Subnet:"+ethConfig[3]);
            IpNetworkBuilder ipNetworkBuilder = new IpNetworkBuilder();
            ipNetworkBuilder.withSubnet(ethConfig[3], ethConfig[3].length());
            network = ipNetworkBuilder.build();
            Transport defaultTransport = new DefaultTransport(ipNetworkBuilder.build());

            String ccuName = L.ccu().getCCUName();
            HashMap ccuinfo = CCUHsApi.getInstance().read("device and ccu");
            if (ccuinfo.size() > 0) {
                ccuName = ccuinfo.get("dis").toString();
            }
            localDevice = new LocalDevice(L.ccu().getSmartNodeAddressBand() + 99, ccuName, defaultTransport);
            localDevice.writePropertyInternal(PropertyIdentifier.firmwareRevision, new CharacterString("4.13"));
            HashMap site = CCUHsApi.getInstance().read("site");
            String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
            localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(siteGUID));
            localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new CharacterString(Integer.toString(BuildConfig.VERSION_CODE)));
            //localDevice.writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(15));
            localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.noSegmentation);
            localDevice.writePropertyInternal(PropertyIdentifier.location, new CharacterString("Floor 1 at this building in this site"));
            localDevice.writePropertyInternal(PropertyIdentifier.description, new CharacterString("75F BACnet Device-V1.2-20-02-2020"));
            localDevice.getServicesSupported().setTimeSynchronization(true);
            localDevice.writePropertyInternal(PropertyIdentifier.utcOffset, new SignedInteger(BACnetUtils.getUtcOffset()));
            localDevice.getServicesSupported();
            localDevice.getEventHandler().addListener(new Listener());
            localDevice.withPassword(BACnetUtils.PASSWORD);
            //localDevice.initialize();
            Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + localDevice.getDeviceObject().getObjectName()+" IP:"+localDevice.getNetwork().getAllLocalAddresses()[0]+" IP2:"+localDevice.getNetwork().getAllLocalAddresses()[1]+" IP3:"+localDevice.getNetwork().getAllLocalAddresses()[2]);
            Log.i("Bacnet", "Address"+localDevice.getNetwork());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localDevice;
    }

    public static LocalDevice enableBACnetWifi(){
        try {
            dhcpInfo = wifiManager.getDhcpInfo();
            String subNetmask = Formatter.formatIpAddress(dhcpInfo.gateway);
            String broadCast = Formatter.formatIpAddress(wifiManager.getDhcpInfo().netmask);
            Log.i("Bacnet","Wifi address :"+dhcpInfo.gateway+" dns:"+dhcpInfo.dns1+" Subnet:"+subNetmask);
            IpNetworkBuilder ipNetworkBuilder = new IpNetworkBuilder();
            ipNetworkBuilder.withSubnet(subNetmask, 24);
            network = ipNetworkBuilder.build();
            Transport defaultTransport = new DefaultTransport(ipNetworkBuilder.build());
            String ccuName = L.ccu().getCCUName();
            HashMap ccuinfo = CCUHsApi.getInstance().read("device and ccu");
            if (ccuinfo.size() > 0) {
                ccuName = ccuinfo.get("dis").toString();
            }
            localDevice = new LocalDevice(L.ccu().getSmartNodeAddressBand() + 99, ccuName, defaultTransport);
            defaultTransport.setLocalDevice(localDevice);
            Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName);
            Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber()+" UTC:"+BACnetUtils.getUtcOffset());
            localDevice.writePropertyInternal(PropertyIdentifier.firmwareRevision, new CharacterString("4.13"));
            HashMap site = CCUHsApi.getInstance().read("site");
            String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
            //tvSerialNumber.setText(siteGUID == null? site.get("id").toString() :siteGUID);
            localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(siteGUID));
            localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new CharacterString(Integer.toString(BuildConfig.VERSION_CODE)));
            //localDevice.writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(15));
            localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.noSegmentation);
            localDevice.writePropertyInternal(PropertyIdentifier.location, new CharacterString("Floor 1 at this building in this site"));
            localDevice.writePropertyInternal(PropertyIdentifier.description, new CharacterString("75F BACnet Device-V1.2-20-02-2020"));
            localDevice.getServicesSupported().setTimeSynchronization(true);
            localDevice.writePropertyInternal(PropertyIdentifier.utcOffset, new SignedInteger(BACnetUtils.getUtcOffset()));
            localDevice.getServicesSupported();
            Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName + " Serial:" + site.get("id").toString() + " GUID:" + siteGUID);
            localDevice.getEventHandler().addListener(new Listener());
            localDevice.withPassword(BACnetUtils.PASSWORD);
            //localDevice.initialize();
            //Thread.sleep(500);
            /*if(localDevice.isInitialized()) {
                localDevice.sendLocalBroadcast(new WhoIsRequest());
                localDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()), localDevice.get(PropertyIdentifier.maxApduLengthAccepted), Segmentation.segmentedBoth,localDevice.get(PropertyIdentifier.vendorIdentifier)));
                Log.i("Bacnet", "Device Initiated:" + localDevice.getInstanceNumber() +" WHOis and Iam Sent");
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localDevice;
    }

    public void sendWhoIs(final LocalDevice localDevice){
            if(localDevice!= null) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                        Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber()+" isInitialized:"+localDevice.isInitialized());
                        localDevice.initialize();
                        Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber()+" after 60 Seconds isInitialized:"+localDevice.isInitialized());
                        localDevice.sendLocalBroadcast(new WhoIsRequest());
                        /*localDevice.sendGlobalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()), localDevice.get(PropertyIdentifier.maxApduLengthAccepted),
                                Segmentation.noSegmentation,localDevice.get(PropertyIdentifier.vendorIdentifier)));*/
                        localDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()), localDevice.get(PropertyIdentifier.maxApduLengthAccepted),
                                Segmentation.noSegmentation,localDevice.get(PropertyIdentifier.vendorIdentifier)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                    }
                }, 60000);
            }
    }

    public static LocalDevice enableBACnetManual(String ipAddress, String broadcast, String subNet, boolean isEthernet){
        try {
            //adb shell ip addr add 192.168.1.77 broadcast 192.168.1.255 dev wlan0
            String lanType = "";
            if(isEthernet){
                lanType="eth0";
            }else{
                lanType="wlan0";
            }
            String checkEthernetConnected = ShellExecuter("ip addr add "+ipAddress+" broadcast " +broadcast+" dev "+lanType);
            String subNetmask = broadcast;
            IpNetworkBuilder ipNetworkBuilder = new IpNetworkBuilder();
            ipNetworkBuilder.withLocalBindAddress(ipAddress);
            ipNetworkBuilder.withBroadcast(broadcast,broadcast.length());
            //ipNetworkBuilder.withSubnet(subNetmask, subNetmask.length());
            network = ipNetworkBuilder.build();
            Transport defaultTransport = new DefaultTransport(ipNetworkBuilder.build());
            String ccuName = L.ccu().getCCUName();
            HashMap ccuinfo = CCUHsApi.getInstance().read("device and ccu");
            if (ccuinfo.size() > 0) {
                ccuName = ccuinfo.get("dis").toString();
            }
            localDevice = new LocalDevice(L.ccu().getSmartNodeAddressBand() + 99, ccuName, defaultTransport);
            Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName);

            localDevice.writePropertyInternal(PropertyIdentifier.firmwareRevision, new CharacterString("4.12"));


            HashMap site = CCUHsApi.getInstance().read("site");
            String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
            Log.i("Bacnet", "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName + " Serial:" + site.get("id").toString() + " GUID:" + siteGUID);
            //tvSerialNumber.setText(siteGUID == null? site.get("id").toString() :siteGUID);
            localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(site.get("id").toString()));
            localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new UnsignedInteger(BuildConfig.VERSION_CODE));
            localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, new UnsignedInteger(15));
            localDevice.getServicesSupported();

            localDevice.getEventHandler().addListener(new Listener());
            localDevice.withPassword(BACnetUtils.PASSWORD);
            localDevice.initialize();
            Thread.sleep(100);
            if(localDevice.isInitialized()) {
                localDevice.sendLocalBroadcast(new WhoIsRequest());
                localDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device,localDevice.getInstanceNumber()),new UnsignedInteger(1476), Segmentation.segmentedBoth,new UnsignedInteger(1181)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localDevice;
    }

    public static void setNetwork(String ipAddress, String broadcast, String subNet, boolean isEthernet){
        try {

            //Todo Shell Commands to setup network manually
            //adb shell ip addr add 192.168.0.72 broadcast 192.168.0.255 dev eth0
            //adb shell ifconfig eth0 192.168.0.72 netmask 255.255.255.0 up

            String lanType = "";
            if(isEthernet){
                lanType="eth0";
            }else{
                lanType="wlan0";
            }
            //ShellExecuter("ip addr add "+ipAddress+" broadcast " +broadcast+" dev "+lanType);
            rootCommand("ip addr add "+ipAddress+" broadcast " +broadcast+" dev "+lanType);
            //Thread.sleep(100);
            rootCommand("ifconfig "+lanType+" "+ipAddress+" netmask " +subNet+" up ");
            //Thread.sleep(200);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLocalDevice(LocalDevice localDev){
        localDevice = localDev;
        baCnetUpdateJob = new BACnetUpdateJob(localDevice);
        baCnetUpdateJob.scheduleJob("BACnetUpdateJob", 60,
                15, TimeUnit.SECONDS);
        Watchdog.getInstance().addMonitor(baCnetUpdateJob);
    }

    public LocalDevice getLocalDevice(){
        return baCnetUpdateJob.getBacnetDevice();
    }

    public boolean terminateBACnet(){
        return baCnetUpdateJob.terminateBACnet();
    }

    public static void setTime(String convertedTime){
        rootCommand("date " + convertedTime);
        rootCommand("am broadcast -a android.intent.action.TIME_SET");
        String currentDate = ShellExecuter("date");
        Log.i("Bacnet", "Time:" + localDevice.get(PropertyIdentifier.localTime).toString() +" Date:" + currentDate);
    }

    public static TimeZone checkTimeZone(String timezoneSet){
        TimeZone timeZone = null;
        int inSeconds = (int)TimeUnit.MINUTES.toSeconds(Integer.parseInt(timezoneSet));
        ZoneOffset zoneOffSet = org.threeten.bp.ZoneOffset.ofTotalSeconds(inSeconds);
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            Log.i("Bacnet", "Offset :"+zoneOffSet.toString()+" Timezone:"+displayTimeZone(TimeZone.getTimeZone(id)));
            if(displayTimeZone(TimeZone.getTimeZone(id)).contains(zoneOffSet.toString())){
                timeZone = TimeZone.getTimeZone(id);
                Log.i("Bacnet", "timeZone :"+timeZone.getDisplayName());
            }
        }
        return timeZone;
    }

    private static String displayTimeZone(TimeZone tz) {

        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset()) - TimeUnit.HOURS.toMinutes(hours);
        // avoid -4:-30 issue
        minutes = Math.abs(minutes);
        String result = "";
        if (hours > 0) {
            result = String.format("(GMT+%d:%02d) %s", hours, minutes, tz.getID());
        } else {
            result = String.format("(GMT%d:%02d) %s", hours, minutes, tz.getID());
        }
        return result;

    }

}
