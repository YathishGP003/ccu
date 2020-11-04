package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatDelegate;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.event.DeviceEventAdapter;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.npdu.ip.IpNetwork;
import com.renovo.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.Service;
import com.renovo.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.renovo.bacnet4j.service.confirmed.WritePropertyRequest;
import com.renovo.bacnet4j.service.unconfirmed.IAmRequest;
import com.renovo.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.renovo.bacnet4j.transport.DefaultTransport;
import com.renovo.bacnet4j.transport.Transport;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.TimeStamp;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.RestartReason;
import com.renovo.bacnet4j.type.enumerated.Segmentation;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.SignedInteger;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.threeten.bp.ZoneOffset;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
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
import a75f.io.logic.watchdog.Watchdog;
import a75f.io.modbusbox.EquipsManager;
import a75f.io.renatus.registration.InstallerOptions;
import a75f.io.renatus.util.Prefs;
import a75f.io.usbserial.SerialEvent;
import a75f.io.usbserial.UsbModbusService;
import a75f.io.usbserial.UsbService;


/**
 * Created by rmatt isOn 7/19/2017.
 */

public abstract class UtilityApplication extends Application {
    public static LocalDevice localDevice = null;
    public static IpNetwork network;
    public static DhcpInfo dhcpInfo;
    public static WifiManager wifiManager;
    public static Context context = null;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED

                    NotificationHandler.setCMConnectionStatus(true);
                    LSerial.getInstance().setResetSeedMessage(true);
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
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

    private UsbService usbService;
    private UsbModbusService usbModbusService;

    private DeviceUpdateJob deviceUpdateJob;
    private static BACnetUpdateJob baCnetUpdateJob;
    private static Prefs prefs;
    private static final String LOG_PREFIX = "CCU_UTILITYAPP";
    private BroadcastReceiver mNetworkReceiver;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                Log.d(LOG_PREFIX, "utility Application -" + arg1.isBinderAlive() + "," + arg1.toString() + "," + arg0.getClassName() + "," + arg1.getInterfaceDescriptor());
                if (arg1.isBinderAlive()) {
                    usbService = ((UsbService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setUSBService(usbService);

                    //TODO: research what cts and dsr changes are.  For now no handler will be used, because I'm uncertain if the information is relevant.
                    usbService.setHandler(null);

                    //Todo : modbus USB Serial to tested with real device
                    usbModbusService = ((UsbModbusService.UsbBinder) arg1).getService();
                    LSerial.getInstance().setModbusUSBService(usbModbusService);
                    usbModbusService.setHandler(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
            usbModbusService = null;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Globals.getInstance().setApplicationContext(this);
        AlertManager.getInstance(this).setApplicationContext(this);

        //Modbus EquipmendManager
        EquipsManager.getInstance(this).setApplicationContext(this);

        setUsbFilters();  // Start listening notifications from UsbService
        startService(new Intent(this, OTAUpdateHandlerService.class));  // Start OTA update event + timer handler service
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        EventBus.getDefault().register(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        deviceUpdateJob = new DeviceUpdateJob();
        deviceUpdateJob.scheduleJob("DeviceUpdateJob", 60,
                15, TimeUnit.SECONDS);
        Watchdog.getInstance().addMonitor(deviceUpdateJob);
        context = getApplicationContext();
        prefs = new Prefs(context);

        mNetworkReceiver = new NetworkChangeReceiver();
        context.registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        InitialiseBACnet();
    }

    private void setUsbFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            this.startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onTerminate() {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        super.onTerminate();
    }


    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSerialEvent(SerialEvent event) {
        LSerial.handleSerialEvent(this, event);
    }

    static class Listener extends DeviceEventAdapter {
        public void listenerException(Throwable e) {
            Log.i(LOG_PREFIX, "DiscoveryTest listenerException " + e.getMessage());
        }

        public void iAmReceived(RemoteDevice d) {
            Log.i(LOG_PREFIX, "DiscoveryTest Whois Received from " + d.getName() + " Ip:" + d.getAddress());
        }

        public void requestReceived(final Address from, final Service service) {

            Log.i(LOG_PREFIX, "DiscoveryTest Service Request Recieved " + from.getMacAddress() + " Choice ID:" + service.getChoiceId() + " Service:" + service.toString() + " Service Data:" + service.getNetworkPriority());
            if (((int) service.getChoiceId() == WhoIsRequest.TYPE_ID)) {
                Log.i(LOG_PREFIX, "WhoIS Service from :" + from.getNetworkNumber());
                localDevice.send(from, new IAmRequest(new ObjectIdentifier(ObjectType.device, localDevice.getInstanceNumber()), localDevice.get(PropertyIdentifier.maxApduLengthAccepted), Segmentation.noSegmentation, localDevice.get(PropertyIdentifier.vendorIdentifier)));
            }
            if (((int) service.getChoiceId() == WritePropertyRequest.TYPE_ID)) {
                WritePropertyRequest writePropertyRequest = (WritePropertyRequest) service;
                if (writePropertyRequest.getPropertyIdentifier().equals(PropertyIdentifier.utcOffset)) {
                    Log.i(LOG_PREFIX, "UTC Value:" + writePropertyRequest.getPropertyValue());
                    TimeZone timeZone = checkTimeZone(writePropertyRequest.getPropertyValue().toString());
                    if (timeZone == null) {
                        Log.i(LOG_PREFIX, "Invalid Time Zone UTC");
                    } else {
                        Log.i(LOG_PREFIX, "Valid Time Zone:" + timeZone);
                    }
                }
            }
            if (((int) service.getChoiceId() == ReinitializeDeviceRequest.TYPE_ID)) {
                try {
                    Log.i(LOG_PREFIX, "ReInitialze Device Service:" + service);
                    ReinitializeDeviceRequest reinitializeDeviceRequest = (ReinitializeDeviceRequest) service;
                    ReinitializeDeviceRequest.ReinitializedStateOfDevice reinitializedStateOfDevice = reinitializeDeviceRequest.getReinitializedStateOfDevice();
                    String devicePassword = reinitializeDeviceRequest.getPasswordRecieved();
                    Log.i(LOG_PREFIX, "ReInitialze Device Service State:" + reinitializedStateOfDevice + " Password:" + devicePassword);
                    if (localDevice.isInitialized()) {
                        Log.i(LOG_PREFIX, "ReInitialze Device Service State isIntialized:" + localDevice.isInitialized());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }

        public void propertyWritten(final Address from, final BACnetObject obj, final PropertyValue pv) {
            // Override as required
            try {
                Log.i(LOG_PREFIX, "Object Type:" + obj.getObjectName() + " " + obj.readProperty(PropertyIdentifier.objectType) + " PID:" + pv.getPropertyIdentifier().toString());
                if (obj.readProperty(PropertyIdentifier.objectType) == ObjectType.schedule) {
                    if (pv.getPropertyIdentifier().equals(PropertyIdentifier.weeklySchedule)) {
                        BACnetUtils.updateBacnetChanges(from, obj, pv, prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)), true);
                    }
                    if (pv.getPropertyIdentifier().equals(PropertyIdentifier.exceptionSchedule)) {
                        Log.i(LOG_PREFIX, "Schedule Object Value:" + obj.toString() + " Effective Period:" + pv);
                        BACnetScheduler.addNewVacations(pv);
                    }
                }
                if (obj.readProperty(PropertyIdentifier.objectType) == ObjectType.calendar) {
                    if (pv.getPropertyIdentifier().equals(PropertyIdentifier.dateList)) {
                        Log.i(LOG_PREFIX, "Calendar Object Value:" + obj.toString() + " Value:" + pv);
                        BACnetUtils.updateBacnetChanges(from, obj, pv, prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)), false);
                    }
                }
            } catch (BACnetServiceException e) {
                e.printStackTrace();
            }
            BACnetUtils.updateBacnetChanges(from, obj, pv, prefs.getBoolean(context.getString(R.string.USE_SAME_TEMP_ALL_DAYS)), false);
        }

        @Override
        public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
            Log.i(LOG_PREFIX, "Address " + from.getDescription() + " " + from.getMacAddress() + " DateTime:" + dateTime.toString() + " UTC:" + utc + " Support Sync:" + localDevice.getServicesSupported().isTimeSynchronization() + " SyncDate:" + BACnetUtils.convertDateTime(dateTime));
            synchronized (dateTime) {
                if (utc) {
                    String convertedTime = BACnetUtils.convertUTCtime(dateTime, BACnetUtils.getUtcOffset());
                    setTime(convertedTime);
                } else {
                    setTime(BACnetUtils.convertDateTime(dateTime));
                    dateTime.notify();
                }
            }
        }

        @Override
        public void covNotificationReceived(final UnsignedInteger subscriberProcessIdentifier,
                                            final ObjectIdentifier initiatingDeviceIdentifier,
                                            final ObjectIdentifier monitoredObjectIdentifier, final UnsignedInteger timeRemaining,
                                            final SequenceOf<PropertyValue> listOfValues) {

        }


        @Override
        public void eventNotificationReceived(UnsignedInteger processIdentifier, ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass, UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType, Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues) {
            Log.i(LOG_PREFIX, "processIdentifier:" + processIdentifier + " initiatingDeviceIdentifier:" + initiatingDeviceIdentifier + " eventObjectIdentifier:" + eventObjectIdentifier + " timeStamp:" + timeStamp + " notificationClass:" + notificationClass + " priority:" + priority + " eventType:" + eventType + " messageText:" + messageText + " notifyType:" + notifyType + " ackRequired:" + ackRequired + " fromState:" + fromState + " toState:" + toState + " eventValues:" + eventValues);
            super.eventNotificationReceived(processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText, notifyType, ackRequired, fromState, toState, eventValues);
        }
    }

    public static String ShellExecuter(String command) {
        Log.i(LOG_PREFIX, "Shell Command:" + command);
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = output.toString();
        return response;
    }

    public static void rootCommand(String command) {
        Process su = null;
        try {
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command + "\n");
            Log.i(LOG_PREFIX, "Root Command:" + command);
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
            while ((line = reader.readLine()) != null) {
                ipLines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = ipLines.get(1);
        return response;
    }

    public static boolean CheckEthernet() {
        boolean isEthernetConnected = false;
        String checkEthernetConnected = ShellExecuter("cat /sys/class/net/eth0/operstate");
        if (checkEthernetConnected.contains("up")) {
            isEthernetConnected = true;
        } else if (checkEthernetConnected.contains("down")) {
            isEthernetConnected = false;
        }
        return isEthernetConnected;
    }

    public static String getIPConfig() {
        //Get Network Configuration of Ethernet and format for usage
        try {
            String networkConfig = ReadIp("ifconfig eth0");
            networkConfig = networkConfig.replaceAll("\\s", ""); // Remove Space
            networkConfig = networkConfig.replaceAll("Bcast", "");
            networkConfig = networkConfig.replaceAll("Mask", "");
            return networkConfig;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getWiFiConfig() {
        //Get Network Configuration of Wifi and format for usage
        try {
            String networkConfig = ReadIp("ifconfig wlan0");
            networkConfig = networkConfig.replaceAll("\\s", ""); // Remove Space
            networkConfig = networkConfig.replaceAll("Bcast", "");
            networkConfig = networkConfig.replaceAll("Mask", "");
            return networkConfig;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LocalDevice enableBACnet(String networkConfig) {
        try {
            String[] ethConfig = networkConfig.split(":");

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
            String ccuGUID = CCUHsApi.getInstance().getGUID(CCUHsApi.getInstance().getCcuId().toString());
            localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(ccuGUID));
            localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new CharacterString(Integer.toString(BuildConfig.VERSION_CODE)));

            localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.noSegmentation);
            localDevice.writePropertyInternal(PropertyIdentifier.location, new CharacterString("Floor 1 at this building in this site"));
            localDevice.writePropertyInternal(PropertyIdentifier.description, new CharacterString("75F BACnet Device-V1.2-20-02-2020"));
            localDevice.getServicesSupported().setTimeSynchronization(true);
            localDevice.writePropertyInternal(PropertyIdentifier.utcOffset, new SignedInteger(BACnetUtils.getUtcOffset()));
            localDevice.getServicesSupported();
            localDevice.getEventHandler().addListener(new Listener());
            localDevice.withPassword(BACnetUtils.PASSWORD);

            Log.i(LOG_PREFIX, "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + localDevice.getDeviceObject().getObjectName() + " IP:" + localDevice.getNetwork().getAllLocalAddresses()[0] + " IP2:" + localDevice.getNetwork().getAllLocalAddresses()[1] + " IP3:" + localDevice.getNetwork().getAllLocalAddresses()[2]);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return localDevice;
    }

    public static LocalDevice enableBACnetWifi() {
        try {
            dhcpInfo = wifiManager.getDhcpInfo();
            String subNetmask = Formatter.formatIpAddress(dhcpInfo.gateway);
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
            localDevice.writePropertyInternal(PropertyIdentifier.firmwareRevision, new CharacterString("4.13"));
            HashMap site = CCUHsApi.getInstance().read("site");
            String siteGUID = CCUHsApi.getInstance().getGUID(site.get("id").toString());
            String ccuGUID = CCUHsApi.getInstance().getGUID(CCUHsApi.getInstance().getCcuId().toString());
            localDevice.writePropertyInternal(PropertyIdentifier.serialNumber, new CharacterString(ccuGUID));
            localDevice.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new CharacterString(Integer.toString(BuildConfig.VERSION_CODE)));
            localDevice.writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.noSegmentation);
            localDevice.writePropertyInternal(PropertyIdentifier.location, new CharacterString("Floor 1 at this building in this site"));
            localDevice.writePropertyInternal(PropertyIdentifier.description, new CharacterString("75F BACnet Device-V1.2-20-02-2020"));
            localDevice.getServicesSupported().setTimeSynchronization(true);
            localDevice.writePropertyInternal(PropertyIdentifier.utcOffset, new SignedInteger(BACnetUtils.getUtcOffset()));
            localDevice.getServicesSupported();
            Log.i(LOG_PREFIX, "Device Number:" + localDevice.getInstanceNumber() + " Device Name:" + ccuName + " Serial:" + site.get("id").toString() + " GUID:" + siteGUID);
            localDevice.getEventHandler().addListener(new Listener());
            localDevice.withPassword(BACnetUtils.PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localDevice;
    }

    public void sendWhoIs(final LocalDevice localDevice) {
        if (localDevice != null) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        localDevice.initialize();
                        localDevice.sendLocalBroadcast(new WhoIsRequest());
                        localDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device, localDevice.getInstanceNumber()), localDevice.get(PropertyIdentifier.maxApduLengthAccepted),
                                Segmentation.noSegmentation, localDevice.get(PropertyIdentifier.vendorIdentifier)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }, 1000);
        }
    }

    public static void setNetwork(String ipAddress, String broadcast, String subNet, boolean isEthernet) {
        try {
            //Shell Commands to setup network manually
            String lanType = "";
            if (isEthernet) {
                lanType = "eth0";
            } else {
                lanType = "wlan0";
            }
            rootCommand("ip addr add " + ipAddress + " broadcast " + broadcast + " dev " + lanType);
            rootCommand("ifconfig " + lanType + " " + ipAddress + " netmask " + subNet + " up ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLocalDevice(LocalDevice localDev, boolean bacnetMode) {
        localDevice = localDev;
        baCnetUpdateJob = new BACnetUpdateJob(localDevice);
        setBacnetObject(baCnetUpdateJob);
        baCnetUpdateJob.scheduleJob("BACnetUpdateJob", 60,
                15, TimeUnit.SECONDS);
        Watchdog.getInstance().addMonitor(baCnetUpdateJob);
        prefs.setBoolean("UseBACnet", true);
        prefs.setBoolean("UseBACnetAuto", bacnetMode);
    }

    public LocalDevice getLocalDevice() {
        return baCnetUpdateJob.getBacnetDevice();
    }

    public boolean isBACnetEnabled() {
        return prefs.getBoolean("UseBACnet");
    }

    public boolean isAutoMode() {
        return prefs.getBoolean("UseBACnetAuto");
    }

    public void setBacnetObject(BACnetUpdateJob bacnetObject) {
        this.baCnetUpdateJob = bacnetObject;
    }

    public BACnetUpdateJob getBacnetObject() {
        return baCnetUpdateJob;
    }

    public boolean terminateBACnet() {
        prefs.setBoolean("UseBACnet", false);
        return getBacnetObject().terminateBACnet();
    }

    public static void setTime(String convertedTime) {
        rootCommand("date " + convertedTime);
        rootCommand("am broadcast -a android.intent.action.TIME_SET");
        String currentDate = ShellExecuter("date");
        Log.i(LOG_PREFIX, "Time:" + localDevice.get(PropertyIdentifier.localTime).toString() + " Date:" + currentDate);
    }

    public static TimeZone checkTimeZone(String timezoneSet) {
        TimeZone timeZone = null;
        int inSeconds = (int) TimeUnit.MINUTES.toSeconds(Integer.parseInt(timezoneSet));
        ZoneOffset zoneOffSet = org.threeten.bp.ZoneOffset.ofTotalSeconds(inSeconds);
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            Log.i(LOG_PREFIX, "Offset :" + zoneOffSet.toString() + " Timezone:" + displayTimeZone(TimeZone.getTimeZone(id)));
            if (displayTimeZone(TimeZone.getTimeZone(id)).contains(zoneOffSet.toString())) {
                timeZone = TimeZone.getTimeZone(id);
                Log.i(LOG_PREFIX, "timeZone :" + timeZone.getDisplayName());
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setWifiasDefault() {
        final ConnectivityManager connMgr = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder request = new NetworkRequest.Builder();
        request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        connMgr.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
            @SuppressLint("NewApi")
            @Override
            public void onAvailable(Network network) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.d(LOG_PREFIX, "Current Network:" + ConnectivityManager.getProcessDefaultNetwork());
                    ConnectivityManager.setProcessDefaultNetwork(network);
                } else {
                    Log.d(LOG_PREFIX, "Current Network:" + connMgr.getBoundNetworkForProcess());
                    connMgr.bindProcessToNetwork(network);
                }
                Log.d(LOG_PREFIX, "Network Changed:" + network);
            }
        });
    }

    public boolean checkNetworkConnected() {
        if (!prefs.getBoolean("BACnetLAN")) {
            ConnectivityManager connManager = (ConnectivityManager) Globals.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return (networkInfo != null && networkInfo.isConnected());
        } else {
            return CheckEthernet();
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isBACnetEnabled() && checkNetworkConnected()) {
                if (localDevice == null)
                    InitialiseBACnet();
                if (localDevice != null && localDevice.isInitialized()) {
                    localDevice.sendLocalBroadcast(new IAmRequest(new ObjectIdentifier(ObjectType.device, localDevice.getInstanceNumber()),
                            localDevice.get(PropertyIdentifier.maxApduLengthAccepted),
                            Segmentation.noSegmentation, localDevice.get(PropertyIdentifier.vendorIdentifier)));
                }
            }
        }
    }

    public void InitialiseBACnet() {
        if (isBACnetEnabled() && checkNetworkConnected()) { // Check for BACnet Enabled or Not
            LocalDevice localDevice = null;
            String networkConfig;
            if (isAutoMode()) { // Check for BACnet Enabled in Auto or Manual
                if (CheckEthernet()) {
                    networkConfig = getIPConfig();
                    localDevice = enableBACnet(networkConfig);
                } else {
                    localDevice = enableBACnetWifi();
                }
            } else {
                networkConfig = prefs.getString("BACnetConfig");
                if (prefs.getBoolean("BACnetLAN")) { // Check for BACnet Enabled in Ethernet or Wifi
                    localDevice = enableBACnet(networkConfig);
                } else {
                    localDevice = enableBACnetWifi();
                }
            }
            try {
                localDevice.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setLocalDevice(localDevice, isAutoMode());
        }
    }
}
